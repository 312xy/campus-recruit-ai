package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表对应的实体类。
 *
 * 人话讲解：
 * - 数据库里有一张 users 表，这张表每一行就是"一个用户"。
 * - 我们用 @TableName("users") 告诉 MyBatis-Plus：这个类对应数据库里的 users 表。
 *   （表名没用 user 而用 users，因为 user 是 H2/MySQL 的保留关键字，直接当表名会报语法错误。）
 * - 类里的每个字段（username、password...）对应表里的一列。
 * - @TableId 标记主键，IdType.AUTO 表示主键由数据库自增（1、2、3...）。
 * - Lombok 的 @Data 会自动生成 getter/setter/toString，省得我们手写一堆样板代码。
 *
 * 面试必懂点：
 * - 为什么密码不能明文存？明文一旦数据库泄露，所有用户密码就全曝光了。
 *   所以这里 password 字段存的是"加密后的密文"（BCrypt 哈希），后面注册时会处理。
 * - 实体类（Entity/Model）是"贫血模型"还是"充血模型"？我们这里是简单的贫血模型，
 *   只存数据，业务逻辑放在 Service 层。这是国内项目最常见的写法。
 */
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名，唯一 */
    private String username;

    /** 加密后的密码（BCrypt 密文，不是明文） */
    private String password;

    /** 邮箱，可空 */
    private String email;

    /** 角色：USER 普通用户 / ADMIN 管理员，用于后面权限扩展 */
    private String role;

    /** 注册时间，数据库默认填当前时间 */
    private LocalDateTime createdAt;
}
