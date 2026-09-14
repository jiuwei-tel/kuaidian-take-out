package com.sky.service;

public interface VoucherService {

    /**
     * 秒杀优惠券
     * @param voucherId 优惠券id
     * @return 秒杀订单id
     */
    Long seckillVoucher(Long voucherId);
}
