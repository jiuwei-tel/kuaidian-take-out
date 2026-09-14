import request from './request'

/** 今日运营数据：营业额、有效订单数、订单完成率、平均客单价、新增用户数 */
export function businessData() {
  return request({
    url: '/admin/workspace/businessData',
    method: 'get',
  })
}

/** 订单管理总览：待接单 / 待派送 / 已完成 / 已取消 / 全部订单 */
export function overviewOrders() {
  return request({
    url: '/admin/workspace/overviewOrders',
    method: 'get',
  })
}

/** 菜品总览：已启售 / 已停售 */
export function overviewDishes() {
  return request({
    url: '/admin/workspace/overviewDishes',
    method: 'get',
  })
}

/** 套餐总览：已启售 / 已停售 */
export function overviewSetmeals() {
  return request({
    url: '/admin/workspace/overviewSetmeals',
    method: 'get',
  })
}
