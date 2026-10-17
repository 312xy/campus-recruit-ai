package com.campusrecruit.ai.ai;

import com.campusrecruit.ai.dto.JobParseResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模拟面试提示词。
 *
 * 设计要点：
 * - 出题：基于岗位的结构化标签，让 AI 一次性出 5 道题，类型覆盖技术基础/项目/算法/沟通/反问。
 *   一次性出完所有题的理由：避免每答一题就重新调一次大模型重新出题（用户视觉上"题一会一会不一样"很糟）。
 *   出完缓存到 questions_json，后续接口不再调大模型。
 * - 单题评判：把"题目 + 用户答案 + 岗位关键技能"喂给 AI，让 AI 给出 score + feedback。
 *   feedback 要短小（面试官点评风格），不要冗长。
 * - 总结：所有题答完才调一次 AI 给出整体评分 + 录用建议，避免每次都算浪费钱。
 *
 * 防幻觉：题目内容必须和岗位标签里的技能/职责直接相关；评语里提到的"知识点"必须能在题面/答案里找到出处。
 *
 * 用 Java 17 文本块（""" ... """）写大段文字 —— 不与字符串里的双引号冲突，可读性远好于 \n 转义拼接。
 */
public final class InterviewPrompts {

    private InterviewPrompts() {}

    /** 系统提示词（出题） */
    public static String generateSystemPrompt() {
        return """
                你是一名经验丰富的技术面试官，会针对候选人应聘的岗位出 5 道结构化面试题。
                题目要覆盖：技术基础、项目深挖、算法/编码、沟通表达、反问候选人的问题（让候选人提问）。
                输出必须是严格的 JSON（不要 ```json 包裹），不要任何解释性文字。
                """;
    }

    /** 用户提示词（出题） */
    public static String generateUserPrompt(JobParseResult job) {
        String jobJson = toJson(job);
        return """
                请针对下面这份【岗位结构化数据】出 5 道结构化面试题。

                【硬性约束】
                1. 题目数量 = 5，按【技术基础 → 项目深挖 → 算法/编码 → 沟通表达 → 反问候选人】顺序排列。
                2. 每题要有 type（technical_baseline / project_dive / algorithm / communication / candidate_question）和 content（题面，1-2 句话，简洁）。
                3. 技术题要引用岗位的必备/加分技能；项目题要能引导候选人讲自己的项目经验；反问题要引导候选人向面试官提问。
                4. 题面不要泄露原岗位标签的字段名。

                【输出 JSON 格式】
                {
                  "questions": [
                    { "index": 0, "type": "technical_baseline", "content": "..." },
                    { "index": 1, "type": "project_dive", "content": "..." },
                    { "index": 2, "type": "algorithm", "content": "..." },
                    { "index": 3, "type": "communication", "content": "..." },
                    { "index": 4, "type": "candidate_question", "content": "..." }
                  ]
                }

                【岗位结构化数据】
                """ + jobJson;
    }

    /** 单题评判：系统提示词 */
    public static String evaluateSystemPrompt() {
        return """
                你是一名严谨的技术面试官，正在对候选人的一道题作答评分。
                评分要客观（不要无脑打高分），feedback 要短小（2-4 句口语化点评）。
                输出必须是严格的 JSON（不要 ```json 包裹），不要任何解释性文字。
                """;
    }

    /** 单题评判：用户提示词 */
    public static String evaluateUserPrompt(String jobTitle, String question, String userAnswer) {
        return """
                请评估候选人对下面这道题的作答表现。

                【岗位】
                """ + safe(jobTitle) + "\n\n【题目】\n" + safe(question)
                + "\n\n【候选人作答】\n" + safe(userAnswer) + """

                【输出 JSON 格式】
                {
                  "score": 0,
                  "feedback": "短点评（2-4 句），比如：'回答切中要点，能讲清楚 Spring Boot 自动配置原理；但缺少对底层 Bean 生命周期的描述，建议补充。'",
                  "highlights": ["亮点：1-2 条"],
                  "improvements": ["改进建议：1-2 条"]
                }

                【硬性约束】
                1. score 0-100，不要打 100/0 极端分；技术题作答完整给 60-80，全方位给出给 80+。
                2. feedback/亮点/改进 里提到的每一个具体知识点、术语，必须能在候选人作答里找到出处，或者能从题目上下文推出。不允许凭空表扬/凭空批评。
                """;
    }

    /** 终评：系统提示词 */
    public static String summarySystemPrompt() {
        return """
                你是资深终面面试官，正在给候选人的整场面试表现写综合评价。
                总结要客观、有条理，2 段之内讲完。
                输出必须是严格的 JSON（不要 ```json 包裹），不要任何解释性文字。
                """;
    }

    /** 终评：用户提示词 */
    public static String summaryUserPrompt(String jobTitle, List<QASummary> qa) {
        String qaText = qa.stream()
                .map(q -> "Q" + (q.index + 1) + "（" + q.score + "分）：" + q.feedback)
                .collect(Collectors.joining("\n"));
        String head = "下面是候选人的整场面试作答（针对岗位：「" + safe(jobTitle) + "」），请给出综合评价。\n\n"
                + "【每题得分与点评】\n" + qaText + "\n\n";
        String tail = """
                【输出 JSON 格式】
                {
                  "overallScore": 0,
                  "level": "差|一般|良好|优秀",
                  "summary": "整体表现 2-3 句综合点评",
                  "strengths": ["候选人整体亮点：2-3 条"],
                  "weaknesses": ["候选人整体短板：2-3 条"],
                  "recommendation": "强烈推荐|推荐|待定|不推荐（要给出 1-2 句理由）"
                }

                【硬性约束】
                1. overallScore 应该是各题分数的合理加权平均（不必严格平均，面试权重可以略向技术基础和项目深挖倾斜）。
                2. summary/strengths/weaknesses/recommendation 里提到的具体表现必须能在上面每题点评里找到出处。
                """;
        return head + tail;
    }

    /** 内部 helper：简单的转义避免 null 引起 NPE */
    private static String safe(String s) { return s == null ? "" : s; }

    private static String toJson(Object o) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o); }
        catch (Exception e) { return "{}"; }
    }

    /** 单题信息（终评时拼装用） */
    public static class QASummary {
        public int index;
        public int score;
        public String feedback;
        public QASummary(int index, int score, String feedback) {
            this.index = index; this.score = score; this.feedback = feedback;
        }
    }
}