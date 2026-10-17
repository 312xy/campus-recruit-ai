package com.campusrecruit.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusrecruit.ai.client.DeepSeekClient;
import com.campusrecruit.ai.ai.MatchPrompts;
import com.campusrecruit.ai.dto.JobParseResult;
import com.campusrecruit.ai.dto.MatchParseResult;
import com.campusrecruit.ai.dto.MatchRequest;
import com.campusrecruit.ai.dto.MatchResponse;
import com.campusrecruit.ai.dto.ResumeParseResult;
import com.campusrecruit.ai.entity.Job;
import com.campusrecruit.ai.entity.MatchResult;
import com.campusrecruit.ai.entity.Resume;
import com.campusrecruit.ai.mapper.JobMapper;
import com.campusrecruit.ai.mapper.MatchResultMapper;
import com.campusrecruit.ai.mapper.ResumeMapper;
import com.campusrecruit.ai.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 匹配评分实现。
 *
 * 人话讲解（流程）：
 *   1. 校验：选中的简历和岗位都得是"我的"，否则视为越权 → 直接抛业务异常。
 *   2. 入库 PARSING：先把"评估任务"存进 match_results 表，AI 还没返回时前端也能看到一条记录。
 *   3. 调 AI：用 Resume 和 Job 的【结构化字段】（不是原文）拼提示词，让 AI 评分 + 给差距分析。
 *   4. 反序列化体检：AI 输出的 JSON 必须能映射成 MatchParseResult，否则不算成功。
 *   5. 更新 PARSED / FAILED：成功把分数和 JSON 落库，失败记 error_message。
 *
 * 面试必懂点：
 * - 【为什么用结构化字段而不是原文拼提示词？】
 *   简历和岗位都已经被 AI 解析过一次了，再让 AI 从零读两份长文本既费 token 又容易走神。
 *   用解析后的 JSON 当输入相当于"让 AI 在已经结构化的数据上做减法"，又快又准。
 * - 【为什么必须做反序列化体检？】
 *   AI 模型输出不稳定：可能多返回字段、嵌套结构偏差、JSON 里有 markdown 围栏。
 *   把它转成强类型对象时如果报错，说明这次输出不可信 —— 标 FAILED 让用户重试，
 *   而不是把脏数据存进库（面试官会问"你怎么保证 AI 输出可信？"）。
 * - 【同步等十几秒会让 HTTP 请求"卡住"吗？】
 *   会。这是 MVP 的取舍：要么同步（实现简单 / 用户立刻看到结果），
 *   要么异步任务（前端要轮询/订阅，体验更复杂）。我们在 HTTP 端不会超时是因为
 *   DeepSeekClient 已经设了 120 秒读取超时 + 前端 axios 也单独把 timeout 提到 120s。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchResultMapper matchResultMapper;
    private final ResumeMapper resumeMapper;
    private final JobMapper jobMapper;
    private final DeepSeekClient deepSeekClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public MatchResponse evaluate(Long userId, MatchRequest req) {
        // 1) 校验归属 + 解析状态
        Resume resume = resumeMapper.selectOne(new QueryWrapper<Resume>()
                .eq("id", req.getResumeId()).eq("user_id", userId));
        if (resume == null) {
            throw new RuntimeException("简历不存在或不属于你");
        }
        if (!"PARSED".equals(resume.getStatus())) {
            throw new RuntimeException("这份简历还没解析完，请先解析简历");
        }
        Job job = jobMapper.selectOne(new QueryWrapper<Job>()
                .eq("id", req.getJobId()).eq("user_id", userId));
        if (job == null) {
            throw new RuntimeException("岗位不存在或不属于你");
        }
        if (!"PARSED".equals(job.getStatus())) {
            throw new RuntimeException("这个岗位还没解析完，请先解析岗位");
        }

        // 2) 先入库一条 PARSING 记录
        MatchResult mr = new MatchResult();
        mr.setUserId(userId);
        mr.setResumeId(req.getResumeId());
        mr.setJobId(req.getJobId());
        mr.setStatus("PARSING");
        mr.setCreatedAt(LocalDateTime.now());
        mr.setUpdatedAt(LocalDateTime.now());
        matchResultMapper.insert(mr);
        final Long matchId = mr.getId();

        // 3) 调 AI
        try {
            ResumeParseResult resumeParsed = MAPPER.readValue(resume.getParsedJson(), ResumeParseResult.class);

            String system = MatchPrompts.systemPrompt();
            String user = MatchPrompts.userPrompt(resumeParsed, job.getParsedJson());

            String aiJson = deepSeekClient.chatJson(system, user);
            String clean = deepSeekClient.extractJson(aiJson);

            // 4) 反序列化体检
            MatchParseResult parsed = MAPPER.readValue(clean, MatchParseResult.class);
            if (parsed.getScore() == null) {
                throw new RuntimeException("AI 没给出有效分数");
            }

            // 5) 落库
            mr.setScore(parsed.getScore());
            mr.setParsedJson(MAPPER.writeValueAsString(parsed));
            mr.setStatus("PARSED");
            mr.setUpdatedAt(LocalDateTime.now());
            matchResultMapper.updateById(mr);
            return MatchResponse.from(mr);

        } catch (Exception e) {
            log.error("匹配评估失败, matchId={}, userId={}, err={}", matchId, userId, e.toString());
            mr.setStatus("FAILED");
            mr.setErrorMessage(truncate(e.getMessage(), 500));
            mr.setUpdatedAt(LocalDateTime.now());
            try { matchResultMapper.updateById(mr); } catch (Exception ignore) {}
            throw new RuntimeException("匹配评估失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage()));
        }
    }

    @Override
    public List<MatchResponse> listMine(Long userId) {
        List<MatchResult> rows = matchResultMapper.selectList(new QueryWrapper<MatchResult>()
                .eq("user_id", userId).orderByDesc("created_at"));
        return rows.stream().map(MatchResponse::from).toList();
    }

    @Override
    public MatchResponse getMine(Long userId, Long id) {
        MatchResult mr = mustMine(userId, id);
        return MatchResponse.from(mr);
    }

    @Override
    public void deleteMine(Long userId, Long id) {
        MatchResult mr = mustMine(userId, id);
        matchResultMapper.deleteById(mr.getId());
    }

    private MatchResult mustMine(Long userId, Long id) {
        MatchResult mr = matchResultMapper.selectOne(new QueryWrapper<MatchResult>()
                .eq("id", id).eq("user_id", userId));
        if (mr == null) {
            throw new RuntimeException("匹配记录不存在或不属于你");
        }
        return mr;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // --- 提示词拼装辅助 ---
    // MatchPrompts.userPrompt 已经能直接接收 ResumeParseResult 和 Job 实体，
    // 所以这里不再需要额外的 wrapper。方法保留为空，避免外部调用残留依赖。
}