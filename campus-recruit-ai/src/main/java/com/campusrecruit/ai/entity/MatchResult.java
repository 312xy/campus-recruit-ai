package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 匹配结果实体：一次"简历 × 岗位"的 AI 评估。
 *
 * 人话讲解：
 * - 一条 MatchResult 记录 = "用户把哪份简历 + 哪个岗位喂给 AI，AI 给出的匹配分和理由"。
 * - 它不存简历原文或岗位原文 —— 那些在 resumes / jobs 表里。
 *   本表只存"评估结果"，就像"试卷的批改结果"，不需要再抄一遍题目。
 *
 * 面试必懂点：
 * - 【score 单独一列 + parsed_json 存详细】是常见取舍：
 *   score 用于"按分数排序""看最高分"这类高频查询（不必每次解析 JSON）；
 *   parsed_json 里放 AI 给的亮点/差距/建议（详情，但查询频率低）。
 * - 【错误独立列】和简历/岗位表保持一致的做法：失败原因走 error_message 单独存，
 *   这样前端拿到的 DTO 里既有 JSON 详细结果，也有专门字段展示错误。
 */
@Data
@TableName("match_results")
public class MatchResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 谁请求的评估 */
    private Long userId;

    /** 被评估的简历 id */
    private Long resumeId;

    /** 被评估的岗位 id */
    private Long jobId;

    /** 综合匹配分 0-100；解析失败时为 null */
    private Integer score;

    /** AI 输出的完整 JSON（含亮点/差距/建议等详细字段） */
    private String parsedJson;

    /** 状态：PARSING 评估中 / PARSED 成功 / FAILED 失败 */
    private String status;

    /** 失败原因（成功时为 null） */
    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}