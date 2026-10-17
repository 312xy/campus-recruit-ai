package com.campusrecruit.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** AI 给出的整场面试总结 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewSummary {

    /** 综合分 0-100 */
    private Integer overallScore;

    /** 评级：差/一般/良好/优秀 */
    private String level;

    /** 2-3 句综合点评 */
    private String summary;

    /** 亮点 2-3 条 */
    private List<String> strengths;

    /** 短板 2-3 条 */
    private List<String> weaknesses;

    /** 录用建议：强烈推荐/推荐/待定/不推荐 + 理由 */
    private String recommendation;
}