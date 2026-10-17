package com.campusrecruit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusrecruit.ai.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位表的数据库操作接口。
 *
 * 人话讲解：
 * - 这个接口我们一行实现代码都不用写！因为它继承了 MyBatis-Plus 的 BaseMapper，
 *   框架在启动时自动帮我们生成实现类（这就是所谓的"动态代理"）。
 * - 继承之后白送这些方法：
 *     insert(entity)          插入一条
 *     deleteById(id)          按主键删除
 *     updateById(entity)      按主键更新（只更新非 null 字段）
 *     selectById(id)          按主键查询
 *     selectList(wrapper)     条件查询列表
 *     selectCount(wrapper)    条件统计
 *
 * 面试必懂点：
 * - BaseMapper 的方法都是"单表 CRUD"，不要用它写复杂多表 SQL。
 *   真正的多表关联查询要么自己写 XML/注解 SQL，要么用 VO 承接结果集。
 * - @Mapper 注解是给 MyBatis 的"扫描标记"，
 *   告诉启动类"这个接口是操作数据库的，请为它生成实现类"。
 *   （启动类上已经加了 @MapperScan 包扫描，两种方式保留一种即可，这里保留更直观。）
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
