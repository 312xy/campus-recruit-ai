package com.campusrecruit.ai.web;

import com.campusrecruit.ai.dto.InterviewAnswerRequest;
import com.campusrecruit.ai.dto.InterviewSessionResponse;
import com.campusrecruit.ai.service.CurrentUserService;
import com.campusrecruit.ai.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟面试接口。
 *
 *   POST   /api/interviews/start         { jobId } → 创建会话 + AI 一次出 5 道题
 *   GET    /api/interviews               我的所有会话（按时间倒序）
 *   GET    /api/interviews/{id}          单场会话详情（题目 + 每题作答 + 总结）
 *   POST   /api/interviews/{id}/answer   { questionIndex, answer } → 提交一题（AI 评分）；最后一题答完自动生成总结
 *   DELETE /api/interviews/{id}          删除会话
 *
 * 面试可讲点：
 * - 【为什么不一次性让用户传所有题答案？】
 *   模拟面试要"交互感" —— 一道题一道题答、做一题评一题，最后看总结。
 *   一次性传所有答案会让 AI 评分变成"批改作业"而不是"模拟面试"。
 * - 【为什么会话接口都走 GET？】
 *   拿数据用 GET（idempotent），改状态用 POST。删除用 DELETE。
 */
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final CurrentUserService currentUserService;

    public InterviewController(InterviewService interviewService, CurrentUserService currentUserService) {
        this.interviewService = interviewService;
        this.currentUserService = currentUserService;
    }

    /** 开始一场面试 */
    @PostMapping("/start")
    public InterviewSessionResponse start(@RequestBody StartRequest req) {
        if (req.jobId == null) throw new RuntimeException("jobId 不能为空");
        return interviewService.start(currentUserService.requireCurrentUserId(), req.jobId);
    }

    /** 我的会话列表 */
    @GetMapping
    public List<InterviewSessionResponse> list() {
        return interviewService.listMine(currentUserService.requireCurrentUserId());
    }

    /** 会话详情 */
    @GetMapping("/{id}")
    public InterviewSessionResponse detail(@PathVariable Long id) {
        return interviewService.getMine(currentUserService.requireCurrentUserId(), id);
    }

    /** 提交一题答案（AI 评判） */
    @PostMapping("/{id}/answer")
    public InterviewSessionResponse submitAnswer(@PathVariable Long id, @Valid @RequestBody InterviewAnswerRequest req) {
        return interviewService.submitAnswer(currentUserService.requireCurrentUserId(), id, req);
    }

    /** 删除会话 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        interviewService.deleteMine(currentUserService.requireCurrentUserId(), id);
        return Map.of("success", true);
    }

    public static class StartRequest {
        public Long jobId;
    }
}