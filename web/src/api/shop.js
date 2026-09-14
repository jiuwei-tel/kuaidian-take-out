import request from './request'

/** 设置店铺营业状态：1营业中 0打烊中 */
export function setShopStatus(status) {
  return request({
    url: `/admin/shop/${status}`,
    method: 'put',
  })
}

/**
 * 获取店铺营业状态。
 *
 * 这个接口读的是 Redis 的 SHOP_STATUS，后端没连上 Redis 时会直接 500。
 * 营业状态只是个指示器，不值得为一个它失败就弹全局报错，
 * 所以默认 silent，由调用方（stores/shop.js）自己降级处理。
 */
export function getShopStatus(config = {}) {
  return request({
    url: '/admin/shop/status',
    method: 'get',
    silent: true,
    ...config,
  })
}
