package com.campusrecruit.ai.dto;

import com.campusrecruit.ai.entity.Membership;
import lombok.Data;

import java.time.LocalDateTime;

/** 会员状态返回 */
@Data
public class MembershipResponse {

    private Long id;
    private Long userId;
    private String plan;
    private String planName;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;

    /** 是否当前有效（now 在 startsAt 和 expiresAt 之间） */
    private Boolean active;

    /** 距到期还有几天（负数代表已过期） */
    private Long daysLeft;

    public static MembershipResponse from(Membership m) {
        if (m == null) return null;
        MembershipResponse r = new MembershipResponse();
        r.id = m.getId();
        r.userId = m.getUserId();
        r.plan = m.getPlan();
        r.planName = PlanDto.allPlans().stream()
                .filter(p -> p.getCode().equals(m.getPlan()))
                .findFirst().map(PlanDto::getName).orElse(m.getPlan());
        r.startsAt = m.getStartsAt();
        r.expiresAt = m.getExpiresAt();
        LocalDateTime now = LocalDateTime.now();
        boolean active = m.getExpiresAt() != null && m.getExpiresAt().isAfter(now)
                && (m.getStartsAt() == null || !m.getStartsAt().isAfter(now));
        r.active = active;
        if (m.getExpiresAt() != null) {
            r.daysLeft = java.time.Duration.between(now, m.getExpiresAt()).toDays();
        }
        return r;
    }
}