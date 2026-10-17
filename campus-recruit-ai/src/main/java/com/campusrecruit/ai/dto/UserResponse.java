package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.User;
import lombok.Data;

/**
 * 返回给前端的"用户信息"（安全版本）。
 *
 * 人话讲解：
 * - 注册/查询用户时，如果把数据库实体 User 直接返回，里面的 password 字段
 *   （哪怕是 BCrypt 密文）也会被序列化成 JSON 发给前端 —— 这是不该暴露的信息。
 * - 所以我们专门做一个 UserResponse，只装"可以给前端看"的字段。
 *
 * 面试必懂点：
 * - 为什么不能让实体类（Entity）直接当接口返回值？
 *   ① 安全：会泄露不该给的字段（密码、内部状态、逻辑删除标记等）；
 *   ② 解耦：数据库表结构改字段时，不至于直接把前端接口协议改崩；
 *   ③ 灵活：返回给前端的字段常常是"多张表拼起来的"，和单表实体并不一一对应。
 *   所以规范做法是 Entity（数据库）↔ DTO（对外接口）分开，中间做一次转换。
 */
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;

    /** 把数据库实体转成"安全"的返回对象：只挑能公开的字段 */
    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}
