package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟面试的单题答题记录。
 *
 * 字段说明：
 * - question_text 冗余存题面（sessions 表里的 questions_json 是数组；
 *   本表按 question_index 定位到具体题面，避免解析 JSON 也能拿到题文）。
 * - user_answer 用户作答内容。
 * - score 单题分 0-100；feedback 单题点评（AI 给）。
 *
 * 设计上不强制 (session_id, question_index) 唯一，因为允许"覆盖重答"。
 */
@Data
@TableName("interview_answers")
public class InterviewAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Integer questionIndex;
    private String questionText;
    private String userAnswer;
    private Integer score;
    private String feedback;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}