package com.campusrecruit.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusrecruit.ai.entity.User;
import com.campusrecruit.ai.mapper.UserMapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * "当前登录用户"工具类。
 *
 * 人话讲解：
 * - 前面 JwtAuthenticationFilter 验完 token 后，会把"用户名"记到 SecurityContextHolder 里。
 * - 后面很多业务（简历、匹配、面试）都需要知道"这是谁的数据"，
 *   所以统一抽一个地方来取，避免每个 Service 都重复写这一段。
 * - 拿到用户名后，回数据库查一次完整的 User 对象（顺便能拿到 id、role）。
 *
 * 面试必懂点：
 * - 为什么不能相信前端传来的 userId？因为请求参数是用户可以随便改的，
 *   把 ?userId=2 改成 3 就能看别人的简历 —— 这叫"越权访问(IDOR)"，
 *   是 Web 安全里非常高频的漏洞。正确做法是**从 token 里取身份**，
 *   而不是从请求参数里取。我们这里就是这么做的。
 * - 每次都查一次数据库，性能有问题吗？username 上有唯一索引，是毫秒级的主键级查询，
 *   完全可接受；好处是能拿到用户"最新"的信息（比如刚升级了会员，立刻生效）。
 *   如果确实想省这一次查询，可以把 userId 直接塞进 JWT 的 claims 里，
 *   代价是用户信息变更后要等 token 过期才同步。两种做法各有取舍，面试能讲清就好。
 */
@Service
public class CurrentUserService {

    private final UserMapper userMapper;

    public CurrentUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 取当前登录用户，取不到就抛异常（会被 GlobalExceptionHandler 转成 JSON 返回） */
    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 注意：Spring Security 对"未登录"的请求会放一个 AnonymousAuthenticationToken，
        // 它的 isAuthenticated() 居然返回 true，所以必须单独判断，
        // 否则会把匿名请求误当成已登录用户（然后拿到一个叫 anonymousUser 的假用户）。
        if (auth == null || auth instanceof AnonymousAuthenticationToken || auth.getName() == null) {
            throw new RuntimeException("未登录或登录已过期，请先登录");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", auth.getName()));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    /** 只要 id 的场景 */
    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }
}
