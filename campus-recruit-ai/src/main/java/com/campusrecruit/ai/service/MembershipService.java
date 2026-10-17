package com.campusrecruit.ai.service;

import com.campusrecruit.ai.dto.CreateOrderRequest;
import com.campusrecruit.ai.dto.MembershipResponse;
import com.campusrecruit.ai.dto.OrderResponse;
import com.campusrecruit.ai.dto.PayResponse;
import com.campusrecruit.ai.dto.PlanDto;

import java.util.List;

/**
 * 会员/订单 Service 接口。
 *
 * 业务流程（DB 模拟支付）：
 *   1. 用户选套餐 → 调用 createOrder 生成 PENDING 订单
 *   2. 前端展示订单信息（含金额、套餐、订单号）
 *   3. 用户点"确认支付" → 调用 payOrder 把订单改 PAID 并自动续期/开通会员
 *
 * MVP 不接真实支付网关（合规边界、密钥管理、退款链路都是工程量）；
 * "模拟支付"演示效果对面试/演示足够。
 */
public interface MembershipService {

    /** 当前可选套餐 */
    List<PlanDto> listPlans();

    /** 创建订单（PENDING 状态） */
    OrderResponse createOrder(Long userId, CreateOrderRequest req);

    /** 模拟"支付"：改订单为 PAID，续期/开通会员 */
    PayResponse payOrder(Long userId, String orderNo);

    /** 我的当前会员（可能为 null） */
    MembershipResponse getMyMembership(Long userId);

    /** 我的所有订单 */
    List<OrderResponse> listMyOrders(Long userId);
}