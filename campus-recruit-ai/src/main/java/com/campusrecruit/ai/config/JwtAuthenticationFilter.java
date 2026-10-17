package com.campusrecruit.ai.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：在每个请求进来时，先检查并验证 token。
 *
 * 人话讲解：
 * - 它像小区门口的保安。每个访客（HTTP 请求）进来，保安先看请求头里有没有
 *   "Authorization: Bearer xxxx" 这张"门禁卡"。
 * - 有卡 → 验一下真伪（jwtUtil.isTokenValid），真卡 → 把"持卡人身份"记到上下文里，
 *   放行让后面的 Controller 知道"当前是谁"。
 * - 没卡/假卡 → 不记身份，交给后面的 Security 规则处理（需要登录的接口会被拦成 401）。
 *
 * 面试必懂点：
 * - 它继承 OncePerRequestFilter，保证每个请求只过一次。
 * - "把身份写进 SecurityContextHolder" 是 Spring Security 的核心机制：之后在
 *   Controller 里用 @AuthenticationPrincipal 或 SecurityContext 就能拿到当前用户名。
 * - 这个过滤器被插在 UsernamePasswordAuthenticationFilter 之前（见 SecurityConfig），
 *   所以我们走的是 token 鉴权，不走表单登录。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    String username = jwtUtil.extractUsername(token);
                    // 生成一个"已认证"的身份对象，放进安全上下文
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                // token 非法/过期：不设置认证即可，后续规则会拦截
            }
        }
        filterChain.doFilter(request, response);
    }
}
