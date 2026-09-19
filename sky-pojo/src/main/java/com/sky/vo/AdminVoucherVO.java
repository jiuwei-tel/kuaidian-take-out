package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端优惠券列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVoucherVO implements Serializable {

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

    //创建时间
    private LocalDateTime createTime;

    //已被领取的张数
    private Integer receivedCount;
}
