<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelOrder,
  getHistoryOrders,
  getOrderDetail,
  payOrder,
  urgeOrder,
} from '@/api/customer'
import { ORDER_STATUS_DOT, ORDER_STATUS_TEXT } from '@/constants'
import { fmtDateTime, fmtMoney } from '@/utils/format'

/* 顾客关心的分法，和后台上那份从「待接单」起的页签不是一套。
   「进行中」后端没有对应的单一状态值，拉回来在前端筛。 */
const TABS = [
  { key: 'all', label: '全部' },
  { key: 'unpaid', label: '待付款', status: 1 },
  { key: 'doing', label: '进行中', local: [2, 3, 4] },
  { key: 'done', label: '已完成', status: 5 },
  { key: 'canceled', label: '已取消', status: 6 },
]

const activeTab = ref('all')
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const expandedId = ref(null)
const detailMap = ref({})

// 接口那边没写 order by，顺序不保证，这里自己按时间倒序排
const sorted = computed(() => {
  const list = [...orders.value]
  list.sort((a, b) => {
    const byTime = String(b.orderTime || '').localeCompare(String(a.orderTime || ''))
    return byTime !== 0 ? byTime : (b.id || 0) - (a.id || 0)
  })
  return list
})

const visible = computed(() => {
  const tab = TABS.find((t) => t.key === activeTab.value)
  if (tab?.local) return sorted.value.filter((o) => tab.local.includes(o.status))
  return sorted.value
})

