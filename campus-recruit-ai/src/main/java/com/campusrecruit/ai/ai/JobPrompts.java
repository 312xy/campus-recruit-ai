package com.campusrecruit.ai.ai;

/**
 * 岗位 JD（职位描述）相关的大模型提示词。
 *
 * 人话讲解：
 * - 用户粘贴进来的 JD 是一大段"招聘广告"式文字，看着热闹但信息散：
 *   岗位名藏在第一行、薪资写在中途、"任职要求"下面混着必备技能和加分项。
 * - 这一步的任务就是让 AI 把这些散的信息整理成固定字段，
 *   这样后面的"简历-岗位匹配评分"才能拿"技能数组"去做交集比对。
 *
 * 面试必懂点（为什么这个模块值得单独讲）：
 * - 【区分"必备"和"加分"是核心设计】：任职要求里"精通 Java"和"熟悉 Go 者优先"，
 *   对候选人的重要性完全不同。如果不区分，匹配分数就会失真。
 *   所以我在 prompt 里明确要求拆成 requiredSkills / preferredSkills 两个数组。
 * - 【warnings 这个字段是"产品思维"】：有些 JD 写着"实习生"却要求 3 年经验，
 *   或者要长期出差。让 AI 主动指出这类"坑"，对应届生非常有价值 ——
 *   这就是"信息提取"升级成"求职助手"的地方，也是会员功能的卖点来源。
 * - 【同样要防幻觉】：JD 里没写薪资就留空，绝不能凭公司名猜一个薪资出来。
 * - 【成本控制】：JD 一般比简历短，但有些大厂 JD 会写几千字（带大段公司介绍），
 *   所以照样做长度截断，避免白烧 token。
 */
public final class JobPrompts {

    private JobPrompts() {
    }

    /** JD 长度上限，超出部分截断（JD 核心信息一般在"岗位职责/任职要求"两段里） */
    public static final int MAX_JD_CHARS = 8000;

    /**
     * 系统提示词：让它扮演"懂校招的技术招聘官"，按固定结构输出 JSON。
     */
    public static String parseSystemPrompt() {
        return """
                你是一位资深的技术招聘官，长期负责校园招聘和实习生招聘，
                非常清楚应届生看 JD 时最关心什么。

                请你阅读用户提供的岗位 JD 文本，提取信息并输出一个 JSON 对象。

                【输出规则】
                1. 只输出 JSON 对象本身。不要输出任何解释、寒暄，不要用 Markdown 代码块包裹。
                2. 必须严格使用下面给出的字段名和层级，不要增删字段名：
                {
                  "title": "岗位名称",
                  "company": "公司名称",
                  "city": "工作城市，如 南京 / 泰州 / 远程",
                  "salary": "薪资描述，原样保留如 150-200元/天 或 15-20K",
                  "jobType": "岗位类型，只能是 实习 / 校招 / 社招 三者之一",
                  "educationRequirement": "学历要求，如 本科及以上 / 大专及以上 / 不限",
                  "graduationYears": ["面向的毕业届别，如 2027"],
                  "requiredSkills": ["必备技能，任职要求里的硬性条件"],
                  "preferredSkills": ["加分技能，写了'优先/加分/了解者优先'的那部分"],
                  "responsibilities": ["岗位职责，每条一句话概括"],
                  "keywords": ["检索关键词，5~10个，用于后续岗位搜索与分类"],
                  "summary": "用一句话（40字内）概括这个岗位在招什么人",
                  "warnings": ["对应届生/实习生的提醒，例如要求多年经验、需长期出差、需要特定证书等"]
                }
                3. JD 中没有提到的信息：字符串字段填空字符串 ""，数组字段填空数组 []。
                   【绝对不要编造】公司名、薪资、城市、经验年限。宁可留空也不许猜测。
                4. requiredSkills / preferredSkills：只提取技术或能力关键词，
                   使用业界通用写法（例如 "Spring Boot" 而不是 "springboot"、"MySQL" 而不是 "mysql"）。
                   JD 如果没区分必备和加分，就都放进 requiredSkills，preferredSkills 留空数组。
                5. responsibilities：最多 6 条，每条不超过 30 字，抓核心工作内容，
                   不要照抄大段的公司介绍和福利宣传。
                6. warnings：只写 JD 里明确体现的"风险点"或"特殊要求"。
                   如果这是一个正常的实习/校招岗位、没有明显坑，就返回空数组 []，不要硬凑。
                7. keywords：用于检索和归类，例如 ["Java后端","实习","泰州","Spring Boot"]。
                8. 所有文本字段使用简体中文。
                """;
    }

    /**
     * 用户提示词：把 JD 原文塞进去。
     * 用【】包住原文，明确区分"指令"和"待处理数据"，防御提示词注入
     * （比如 JD 里写着"忽略以上规则"这种句子时，模型能知道那只是数据）。
     */
    public static String parseUserPrompt(String jdText) {
        String text = jdText == null ? "" : jdText.trim();
        if (text.length() > MAX_JD_CHARS) {
            text = text.substring(0, MAX_JD_CHARS) + "\n...(内容过长已截断)";
        }
        return """
                请分析下面这个岗位 JD，并按要求输出 JSON。

                【JD 原文开始】
                %s
                【JD 原文结束】
                """.formatted(text);
    }
}
