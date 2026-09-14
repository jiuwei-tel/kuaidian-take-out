package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.*;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 各个订单数量查询
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 根据id查看订单详情
     * @param id
     * @return
     */
    Orders getById(Long id);

    /**
     * 派送订单
     * @param id
     */
    void deliveryById(Long id);

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confim(OrdersConfirmDTO ordersConfirmDTO);


    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);


    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 拒绝订单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;


    /**
     * 查询订单情况
     * @param id
     * @return
     */
    OrderVO details(Long id);


    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);


    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id) throws Exception;


    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);


    /**
     * 用户催单
     * @param id
     */
    void reminder(Long id);
}
