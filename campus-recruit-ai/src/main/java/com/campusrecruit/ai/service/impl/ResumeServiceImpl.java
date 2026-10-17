package com.campusrecruit.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusrecruit.ai.ai.ResumePrompts;
import com.campusrecruit.ai.client.DeepSeekClient;
import com.campusrecruit.ai.dto.ResumeParseResult;
import com.campusrecruit.ai.dto.ResumeResponse;
import com.campusrecruit.ai.entity.Resume;
import com.campusrecruit.ai.mapper.ResumeMapper;
import com.campusrecruit.ai.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历业务实现 —— 这个类是"AI 能力落到业务上"的地方，面试重点讲这个流程。
 *
 * ---------- 人话讲解：解析一份简历的完整流程 ----------
 * ① 先校验输入（太短/太长都直接拒绝，省得白白花钱调 AI）；
 * ② 把简历原文先存进数据库，状态标成 PARSING（解析中）；
 * ③ 组织提示词，调用 DeepSeek 的 JSON 模式，拿回一段 JSON 文本；
 * ④ 清洗 + 反序列化成 ResumeParseResult 对象，做一次结构校验；
 * ⑤ 把校验通过的 JSON 存回 parsed_json 字段，状态改成 PARSED；
 * ⑥ 任何一步出错，状态改成 FAILED，并把原因告诉用户（原文还在，用户可以重试）。
 *
 * ---------- 面试必懂点 ----------
 * - 【为什么先入库、再调 AI，而不是调完 AI 再一起入库？】
 *   因为调 AI 是"会失败的外部调用"（网络超时、余额不足、模型抽风）。
 *   如果先调 AI 再入库，一旦 AI 失败，用户辛苦粘的简历原文就丢了。
 *   先入库保证"数据不丢"，FAILED 状态下原文还在，用户可以重试 —— 这叫"先保证数据落地"。
 * - 【为什么要做一次 readValue 校验？】
 *   存进库前先用 Jackson 反序列化一遍，如果 AI 返回的 JSON 结构不对，
 *   这一步就会报错，我们就能及时发现并把状态标成 FAILED，
 *   而不是等前端拿到数据渲染时才发现是坏的。
 * - 【越权校验】：getMine / deleteMine 里都判断了 resume.userId 是否等于当前登录用户 id。
 *   如果不判断，用户把 URL 里的 id 从 5 改成 6，就能看到别人的简历。
 *   这是 Web 安全里最常见的越权漏洞（IDOR），一定要主动做。
 */
@Service
public class ResumeServiceImpl implements ResumeService {

    /** 简历文本长度下限：太短了 AI 也提取不出什么，直接拦住省钱 */
    private static final int MIN_CHARS = 20;
    /** 上限：防止用户粘贴一整本书，把 token 额度烧光 */
    private static final int MAX_CHARS = 20000;

    private final ResumeMapper resumeMapper;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeServiceImpl(ResumeMapper resumeMapper, DeepSeekClient deepSeekClient) {
        this.resumeMapper = resumeMapper;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public ResumeResponse parseAndSave(Long userId, String fileName, String rawText) {
        // ① 输入校验
        if (rawText == null || rawText.trim().length() < MIN_CHARS) {
            throw new RuntimeException("简历内容太短（至少 " + MIN_CHARS + " 个字符），请粘贴完整简历内容");
        }
        if (rawText.length() > MAX_CHARS) {
            throw new RuntimeException("简历内容过长（超过 " + MAX_CHARS + " 字符），请精简后再试");
        }

        // ② 先落库，状态 PARSING
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(fileName == null || fileName.isBlank() ? "手动粘贴" : fileName);
        resume.setRawText(rawText);
        resume.setStatus("PARSING");
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        // ③④ 调 AI + 清洗 + 校验
        try {
            String raw = deepSeekClient.chatJson(
                    ResumePrompts.parseSystemPrompt(),
                    ResumePrompts.parseUserPrompt(rawText));

            String json = DeepSeekClient.extractJson(raw);

            // 反序列化一次，既是转换也是"结构体检"：结构不对会在这里抛异常
            ResumeParseResult parsed = objectMapper.readValue(json, ResumeParseResult.class);

            // ⑤ 存回结构化结果
            resume.setParsedJson(objectMapper.writeValueAsString(parsed));
            resume.setStatus("PARSED");
        } catch (Exception e) {
            // ⑥ 失败也要落库，别让用户白等
            resume.setStatus("FAILED");
            resume.setUpdatedAt(LocalDateTime.now());
            resumeMapper.updateById(resume);
            throw new RuntimeException("简历解析失败：" + e.getMessage());
        }

        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        return ResumeResponse.from(resumeMapper.selectById(resume.getId()));
    }

    @Override
    public List<ResumeResponse> listMine(Long userId) {
        List<Resume> list = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .orderByDesc(Resume::getCreatedAt));
        return list.stream().map(ResumeResponse::from).toList();
    }

    @Override
    public ResumeResponse getMine(Long userId, Long resumeId) {
        return ResumeResponse.from(requireOwnResume(userId, resumeId));
    }

    @Override
    public void deleteMine(Long userId, Long resumeId) {
        requireOwnResume(userId, resumeId);
        resumeMapper.deleteById(resumeId);
    }

    /**
     * 取"属于我的那份简历"，不存在或不属于我，一律报"简历不存在"。
     * 注意：这里故意不区分"不存在"和"不是你的" ——
     * 如果报"这不是你的简历"，等于告诉攻击者"这个 id 确实存在"，属于信息泄露。
     */
    private Resume requireOwnResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new RuntimeException("简历不存在");
        }
        return resume;
    }
}
