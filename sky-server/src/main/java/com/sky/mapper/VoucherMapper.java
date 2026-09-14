package com.sky.mapper;

import com.sky.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VoucherMapper {

    /**
     * 根据id查询优惠券
     * @param id
     * @return
     */
    @Select("select * from voucher where id = #{id}")
    Voucher getById(Long id);

    /**
     * 乐观锁扣减库存：只有库存大于0才扣减，返回影响行数
     * @param id
     * @return 影响行数：1=扣减成功，0=库存已不足（超卖拦截）
     */
    @Update("update voucher set stock = stock - 1 where id = #{id} and stock > 0")
    int deductStock(Long id);
}
