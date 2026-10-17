package com.campusrecruit.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求的数据载体。
 *
 * 人话讲解：
 * - 登录只需要用户名 + 密码，比注册简单。
 * - 同样用 @NotBlank 做非空校验。
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
