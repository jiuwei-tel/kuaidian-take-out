import request from './request'

/** 新增套餐 */
export function addSetmeal(data) {
  return request({
    url: '/admin/setmeal',
    method: 'post',
    data,
  })
}

/** 套餐分页查询 */
export function pageSetmeal(params) {
  return request({
    url: '/admin/setmeal/page',
    method: 'get',
    params,
  })
}

/**
 * 删除套餐
 * 后端签名是 @RequestParam List<Long> id，注意参数名是单数 id（和菜品不一样，菜品是 ids）
 */
export function deleteSetmeal(id) {
  return request({
    url: '/admin/setmeal',
    method: 'delete',
    params: { id: [].concat(id).join(',') },
  })
}

/** 根据 id 查询套餐（含菜品明细） */
export function getSetmealById(id) {
  return request({
    url: `/admin/setmeal/${id}`,
    method: 'get',
  })
}

/** 修改套餐 */
export function editSetmeal(data) {
  return request({
    url: '/admin/setmeal',
    method: 'put',
    data,
  })
}

/** 套餐起售 / 停售 */
export function setmealStatus(status, id) {
  return request({
    url: `/admin/setmeal/status/${status}`,
    method: 'post',
    params: { id },
  })
}
