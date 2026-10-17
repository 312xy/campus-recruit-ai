package com.campusrecruit.ai.dto;

import lombok.Data;

/** 单题答题的请求体 */
@Data
public class InterviewAnswerRequest {

    /** 题序号 0~N */
    private Integer questionIndex;

    /** 用户的作答内容 */
    private String answer;
}