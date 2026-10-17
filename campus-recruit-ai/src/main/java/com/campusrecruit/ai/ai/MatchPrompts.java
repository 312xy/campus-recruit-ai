package com.campusrecruit.ai.ai;

import com.campusrecruit.ai.dto.MatchParseResult;
import com.campusrecruit.ai.dto.ResumeParseResult;

import java.util.Map;

/**
 * 匹配评分的提示词集合（和 ResumePrompts / JobPrompts 一个套路，集中管理方便调优）。
 *
 * 人话讲解：
 * - 我们让 AI 做一件事：拿到"简历的结构化字段"+"岗位的结构化字段"，算出综合匹配分，
 *   并给出差距分析。提示词要把这件事讲清楚，并把输出格式强制成 JSON（用 JSON mode）。
 * - 简历和岗位都已经解析过了（parsed_json 里有结构化字段），所以提示词只需要把这两个 JSON
 *   拼在一起告诉 AI，不再传原始长文本 —— 省 token、降低 AI 出错率。
 *
 * 面试必懂点：
 * - "用解析后的结构化字段做评分"比"把简历+岗位原始文本扔给 AI 让它读"更稳：
 *   1) token 省很多；2) AI 不容易"看到几个相关词就乱打分"；3) 可解释性强，差距分析时
 *      能精确到具体技能（"你会 Spring Boot 但缺 MySQL 索引优化经验"），而不是泛泛而谈。
 * - 防幻觉约束仍然重要：AI 容易在"差距分析"里编造出简历/岗位都没提到的技能，必须明令禁止。
 */
public final class MatchPrompts {

    private MatchPrompts() {}

    /** 用户提示词：把简历和岗位的结构化数据喂给 AI，让它算分+分析 */
    public static String userPrompt(ResumeParseResult resume, String jobJson) {
        String resumeJson = toJsonString(resume);
        return "你是求职匹配评分官。请根据下面这份【简历结构化数据】和【岗位结构化数据】，"
                + "为候选人计算综合匹配分（0-100 整数，越高代表越匹配），"
                + "并给出亮点、差距、行动建议。\n"
                + "\n【硬性约束 - 必读】\n"
                + "1. 严禁编造：亮点/差距/建议里提到的每一项技能、项目、知识点，必须能从简历或岗位数据里直接找到出处。\n"
                + "2. 分数不要虚高也不要全打 50：技能重合度高就给高分，重合度低要直说低。\n"
                + "3. matchingSkills/missingSkills/extraSkills 必须是字符串数组，技能名直接来自双方数据，不要翻译、不要改写。\n"
                + "4. 如果简历关键信息缺失（如学历未识别），差距里要明确指出，而不是回避。\n"
                + "\n【输出格式 - 严格 JSON】\n"
                + "{\n"
                + "  \"score\": 0,\n"
                + "  \"level\": \"差|一般|良好|优秀\",\n"
                + "  \"matchingSkills\": [\"双方都有的技能\"],\n"
                + "  \"missingSkills\": [\"岗位要求但简历没有的技能\"],\n"
                + "  \"extraSkills\": [\"简历有但岗位没提到的技能（亮点）\"],\n"
                + "  \"highlights\": [\"为什么能拿到这个分：1-3 条\"],\n"
                + "  \"gaps\": [\"短板/差距：1-3 条，要具体\"],\n"
                + "  \"suggestions\": [\"接下来 1-2 周建议做的准备：1-3 条\"]\n"
                + "}\n"
                + "\n【简历结构化数据】\n"
                + resumeJson
                + "\n\n【岗位结构化数据】\n"
                + jobJson;
    }

    /** 系统提示词：定义 AI 的角色 + JSON 模式 */
    public static String systemPrompt() {
        return "你是一名严谨的求职匹配评分官，"
                + "擅长客观评估候选人与岗位的契合度。"
                + "你的输出必须是严格的 JSON（不要 ```json 包裹），不要任何解释性文字。";
    }

    /** 把对象尽量简单地序列化成 JSON 字符串（避免双重转义/丢失字段） */
    private static String toJsonString(Object obj) {
        if (obj == null) return "{}";
        try {
            return ObjectMapperInstanceHolder.INSTANCE.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /** 解析后真正给前端用的结构（也是 AI 输出的目标结构） */
    public static MatchParseResult empty() {
        MatchParseResult r = new MatchParseResult();
        r.setScore(0);
        r.setLevel("差");
        r.setMatchingSkills(java.util.Collections.emptyList());
        r.setMissingSkills(java.util.Collections.emptyList());
        r.setExtraSkills(java.util.Collections.emptyList());
        r.setHighlights(java.util.Collections.emptyList());
        r.setGaps(java.util.Collections.emptyList());
        r.setSuggestions(java.util.Collections.emptyList());
        return r;
    }

    /**
     * 全局共享的 Jackson 实例（避免每次 new ObjectMapper()）。
     * 嵌套类是 Holder 模式：只在第一次被访问时初始化，线程安全。
     */
    static final class ObjectMapperInstanceHolder {
        static final com.fasterxml.jackson.databind.ObjectMapper INSTANCE =
                new com.fasterxml.jackson.databind.ObjectMapper();
    }
}