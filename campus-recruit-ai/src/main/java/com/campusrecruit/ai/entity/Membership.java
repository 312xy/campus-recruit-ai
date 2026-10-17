package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员实体：每个用户当前生效的会员（user_id 唯一）。
 *
 * 关键字段：
 * - expiresAt 是判断会员有效期的唯一权威字段；过期记录不删除，仅靠 expiresAt < now 判定无效。
 * - startsAt 本次会员的开始时间（续费时 = max(now, 当前 expiresAt)）。
 * - autoRenew MVP 暂不实现真实续费逻辑，只是占位字段。
 */
@Data
@TableName("memberships")
public class Membership {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** MONTHLY / QUARTERLY / YEARLY */
    private String plan;

    private LocalDateTime startsAt;

    private LocalDateTime expiresAt;

    private Boolean autoRenew;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}