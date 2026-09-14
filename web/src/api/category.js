import request from './request'

/** 新增分类 */
export function addCategory(data) {
  return request({
    url: '/admin/category',
    method: 'post',
    data,
  })
}

/** 分类分页查询 */
export function pageCategory(params) {
  return request({
    url: '/admin/category/page',
    method: 'get',
    params,
  })
}

/** 根据 id 删除分类 */
export function deleteCategory(id) {
  return request({
    url: '/admin/category',
    method: 'delete',
    params: { id },
  })
}

/** 修改分类 */
export function editCategory(data) {
  return request({
    url: '/admin/category',
    method: 'put',
    data,
  })
}

/** 启用 / 禁用分类 */
export function enableOrDisableCategory(status, id) {
  return request({
    url: `/admin/category/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/** 根据类型查询分类（1菜品分类 2套餐分类），type 不传则查全部 */
export function categoryList(type) {
  return request({
    url: '/admin/category/list',
    method: 'get',
    params: { type },
  })
}
