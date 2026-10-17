package com.campusrecruit.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * "粘贴文本解析简历"的请求体。
 *
 * 人话讲解：
 * - 前端 POST 过来的 JSON 长这样：{"fileName":"我的简历.txt","rawText":"张三 本科 ..." }
 * - @NotBlank 表示这个字段不能为空，否则 Spring 会在进 Controller 之前就拦下来，
 *   返回 400 和友好提示（由 GlobalExceptionHandler 统一处理）。
 *
 * 面试必懂点：
 * - 参数校验放在 DTO 上用注解，而不是在 Service 里写一堆 if：
 *   这是"声明式校验"，代码更干净；而且校验失败连业务方法都不会进，节省资源。
 * - 但注意：注解校验只解决"格式对不对"，业务规则（比如"简历不能重复提交"）
 *   还是得在 Service 层判断。两者互补，不是二选一。
 */
@Data
public class ResumeTextRequest {

    /** 文件名，可空（用户手动粘贴时为空） */
    @Size(max = 255, message = "文件名过长")
    private String fileName;

    /** 简历原文 */
    @NotBlank(message = "简历内容不能为空")
    private String rawText;
}
