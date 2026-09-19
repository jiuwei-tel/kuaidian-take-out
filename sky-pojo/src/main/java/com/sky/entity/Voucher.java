package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //优惠券名称
    private String name;

    //抵扣金额
    private BigDecimal value;

    //使用门槛：订单金额达到该值才能使用
    private BigDecimal minAmount;

    //剩余库存
    private Integer stock;

    //状态：1上架 0下架
    private Integer status;

    //秒杀开始时间
    private LocalDateTime beginTime;

    //秒杀结束时间
    private LocalDateTime endTime;

    //创建时间
    private LocalDateTime createTime;

    //创建人
    private Long createUser;

    //更新时间
    private LocalDateTime updateTime;

    //更新人
    private Long updateUser;
}
