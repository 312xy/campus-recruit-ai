package com.campusrecruit.ai.web;

import com.campusrecruit.ai.dto.AuthResponse;
import com.campusrecruit.ai.dto.LoginRequest;
import com.campusrecruit.ai.dto.RegisterRequest;
import com.campusrecruit.ai.dto.UserResponse;
import com.campusrecruit.ai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关的接口（Controller 层 = 接收前端请求、返回结果）。
 *
 * 人话讲解：
 * - @RestController 表示这是个"接口控制器"，返回的数据直接变成 JSON 发给前端。
 * - @RequestMapping("/api/auth") 给下面所有接口加统一前缀：
 *      POST /api/auth/register  → 注册（任何人可调）
 *      POST /api/auth/login      → 登录（任何人可调，返回 token）
 *      GET  /api/auth/me         → 返回当前登录用户名（必须带 token 才能调）
 * - @Valid 配合 DTO 上的 @NotBlank 等注解，自动做参数校验。
 * - /api/auth/me 里用 SecurityContextHolder 取当前登录用户：这正是前面
 *   JwtAuthenticationFilter 把身份"记下来"的地方。
 *
 * 面试必懂点：
 * - 注意 register 返回的是 UserResponse（安全 DTO），不是数据库实体 User。
 *   直接把实体返回会把 password 密文也发给前端，属于典型的信息泄露。
 * - /me 不带 token 时必须被拦成 401，这个拦截不是写在 Controller 里，
 *   而是 SecurityConfig 的授权规则 + JwtAuthenticationFilter 共同完成的
 *   —— 这就是"认证(你是谁)与授权(你能干什么)"分层的好处。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 注册：成功后返回该用户的可公开信息（不含密码） */
    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(userService.register(request));
    }

    /** 登录：校验用户名密码，成功后返回 JWT token */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return new AuthResponse(token, "Bearer");
    }

    /**
     * 获取当前登录用户。
     * 因为 SecurityConfig 里 /api/auth/me 要求"已认证"，没带有效 token 的请求
     * 根本进不到这个方法（会在过滤器链里被挡下并返回 401）。
     */
    @GetMapping("/me")
    public java.util.Map<String, String> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return java.util.Map.of("username", auth.getName());
    }
}
