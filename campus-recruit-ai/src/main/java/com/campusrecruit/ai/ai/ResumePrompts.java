package com.campusrecruit.ai.ai;

/**
 * 简历相关的大模型提示词（Prompt）模板，集中放一个类里管理。
 *
 * 人话讲解：
 * - "提示词"就是我们发给大模型的那段话。它决定了 AI 输出的质量，
 *   在 AI 应用里，提示词的重要性不亚于代码本身 —— 所以单独抽一个类放。
 * - 为什么不用拼接字符串散落在业务代码里？
 *   ① 提示词要反复改（调优），集中放方便对比和复用；
 *   ② 同一个提示词可能被多个业务用到；
 *   ③ 将来想支持多模型（换个 prompt 风格），只改这里。
 *
 * 面试必懂点（提示词工程，问得很多）：
 * - 三个关键技巧本次都用上了：
 *   1) 角色设定：让它扮演"资深 HR + 简历顾问"，输出质量比自己瞎问好得多；
 *   2) 输出格式强约束：明确要求"只输出 JSON、字段名固定"，
 *      再配合 API 的 JSON mode（response_format=json_object），让结果能被程序直接解析；
 *   3) 防幻觉："没有的信息填空字符串、绝对不要编造" ——
 *      否则模型很可能给你编一个不存在的大厂实习经历，那产品就废了。
 * - 温度（temperature）为什么调低？抽取类任务要的是"准确、稳定"，
 *   不是"有创意"。我们在 DeepSeekClient 里对 JSON 模式用 0.2。
 * - 为什么要在 prompt 里出现 "JSON" 这个单词？
 *   因为 DeepSeek（以及 OpenAI）开了 json_object 模式后，
 *   强制要求提示词里必须包含 "json" 字样，否则会直接报错。这是个很容易踩的坑。
 */
public final class ResumePrompts {

    private ResumePrompts() {
    }

    /** 简历文本太长容易超出模型上下文，也浪费钱，这里做一次截断保护 */
    public static final int MAX_RESUME_CHARS = 8000;

    /**
     * 系统提示词：定角色、定格式、定规矩。
     * 用 Java 17 的文本块（"""）书写，比一堆 + 拼接可读得多。
     */
    public static String parseSystemPrompt() {
        return """
                你是一位资深的互联网技术招聘 HR，同时擅长为应届生做简历诊断。

                请你从用户提供的简历文本中提取信息，输出一个 JSON 对象。

                【输出规则】
                1. 只输出 JSON 对象本身。不要输出任何解释、寒暄，不要用 Markdown 代码块包裹。
                2. 必须严格使用下面给出的字段名和层级，不要增删字段名：
                {
                  "name": "姓名",
                  "phone": "手机号",
                  "email": "邮箱",
                  "degree": "最高学历，如 本科/硕士/大专",
                  "school": "最高学历的学校名",
                  "major": "专业",
                  "graduationYear": "毕业年份，如 2027",
                  "skills": ["技能关键词"],
                  "educations": [
                    {"school": "学校", "major": "专业", "degree": "学历", "startDate": "开始时间", "endDate": "结束时间", "description": "在校经历/主修课程/绩点等"}
                  ],
                  "internships": [
                    {"company": "公司名", "position": "岗位", "startDate": "开始时间", "endDate": "结束时间", "description": "做了什么、用了什么技术、有什么结果"}
                  ],
                  "projects": [
                    {"name": "项目名", "role": "担任角色", "technologies": ["用到的技术"], "description": "项目做了什么、你的职责、技术难点"}
                  ],
                  "selfEvaluation": "自我评价",
                  "highlights": ["简历亮点"],
                  "issues": ["待改进项"]
                }
                3. 简历中没有提到的信息：字符串字段填空字符串 ""，数组字段填空数组 []。
                   【绝对不要编造】学校、公司、项目、时间、数字。宁可留空也不许猜测。
                4. skills：提取 5~15 个技术或技能关键词，使用业界通用写法（例如用 "Spring Boot" 而不是 "springboot"、"MySQL" 而不是 "mysql"）。
                5. highlights：基于简历已有内容，总结 2~4 条真实亮点（例如项目含金量、技术匹配度、经历连续性）。
                6. issues：给出 2~4 条【具体可执行】的改进建议，
                   要指出缺什么、怎么写更好（例如"项目描述停留在罗列技术栈，缺少量化结果，建议补充 QPS/耗时/用户量"），
                   不要写"继续努力"这类空话。
                7. 所有文本字段使用简体中文。
                """;
    }

    /**
     * 用户提示词：把简历原文塞进去。
     * 注意用【】把原文包起来，让模型清楚"哪部分是待处理数据"，
     * 避免简历里的话被误当成指令（这叫"提示词注入"的初级防护）。
     */
    public static String parseUserPrompt(String resumeText) {
        String text = resumeText == null ? "" : resumeText.trim();
        if (text.length() > MAX_RESUME_CHARS) {
            // 简单截断：保留前半部分（简历最核心的信息一般在前半段）
            text = text.substring(0, MAX_RESUME_CHARS) + "\n...(内容过长已截断)";
        }
        return """
                请解析下面这份简历，并按要求输出 JSON。

                【简历原文开始】
                %s
                【简历原文结束】
                """.formatted(text);
    }
}
