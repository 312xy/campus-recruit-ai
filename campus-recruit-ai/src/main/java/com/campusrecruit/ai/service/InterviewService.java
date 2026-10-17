package com.campusrecruit.ai.service;

import com.campusrecruit.ai.dto.InterviewAnswerRequest;
import com.campusrecruit.ai.dto.InterviewSessionResponse;

import java.util.List;

/**
 * 模拟面试 Service 接口。
 */
public interface InterviewService {

    /** 开始一场面试（基于指定岗位）；同步生成题目（一次大模型调用 5-15s） */
    InterviewSessionResponse start(Long userId, Long jobId);

    /** 我的面试历史 */
    List<InterviewSessionResponse> listMine(Long userId);

    /** 单场面试详情（含题目 + 每题作答 + 总结） */
    InterviewSessionResponse getMine(Long userId, Long sessionId);

    /** 提交一题答案（同步等 AI 评分）；若答完所有题则自动生成总结 */
    InterviewSessionResponse submitAnswer(Long userId, Long sessionId, InterviewAnswerRequest req);

    /** 删除一场面试 */
    void deleteMine(Long userId, Long sessionId);
}