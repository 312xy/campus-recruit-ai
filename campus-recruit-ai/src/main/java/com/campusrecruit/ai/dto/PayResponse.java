package com.campusrecruit.ai.dto;

import lombok.Data;

/** 模拟"支付"完成后返回给前端的结构 */
@Data
public class PayResponse {

    private OrderResponse order;

    /** 支付完成后的会员状态 */
    private MembershipResponse membership;

    public PayResponse() {}

    public PayResponse(OrderResponse order, MembershipResponse membership) {
        this.order = order;
        this.membership = membership;
    }
}