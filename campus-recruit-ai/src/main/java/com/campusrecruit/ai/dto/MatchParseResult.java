package com.campusrecruit.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 匹配评分输出的结构（同时也是入库时反序列化的目标）。
 *
 * 字段含义见 MatchPrompts 的输出格式说明，这里不再重复。
 * 反序列化失败会被 Service 捕获并标 FAILED，前端拿到 errorMessage 而不是崩。
 */
@Data
public class MatchParseResult {

    /** 综合匹配分 0-100 */
    private Integer score;

    /** 评级：差 / 一般 / 良好 / 优秀 */
    private String level;

    /** 双方都有的技能 */
    private List<String> matchingSkills;

    /** 岗位要求但简历没有的技能 */
    private List<String> missingSkills;

    /** 简历有但岗位没提到的技能（候选人的亮点） */
    private List<String> extraSkills;

    /** 亮点：1-3 条 */
    private List<String> highlights;

    /** 差距：1-3 条 */
    private List<String> gaps;

    /** 行动建议：1-3 条 */
    private List<String> suggestions;
}