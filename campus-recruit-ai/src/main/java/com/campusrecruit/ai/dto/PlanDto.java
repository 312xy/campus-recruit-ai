package com.campusrecruit.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 套餐定义（前端用）。
 *
 * 价格以"元"为单位展示，但实际入库/支付用"分"。
 */
@Data
@AllArgsConstructor
public class PlanDto {

    /** 套餐代码：MONTHLY / QUARTERLY / YEARLY */
    private String code;

    /** 套餐展示名 */
    private String name;

    /** 几天 */
    private int days;

    /** 价格（元） */
    private int priceYuan;

    /** 价格（分）— 入库/支付用 */
    private int priceCents;

    /** 推荐卖点 */
    private List<String> features;

    /** 是否推荐（UI 上加个"最受欢迎"角标） */
    private boolean recommended;

    public static List<PlanDto> allPlans() {
        return List.of(
                new PlanDto("MONTHLY", "月度会员", 30, 9, 900,
                        List.of("解锁高级 AI 评分", "解锁模拟面试", "30 天内有效"), false),
                new PlanDto("QUARTERLY", "季度会员", 90, 25, 2500,
                        List.of("月度全部权益", "解锁 简历改写（即将上线）", "90 天有效", "立省 2 元"), true),
                new PlanDto("YEARLY", "年度会员", 365, 89, 8900,
                        List.of("季度全部权益", "解锁 1v1 简历精修（规划中）", "365 天有效", "省 ¥18"), false)
        );
    }
}