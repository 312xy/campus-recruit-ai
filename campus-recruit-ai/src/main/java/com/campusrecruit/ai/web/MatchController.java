package com.campusrecruit.ai.web;

import com.campusrecruit.ai.dto.MatchRequest;
import com.campusrecruit.ai.dto.MatchResponse;
import com.campusrecruit.ai.service.CurrentUserService;
import com.campusrecruit.ai.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 匹配评分接口。
 *
 * 人话讲解：
 *   POST   /api/matches          { resumeId, jobId } → 发起评估（同步等十几秒）
 *   GET    /api/matches          我的评估历史列表（按时间倒序）
 *   GET    /api/matches/{id}     评估详情（含完整 AI 分析）
 *   DELETE /api/matches/{id}     删除评估记录
 *
 * 面试必懂点：
 * - 【为什么 POST 是同步而不是异步轮询？】
 *   MVP 阶段先做"立刻看到结果"的体验。代价：AI 调用通常 10~30s，HTTP 连接占着不放，
 *   所以前端 axios 必须把 timeout 提到 120s（参见前端 src/api/match.js），
 *   后端 DeepSeekClient 也已经设了读取超时上限。
 *   真实的做法是"任务队列 + 客户端轮询 / SSE / WebSocket 推送"，
 *   但 MVP 同步实现能 100 行代码把核心流程走通，先这样做。
 * - 【为什么用"先选 resumeId/jobId"而不是让 AI 自己去全表挑？】
 *   匹配评估是用户的"主观选择"——同一份简历可以分别对多个岗位做匹配，结果要分别保留。
 *   让用户先选才能让结果有归属、有历史。
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final CurrentUserService currentUserService;

    public MatchController(MatchService matchService, CurrentUserService currentUserService) {
        this.matchService = matchService;
        this.currentUserService = currentUserService;
    }

    /** 发起一次匹配评估（同步等结果） */
    @PostMapping
    public MatchResponse evaluate(@Valid @RequestBody MatchRequest request) {
        return matchService.evaluate(currentUserService.requireCurrentUserId(), request);
    }

    /** 我的评估历史列表 */
    @GetMapping
    public List<MatchResponse> list() {
        return matchService.listMine(currentUserService.requireCurrentUserId());
    }

    /** 评估详情 */
    @GetMapping("/{id}")
    public MatchResponse detail(@PathVariable Long id) {
        return matchService.getMine(currentUserService.requireCurrentUserId(), id);
    }

    /** 删除评估记录 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        matchService.deleteMine(currentUserService.requireCurrentUserId(), id);
        return Map.of("success", true);
    }
}