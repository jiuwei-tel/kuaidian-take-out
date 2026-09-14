import { ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/employee'
import {
  clearAuth,
  getToken,
  getUserInfo,
  setToken,
  setUserInfo,
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(getUserInfo())

  async function login(form) {
    // 返回 EmployeeLoginVO: { id, userName, name, token }
    const data = await loginApi(form)
    token.value = data.token
    userInfo.value = {
      id: data.id,
      userName: data.userName,
      name: data.name,
    }
    setToken(data.token)
    setUserInfo(userInfo.value)
    return data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 后端退出接口是无状态的，失败也不影响本地清理
    }
    clearAuth()
    token.value = ''
    userInfo.value = null
  }

  return { token, userInfo, login, logout }
})
