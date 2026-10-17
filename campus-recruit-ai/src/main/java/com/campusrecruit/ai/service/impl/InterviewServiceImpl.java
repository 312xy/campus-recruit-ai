package com.campusrecruit.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusrecruit.ai.ai.InterviewPrompts;
import com.campusrecruit.ai.client.DeepSeekClient;
import com.campusrecruit.ai.dto.InterviewAnswerRequest;
import com.campusrecruit.ai.dto.InterviewEvaluation;
import com.campusrecruit.ai.dto.InterviewQuestion;
import com.campusrecruit.ai.dto.InterviewSessionResponse;
import com.campusrecruit.ai.dto.InterviewSummary;
import com.campusrecruit.ai.dto.JobParseResult;
import com.campusrecruit.ai.entity.InterviewAnswer;
import com.campusrecruit.ai.entity.InterviewSession;
import com.campusrecruit.ai.entity.Job;
import com.campusrecruit.ai.mapper.InterviewAnswerMapper;
import com.campusrecruit.ai.mapper.InterviewSessionMapper;
import com.campusrecruit.ai.mapper.JobMapper;
import com.campusrecruit.ai.service.InterviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 模拟面试实现。
 *
 * 流程：
 *   start   → 校验岗位归属 → AI 一次性生成 5 道题 → 入库 sessions(questions_json)
 *   submitAnswer → 校验会话归属 → 取出题面 + 用户作答 → AI 单题评分 → upsert 到 answers 表
 *                → 若已答完所有题 → 再调一次 AI 生成 summary → 更新 session.status=FINISHED
 *
 * 面试可讲点：
 * - 出题和单题评判要分开 prompt（不同任务的指令不同；混在一起 AI 会"取平均"）。
 * - 答题采用"覆盖式"：同一题多次提交会更新原评分（前端的"我想再答一次"按钮）。
 * - 终评只生成一次，避免每次访问会话都重算（保存 summary_json）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewAnswerMapper answerMapper;
    private final JobMapper jobMapper;
    private final DeepSeekClient deepSeekClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public InterviewSessionResponse start(Long userId, Long jobId) {
        Job job = jobMapper.selectOne(new QueryWrapper<Job>()
                .eq("id", jobId).eq("user_id", userId));
        if (job == null) throw new RuntimeException("岗位不存在或不属于你");
        if (!"PARSED".equals(job.getStatus())) throw new RuntimeException("该岗位还没解析完");

        JobParseResult parsed;
        try { parsed = MAPPER.readValue(job.getParsedJson(), JobParseResult.class); }
        catch (Exception e) { throw new RuntimeException("岗位解析数据异常"); }

        // AI 一次性出 5 道题
        String aiJson;
        try {
            aiJson = deepSeekClient.chatJson(
                    InterviewPrompts.generateSystemPrompt(),
                    InterviewPrompts.generateUserPrompt(parsed));
            aiJson = deepSeekClient.extractJson(aiJson);
        } catch (Exception e) {
            throw new RuntimeException("面试题生成失败：" + safe(e.getMessage()));
        }

        // 把 AI 输出转成 Question 列表：兼容两种结构（带 questions 包装 / 直接数组）
        List<InterviewQuestion> questions;
        try {
            var node = MAPPER.readTree(aiJson);
            if (node.has("questions") && node.get("questions").isArray()) {
                questions = MAPPER.readValue(node.get("questions").toString(),
                        MAPPER.getTypeFactory().constructCollectionType(List.class, InterviewQuestion.class));
            } else if (node.isArray()) {
                questions = MAPPER.readValue(aiJson,
                        MAPPER.getTypeFactory().constructCollectionType(List.class, InterviewQuestion.class));
            } else {
                throw new RuntimeException("AI 没按约定的 JSON 结构出题");
            }
        } catch (Exception e) {
            log.error("面试题解析失败: aiJson={}", aiJson);
            throw new RuntimeException("面试题格式异常，请重试");
        }

        // 容错：补全 index
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getIndex() == null) questions.get(i).setIndex(i);
        }

        InterviewSession s = new InterviewSession();
        s.setUserId(userId);
        s.setJobId(jobId);
        s.setJobTitle(parsed.getTitle() == null ? "未识别岗位" : (parsed.getCompany() == null ? parsed.getTitle() : parsed.getTitle() + " · " + parsed.getCompany()));
        s.setStatus("ACTIVE");
        try {
            s.setQuestionsJson(MAPPER.writeValueAsString(java.util.Map.of("questions", questions)));
        } catch (Exception e) { throw new RuntimeException("面试题序列化失败"); }
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(s);

        return InterviewSessionResponse.from(s, new ArrayList<>());
    }

    @Override
    public List<InterviewSessionResponse> listMine(Long userId) {
        List<InterviewSession> rows = sessionMapper.selectList(new QueryWrapper<InterviewSession>()
                .eq("user_id", userId).orderByDesc("created_at"));
        List<InterviewSessionResponse> out = new ArrayList<>();
        for (InterviewSession s : rows) {
            List<InterviewAnswer> ans = answerMapper.selectList(new QueryWrapper<InterviewAnswer>()
                    .eq("session_id", s.getId()).orderByAsc("question_index"));
            out.add(InterviewSessionResponse.from(s, ans));
        }
        return out;
    }

    @Override
    public InterviewSessionResponse getMine(Long userId, Long sessionId) {
        InterviewSession s = mustSession(userId, sessionId);
        List<InterviewAnswer> ans = answerMapper.selectList(new QueryWrapper<InterviewAnswer>()
                .eq("session_id", s.getId()).orderByAsc("question_index"));
        return InterviewSessionResponse.from(s, ans);
    }

    @Override
    @Transactional
    public InterviewSessionResponse submitAnswer(Long userId, Long sessionId, InterviewAnswerRequest req) {
        if (req.getQuestionIndex() == null) throw new RuntimeException("questionIndex 不能为空");
        if (req.getAnswer() == null || req.getAnswer().trim().isEmpty()) throw new RuntimeException("请先填写答案");

        InterviewSession s = mustSession(userId, sessionId);
        if ("FINISHED".equals(s.getStatus())) {
            throw new RuntimeException("这场面试已结束");
        }

        // 取出题面
        String questionText = pickQuestionText(s, req.getQuestionIndex());
        if (questionText == null) throw new RuntimeException("题序号超出范围");

        // AI 单题评判
        InterviewEvaluation ev;
        try {
            String aiJson = deepSeekClient.chatJson(
                    InterviewPrompts.evaluateSystemPrompt(),
                    InterviewPrompts.evaluateUserPrompt(s.getJobTitle(), questionText, req.getAnswer()));
            String clean = deepSeekClient.extractJson(aiJson);
            ev = MAPPER.readValue(clean, InterviewEvaluation.class);
            if (ev.getScore() == null) throw new RuntimeException("AI 没给出分数");
        } catch (Exception e) {
            throw new RuntimeException("评分失败：" + safe(e.getMessage()));
        }

        // upsert 答题记录（覆盖式）
        InterviewAnswer existed = answerMapper.selectOne(new QueryWrapper<InterviewAnswer>()
                .eq("session_id", s.getId()).eq("question_index", req.getQuestionIndex()));
        InterviewAnswer row = existed == null ? new InterviewAnswer() : existed;
        row.setSessionId(s.getId());
        row.setQuestionIndex(req.getQuestionIndex());
        row.setQuestionText(questionText);
        row.setUserAnswer(req.getAnswer());
        row.setScore(ev.getScore());
        row.setFeedback(ev.getFeedback());
        LocalDateTime now = LocalDateTime.now();
        if (existed == null) row.setCreatedAt(now);
        row.setUpdatedAt(now);
        if (existed == null) answerMapper.insert(row); else answerMapper.updateById(row);

        // 答完所有题？生成终评
        InterviewSessionResponse latest = getMine(userId, sessionId);
        if (latest.getQuestions() != null && latest.getAnswers().size() >= latest.getQuestions().size()) {
            // 触发终评（如果还没生成）
            if (latest.getSummary() == null) {
                InterviewSummary summary = generateSummary(s, latest);
                s.setSummaryJson(toJson(summary));
                s.setTotalScore(summary.getOverallScore());
                s.setStatus("FINISHED");
                s.setUpdatedAt(LocalDateTime.now());
                sessionMapper.updateById(s);
                latest = getMine(userId, sessionId);
            }
        }
        return latest;
    }

    @Override
    public void deleteMine(Long userId, Long sessionId) {
        InterviewSession s = mustSession(userId, sessionId);
        answerMapper.delete(new QueryWrapper<InterviewAnswer>().eq("session_id", s.getId()));
        sessionMapper.deleteById(s.getId());
    }

    private InterviewSession mustSession(Long userId, Long sessionId) {
        InterviewSession s = sessionMapper.selectOne(new QueryWrapper<InterviewSession>()
                .eq("id", sessionId).eq("user_id", userId));
        if (s == null) throw new RuntimeException("面试会话不存在或不属于你");
        return s;
    }

    private String pickQuestionText(InterviewSession s, int idx) {
        try {
            var node = MAPPER.readTree(s.getQuestionsJson());
            var arr = node.get("questions");
            if (arr != null && arr.isArray() && idx < arr.size()) {
                return arr.get(idx).get("content").asText();
            }
        } catch (Exception ignore) {}
        return null;
    }

    private InterviewSummary generateSummary(InterviewSession s, InterviewSessionResponse view) {
        List<InterviewPrompts.QASummary> qa = new ArrayList<>();
        for (var a : view.getAnswers()) {
            qa.add(new InterviewPrompts.QASummary(
                    a.getQuestionIndex() == null ? 0 : a.getQuestionIndex(),
                    a.getScore() == null ? 0 : a.getScore(),
                    a.getFeedback() == null ? "" : a.getFeedback()));
        }
        String aiJson;
        try {
            aiJson = deepSeekClient.chatJson(
                    InterviewPrompts.summarySystemPrompt(),
                    InterviewPrompts.summaryUserPrompt(s.getJobTitle(), qa));
            aiJson = deepSeekClient.extractJson(aiJson);
            return MAPPER.readValue(aiJson, InterviewSummary.class);
        } catch (Exception e) {
            // 终评失败时给个"兜底总结"，不抛异常（不浪费前面所有评分）
            log.warn("终评生成失败: {}", e.getMessage());
            InterviewSummary fb = new InterviewSummary();
            int avg = (int) qa.stream().mapToInt(x -> x.score).average().orElse(0);
            fb.setOverallScore(avg);
            fb.setLevel(avg >= 80 ? "优秀" : avg >= 60 ? "良好" : avg >= 40 ? "一般" : "差");
            fb.setSummary("系统暂时无法生成综合点评，请参考每题的详细评分。");
            fb.setStrengths(List.of());
            fb.setWeaknesses(List.of());
            fb.setRecommendation("待定");
            return fb;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String toJson(Object o) {
        try { return MAPPER.writeValueAsString(o); }
        catch (Exception e) { return "{}"; }
    }
}