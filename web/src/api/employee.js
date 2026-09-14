import request from './request'

/** 员工登录 */
export function login(data) {
  return request({
    url: '/admin/employee/login',
    method: 'post',
    data,
  })
}

/** 员工退出 */
export function logout() {
  return request({
    url: '/admin/employee/logout',
    method: 'post',
  })
}

/** 新增员工 */
export function addEmployee(data) {
  return request({
    url: '/admin/employee',
    method: 'post',
    data,
  })
}

/** 员工分页查询 */
export function pageEmployee(params) {
  return request({
    url: '/admin/employee/page',
    method: 'get',
    params,
  })
}

/** 启用 / 禁用员工账号 */
export function enableOrDisableEmployee(status, id) {
  return request({
    url: `/admin/employee/status/${status}`,
    method: 'post',
    params: { id },
  })
}

/** 编辑员工信息 */
export function editEmployee(data) {
  return request({
    url: '/admin/employee',
    method: 'put',
    data,
  })
}

/** 根据 id 查询员工 */
export function getEmployeeById(id) {
  return request({
    url: `/admin/employee/${id}`,
    method: 'get',
  })
}
