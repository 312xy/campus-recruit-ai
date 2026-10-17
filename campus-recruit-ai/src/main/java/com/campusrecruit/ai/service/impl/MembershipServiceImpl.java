package com.campusrecruit.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusrecruit.ai.dto.CreateOrderRequest;
import com.campusrecruit.ai.dto.MembershipResponse;
import com.campusrecruit.ai.dto.OrderResponse;
import com.campusrecruit.ai.dto.PayResponse;
import com.campusrecruit.ai.dto.PlanDto;
import com.campusrecruit.ai.entity.Membership;
import com.campusrecruit.ai.entity.Order;
import com.campusrecruit.ai.mapper.MembershipMapper;
import com.campusrecruit.ai.mapper.OrderMapper;
import com.campusrecruit.ai.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会员/订单实现（DB 模拟支付版）。
 *
 * 关键逻辑（面试可讲）：
 * - 金额永远用"分"整数（amountCents）：避免 0.1+0.2 浮点误差。这是金融系统通用惯例。
 * - 订单号用 UUID：避免被外部猜到/枚举（自增 id 不适合做对外凭据）。
 * - 续费时 starts_at = max(now, 当前 expires_at)：已经到期的从现在开算；没到期的接在后面加，
 *   "不浪费已买的会员时间"——这是用户角度最直观的预期。
 * - 用 @Transactional 保证"改订单状态 + 续期会员"两步要么都成功要么都回滚。
 *   真实支付里这是必须的（半成功状态会让用户付了钱但没拿到会员）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private final OrderMapper orderMapper;
    private final MembershipMapper membershipMapper;

    @Override
    public List<PlanDto> listPlans() {
        return PlanDto.allPlans();
    }

    @Override
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        if (req.getPlan() == null) throw new RuntimeException("请选择套餐");
        PlanDto plan = PlanDto.allPlans().stream()
                .filter(p -> p.getCode().equalsIgnoreCase(req.getPlan()))
                .findFirst().orElseThrow(() -> new RuntimeException("套餐不存在：" + req.getPlan()));

        Order o = new Order();
        o.setUserId(userId);
        o.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        o.setPlan(plan.getCode());
        o.setPlanName(plan.getName());
        o.setAmountCents(plan.getPriceCents());
        o.setStatus("PENDING");
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(o);

        return OrderResponse.from(o);
    }

    @Override
    @Transactional
    public PayResponse payOrder(Long userId, String orderNo) {
        Order o = orderMapper.selectOne(new QueryWrapper<Order>()
                .eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) throw new RuntimeException("订单不存在或不属于你");
        if ("PAID".equals(o.getStatus())) {
            // 幂等：已支付就直接返回当前会员（"再点一次支付"不报错）
            Membership cur = currentMembership(userId);
            return new PayResponse(OrderResponse.from(o), MembershipResponse.from(cur));
        }
        if (!"PENDING".equals(o.getStatus())) {
            throw new RuntimeException("订单状态异常：" + o.getStatus());
        }

        // 1) 订单改 PAID
        LocalDateTime now = LocalDateTime.now();
        o.setStatus("PAID");
        o.setPaidAt(now);
        o.setUpdatedAt(now);
        orderMapper.updateById(o);

        // 2) 续期/开通会员
        Membership m = renewOrCreateMembership(userId, o.getPlan(), now);

        log.info("模拟支付完成：userId={}, orderNo={}, plan={}, expiresAt={}",
                userId, orderNo, o.getPlan(), m.getExpiresAt());

        return new PayResponse(OrderResponse.from(o), MembershipResponse.from(m));
    }

    @Override
    public MembershipResponse getMyMembership(Long userId) {
        return MembershipResponse.from(currentMembership(userId));
    }

    @Override
    public List<OrderResponse> listMyOrders(Long userId) {
        List<Order> rows = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("user_id", userId).orderByDesc("created_at"));
        return rows.stream().map(OrderResponse::from).toList();
    }

    /** 续期/新建会员：核心逻辑 */
    private Membership renewOrCreateMembership(Long userId, String planCode, LocalDateTime now) {
        int days = PlanDto.allPlans().stream()
                .filter(p -> p.getCode().equals(planCode))
                .findFirst().orElseThrow(() -> new RuntimeException("套餐不存在"))
                .getDays();

        Membership m = currentMembership(userId);

        if (m == null) {
            // 【首次开通】startsAt=now, expiresAt=now+days
            m = new Membership();
            m.setUserId(userId);
            m.setPlan(planCode);
            m.setStartsAt(now);
            m.setExpiresAt(now.plusDays(days));
            m.setAutoRenew(false);
            m.setCreatedAt(now);
        } else if (m.getExpiresAt() != null && m.getExpiresAt().isAfter(now)) {
            // 【有效期内续费】startsAt 保持不变（用户从原开始时间起一直是会员），
            // 只把 expiresAt 往后顺延 days —— "不浪费已买的会员时间"。
            // 注意不要动 startsAt：如果把它改成原 expiresAt（未来时间），
            // "startsAt > now" 会让前端把会员判成"尚未生效"（active=false），钱花了却显示没会员。
            m.setExpiresAt(m.getExpiresAt().plusDays(days));
        } else {
            // 【过期后续费】重新起算
            m.setStartsAt(now);
            m.setExpiresAt(now.plusDays(days));
        }
        m.setPlan(planCode);
        m.setUpdatedAt(now);

        if (m.getId() == null) membershipMapper.insert(m);
        else membershipMapper.updateById(m);

        return m;
    }

    private Membership currentMembership(Long userId) {
        return membershipMapper.selectOne(new QueryWrapper<Membership>()
                .eq("user_id", userId));
    }
}