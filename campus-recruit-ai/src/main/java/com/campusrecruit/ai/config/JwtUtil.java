package com.campusrecruit.ai.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责"盖章（生成 token）"和"验章（解析 token）"。
 *
 * 人话讲解：
 * - JWT 可以理解成一张"加密的身份证"。登录成功后，服务器用密钥（secret）给它盖章，
 *   生成一段字符串 token 发给前端。前端之后每次请求都在请求头里带这个 token。
 * - 服务器收到后，用同一个密钥"验章"：章对得上 → 知道你是谁；对不上/过期 → 拒绝。
 * - 关键点：token 里本身携带了用户信息（这里我们放了 username），服务器不用存任何
 *   登录状态，所以叫"无状态（stateless）"。
 *
 * 面试必懂点：
 * - JWT 三段式：header（算法）. payload（数据，如用户名、过期时间）. signature（签名）。
 *   签名 = 用密钥对前两段做哈希，别人没有密钥就伪造不了。
 * - secret 必须足够长（HS256 要求 ≥256 位 = 32 字节）。我们写在配置文件里，并用
 *   环境变量 JWT_SECRET 覆盖，生产环境绝不能硬编码在代码里。
 * - token 无法轻易吊销（除非做黑名单/短过期+刷新机制），这是 JWT 和 Session 的核心取舍。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** 由 secret 生成真正用于签名的密钥对象 */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token：把用户名写进 payload，并设置签发时间和过期时间 */
    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    /** 从 token 里取出用户名（subject） */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /** 校验 token 是否有效（签名对 + 没过期） */
    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
