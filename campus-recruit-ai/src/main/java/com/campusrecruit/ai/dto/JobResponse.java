package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的岗位对象。
 *
 * 人话讲解：
 * - 数据库里存的是 parsedJson（一长串 JSON 字符串），前端想要的是"拆好的对象"，
 *   所以返回前用 Jackson 把字符串转成 JobParseResult。
 * - 另外抽出 title / company 两个"冗余字段"直接给前端：
 *   列表页只需要显示岗位名和公司，没必要让前端每次都去解析 JSON。
 *
 * 面试必懂点：
 * - 【冗余字段的取舍】：title/company 在 parsed_json 里已经有了，
 *   这里再单独给一份，属于"为展示做的冗余"。好处是前端列表渲染简单、快；
 *   代价是要保证两者一致（我们只在解析成功时一次性写入，不存在更新不一致的问题）。
 *   如果以后支持"手动修改岗位名"，就要注意同步更新这两个地方。
 * - from() 里对 JSON 解析失败做了兜底（parsed = null），
 *   保证历史脏数据不会让整个列表接口 500。
 */
@Data
public class JobResponse {

    private Long id;

    /** 岗位名（从 AI 标签里冗余出来，方便列表直接展示） */
    private String title;

    /** 公司名（同上） */
    private String company;

    private String status;

    private LocalDateTime createdAt;

    /** AI 打好的完整标签；解析失败时为 null */
    private JobParseResult parsed;

    /** 失败原因，方便前端提示用户 */
    private String errorMessage;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JobResponse from(Job job) {
        JobResponse dto = new JobResponse();
        dto.setId(job.getId());
        dto.setStatus(job.getStatus());
        dto.setCreatedAt(job.getCreatedAt());

        if ("PARSED".equals(job.getStatus()) && job.getParsedJson() != null) {
            try {
                JobParseResult parsed = MAPPER.readValue(job.getParsedJson(), JobParseResult.class);
                dto.setParsed(parsed);
                dto.setTitle(parsed.getTitle());
                dto.setCompany(parsed.getCompany());
            } catch (Exception e) {
                dto.setParsed(null);
                dto.setErrorMessage("标签读取失败：" + e.getMessage());
            }
        } else if ("FAILED".equals(job.getStatus())) {
            dto.setErrorMessage("AI 打标签失败，可重新提交或精简 JD 内容后再试");
        }

        // 兜底：AI 没识别出岗位名/公司时，给个占位文案，避免前端显示空白行
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            dto.setTitle("未识别岗位名");
        }
        if (dto.getCompany() == null || dto.getCompany().isBlank()) {
            dto.setCompany("未识别公司");
        }
        return dto;
    }
}
