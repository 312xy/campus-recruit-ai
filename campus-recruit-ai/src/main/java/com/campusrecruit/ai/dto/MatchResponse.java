package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.MatchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 给前端用的匹配结果 DTO。
 *
 * 设计要点：
 * - 跟 ResumeResponse / JobResponse 一样的套路：实体 + 解析 JSON → 给前端一个友好对象。
 * - 不直接返回实体，避免泄漏内部字段（比如以后加的 internal_notes 之类）。
 */
@Data
@Slf4j
public class MatchResponse {

    private Long id;
    private Long resumeId;
    private Long jobId;
    private Integer matchedScore;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private MatchParseResult parsed;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static MatchResponse from(MatchResult r) {
        MatchResponse resp = new MatchResponse();
        resp.id = r.getId();
        resp.resumeId = r.getResumeId();
        resp.jobId = r.getJobId();
        resp.matchedScore = r.getScore();
        resp.status = r.getStatus();
        resp.errorMessage = r.getErrorMessage();
        resp.createdAt = r.getCreatedAt();

        if (r.getParsedJson() != null && !r.getParsedJson().isBlank()) {
            try {
                resp.parsed = MAPPER.readValue(r.getParsedJson(), MatchParseResult.class);
            } catch (Exception e) {
                log.warn("匹配结果 JSON 反序列化失败，id={}, err={}", r.getId(), e.getMessage());
                resp.parsed = null;
            }
        }
        return resp;
    }
}