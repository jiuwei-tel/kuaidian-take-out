import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearAuth, getToken } from '@/utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 15000,
})

/* --------------------------------------------------------------------------
   请求拦截：管理端把 JWT 放在请求头 token 里
   （用户端对应的是 authentication，本后台只用管理端，所以固定 token）
   -------------------------------------------------------------------------- */
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.token = token
    }
    // 上传文件时让浏览器自己带 boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => Promise.reject(error)
)

/* --------------------------------------------------------------------------
   响应拦截：后端的 Result 约定是 code=1 成功、code=0 失败（不是 HTTP 200/500）
   -------------------------------------------------------------------------- */
let sessionExpiredShown = false

function handleSessionExpired() {
  if (sessionExpiredShown) return
  sessionExpiredShown = true
  clearAuth()
  ElMessageBox.alert('登录状态已过期，请重新登录', '需要重新登录', {
    type: 'warning',
    confirmButtonText: '去登录',
    showClose: false,
  })
    .catch(() => {})
    .finally(() => {
      sessionExpiredShown = false
      window.location.hash = '#/login'
    })
}

service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 文件下载之类的非 JSON 响应，原样返回
    if (!res || typeof res !== 'object' || !('code' in res)) {
      return res
    }

    if (res.code === 1) {
      return res.data
    }

    const msg = res.msg || '请求失败'
    if (!response.config?.silent) ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error?.response?.status

    if (status === 401) {
      // JwtTokenAdminInterceptor 校验失败时就是返回 401
      handleSessionExpired()
      return Promise.reject(error)
    }

    let msg = '网络异常，请检查后端服务是否已启动'
    if (error.code === 'ECONNABORTED') {
      msg = '请求超时，请稍后重试'
    } else if (status === 404) {
      msg = '接口不存在（404）'
    } else if (status >= 500) {
      msg = `服务器异常（${status}）`
    } else if (error?.response?.data?.msg) {
      msg = error.response.data.msg
    }

    // 调用方传 silent: true 的请求（例如营业状态这种非关键指示器），
    // 失败时自己降级处理，不弹全局错误条。
    if (!error.config?.silent) ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