async function load() {
  loading.value = true
  try {
    const tab = TABS.find((t) => t.key === activeTab.value)
    // status 为空时不能把这个参数带上，后端收到空串会报错
    const params = { page: page.value, pageSize }
    if (tab?.status) params.status = tab.status

    const res = await getHistoryOrders(params)
    orders.value = res?.records || []
    total.value = res?.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  if (activeTab.value === key) return
  activeTab.value = key
  page.value = 1
  expandedId.value = null
  load()
}

async function toggle(order) {
  if (expandedId.value === order.id) {
    expandedId.value = null
    return
  }
  expandedId.value = order.id
  if (!detailMap.value[order.id]) {
    try {
      const vo = await getOrderDetail(order.id)
      detailMap.value = { ...detailMap.value, [order.id]: vo }
    } catch {
      detailMap.value = { ...detailMap.value, [order.id]: { orderDetailList: [] } }
    }
  }
}

function detailOf(id) {
  return detailMap.value[id]
}

async function onCancel(order) {
  try {
    await ElMessageBox.confirm(`订单号 ${order.number}，取消之后没法恢复`, '要取消这一单吗', {
      confirmButtonText: '取消订单',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  await cancelOrder(order.id)
  ElMessage.success('订单已取消')
  await load()
}

async function onPay(order) {
  await payOrder({ orderNumber: order.number, payMethod: 1 })
  ElMessage.success('支付完成')
  await load()
}

async function onUrge(order) {
  await urgeOrder(order.id)
  ElMessage.success('已经帮您催了，商家马上处理')
}

function actionsOf(order) {
  if (order.status === 1) {
    return [
      { key: 'pay', label: '去支付', run: onPay },
      { key: 'cancel', label: '取消订单', danger: true, run: onCancel },
    ]
  }
  if ([2, 3, 4].includes(order.status)) {
    return [{ key: 'urge', label: '催单', run: onUrge }]
  }
  return []
}

onMounted(load)
</script>

<template>
  <div class="orders-page">
    <h1 class="page-h">我的订单</h1>

    <nav class="tabs" aria-label="订单筛选">
      <button
        v-for="t in TABS"
        :key="t.key"
        type="button"
        class="tab"
        :class="{ 'is-on': t.key === activeTab }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </button>
    </nav>

    <p v-if="loading" class="hint">正在查订单…</p>

    <template v-else-if="visible.length">
      <article v-for="o in visible" :key="o.id" class="order">
        <header class="order-head" @click="toggle(o)">
          <span class="dot" :class="ORDER_STATUS_DOT[o.status]" aria-hidden="true"></span>
          <span class="order-status">{{ ORDER_STATUS_TEXT[o.status] || '状态未知' }}</span>
          <span class="order-no num">{{ o.number }}</span>
          <span class="order-amount num money">{{ fmtMoney(o.amount) }}</span>
        </header>

        <div class="order-meta">
          <span class="num">{{ fmtDateTime(o.orderTime) }}</span>
          <span v-if="o.address" class="order-addr">{{ o.address }}</span>
        </div>

        <transition name="expand">
          <div v-if="expandedId === o.id" class="order-detail">
            <ul v-if="detailOf(o.id)" class="lines">
              <li v-for="d in detailOf(o.id).orderDetailList" :key="d.id" class="line">
                <span class="line-name">{{ d.name }}</span>
                <span v-if="d.dishFlavor" class="line-flavor">{{ d.dishFlavor }}</span>
                <span class="leader" aria-hidden="true"></span>
                <span class="line-times num">×{{ d.number }}</span>
              </li>
            </ul>
            <p v-else class="hint-sm">正在读订单明细…</p>
          </div>
        </transition>

        <footer v-if="actionsOf(o).length" class="order-actions">
          <button
            v-for="a in actionsOf(o)"
            :key="a.key"
            type="button"
            class="act"
            :class="{ 'act-danger': a.danger }"
            @click="a.run(o)"
          >
            {{ a.label }}
          </button>
        </footer>
      </article>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </template>

    <p v-else class="hint">这里还没有订单，先去菜单挑两个菜</p>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 26px 24px 40px;
}

.page-h {
  margin: 0 0 18px;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--edge);
}

.tab {
  padding: 7px 14px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--text-2);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.tab:hover {
  background: var(--surface-sunken);
  color: var(--text);
}

.tab.is-on {
  background: var(--teal-soft);
  color: var(--teal);
  font-weight: 600;
}

/* ---------------- 订单卡 ---------------- */
.order {
  padding: 16px 18px;
  border: 1px solid var(--edge);
  border-radius: var(--radius);
  background: var(--surface);
}

.order + .order {
  margin-top: 12px;
}

.order-head {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.order-status {
  font-size: 14px;
  font-weight: 600;
}

.order-no {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-3);
}

.order-amount {
  min-width: 74px;
  font-size: 15px;
  font-weight: 600;
  text-align: right;
}

.order-meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--text-3);
}

.order-addr {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--edge-strong);
}

.lines {
  margin: 0;
  padding: 0;
  list-style: none;
}

.line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 4px 0;
}

.line-name {
  font-size: 13.5px;
  color: var(--text-2);
}

.line-flavor {
  font-size: 12px;
  color: var(--text-3);
}

.leader {
  flex: 1;
  min-width: 20px;
  border-bottom: 1px dotted var(--edge-strong);
  transform: translateY(-3px);
}

.line-times {
  font-size: 12.5px;
  color: var(--text-3);
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--edge);
}

.act {
  padding: 7px 16px;
  border: 1px solid var(--edge-strong);
  border-radius: var(--radius-sm);
  background: var(--surface);
  font-family: var(--font-sans);
  font-size: 13px;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.15s ease;
}

.act:hover {
  border-color: var(--teal);
  color: var(--teal);
}

.act-danger:hover {
  border-color: var(--fire);
  color: var(--fire);
}

.hint {
  margin: 48px 0;
  font-size: 13.5px;
  color: var(--text-3);
  text-align: center;
}

.hint-sm {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-3);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.expand-enter-active,
.expand-leave-active {
  transition: opacity 0.16s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
}

@media (max-width: 860px) {
  .orders-page {
    padding: 18px 16px 32px;
  }

  .order-no {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .expand-enter-active,
  .expand-leave-active {
    transition: none;
  }
}
</style>
