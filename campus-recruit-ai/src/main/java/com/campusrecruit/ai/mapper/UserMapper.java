package com.campusrecruit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusrecruit.ai.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表的数据库操作接口。
 *
 * 人话讲解：
 * - 这就是"你和数据库说话的翻译官"。你想"插一条用户""按用户名查用户"，
 *   不用自己写 SQL，调这个接口的方法就行。
 * - 它继承了 MyBatis-Plus 的 BaseMapper<User>，所以"增删改查"的基础方法
 *   （insert / selectOne / selectCount / deleteById ...）已经白送给你了。
 * - 我们后面几乎不写 XML 的 SQL，全靠 BaseMapper 提供的方法 + 条件构造器（Wrapper）。
 *
 * 面试必懂点：
 * - 为什么用 MyBatis-Plus 而不是直接写 JDBC？因为 JDBC 要手写连接、Statement、
 *   结果集解析，太啰嗦；MyBatis-Plus 帮我们封装好了，国内实习/校招项目里非常常见。
 * - @Mapper 让 Spring 在启动时自动创建这个接口的代理实现类（底层还是 JDBC）。
 *   另外启动类上的 @MapperScan 已经扫到这个包了，所以写不写 @Mapper 都行，
 *   但我们两个都写上，最稳妥、最易懂。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
