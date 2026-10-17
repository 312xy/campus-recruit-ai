package com.campusrecruit.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 解析简历后，得到的"结构化结果"。
 *
 * 人话讲解：
 * - 用户丢进来的简历是一大段自由文本（格式五花八门），
 *   我们这个类就是"把这段文本变成程序能用的字段"之后的样子。
 * - 这些字段名会和发给大模型的 prompt 里的字段说明一一对应
 *   （见 ResumePrompts），大模型按这个结构吐 JSON，Jackson 自动装进这个对象。
 *
 * 面试必懂点：
 * - @JsonIgnoreProperties(ignoreUnknown = true)：大模型有时会"多返回"几个我们没定义的字段，
 *   如果不加这个注解，Jackson 遇到未知字段会直接抛异常，整个解析就失败了。
 *   对 AI 这种"输出不完全可控"的数据源，必须做容错。
 * - 为什么用 List<String> skills 而不是把技能拼成一个字符串？
 *   因为后面"岗位匹配"要用技能做交集比对（简历有的技能 ∩ 岗位要求的技能），
 *   数组结构才能算，字符串只能做模糊搜索。
 * - highlights / issues 是我特意加的"增值字段"：不只是搬运信息，
 *   还让 AI 顺便给出"这份简历的亮点和待改进点"。这一步把"信息提取"升级成了"简历诊断"，
 *   也是这个产品能卖会员的核心差异点之一。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeParseResult {

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 最高学历：本科 / 硕士 / 大专 等 */
    private String degree;

    /** 最高学历对应的学校 */
    private String school;

    /** 专业 */
    private String major;

    /** 毕业年份（如 2027） */
    private String graduationYear;

    /** 技能关键词列表，如 ["Java","Spring Boot","MySQL","Redis"] */
    private List<String> skills;

    /** 教育经历（可能多段） */
    private List<EducationItem> educations;

    /** 实习经历（可能多段，也可能没有） */
    private List<InternshipItem> internships;

    /** 项目经历（可能多段） */
    private List<ProjectItem> projects;

    /** 自我评价 */
    private String selfEvaluation;

    /** AI 提炼的简历亮点 */
    private List<String> highlights;

    /** AI 指出的问题 / 待补充项 */
    private List<String> issues;

    /* ============ 下面是嵌套的明细结构 ============ */

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EducationItem {
        private String school;
        private String major;
        private String degree;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InternshipItem {
        private String company;
        private String position;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectItem {
        private String name;
        private String role;
        /** 用到的技术栈 */
        private List<String> technologies;
        private String description;
    }
}
