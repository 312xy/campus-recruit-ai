package com.campusrecruit.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusrecruit.ai.entity.MatchResult;

/**
 * 匹配结果表的 Mapper。和 JobMapper / ResumeMapper 一样继承 BaseMapper 就够用了，
 * CRUD 全是 MyBatis-Plus 自动生成的；复杂的关联查询会在 Service 层组装。
 */
public interface MatchResultMapper extends BaseMapper<MatchResult> {
}