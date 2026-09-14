<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="page-title">工作台</h2>
        <p class="page-sub">{{ todayText }} · 今日经营概览</p>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新数据</el-button>
      </div>
    </div>

    <!-- 今日数据：一条横带，用细分隔线切开，而不是五张卡片 -->
    <section class="stat-strip panel">
      <div v-for="item in statItems" :key="item.key" class="stat-cell">
        <p class="stat-label">{{ item.label }}</p>
        <p class="stat-value num">
          <span class="stat-num">{{ item.value }}</span>
          <span v-if="item.unit" class="stat-unit">{{ item.unit }}</span>
        </p>
      </div>
    </section>

    <div class="dash-grid">
      <!-- 订单管理 -->
      <section class="panel">
        <header class="panel-head">
          <span class="panel-title">订单管理</span>
          <el-button link type="primary" @click="goOrder({})">
            查看全部订单
          </el-button>
        </header>
        <div class="panel-body panel-body--flush">
          <button
            v-for="row in orderRows"
            :key="row.key"
            type="button"
            class="mrow"
            :class="{ 'is-alert': row.alert && row.count > 0 }"
            @click="goOrder(row.query)"
          >
            <span class="mrow-label">
              <span class="dot" :class="row.dotClass">{{ row.label }}</span>
            </span>
            <span class="mrow-value num">{{ row.count }}</span>
          </button>
        </div>
      </section>

      <div class="dash-col">
        <!-- 菜品总览 -->
        <section class="panel">
          <header class="panel-head">
            <span class="panel-title">菜品总览</span>
            <el-button link type="primary" @click="router.push('/manage/dish')">
              菜品管理
            </el-button>
          </header>
          <div class="panel-body">
            <div class="split">
              <div class="split-cell">
                <p class="split-label">已启售</p>
                <p class="split-value num">{{ dishes.sold }}</p>
              </div>
              <div class="split-cell">
                <p class="split-label">已停售</p>
                <p class="split-value num is-muted">{{ dishes.discontinued }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 套餐总览 -->
        <section class="panel">
          <header class="panel-head">
            <span class="panel-title">套餐总览</span>
            <el-button link type="primary" @click="router.push('/manage/setmeal')">
              套餐管理
            </el-button>
          </header>
          <div class="panel-body">
            <div class="split">
              <div class="split-cell">
                <p class="split-label">已启售</p>
                <p class="split-value num">{{ setmeals.sold }}</p>
              </div>
              <div class="split-cell">
                <p class="split-label">已停售</p>
                <p class="split-value num is-muted">{{ setmeals.discontinued }}</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import {
  businessData,
  overviewDishes,
  overviewOrders,
  overviewSetmeals,
} from '@/api/workspace'
import { useNoticeStore } from '@/stores/notice'
import { fmtMoney, fmtPercent } from '@/utils/format'

dayjs.locale('zh-cn')

const router = useRouter()
const noticeStore = useNoticeStore()

const loading = ref(false)

const business = ref({
  turnover: 0,
  validOrderCount: 0,
  orderCompletionRate: 0,
  unitPrice: 0,
  newUsers: 0,
})

const orders = ref({
  waitingOrders: 0,
  deliveredOrders: 0,
  completedOrders: 0,
  cancelledOrders: 0,
  allOrders: 0,
})

const dishes = ref({ sold: 0, discontinued: 0 })
const setmeals = ref({ sold: 0, discontinued: 0 })

const todayText = computed(() => dayjs().format('YYYY年M月D日 dddd'))

const statItems = computed(() => [
  { key: 'turnover', label: '今日营业额', value: fmtMoney(business.value.turnover), unit: '元' },
  { key: 'orders', label: '有效订单', value: business.value.validOrderCount ?? 0, unit: '单' },
  {
    key: 'rate',
    label: '订单完成率',
    value: fmtPercent(business.value.orderCompletionRate),
    unit: '',
  },
  { key: 'unit', label: '平均客单价', value: fmtMoney(business.value.unitPrice), unit: '元' },
  { key: 'users', label: '新增用户', value: business.value.newUsers ?? 0, unit: '人' },
])

const orderRows = computed(() => [
  {
    key: 'waiting',
    label: '待接单',
    dotClass: 'dot-wait',
    count: orders.value.waitingOrders ?? 0,
    alert: true,
    query: { status: 2 },
  },
  {
    key: 'delivered',
    label: '待派送',
    dotClass: 'dot-accept',
    count: orders.value.deliveredOrders ?? 0,
    alert: false,
    query: { status: 3 },
  },
  {
    key: 'completed',
    label: '已完成',
    dotClass: 'dot-done',
    count: orders.value.completedOrders ?? 0,
    alert: false,
    query: { status: 5 },
  },
  {
    key: 'cancelled',
    label: '已取消',
    dotClass: 'dot-cancel',
    count: orders.value.cancelledOrders ?? 0,
    alert: false,
    query: { status: 6 },
  },
  {
    key: 'all',
    label: '全部订单',
    dotClass: 'dot-send',
    count: orders.value.allOrders ?? 0,
    alert: false,
    query: {},
  },
])

function goOrder(query) {
  router.push({ path: '/manage/order', query })
}

async function loadAll() {
  loading.value = true
  // 四个接口互不依赖，并发拉；任何一个失败都不该拖垮整页
  const results = await Promise.allSettled([
    businessData(),
    overviewOrders(),
    overviewDishes(),
    overviewSetmeals(),
  ])

  const [b, o, d, s] = results
  if (b.status === 'fulfilled' && b.value) business.value = b.value
  if (o.status === 'fulfilled' && o.value) orders.value = o.value
  if (d.status === 'fulfilled' && d.value) dishes.value = d.value
  if (s.status === 'fulfilled' && s.value) setmeals.value = s.value

  loading.value = false
}

// 有催单进来就把数据刷新一遍
watch(
  () => noticeStore.reminderTick,
  () => {
    loadAll()
  }
)

onMounted(loadAll)
</script>

<style scoped>
/* ---------------- 今日数据横带 ---------------- */
.stat-strip {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  overflow: hidden;
}

.stat-cell {
  padding: 18px 20px 16px;
  border-left: 1px solid var(--edge);
}

.stat-cell:first-child {
  border-left: none;
}

.stat-label {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: var(--text-3);
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin: 0;
  line-height: 1.1;
  color: var(--text);
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.stat-unit {
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--text-3);
}

/* ---------------- 下方两栏 ---------------- */
.dash-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
  align-items: start;
}

