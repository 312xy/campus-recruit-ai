package com.campusrecruit.ai.web;

import com.campusrecruit.ai.dto.ResumeResponse;
import com.campusrecruit.ai.dto.ResumeTextRequest;
import com.campusrecruit.ai.service.CurrentUserService;
import com.campusrecruit.ai.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 简历相关接口。
 *
 * 人话讲解（接口清单）：
 *   POST   /api/resumes/parse     粘贴文本 → 让 AI 解析成结构化简历
 *   POST   /api/resumes/upload    上传 .txt/.md 文件 → 读成文本后同样走解析
 *   GET    /api/resumes           我的简历列表
 *   GET    /api/resumes/{id}      我的某份简历详情（含结构化解析结果）
 *   DELETE /api/resumes/{id}      删除我的某份简历
 *
 * 这几个接口都需要登录（带 token），因为 SecurityConfig 里除了注册/登录，
 * 其余接口一律 authenticated()。
 *
 * 面试必懂点：
 * - 【接口怎么拿到"当前用户"】注意这里没有任何地方从请求参数里取 userId，
 *   全部通过 CurrentUserService 从 token 里解析。前端想伪造别人的身份都做不到。
 * - 【为什么分成 parse 和 upload 两个接口】
 *   parse 收 JSON（用户直接粘贴文本，最稳、演示效果最好）；
 *   upload 收 multipart/form-data（文件上传，走另一套编码方式，需单独处理）。
 *   两者最终都调用同一个 Service 方法，复用同一套 AI 流程。
 * - 【文件上传的安全边界】这里只允许 .txt/.md 纯文本，因为 PDF/Word 需要额外的
 *   文档解析库（PDFBox/POI），且要防范"恶意文件"（zip 炸弹、超大文件）。
 *   真实生产还要限制大小、校验真实 MIME 类型、把文件存到对象存储而不是本地磁盘。
 *   本次 MVP 先用纯文本打通主流程，这是明确的取舍（面试可以这么讲）。
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    /** 上传文件大小上限（与 application.yml 里的 multipart 配置配合） */
    private static final long MAX_UPLOAD_BYTES = 2 * 1024 * 1024;

    private final ResumeService resumeService;
    private final CurrentUserService currentUserService;

    public ResumeController(ResumeService resumeService, CurrentUserService currentUserService) {
        this.resumeService = resumeService;
        this.currentUserService = currentUserService;
    }

    /** 粘贴文本 → AI 解析 */
    @PostMapping("/parse")
    public ResumeResponse parse(@Valid @RequestBody ResumeTextRequest request) {
        return resumeService.parseAndSave(
                currentUserService.requireCurrentUserId(),
                request.getFileName(),
                request.getRawText());
    }

    /** 上传 txt/md 文件 → 读成文本 → AI 解析 */
    @PostMapping("/upload")
    public ResumeResponse upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new RuntimeException("文件不能超过 2MB");
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".txt") || lower.endsWith(".md"))) {
            throw new RuntimeException("目前仅支持 .txt / .md 纯文本文件。"
                    + "如果简历是 PDF 或 Word，请打开后全选复制，粘贴到下面的文本框里提交。");
        }

        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            return resumeService.parseAndSave(currentUserService.requireCurrentUserId(), name, text);
        } catch (java.io.IOException e) {
            throw new RuntimeException("文件读取失败，请重试");
        }
    }

    /** 我的简历列表 */
    @GetMapping
    public List<ResumeResponse> list() {
        return resumeService.listMine(currentUserService.requireCurrentUserId());
    }

    /** 简历详情 */
    @GetMapping("/{id}")
    public ResumeResponse detail(@PathVariable Long id) {
        return resumeService.getMine(currentUserService.requireCurrentUserId(), id);
    }

    /** 删除简历 */
    @DeleteMapping("/{id}")
    public java.util.Map<String, Object> delete(@PathVariable Long id) {
        resumeService.deleteMine(currentUserService.requireCurrentUserId(), id);
        return java.util.Map.of("success", true);
    }
}
