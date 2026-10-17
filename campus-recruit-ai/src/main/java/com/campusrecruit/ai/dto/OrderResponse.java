package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;

/** 给前端看的订单 DTO */
@Data
public class OrderResponse {

    private Long id;
    private String orderNo;
    private String plan;
    private String planName;
    private Integer amountCents;
    private Integer amountYuan;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static OrderResponse from(Order o) {
        OrderResponse r = new OrderResponse();
        r.id = o.getId();
        r.orderNo = o.getOrderNo();
        r.plan = o.getPlan();
        r.planName = o.getPlanName();
        r.amountCents = o.getAmountCents();
        r.amountYuan = o.getAmountCents() == null ? null : o.getAmountCents() / 100;
        r.status = o.getStatus();
        r.paidAt = o.getPaidAt();
        r.createdAt = o.getCreatedAt();
        return r;
    }
}