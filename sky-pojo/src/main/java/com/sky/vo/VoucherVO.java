package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可抢的优惠券（用户端领券中心的列表项）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //优惠券名称
    private String name;

    //抵扣金额
    private BigDecimal value;

    //使用门槛
    private BigDecimal minAmount;

    //剩余库存
    private Integer stock;

    //1上架 0下架
    private Integer status;

    //活动开始时间
    private LocalDateTime beginTime;

    //活动结束时间
    private LocalDateTime endTime;

    //当前用户是否已经抢过（抢过的不能再抢）
    private Boolean got;
}
