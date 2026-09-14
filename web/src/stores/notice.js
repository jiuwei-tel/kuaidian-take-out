import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 来单提醒的轻量事件总线。
 * 布局层负责连 WebSocket，其他页面（比如工作台）靠 reminderTick 的变化感知「有新单了」，
 * 从而自己去刷新数据 —— 这样就不需要把刷新逻辑写死在 socket 回调里。
 */
export const useNoticeStore = defineStore('notice', () => {
  const reminderTick = ref(0)
  const lastOrderNo = ref('')

  function pushReminder(orderNo) {
    lastOrderNo.value = orderNo || ''
    reminderTick.value += 1
  }

  return { reminderTick, lastOrderNo, pushReminder }
})
