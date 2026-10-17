package com.campusrecruit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusrecruit.ai.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历表的数据库操作接口。
 *
 * 人话讲解：
 * - 和 UserMapper 一样，继承 MyBatis-Plus 的 BaseMapper<Resume>，
 *   就白送了 insert / selectById / updateById / deleteById 等常用方法，
 *   一行 SQL 都不用写。
 * - 要写复杂 SQL（多表 join、按条件统计）时，可以在这里加自定义方法 + @Select 注解，
 *   或者放进 resources/mapper/*.xml。
 *
 * 面试必懂点：
 * - MyBatis-Plus 底层仍然是 MyBatis，只是把"单表增删改查"模板化了。
 *   面试如果被问"为什么要用 MyBatis-Plus 而不是 JPA"，
 *   可以答：国内团队普遍用 MyBatis，SQL 可控、好定位慢查询；
 *   MyBatis-Plus 在此之上省掉单表样板代码，属于平滑增强。
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
