package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户已抢到的优惠券（我的券）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucherVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //领取记录id，下单时把它作为 voucherId 传给后端
    private Long id;

    //优惠券id
    private Long voucherId;

    //优惠券名称
    private String name;

    //抵扣金额
    private BigDecimal value;

    //使用门槛
    private BigDecimal minAmount;

    //0未使用 1已使用
    private Integer status;

    //领取时间
    private LocalDateTime createTime;

    //使用时间
    private LocalDateTime usedTime;

    //活动开始时间
    private LocalDateTime beginTime;

    //活动结束时间
    private LocalDateTime endTime;

    //是否已过期（后端按当前时间算好，前端直接用）
    private Boolean expired;
}
