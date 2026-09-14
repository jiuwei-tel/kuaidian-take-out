import request from './request'

/**
 * 订单条件搜索
 * 对应后端的 OrdersPageQueryDTO：
 * page, pageSize, number, phone, status, beginTime, endTime, userId
 * beginTime / endTime 是 @DateTimeFormat("yyyy-MM-dd HH:mm:ss")，走 query 参数
 */
export function conditionSearch(params) {
  return request({
    url: '/admin/order/conditionSearch',
    method: 'get',
    params,
  })
}

/** 各状态订单数量统计（待接单 / 待派送 / 派送中） */
export function orderStatistics() {
  return request({
    url: '/admin/order/statistics',
    method: 'get',
  })
}

/** 订单详情 */
export function orderDetail(id) {
  return request({
    url: `/admin/order/details/${id}`,
    method: 'get',
  })
}

/** 派送订单 */
export function deliveryOrder(id) {
  return request({
    url: `/admin/order/delivery/${id}`,
    method: 'put',
  })
}

/** 接单：body { id, status: 3 } */
export function confirmOrder(data) {
  return request({
    url: '/admin/order/confirm',
    method: 'put',
    data,
  })
}

/** 完成订单 */
export function completeOrder(id) {
  return request({
    url: `/admin/order/complete/${id}`,
    method: 'put',
  })
}

/** 取消订单：body { id, cancelReason } */
export function cancelOrder(data) {
  return request({
    url: '/admin/order/cancel',
    method: 'put',
    data,
  })
}

/** 拒单：body { id, rejectionReason } */
export function rejectionOrder(data) {
  return request({
    url: '/admin/order/rejection',
    method: 'put',
    data,
  })
}
