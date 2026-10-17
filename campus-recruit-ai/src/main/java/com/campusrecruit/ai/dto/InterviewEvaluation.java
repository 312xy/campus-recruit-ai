package com.campusrecruit.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** AI 给出的单题评分 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewEvaluation {

    /** 0-100 */
    private Integer score;

    /** 短点评 */
    private String feedback;

    /** 亮点 1-2 条 */
    private List<String> highlights;

    /** 改进建议 1-2 条 */
    private List<String> improvements;
}