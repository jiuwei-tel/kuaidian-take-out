package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //优惠券id
    private Long voucherId;

    //用户id
    private Long userId;

    //使用状态：0未使用 1已使用
    private Integer status;

    //使用时间
    private LocalDateTime usedTime;

    //使用该券的订单id
    private Long orderId;

    //领取时间
    private LocalDateTime createTime;

    //未使用
    public static final Integer UNUSED = 0;

    //已使用
    public static final Integer USED = 1;
}
