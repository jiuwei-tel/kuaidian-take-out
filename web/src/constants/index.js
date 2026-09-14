/* 全站的枚举口径统一放这里，避免各个页面各写一套 */

/** 订单状态：1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 */
export const ORDER_STATUS = {
  PENDING_PAYMENT: 1,
  TO_BE_CONFIRMED: 2,
  CONFIRMED: 3,
  DELIVERY_IN_PROGRESS: 4,
  COMPLETED: 5,
  CANCELLED: 6,
}

export const ORDER_STATUS_TEXT = {
  1: '待付款',
  2: '待接单',
  3: '已接单',
  4: '派送中',
  5: '已完成',
  6: '已取消',
}

/** 状态对应的色点 class，颜色语义与 styles/index.css 里的 --st-* 一致 */
export const ORDER_STATUS_DOT = {
  1: 'dot-wait',
  2: 'dot-wait',
  3: 'dot-accept',
  4: 'dot-send',
  5: 'dot-done',
  6: 'dot-cancel',
}

export const ORDER_TABS = [
  { label: '待接单', value: 2 },
  { label: '待派送', value: 3 },
  { label: '派送中', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 },
]

/** 分类类型：1菜品分类 2套餐分类 */
export const CATEGORY_TYPE = {
  DISH: 1,
  SETMEAL: 2,
}

export const CATEGORY_TYPE_TEXT = {
  1: '菜品分类',
  2: '套餐分类',
}

/** 售卖 / 账号状态：0停用 1启用 */
export const STATUS = {
  DISABLE: 0,
  ENABLE: 1,
}

/** 支付方式：1微信 2支付宝 */
export const PAY_METHOD_TEXT = {
  1: '微信支付',
  2: '支付宝',
}

/** 支付状态：0未支付 1已支付 2退款 */
export const PAY_STATUS_TEXT = {
  0: '未支付',
  1: '已支付',
  2: '已退款',
}

/** 餐具数量状态：1按餐量提供 0选择具体数量 */
export const TABLEWARE_STATUS_TEXT = {
  0: '按选择数量提供',
  1: '按餐量提供',
}

/** 配送状态：1立即送出 0选择具体时间 */
export const DELIVERY_STATUS_TEXT = {
  0: '选择具体时间',
  1: '立即送出',
}
