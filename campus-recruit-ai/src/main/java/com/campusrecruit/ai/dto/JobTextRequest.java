package com.campusrecruit.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * "粘贴 JD 打标签"的请求体。
 *
 * 人话讲解：
 * - 前端 POST 过来的 JSON：{"rawText":"【岗位职责】1. 参与... 【任职要求】精通 Java..."}
 * - @NotBlank 让 Spring 在进 Controller 前就挡掉空内容，返回 400 + 友好提示。
 *
 * 面试必懂点：
 * - 【为什么不让前端传岗位名和公司名？】
 *   这两个信息本来就在 JD 文本里，让 AI 统一抽取，前端只负责传原文。
 *   这样接口更简单，也避免"用户手填的岗位名和 JD 内容对不上"导致的脏数据。
 *   原则：能从一个可信来源推导出来的数据，不要让调用方重复提供。
 * - 注解校验只管"格式/非空"，"内容太短""重复提交"这类业务规则还得在 Service 里判。
 */
@Data
public class JobTextRequest {

    /** 岗位 JD 原文 */
    @NotBlank(message = "JD 内容不能为空")
    private String rawText;
}
