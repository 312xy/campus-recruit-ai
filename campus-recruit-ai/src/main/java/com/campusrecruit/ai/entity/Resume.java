package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历表对应的实体类。
 *
 * 人话讲解：
 * - 一条 Resume 记录 = 某个用户的一份简历。
 * - user_id 是"外键"（这里没建真正的外键约束，靠代码保证），表示这份简历属于谁。
 * - rawText 存用户给的原始简历文本（原封不动存一份，方便以后回溯"AI 是不是解析错了"）。
 * - parsedJson 存大模型吐出来的结构化 JSON 字符串，例如：
 *     {"name":"张三","school":"XX大学","skills":["Java","MySQL"],...}
 *   用 String 而不是对象来接，是因为存进数据库本来就是一段文本；
 *   真正要用结构化数据时，在 Service 层用 Jackson 把它转成 ResumeParseResult 对象。
 *
 * 面试必懂点：
 * - 为什么 parsedJson 不拆成独立字段（name、school 各一列）？
 *   ① 简历结构天然不固定，有人 3 段实习、有人 0 段，数组塞不进普通列；
 *   ② 大模型输出的字段以后想加（比如加"证书"），用 JSON 存不用改表结构；
 *   ③ 代价是不能直接用 SQL 对内部字段做条件查询——如果以后要"按技能搜索简历"，
 *      可以再加一张 resume_skill 冗余表，这就是典型的"读写权衡"。
 * - 状态字段 status 用字符串而不是数字：可读性优先，出问题看日志一眼就懂。
 */
@Data
@TableName("resumes")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    private Long userId;

    /** 上传的文件名（如果是粘贴文本则为空或"手动粘贴"） */
    private String fileName;

    /** 简历原始文本 */
    private String rawText;

    /** AI 解析出的结构化 JSON 字符串 */
    private String parsedJson;

    /** 解析状态：PARSING 解析中 / PARSED 成功 / FAILED 失败 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
