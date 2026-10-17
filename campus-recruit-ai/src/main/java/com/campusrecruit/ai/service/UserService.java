package com.campusrecruit.ai.service;

import com.campusrecruit.ai.dto.LoginRequest;
import com.campusrecruit.ai.dto.RegisterRequest;
import com.campusrecruit.ai.entity.User;

/**
 * 用户业务的接口定义。
 *
 * 人话讲解：
 * - 接口只"说清楚要干什么"（注册、登录），不关心"怎么干"。
 * - 真正的实现放在 UserServiceImpl 里。这样分层清晰，也方便以后换实现/写测试。
 */
public interface UserService {
    /** 注册一个新用户，返回保存后的用户（含自增 id） */
    User register(RegisterRequest request);

    /** 校验用户名密码，成功则返回 JWT token 字符串 */
    String login(LoginRequest request);
}
