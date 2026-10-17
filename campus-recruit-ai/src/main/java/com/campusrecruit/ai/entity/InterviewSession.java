package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟面试会话实体：一次会话 = 针对某个岗位的若干道题问答。
 *
 * 字段含义：
 * - questions_json 缓存 AI 一次性生成的题目数组（避免每次访问都调大模型）。
 * - status: ACTIVE 进行中 / FINISHED 已答完生成总结。
 * - job_title 冗余存岗位名（即使岗位被删，会话里仍能看到当时面的是什么岗）。
 * - total_score 所有题分数的平均值（0-100）。
 * - summary_json 最终总结（整体点评 + 录用建议），AI 在最后一题答完后生成。
 */
@Data
@TableName("interview_sessions")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long jobId;
    private String jobTitle;
    private String questionsJson;
    private String status;
    private Integer totalScore;
    private String summaryJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}