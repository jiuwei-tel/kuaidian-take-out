const TOKEN_KEY = 'sky_admin_token'
const USER_KEY = 'sky_admin_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export function setUserInfo(info) {
  localStorage.setItem(USER_KEY, JSON.stringify(info || {}))
}

export function removeUserInfo() {
  localStorage.removeItem(USER_KEY)
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}

/* --------------------------------------------------------------------------
   网页端点餐端的登录态，和管理端分开存
   后端那边也是两套 JWT 密钥（admin 用 itcast、user 用 itheima），
   前端只是配合着分开，两边互不干扰。
   -------------------------------------------------------------------------- */
const USER_TOKEN_KEY = 'sky_user_token'
const USER_INFO_KEY = 'sky_user_info'

export function getUserToken() {
  return localStorage.getItem(USER_TOKEN_KEY) || ''
}

export function setUserToken(token) {
  localStorage.setItem(USER_TOKEN_KEY, token)
}

// 名字带 Profile 是为了和管理端的 getUserInfo 区分开
export function getUserProfile() {
  try {
    return JSON.parse(localStorage.getItem(USER_INFO_KEY) || 'null')
  } catch {
    return null
  }
}

export function setUserProfile(info) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(info || {}))
}

export function clearUserAuth() {
  localStorage.removeItem(USER_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}
