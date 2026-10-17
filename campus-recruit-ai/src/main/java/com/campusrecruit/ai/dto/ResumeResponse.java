package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.Resume;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的简历对象。
 *
 * 人话讲解：
 * - 数据库里存的是 parsedJson（一长串 JSON 字符串），
 *   但前端更希望拿到"已经拆好的对象"，所以这里在返回前把字符串转成 ResumeParseResult。
 * - 转换失败（比如 AI 返回的 JSON 不合法）就置 parsed = null 并把状态标成 FAILED，
 *   而不是让整个接口 500 —— 接口要"尽量可用"。
 *
 * 面试必懂点：
 * - 这里体现了"存储格式"和"接口格式"解耦：库里存字符串（省事、灵活），
 *   对外给对象（前端好渲染）。中间靠一次 Jackson 转换衔接。
 * - 注意 from() 里对解析异常做了兜底：AI 输出不可控，是必须处理的边界。
 */
@Data
public class ResumeResponse {

    private Long id;
    private String fileName;
    private String status;
    private LocalDateTime createdAt;

    /** AI 解析出的结构化内容；解析失败时为 null */
    private ResumeParseResult parsed;

    /** 解析失败时的原因，方便前端提示用户 */
    private String errorMessage;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ResumeResponse from(Resume resume) {
        ResumeResponse dto = new ResumeResponse();
        dto.setId(resume.getId());
        dto.setFileName(resume.getFileName());
        dto.setStatus(resume.getStatus());
        dto.setCreatedAt(resume.getCreatedAt());

        if ("PARSED".equals(resume.getStatus()) && resume.getParsedJson() != null) {
            try {
                dto.setParsed(MAPPER.readValue(resume.getParsedJson(), ResumeParseResult.class));
            } catch (Exception e) {
                dto.setParsed(null);
                dto.setErrorMessage("解析结果读取失败：" + e.getMessage());
            }
        } else if ("FAILED".equals(resume.getStatus())) {
            dto.setErrorMessage("AI 解析失败，可重新提交或简化简历格式后再试");
        }
        return dto;
    }
}
