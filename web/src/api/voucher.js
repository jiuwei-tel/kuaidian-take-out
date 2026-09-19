import request from './request'

/* --------------------------------------------------------------------------
   优惠券接口。
   用户端那几个挂在 /user 下（请求头 authentication），
   管理端那几个挂在 /admin 下（请求头 token），
   都由 request.js 按 URL 前缀自动带，这里不用管。
   -------------------------------------------------------------------------- */

/* ---------------- 用户端：领券中心 ---------------- */

/** 可抢的优惠券，带「当前用户是否已抢过」 */
export function getVoucherList() {
  return request({
    url: '/user/voucher/list',
    method: 'get',
  })
}

/** 我抢到的券，未使用的排在前面 */
export function getMyVouchers() {
  return request({
    url: '/user/voucher/my',
    method: 'get',
  })
}

/** 抢券，成功返回领取记录id */
export function seckillVoucher(id) {
  return request({
    url: `/user/voucher/seckill/${id}`,
    method: 'post',
  })
}

/* ---------------- 管理端：优惠券管理 ---------------- */

/** 分页查询，支持按名称、状态筛选 */
export function getVoucherPage(params) {
  return request({
    url: '/admin/voucher/page',
    method: 'get',
    params,
  })
}

/** 新增优惠券 */
export function addVoucher(data) {
  return request({
    url: '/admin/voucher',
    method: 'post',
    data,
  })
}

/** 修改优惠券 */
export function updateVoucher(data) {
  return request({
    url: '/admin/voucher',
    method: 'put',
    data,
  })
}

/** 删除优惠券 */
export function deleteVoucher(id) {
  return request({
    url: '/admin/voucher',
    method: 'delete',
    params: { id },
  })
}

/** 上架/下架，status：1 上架、0 下架 */
export function setVoucherStatus(id, status) {
  return request({
    url: `/admin/voucher/status/${status}`,
    method: 'post',
    params: { id },
  })
}
