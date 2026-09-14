import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getShopStatus, setShopStatus } from '@/api/shop'

/**
 * 店铺营业状态存在 Redis 的 SHOP_STATUS 里，后端对这个接口有两条容易踩的行为：
 *
 * 1. Redis 连不上 → 直接 500。营业状态只是个指示器，不该为一个它失败
 *    就满屏弹报错，所以这里静默降级、把开关置为不可用。
 * 2. Redis 是新库、SHOP_STATUS 这个 key 不存在 → 接口返回 data: null，
 *    这是正常的，按「打烊中」显示，让用户自己去点开关。
 */
export const useShopStore = defineStore('shop', () => {
  const status = ref(0)
  const loaded = ref(false)
  const loading = ref(false)
  /** null = 还没探测过；true = 接口可用；false = 接口不可用（多为 Redis 未启动） */
  const available = ref(null)
  const unavailableReason = ref('')

  const isOpen = computed(() => status.value === 1)

  async function fetchStatus() {
    loading.value = true
    try {
      const data = await getShopStatus()
      status.value = data === 1 ? 1 : 0
      loaded.value = true
      available.value = true
      unavailableReason.value = ''
    } catch (error) {
      // 不往外抛：调用方（布局挂载）不需要为它写 catch
      available.value = false
      const code = error?.response?.status
      unavailableReason.value = code
        ? `营业状态接口异常（${code}），通常是后端未连接 Redis`
        : '无法获取营业状态，请检查后端服务'
    } finally {
      loading.value = false
    }
  }

  /** 切换营业状态：这是真实业务动作，失败必须让用户看到，所以照常抛错 */
  async function updateStatus(value) {
    loading.value = true
    try {
      await setShopStatus(value)
      status.value = value
      available.value = true
      return value
    } finally {
      loading.value = false
    }
  }

  return {
    status,
    loaded,
    loading,
    available,
    unavailableReason,
    isOpen,
    fetchStatus,
    updateStatus,
  }
})
