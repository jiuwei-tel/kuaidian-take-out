package com.sky.mapper;

import com.sky.entity.VoucherOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VoucherOrderMapper {

    /**
     * 查询用户是否已抢过该优惠券（一人一单防重）
     * @param voucherId
     * @param userId
     * @return 记录数：>0 表示已抢过
     */
    @Select("select count(*) from voucher_order where voucher_id = #{voucherId} and user_id = #{userId}")
    int countByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") Long userId);

    /**
     * 插入秒杀订单
     * @param voucherOrder
     */
    @Insert("insert into voucher_order (voucher_id, user_id, create_time) values (#{voucherId}, #{userId}, #{createTime})")
    void insert(VoucherOrder voucherOrder);
}
