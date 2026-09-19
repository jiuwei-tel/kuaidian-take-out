import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  clearUserAuth,
  getUserProfile,
  getUserToken,
  setUserProfile,
  setUserToken,
} from '@/utils/auth'
import {
  addToCart,
  getCartList,
  loginByPassword,
  subFromCart,
} from '@/api/customer'

export const useCustomerStore = defineStore('customer', () => {
  /* ---------------- 登录态 ---------------- */
  const token = ref(getUserToken())
  const userInfo = ref(getUserProfile())

  /* ---------------- 购物车 ----------------
     顶栏、点餐页、吸底结算条、确认订单页读的都是同一份，
     所以放在 store 里，别在每个页面各拉一次。 */
  const cartList = ref([])

  const cartCount = computed(() =>
    cartList.value.reduce((sum, item) => sum + (item.number || 0), 0)
  )

  // 后端存在购物车里的 amount 是单价，条目小计要自己乘数量
  const cartAmount = computed(() =>
    cartList.value.reduce(
      (sum, item) => sum + Number(item.amount || 0) * (item.number || 0),
      0
    )
  )

  // 同一个菜品选了不同口味算两条，所以匹配时要带上口味
  function findCartItem(dishId, setmealId, dishFlavor) {
    return cartList.value.find((item) => {
      if (dishId) {
        return (
          item.dishId === dishId &&
          (item.dishFlavor || '') === (dishFlavor || '')
        )
      }
      return item.setmealId === setmealId
    })
  }

  function countOf(dishId, dishFlavor) {
    const item = findCartItem(dishId, null, dishFlavor)
    return item ? item.number : 0
  }

  async function login(form) {
    // 返回 { id, username, token }
    const data = await loginByPassword(form)
    token.value = data.token
    userInfo.value = { id: data.id, username: data.username }
    setUserToken(data.token)
    setUserProfile(userInfo.value)
    return data
  }

  function logout() {
    clearUserAuth()
    token.value = ''
    userInfo.value = null
    cartList.value = []
  }

  async function fetchCart() {
    const list = await getCartList()
    cartList.value = list || []
    return cartList.value
  }

  async function add(dishId, setmealId, dishFlavor) {
    await addToCart({ dishId, setmealId, dishFlavor })
    await fetchCart()
  }

  async function sub(dishId, setmealId, dishFlavor) {
    // 车里没有这条就别发请求了：后端减购的实现直接取 list.get(0)，
    // 空列表会抛异常返回 500。
    if (!findCartItem(dishId, setmealId, dishFlavor)) return
    await subFromCart({ dishId, setmealId, dishFlavor })
    await fetchCart()
  }

  // 下单成功后端已经把购物车清空了，这里只清本地，不用再调一次清理接口
  function clearLocalCart() {
    cartList.value = []
  }

  return {
    token,
    userInfo,
    cartList,
    cartCount,
    cartAmount,
    findCartItem,
    countOf,
    login,
    logout,
    fetchCart,
    add,
    sub,
    clearLocalCart,
  }
})
