<template>
  <div class="layout">
    <Sidebar :collapsed="collapsed" />

    <div class="layout-main">
      <Navbar :collapsed="collapsed" @toggle="collapsed = !collapsed" />

      <main class="layout-content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>

    <!-- 来单提醒：全站可用，右下角叠一叠小票 -->
    <transition-group name="reminder" tag="div" class="reminder-stack">
      <div v-for="item in reminders" :key="item.id" class="reminder">
        <div class="reminder-card receipt">
          <div class="reminder-head">
            <span class="reminder-flag">{{ item.kind === 'urge' ? '顾客催单' : '来单提醒' }}</span>
            <button
              type="button"
              class="reminder-close"
              title="关闭"
              @click="dismiss(item.id)"
            >
              <el-icon><Close /></el-icon>
            </button>
          </div>

          <hr class="perf" />

          <p class="reminder-label">订单号</p>
          <p class="reminder-no num">{{ item.orderNo || item.orderId || '—' }}</p>

          <p class="reminder-time num">{{ item.time }}</p>

          <div class="reminder-actions">
            <el-button type="primary" size="small" @click="goHandle(item)">
              {{ item.kind === 'urge' ? '去看看' : '去接单' }}
            </el-button>
            <el-button size="small" @click="dismiss(item.id)">知道了</el-button>
          </div>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import { useShopStore } from '@/stores/shop'
import { useNoticeStore } from '@/stores/notice'
import { playOrderChime } from '@/utils/sound'

const collapsed = ref(false)
const reminders = ref([])

const router = useRouter()
const shopStore = useShopStore()
const noticeStore = useNoticeStore()

/* --------------------------------------------------------------------------
   来单提醒
   后端 WebSocketServer 暴露的是 /ws/{sid}，顾客催单时群发：
     { "type": 2, "orderId": 12, "content": "订单号：2026091300017" }
   -------------------------------------------------------------------------- */
const MAX_REMINDERS = 3
let socket = null
let reconnectTimer = null
let reconnectDelay = 1000
let manualClosed = false

function socketUrl() {
  const sid = Math.random().toString(36).slice(2, 10)
  const base = import.meta.env.VITE_WS_BASE_URL || '/ws'
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}${base}/${sid}`
}

function pushReminder(payload) {
  const content = String(payload?.content || '')
  const orderNo = content.replace(/^订单号[:：]\s*/, '').trim()

  reminders.value.unshift({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    orderId: payload?.orderId,
    // type=1 是新订单，type=2 是顾客催单，小票上的文案据此区分
    kind: payload?.type === 2 ? 'urge' : 'new',
    orderNo,
    time: dayjs().format('HH:mm:ss'),
  })

  if (reminders.value.length > MAX_REMINDERS) {
    reminders.value = reminders.value.slice(0, MAX_REMINDERS)
  }

  playOrderChime()
  noticeStore.pushReminder(orderNo)
}

function dismiss(id) {
  reminders.value = reminders.value.filter((r) => r.id !== id)
}

function goHandle(item) {
  dismiss(item.id)
  const query = { number: item.orderNo || undefined }
  // 新订单必然在待接单里；催单那一单可能已经被接走了，就别加筛选条件
  if (item.kind === 'new') query.status = 2
  router.push({ path: '/manage/order', query })
}

function connect() {
  try {
    socket = new WebSocket(socketUrl())
  } catch {
    scheduleReconnect()
    return
  }

  socket.onopen = () => {
    reconnectDelay = 1000
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      // 后端只会推这两种：type=1 来单提醒、type=2 顾客催单
      if (payload?.type === 1 || payload?.type === 2) {
        pushReminder(payload)
      }
    } catch {
      // 非 JSON 消息直接忽略
    }
  }

  socket.onclose = () => {
    socket = null
    if (!manualClosed) scheduleReconnect()
  }

  socket.onerror = () => {
    // onerror 之后一定会触发 onclose，重连逻辑放那边，避免重复排期
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connect()
  }, reconnectDelay)
  // 退避，最长 30s
  reconnectDelay = Math.min(reconnectDelay * 1.6, 30000)
}

onMounted(() => {
  // 营业状态只在这里拉一次，全站共享。
  // fetchStatus 内部已静默降级（Redis 没起时后端会 500），不会 reject。
  shopStore.fetchStatus()
  connect()
})

onBeforeUnmount(() => {
  manualClosed = true
  if (reconnectTimer) window.clearTimeout(reconnectTimer)
  if (socket) socket.close()
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.layout-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.layout-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: var(--paper);
}

/* ---------------- 来单提醒 ---------------- */
.reminder-stack {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 18px;
  pointer-events: none;
}

.reminder {
  pointer-events: auto;
  width: 244px;
}

.reminder-card {
  padding: 12px 16px 14px;
  border-radius: 2px;
  box-shadow: var(--shadow-pop);
}

.reminder-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.reminder-flag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--fire);
}

.reminder-flag::before {
  content: '';
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: var(--fire);
}

.reminder-close {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--text-3);
  font-size: 13px;
  cursor: pointer;
}

.reminder-close:hover {
  background: var(--paper);
  color: var(--text);
}

.reminder-label {
  margin: 0 0 2px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-3);
}

.reminder-no {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
  word-break: break-all;
}

.reminder-time {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--text-3);
}

.reminder-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.reminder-actions .el-button {
  flex: 1;
  margin-left: 0;
}

/* 从右侧滑入 */
.reminder-enter-active {
  transition: opacity 0.24s ease, transform 0.3s cubic-bezier(0.2, 0.9, 0.3, 1);
}

.reminder-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
  position: absolute;
  right: 0;
}

.reminder-enter-from,
.reminder-leave-to {
  opacity: 0;
  transform: translateX(28px) rotate(2deg);
}

.reminder-move {
  transition: transform 0.24s ease;
}
</style>