.dash-col {
  display: grid;
  gap: 16px;
}

.dash-col .panel + .panel {
  margin-top: 0;
}

.panel-body--flush {
  padding: 0;
}

/* 订单管理行：左边状态点，右边大数字 */
.mrow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 13px 20px;
  border: 0;
  border-top: 1px solid var(--edge);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 13.5px;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.mrow:first-child {
  border-top: none;
}

.mrow:hover {
  background: var(--surface-sunken);
}

.mrow-value {
  font-size: 19px;
  font-weight: 600;
  color: var(--text-2);
}

/* 待接单大于 0 时才高亮 */
.mrow.is-alert .mrow-value {
  color: var(--fire);
}

.mrow.is-alert:hover {
  background: var(--fire-soft);
}

/* ---------------- 菜品 / 套餐总览 ---------------- */
.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.split-cell {
  padding-left: 14px;
  border-left: 2px solid var(--teal);
}

.split-cell + .split-cell {
  border-left-color: var(--edge-strong);
}

.split-label {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--text-3);
}

.split-value {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.split-value.is-muted {
  color: var(--text-2);
}

@media (max-width: 1100px) {
  .stat-strip {
    grid-template-columns: repeat(3, 1fr);
  }

  .stat-cell:nth-child(3n + 1) {
    border-left: none;
  }

  .stat-cell:nth-child(n + 4) {
    border-top: 1px solid var(--edge);
  }

  .dash-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
