import request from './request'

/** 新增菜品（含口味） */
export function addDish(data) {
  return request({
    url: '/admin/dish',
    method: 'post',
    data,
  })
}

/** 菜品分页查询 */
export function pageDish(params) {
  return request({
    url: '/admin/dish/page',
    method: 'get',
    params,
  })
}

/**
 * 菜品批量删除
 * 后端签名是 @RequestParam List<Long> ids，注意参数名是 ids（复数），
 * 且要拼成 "1,2,3" 这种逗号串，axios 默认的 ids[]=1 形式 Spring 接不到。
 */
export function deleteDish(ids) {
  return request({
    url: '/admin/dish',
    method: 'delete',
    params: { ids: [].concat(ids).join(',') },
  })
}

/** 根据 id 查询菜品（含口味） */
export function getDishById(id) {
  return request({
    url: `/admin/dish/${id}`,
    method: 'get',
  })
}

/** 修改菜品 */
export function editDish(data) {
  return request({
    url: '/admin/dish',
    method: 'put',
    data,
  })
}

/** 菜品起售 / 停售 */
export function dishStatus(status, id) {
  return request({
    url: `/admin/dish/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/** 根据分类 id 查询菜品（套餐编辑时选菜用） */
export function dishListByCategory(categoryId) {
  return request({
    url: '/admin/dish/list',
    method: 'get',
    params: { categoryId },
  })
}
