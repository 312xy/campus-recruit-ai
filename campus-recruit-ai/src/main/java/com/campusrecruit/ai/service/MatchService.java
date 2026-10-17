package com.campusrecruit.ai.service;

import com.campusrecruit.ai.dto.MatchRequest;
import com.campusrecruit.ai.dto.MatchResponse;

import java.util.List;

/**
 * 匹配评分 Service（接口）：让 Controller 不必依赖实现，方便以后换实现/加缓存。
 */
public interface MatchService {

    /** 发起一次匹配评分（同步等结果，等十几秒） */
    MatchResponse evaluate(Long userId, MatchRequest req);

    /** 我的所有匹配记录（按时间倒序） */
    List<MatchResponse> listMine(Long userId);

    /** 单条匹配详情（带完整 AI 分析结果） */
    MatchResponse getMine(Long userId, Long id);

    /** 删除一条匹配记录 */
    void deleteMine(Long userId, Long id);
}