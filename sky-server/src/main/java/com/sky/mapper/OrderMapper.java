package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import javax.annotation.ManagedBean;

@Mapper
public interface OrderMapper {


    /**
     * 用户下单
     * @param orders
     */
    void insert(Orders orders);


}
