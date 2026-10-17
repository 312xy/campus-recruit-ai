package com.campusrecruit.ai.web;

import com.campusrecruit.ai.dto.CreateOrderRequest;
import com.campusrecruit.ai.dto.MembershipResponse;
import com.campusrecruit.ai.dto.OrderResponse;
import com.campusrecruit.ai.dto.PayResponse;
import com.campusrecruit.ai.dto.PlanDto;
import com.campusrecruit.ai.service.CurrentUserService;
import com.campusrecruit.ai.service.MembershipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员/订单接口。
 *
 *   GET    /api/memberships/plans          所有套餐
 *   POST   /api/memberships/orders         创建订单（返回 orderNo + 金额）
 *   POST   /api/memberships/orders/{no}/pay    模拟支付（订单改 PAID + 续期会员）
 *   GET    /api/memberships/me             我的当前会员（可能为 null）
 *   GET    /api/memberships/orders         我的所有订单
 *
 * 面试可讲点：
 * - "DB 模拟支付" 是 MVP 的合规边界 —— 真实接微信/支付宝要资质、对账、回调、密钥管理，工作量远超业务本身。
 *   先用 DB 模拟把"业务闭环"跑通（创建订单 → 支付 → 续期 → 会员判断），后期换实现只动 Service 一层。
 * - 支付成功后给前端返回"完整会员信息"，前端不必再请求 /me 一次（少一次请求 = 体验更顺）。
 */
@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;

    public MembershipController(MembershipService membershipService, CurrentUserService currentUserService) {
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/plans")
    public List<PlanDto> plans() {
        return membershipService.listPlans();
    }

    @PostMapping("/orders")
    public OrderResponse createOrder(@RequestBody CreateOrderRequest req) {
        return membershipService.createOrder(currentUserService.requireCurrentUserId(), req);
    }

    @PostMapping("/orders/{orderNo}/pay")
    public PayResponse pay(@PathVariable String orderNo) {
        return membershipService.payOrder(currentUserService.requireCurrentUserId(), orderNo);
    }

    @GetMapping("/me")
    public MembershipResponse my() {
        return membershipService.getMyMembership(currentUserService.requireCurrentUserId());
    }

    @GetMapping("/orders")
    public List<OrderResponse> myOrders() {
        return membershipService.listMyOrders(currentUserService.requireCurrentUserId());
    }
}