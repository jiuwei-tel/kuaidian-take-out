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

    //下单时间
    private LocalDateTime createTime;
}
