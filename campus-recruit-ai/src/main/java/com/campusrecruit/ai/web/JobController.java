package com.campusrecruit.ai.web;

import com.campusrecruit.ai.dto.JobResponse;
import com.campusrecruit.ai.dto.JobTextRequest;
import com.campusrecruit.ai.service.CurrentUserService;
import com.campusrecruit.ai.service.JobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 岗位相关接口。
 *
 * 人话讲解（接口清单）：
 *   POST   /api/jobs            粘贴岗位 JD → AI 打标签入库
 *   GET    /api/jobs            我录入的岗位列表
 *   GET    /api/jobs/{id}       我的某个岗位详情（含完整标签）
 *   DELETE /api/jobs/{id}       删除岗位
 *
 * 面试必懂点：
 * - 【为什么不提供"批量导入"或"爬招聘网站"？】
 *   爬虫会有合规问题（违反网站条款、被反爬、数据版权），MVP 阶段先让用户自己粘贴。
 *   这一点要主动说清楚，它体现的是"知道什么能做、什么不能做"的工程判断，
 *   面试官听到你说得出合规边界，比看到一个爬虫代码更认可。
 * - 【Controller 只做三件事】：接参数（@Valid 校验）、取当前用户、调 Service。
 *   任何业务判断都放到 Service 里。理由：Controller 里写业务会导致无法复用、
 *   无法被别的入口（定时任务、MQ 消费者）调用，也不好写单元测试。
 * - 【路由设计】：这里用 /api/jobs 表示"我的岗位资源集合"，
 *   REST 风格下"我的"这层语义由 token 决定，而不是写成 /api/my/jobs ——
 *   因为身份来自凭证，不该出现在 URL 里（URL 会被记日志、被分享，凭证不会）。
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final CurrentUserService currentUserService;

    public JobController(JobService jobService, CurrentUserService currentUserService) {
        this.jobService = jobService;
        this.currentUserService = currentUserService;
    }

    /** 粘贴 JD → AI 打标签 */
    @PostMapping
    public JobResponse parse(@Valid @RequestBody JobTextRequest request) {
        return jobService.parseAndSave(currentUserService.requireCurrentUserId(), request.getRawText());
    }

    /** 我录入的岗位列表 */
    @GetMapping
    public List<JobResponse> list() {
        return jobService.listMine(currentUserService.requireCurrentUserId());
    }

    /** 岗位详情 */
    @GetMapping("/{id}")
    public JobResponse detail(@PathVariable Long id) {
        return jobService.getMine(currentUserService.requireCurrentUserId(), id);
    }

    /** 删除岗位 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        jobService.deleteMine(currentUserService.requireCurrentUserId(), id);
        return Map.of("success", true);
    }
}
