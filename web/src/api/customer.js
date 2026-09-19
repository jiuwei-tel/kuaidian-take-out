import request from './request'

/* --------------------------------------------------------------------------
   点餐端（顾客端）接口，都挂在 /user 下。
   请求头走 authentication，由 request.js 按 URL 前缀自动带上，这里不用管。
   -------------------------------------------------------------------------- */

/** 账号密码登录 */
export function loginByPassword(data) {
  return request({
    url: '/user/user/loginByPassword',
    method: 'post',
    data,
  })
}

/** 顾客注册（注册时会顺便建一条默认收货地址） */
export function register(data) {
  return request({
    url: '/user/user/register',
    method: 'post',
    data,
  })
}

/** 重置密码，用注册时留的手机号后四位核对 */
export function resetPassword(data) {
  return request({
    url: '/user/user/resetPassword',
    method: 'post',
    data,
  })
}

/** 营业状态。这个接口后端放行了，不登录也能查，失败了自己降级不弹错误条 */
export function getShopStatus(config = {}) {
  return request({
    url: '/user/shop/status',
    method: 'get',
    silent: true,
    ...config,
  })
}

/** 分类列表，type：1 菜品分类、2 套餐分类 */
export function getCategoryList(type) {
  return request({
    url: '/user/category/list',
    method: 'get',
    params: { type },
  })
}

/** 某个分类下的菜品，categoryId 必传 */
export function getDishList(categoryId) {
  return request({
    url: '/user/dish/list',
    method: 'get',
    params: { categoryId },
  })
}

/** 购物车列表 */
export function getCartList() {
  return request({
    url: '/user/shoppingCart/list',
    method: 'get',
  })
}

/** 加一份进购物车 */
export function addToCart(data) {
  return request({
    url: '/user/shoppingCart/add',
    method: 'post',
    data,
  })
}

/** 从购物车减一份。注意后端对不在车里的商品会报错，调用前先确认存在 */
export function subFromCart(data) {
  return request({
    url: '/user/shoppingCart/sub',
    method: 'post',
    data,
  })
}

/** 地址簿列表 */
export function getAddressList() {
  return request({
    url: '/user/addressBook/list',
    method: 'get',
  })
}

/** 默认收货地址 */
export function getDefaultAddress() {
  return request({
    url: '/user/addressBook/default',
    method: 'get',
  })
}

/** 提交订单 */
export function submitOrder(data) {
  return request({
    url: '/user/order/submit',
    method: 'post',
    data,
  })
}

/** 支付。后端这边是模拟的，不接微信支付，调完订单直接变待接单 */
export function payOrder(data) {
  return request({
    url: '/user/order/payment',
    method: 'put',
    data,
  })
}

/** 历史订单。status 不传表示全部，别传空字符串 */
export function getHistoryOrders(params) {
  return request({
    url: '/user/order/historyOrders',
    method: 'get',
    params,
  })
}

/** 订单详情 */
export function getOrderDetail(id) {
  return request({
    url: `/user/order/orderDetail/${id}`,
    method: 'get',
  })
}

/** 取消订单 */
export function cancelOrder(id) {
  return request({
    url: `/user/order/cancel/${id}`,
    method: 'put',
  })
}

/** 催单，会让商家后台弹提醒 */
export function urgeOrder(id) {
  return request({
    url: `/user/order/reminder/${id}`,
    method: 'get',
  })
}
