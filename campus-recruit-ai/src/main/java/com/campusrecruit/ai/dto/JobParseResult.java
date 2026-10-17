package com.campusrecruit.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 给岗位 JD 打好的标签（结构化结果）。
 *
 * 人话讲解：
 * - 这是 JD 那段"招聘广告文字"被整理后的样子：
 *   岗位名、公司、城市、薪资、技能要求等等，都变成了独立字段。
 * - 字段名和 JobPrompts 里发给大模型的说明一一对应，Jackson 自动装进来。
 *
 * 面试必懂点：
 * - 【requiredSkills 与 preferredSkills 分开】的原因：
 *   后面做匹配评分时，权重完全不同 —— 缺少"必备技能"要大幅扣分，
 *   缺少"加分技能"只轻微扣分。数据结构设计要服务于下游算法，
 *   一开始就把两者混在一起，后面算分只能靠猜。
 * - 【warnings 是差异化卖点】：市面上的匹配工具大多只算分，
 *   我们额外告诉应届生"这个岗位写着实习但要 3 年经验，可能白投"。
 *   这类"帮你避坑"的信息才是用户愿意付费的理由。
 * - @JsonIgnoreProperties(ignoreUnknown = true) 依然是必须的容错：
 *   模型偶尔会多返回几个我们没定义的字段，不加这个注解 Jackson 会直接抛异常。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobParseResult {

    /** 岗位名称，如 "Java 后端开发实习生" */
    private String title;

    /** 公司名称 */
    private String company;

    /** 工作城市 */
    private String city;

    /** 薪资描述（原样保留，如 "150-200元/天"） */
    private String salary;

    /** 岗位类型：实习 / 校招 / 社招 */
    private String jobType;

    /** 学历要求 */
    private String educationRequirement;

    /** 面向的毕业届别，如 ["2027"] */
    private List<String> graduationYears;

    /** 必备技能（硬性条件） */
    private List<String> requiredSkills;

    /** 加分技能（"优先"那部分） */
    private List<String> preferredSkills;

    /** 岗位职责要点 */
    private List<String> responsibilities;

    /** 检索关键词 */
    private List<String> keywords;

    /** 一句话概括在招什么人 */
    private String summary;

    /** 给应届生的提醒（潜在坑） */
    private List<String> warnings;
}
