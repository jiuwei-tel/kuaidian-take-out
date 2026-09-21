package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.exception.VoucherBusinessException;
import com.sky.mapper.*;
import com.sky.properties.WeChatProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.vo.UserVoucherVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况的处理（收货地址为空、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //金额按购物车单价乘数量算，不采信前端传过来的 amount。
        //这里之前是直接拿 DTO 里的 amount 入库，有两个问题：
        //① 前端要是没传这个字段（比如换个端、或者自己调接口），orders.amount 是 not null，
        //   插入直接失败，接口报「未知错误」；
        //② 金额由客户端决定本身就不对，改一下请求体就能一块钱下单。
        BigDecimal amount = BigDecimal.ZERO;
        for (ShoppingCart cart : shoppingCartList) {
            if (cart.getAmount() == null || cart.getNumber() == null) {
                continue;
            }
            amount = amount.add(cart.getAmount().multiply(BigDecimal.valueOf(cart.getNumber())));
        }
        order.setAmount(amount);
        //下面这几个字段在库里都是 not null，而 DTO 里是可空的包装类型，
        //不传就会以 null 落库、插入直接失败。这里给上默认值：
        //打包费 0、餐具数量 0、立即送出、餐具按餐量提供。
        order.setPackAmount(order.getPackAmount() == null ? 0 : order.getPackAmount());
        order.setTablewareNumber(order.getTablewareNumber() == null ? 0 : order.getTablewareNumber());
        order.setDeliveryStatus(order.getDeliveryStatus() == null ? 1 : order.getDeliveryStatus());
        order.setTablewareStatus(order.getTablewareStatus() == null ? 1 : order.getTablewareStatus());

        //优惠券：原价用上面按购物车算出来的 amount，抵扣也在后端算，
        //前端传来的一律不看。BeanUtils 已经把 DTO 的 voucherId 拷进来了，为 null 就是这单不用券。
        if (order.getVoucherId() != null) {
            Long voucherOrderId = order.getVoucherId();
            //必须是自己抢到的券（查的时候带了 user_id 条件，别人的券查不出来）
            UserVoucherVO myVoucher = voucherOrderMapper.getByIdAndUserId(voucherOrderId, userId);
            if (myVoucher == null) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_NOT_FOUND);
            }
            //只能是还没用过的
            if (!VoucherOrder.UNUSED.equals(myVoucher.getStatus())) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_USED);
            }
            //过期了不能用
            if (myVoucher.getEndTime() != null && LocalDateTime.now().isAfter(myVoucher.getEndTime())) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_EXPIRED);
            }
            //订单原价要达到券的使用门槛（拿原价比，不是抵扣后的实付）
            BigDecimal originAmount = order.getAmount();
            if (myVoucher.getMinAmount() != null && originAmount.compareTo(myVoucher.getMinAmount()) < 0) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_AMOUNT_NOT_ENOUGH);
            }
            //抵扣金额不能超过订单原价，实付最低到 0
            BigDecimal deduct = myVoucher.getValue() == null ? BigDecimal.ZERO : myVoucher.getValue();
            if (deduct.compareTo(originAmount) > 0) {
                deduct = originAmount;
            }
            order.setVoucherAmount(deduct);
            order.setAmount(originAmount.subtract(deduct));
        }

        //向订单表插入1条数据
        orderMapper.insert(order);

        //核销优惠券：条件更新，返回 0 说明这张券已经被别处用掉了（比如并发拿同一张券下两单）
        if (order.getVoucherId() != null) {
            int used = voucherOrderMapper.use(order.getVoucherId(), userId, order.getId(), LocalDateTime.now());
            if (used == 0) {
                throw new VoucherBusinessException(MessageConstant.VOUCHER_USED);
            }
            log.info("订单{}使用优惠券{}，抵扣{}元", order.getNumber(), order.getVoucherId(), order.getVoucherAmount());
        }

        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        //向明细表插入n条数据
        orderDetailMapper.insertBath(orderDetailList);

        //清理购物车中的数据
        //这里要用 clean（按 user_id 清空），delete 是按主键删单条，传 userId 进去什么也清不掉
        shoppingCartMapper.clean(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

/*        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "筷点外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        //原来这里是直接调 updateStatus 改库，绕开了 paySuccess，
        //订单状态是更新了，但 paySuccess 里的来单提醒发不出去，
        //商家那边看不到新单提示。走回同一条路，两件事就都能生效。
        log.info("模拟支付成功，按 paySuccess 处理订单：{}", orderNumber);
        paySuccess(orderNumber);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        //当前用户登录id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);


        //通过websocket向客户浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type", 1); //1表示来单提醒
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: "+outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 分页查询订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //page-helper
        //开始分页查询

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        long total = page.getTotal();
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(total, orderVOList);

    }


    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


    /**
     * 查看各个订单数量
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }


    /**
     * 查询订单情况
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }


    /**
     * 根据id查看订单详情
     *
     * @param id
     * @return
     */
    @Override
    public Orders getById(Long id) {
        return orderMapper.getById(id);
    }


    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void deliveryById(Long id) {
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void confim(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }


    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();

        // 管理端取消订单，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        if (payStatus == Orders.PAID) {
            //用户已支付，正常流程这里要发起退款。但微信支付没有接入
            //（application-dev.yml 里没有商户号和证书，WeChatPayUtil.getClient 拿不到私钥文件），
            //原先调 refund 会直接抛 NullPointerException，接口返回 500，取消动作也做不成。
            //这里跳过真实退款，只把支付状态置为「已退款」。
            log.info("订单 {} 已支付，跳过真实退款（微信支付未接入），支付状态置为已退款", ordersDB.getNumber());
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);

    }


    /**
     * 拒绝订单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer status = ordersDB.getPayStatus();

        // 拒单，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        if (status == Orders.PAID) {
            //用户已支付，正常流程这里要发起退款。微信支付没有接入（同 cancel 方法），
            //原先调 refund 会抛 NullPointerException 变成 500，这里跳过并置为已退款。
            log.info("订单 {} 已支付，跳过真实退款（微信支付未接入），支付状态置为已退款", ordersDB.getNumber());
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);
    }


    /**
     * 历史订单查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        //分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId(); //订单id

                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), list);

    }

    /**
     * 用户取消订单
     * @param id
     */
    @Override
    public void userCancelById(Long id) throws Exception {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //微信支付没有接入（同上），真实退款调不通，原先这里也会抛 500，改为只记录
            log.info("订单 {} 取消，跳过真实退款（微信支付未接入），支付状态置为已退款", ordersDB.getNumber());

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }


    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        //查询当前用户的id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 调用购物车服务，将购物车对象批量添加到购物车表中
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    @Override
    public void reminder(Long id) {
        //根据订单id查询订单
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号："+ordersDB.getNumber());

        //通过websocket向客户端发送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));


    }


}
