package com.campusrecruit.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功后的返回数据。
 *
 * 人话讲解：
 * - 登录成功后，后端要把"令牌（token）"发给前端，前端以后每次请求都带着它证明身份。
 * - { "token": "eyJ...", "tokenType": "Bearer" } 就是返回给前端的 JSON。
 * - tokenType 固定写 "Bearer"，这是 HTTP 鉴权的行业约定（Bearer Token）。
 *
 * 面试必懂点：
 * - 为什么不用 Session 而用 Token（JWT）？
 *   Session 把登录状态存在服务器内存里，多个服务器时要共享（Redis），扩展性差；
 *   JWT 把用户信息"加密签名"后直接发给前端，服务器不用存，天然适合前后端分离 +
 *   多实例部署。代价是 token 发出去直到过期都有效（无法轻易吊销），这是它和 Session 的核心取舍。
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
}
