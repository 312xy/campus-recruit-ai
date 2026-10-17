package com.campusrecruit.ai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求的数据载体（DTO = Data Transfer Object，数据传输对象）。
 *
 * 人话讲解：
 * - 前端调 /api/auth/register 时，会发一段 JSON 过来，比如
 *   {"username":"xiaoming","password":"123456","email":"x@qq.com"}。
 * - Spring 会自动把这段 JSON 塞进这个 RegisterRequest 对象里，我们直接用。
 * - @NotBlank / @Size / @Email 是"参数校验"：如果前端没传或乱传，Spring 会直接报错，
 *   不用我们手写 if 判断，省事又安全。
 *
 * 面试必懂点：
 * - 为什么不用 User 实体直接收前端参数？因为"传给后端的字段"和"数据库里的字段"
 *   经常不一样（比如注册时重复密码、确认码，库里没有）。DTO 就是把两者解耦，
 *   避免把数据库的密码字段直接暴露给前端反序列化。
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3-20之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在6-20之间")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;
}
