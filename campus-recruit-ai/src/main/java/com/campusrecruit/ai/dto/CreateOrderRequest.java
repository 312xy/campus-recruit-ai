package com.campusrecruit.ai.dto;

import lombok.Data;

/** 创建订单的请求体 */
@Data
public class CreateOrderRequest {
    /** MONTHLY / QUARTERLY / YEARLY */
    private String plan;
}