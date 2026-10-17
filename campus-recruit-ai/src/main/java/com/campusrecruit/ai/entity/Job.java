package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位表对应的实体类。
 *
 * 人话讲解：
 * - 一条 Job 记录 = 一份岗位 JD（你从招聘网站复制过来粘贴进去的那段文字）。
 * - 和 Resume 是"镜像"设计：都存 rawText（原文）+ parsedJson（AI 处理后的结构化结果）+ status。
 *   这样两个模块的代码套路一致，你理解了一个，另一个自然就懂了。
 * - rawTextHash：JD 原文的 SHA-256 摘要，专门用来判断"这份 JD 我是不是已经录过了"。
 *
 * 面试必懂点：
 * - 【为什么要单独存一个哈希，而不是直接比对原文？】
 *   原文是长文本（CLOB/TEXT），用它做等值查询有几个麻烦：大字段比较慢、
 *   CLOB 在不同数据库里比较行为不一致、原文里的换行符/空格差异会导致"看着一样却不相等"。
 *   哈希把任意长度内容压成 64 位定长字符串，比较快且稳定，
 *   而且天然适合加索引做唯一约束 —— 这就是"内容指纹"的思路。
 * - 【岗位归属】：这里带 userId，表示"这个岗位是我录入的"。
 *   好处是每个用户有自己的一份岗位清单（个性化）；坏处是不能全局共享。
 *   真实产品里会做一张全局岗位池 + 用户关联表（多对多），
 *   但 MVP 阶段先一人一份更简单，也更容易讲清楚。这个取舍要能说出来。
 */
@Data
@TableName("jobs")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 录入这个岗位的用户 id */
    private Long userId;

    /** 岗位 JD 原文（原封不动存一份，方便回溯"AI 是不是标错了"） */
    private String rawText;

    /** JD 原文的 SHA-256 摘要，用于去重 */
    private String rawTextHash;

    /** AI 打好的标签（JSON 字符串） */
    private String parsedJson;

    /** 处理状态：PARSING 解析中 / PARSED 成功 / FAILED 失败 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
