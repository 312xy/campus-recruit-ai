package com.campusrecruit.ai.dto;

import lombok.Data;

/** 一道面试题（出题 + 答题两阶段都会用到） */
@Data
public class InterviewQuestion {

    /** 0~4 题序号 */
    private Integer index;

    /** technical_baseline / project_dive / algorithm / communication / candidate_question */
    private String type;

    /** 题面 */
    private String content;
}