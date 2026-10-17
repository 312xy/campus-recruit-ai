package com.campusrecruit.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusrecruit.ai.dto.LoginRequest;
import com.campusrecruit.ai.dto.RegisterRequest;
import com.campusrecruit.ai.config.JwtUtil;
import com.campusrecruit.ai.entity.User;
import com.campusrecruit.ai.mapper.UserMapper;
import com.campusrecruit.ai.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * UserService 的实现类，写具体的注册/登录逻辑。
 *
 * 人话讲解（注册）：

 * 1) 先按用户名去数据库查一下，如果已经有人用了 → 抛"用户名已存在"。
 * 2) 把前端传来的明文密码，用 BCrypt 加密成一串看不懂的密文。
 * 3) 把新用户插进数据库。
 *
 * 人话讲解（登录）：
 * 1) 按用户名查出用户；查不到 → 抛"用户名或密码错误"（注意：不区分是用户名错还是密码错，
 *    这是安全习惯，避免被别人试探哪些用户名存在）。
 * 2) 用 passwordEncoder.matches(明文, 密文) 比对密码。BCrypt 每次加密结果都不同，
 *    所以不能直接等号比较，必须用 matches 方法。
 * 3) 密码对了 → 用 jwtUtil 生成 token 返回。
 *
 * 面试必懂点：
 * - BCrypt 是"加盐哈希"：同样的密码每次加密出的密文都不一样，但 matches 能验证。
 *   它故意算得慢，抗暴力破解。明文存密码是绝对红线。
 * - 为什么查重要在注册时做、而不是只靠数据库唯一索引？数据库唯一索引也能防重复，
 *   但抛的是底层异常、对用户不友好；我们先查一次，返回清晰的业务错误信息。
 *   （生产上两者会同时用，双保险。）
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public User register(RegisterRequest request) {
        // 1) 查重
        LambdaQueryWrapper<User> exist = new LambdaQueryWrapper<>();
        exist.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(exist) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 2) 加密密码 + 组装实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");

        // 3) 入库
        userMapper.insert(user);
        return user;
    }

    @Override
    public String login(LoginRequest request) {
        // 1) 按用户名查用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(query);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2) 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3) 签发 token：把用户名写进 JWT，前端之后带着它访问受保护接口
        return jwtUtil.generateToken(user.getUsername());
    }
}
