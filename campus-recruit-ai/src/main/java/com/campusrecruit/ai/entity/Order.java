package com.campusrecruit.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体（"模拟支付"用，DB 而不是真实网关）。
 *
 * 关键字段：
 * - orderNo: 业务订单号（对前端展示和去重用），UUID 生成；
 *   区别于自增 id —— id 是数据库内部数字，订单号是给业务用的"凭据"。
 * - amountCents: 金额用"分"为单位整数，避开浮点累加误差。
 * - status: PENDING 已下单未支付 / PAID 已支付 / REFUNDED 已退款。
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 业务订单号（UUID），唯一 */
    private String orderNo;

    /** MONTHLY / QUARTERLY / YEARLY */
    private String plan;

    /** 套餐展示名（冗余存，方便订单列表直接显示） */
    private String planName;

    /** 金额（分） */
    private Integer amountCents;

    /** PENDING / PAID / REFUNDED */
    private String status;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}