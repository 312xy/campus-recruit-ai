package com.campusrecruit.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起匹配评分的请求体：选定一份简历 + 一个岗位。
 *
 * 用 id 而不是传完整对象：前端列表/详情里都有 id，可以无脑从选项里选，
 * 避免传大对象导致接口约定不清晰、版本兼容难做。
 */
@Data
public class MatchRequest {

    @NotNull(message = "请选择简历")
    private Long resumeId;

    @NotNull(message = "请选择岗位")
    private Long jobId;
}