package com.campusrecruit.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusrecruit.ai.ai.JobPrompts;
import com.campusrecruit.ai.client.DeepSeekClient;
import com.campusrecruit.ai.dto.JobParseResult;
import com.campusrecruit.ai.dto.JobResponse;
import com.campusrecruit.ai.entity.Job;
import com.campusrecruit.ai.mapper.JobMapper;
import com.campusrecruit.ai.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 岗位业务实现。
 *
 * ---------- 人话讲解：一份 JD 的处理流程 ----------
 * ① 校验长度（太短没必要调 AI，太长截断保护钱包）；
 * ② 算 JD 原文的内容指纹（SHA-256），查一下我是不是已经录过这份 JD；
 *    - 已录过且成功 → 直接提示"已录入过"，不再花钱调 AI（也不产生重复数据）；
 *    - 之前录失败的 → 把那条失败记录删掉，允许重新提交（失败不该阻止用户重试）；
 * ③ 先入库，状态 PARSING；
 * ④ 调 AI 打标签 → 清洗 JSON → Jackson 反序列化做结构体检；
 * ⑤ 成功改 PARSED / 失败改 FAILED，并把结构化的标签存进 parsed_json。
 *
 * ---------- 面试必懂点 ----------
 * - 【幂等】为什么要在调 AI 之前先去重？
 *   调 AI 是要花钱的（按 token 计费）。用户点到"提交"按钮两次、
 *   或者隔天忘了又粘一遍同一份 JD，如果不去重，就是重复扣费 + 列表里一堆重复项。
 *   在"贵的操作"之前先做廉价的存在性检查，是最常见的省成本手段。
 * - 【为什么要区分 PARSED 和 FAILED 两种情况？】
 *   如果一视同仁地拦截"重复"，那么上次因为网络超时失败了，用户就再也提交不了这份 JD，
 *   只能去改一个字绕过 —— 这是很糟糕的体验。所以：成功过的算真重复（拦），
 *   失败记录不算（删掉旧的重来）。这类"状态驱动的分支判断"是业务代码的日常。
 * - 【hash 为什么用 HexFormat 而不是自己拼？】
 *   Java 17 起标准库有 HexFormat，一行把 byte[] 转成十六进制字符串，
 *   比手写循环拼接更短、更不容易写错。
 * - 越权校验和简历模块完全一致（requireOwnJob）：不存在/不属于我都报"岗位不存在"。
 */
@Service
public class JobServiceImpl implements JobService {

    /** JD 长度下限：太短了没有有效信息，直接拦住，省得白花钱 */
    private static final int MIN_CHARS = 30;
    /** JD 长度上限：有些大厂 JD 会写几千字，超过就截断 */
    private static final int MAX_CHARS = 20000;

    private final JobMapper jobMapper;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobServiceImpl(JobMapper jobMapper, DeepSeekClient deepSeekClient) {
        this.jobMapper = jobMapper;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public JobResponse parseAndSave(Long userId, String rawText) {
        // ① 长度校验
        if (rawText == null || rawText.trim().length() < MIN_CHARS) {
            throw new RuntimeException("JD 内容太短（至少 " + MIN_CHARS + " 个字符），请粘贴完整的岗位描述");
        }
        if (rawText.length() > MAX_CHARS) {
            throw new RuntimeException("JD 内容过长（超过 " + MAX_CHARS + " 字符），请精简后再试");
        }

        String text = rawText.trim();
        String hash = sha256(text);

        // ② 去重：先看这个"内容指纹"是不是已经录过了
        List<Job> sameHash = jobMapper.selectList(
                new LambdaQueryWrapper<Job>()
                        .eq(Job::getUserId, userId)
                        .eq(Job::getRawTextHash, hash));
        for (Job old : sameHash) {
            if ("PARSED".equals(old.getStatus())) {
                // 已经成功录入过 → 直接拦下，不重复花钱调 AI
                throw new RuntimeException("这个岗位 JD 你已经录入过了，可在下方岗位列表里查看");
            }
            // 上次失败了 → 允许重试：把失败记录清掉，避免列表里堆一堆失败垃圾
            jobMapper.deleteById(old.getId());
        }

        // ③ 先落库，状态 PARSING（保证 JD 原文不丢，失败可重试）
        Job job = new Job();
        job.setUserId(userId);
        job.setRawText(text);
        job.setRawTextHash(hash);
        job.setStatus("PARSING");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.insert(job);

        // ④ 调 AI 打标签
        try {
            String raw = deepSeekClient.chatJson(
                    JobPrompts.parseSystemPrompt(),
                    JobPrompts.parseUserPrompt(text));

            String json = DeepSeekClient.extractJson(raw);

            // 反序列化 = 转换 + 结构体检（结构不对就在这里抛异常，不会留脏数据）
            JobParseResult parsed = objectMapper.readValue(json, JobParseResult.class);

            // ⑤ 存结果
            job.setParsedJson(objectMapper.writeValueAsString(parsed));
            job.setStatus("PARSED");
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setUpdatedAt(LocalDateTime.now());
            jobMapper.updateById(job);
            throw new RuntimeException("岗位标签解析失败：" + e.getMessage());
        }

        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        return JobResponse.from(jobMapper.selectById(job.getId()));
    }

    @Override
    public List<JobResponse> listMine(Long userId) {
        List<Job> list = jobMapper.selectList(
                new LambdaQueryWrapper<Job>()
                        .eq(Job::getUserId, userId)
                        .orderByDesc(Job::getCreatedAt));
        return list.stream().map(JobResponse::from).toList();
    }

    @Override
    public JobResponse getMine(Long userId, Long jobId) {
        return JobResponse.from(requireOwnJob(userId, jobId));
    }

    @Override
    public void deleteMine(Long userId, Long jobId) {
        requireOwnJob(userId, jobId);
        jobMapper.deleteById(jobId);
    }

    /**
     * 取"属于我的那个岗位"。不存在或不属于我，都报同一句话。
     * 为什么不区分？如果提示"这不是你的岗位"，就等于告诉攻击者"这个 id 是存在的"，
     * 属于信息泄露（可以拿来枚举数据）。安全上的小细节，面试很爱追问这种。
     */
    private Job requireOwnJob(Long userId, Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null || !job.getUserId().equals(userId)) {
            throw new RuntimeException("岗位不存在");
        }
        return job;
    }

    /** 计算文本的 SHA-256 十六进制摘要，作为"内容指纹"用于去重 */
    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes); // Java 17 原生支持，省去手写十六进制转换
        } catch (Exception e) {
            // SHA-256 是 JDK 必备算法，正常不可能走到这里；真的走到了说明环境异常
            throw new IllegalStateException("计算内容指纹失败：" + e.getMessage());
        }
    }
}
