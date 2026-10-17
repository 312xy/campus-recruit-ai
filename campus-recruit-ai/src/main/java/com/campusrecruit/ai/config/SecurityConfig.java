package com.campusrecruit.ai.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 的总配置。
 *
 * 人话讲解（把它想成"公司门禁规则"）：
 * - 哪些门（接口）谁都能进？→ 只放行"注册、登录"两个接口，外加健康检查 /api/health
 *   和 H2 数据库控制台。注意：只放行 /api/auth/register 和 /api/auth/login，
 *   不能写成 /api/auth/** —— 那样连 /api/auth/me 都变成不用登录就能访问了。
 * - 其它门？→ 必须刷 token（ authenticated() ）才能进，包括 /api/auth/me。
 * - csrf().disable()：因为我们用 token 而不是 cookie 登录，CSRF 攻击针对 cookie，
 *   这里关掉简化开发（前后端分离项目常见做法）。
 * - sessionManagement(STATELESS)：服务器不存登录状态（因为信息都在 token 里），
 *   每次请求都重新验 token。
 * - 把我们的 JwtAuthenticationFilter 插在用户名密码过滤器之前：请求一来，先让它验 token。
 * - exceptionHandling：默认情况下 Spring Security 对未登录请求会"重定向到登录页"，
 *   这对纯 JSON 接口很别扭。这里自定义成直接返回 401 + JSON，前端才好处理。
 *
 * 面试必懂点：
 * - Spring Security 6 不再继承 WebSecurityConfigurerAdapter，而是直接定义一个
 *   SecurityFilterChain 的 Bean（这是新版本的标准写法，面试官很爱问版本差异）。
 * - 认证(Authentication，你是谁) 与 授权(Authorization，你能访问什么) 是两件事：
 *   前者由 JwtAuthenticationFilter 解析 token 完成，后者由这里的 authorizeHttpRequests 完成。
 * - CORS：前端(Vue, 5173端口)和后端(8080端口)端口不同，浏览器出于安全会拦跨域请求，
 *   这里配置允许跨域，开发才不会报 CORS 错误。
 *   （CORS 是浏览器行为，Postman/curl 直接请求不受影响，所以别用 curl 通过就来断定 CORS 配好了。）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // ============ 授权规则（按声明顺序匹配，先命中先生效）============
                //
                // 规则 1：白名单接口 —— 任何人都能访问
                //   注册/登录（没登录的人才需要调它）
                //   健康检查（部署探活用，不该要 token）
                //   H2 控制台（仅开发期用；prod profile 已把它整体关掉）
                //   /error（见下方详细说明，这是必踩的一个坑）
                //
                // 【为什么必须放行 /error —— 一个很隐蔽的坑】
                // Spring Boot 处理"请求了一个不存在的接口"或"未预期异常"时，不会直接把结果
                // 写给客户端，而是会把请求【转发(forward)】到 /error 这个内置的错误处理端点。
                // 而转发属于服务器内部行为：转发后的那个请求里没有浏览器发来的 Authorization 头，
                // 于是 /error 就成了"未登录访问受保护资源"，被 Security 拦下返回 401。
                // 结果：用户请求一个写错的接口，收到的却是"未登录或登录已过期"，前端
                // request.js 一看 401 就把 token 清掉并跳回登录页 —— 明明登录着却被登出了。
                // 放行 /error 后，404 就是 404、500 就是 500，错误信息不再被歪曲。
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health",
                                 "/h2-console/**", "/error").permitAll()
                //
                // 规则 2：所有 /api 数据接口 —— 必须带有效 token
                .requestMatchers("/api/**").authenticated()
                //
                // 规则 3：其余全部放行 —— 这些是【前端页面和静态资源】
                //
                // 为什么要这条？默认 profile 下前端跑在 Vite(5173)，后端只暴露 /api/**，
                // 所以以前只写"规则1 + 规则2"没暴露问题。
                // 但 prod profile 把前端产物打进了同一个 jar（前后端同域），此时浏览器访问
                // / 或 /login 拿的是 index.html —— 若被 Security 拦成 401，演示的人连登录页
                // 都看不到，整个应用直接不可用（这个坑实测踩到过）。
                //
                // 这里刻意不逐个列举 Vue Router 的路径（每加一个页面就得回来改一次，迟早漏），
                // 而是用一条清晰判据：**只有 /api 开头的才是后端数据接口，其余都是前端资源。**
                // 安全性不受影响：真正的数据都由 /api/** 保护，前端文件本身不含敏感信息。
                .anyRequest().permitAll()
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 未登录访问受保护接口：直接回 401 JSON，而不是重定向到登录页
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"error\":\"未登录或登录已过期，请先登录\"}");
            }))
            // H2 控制台用 iframe 展示，需要关闭 X-Frame-Options 才能打开
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 密码加密器：BCrypt。Spring Security 用它来加密和校验密码 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 跨域配置：允许前端端口访问本后端 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
