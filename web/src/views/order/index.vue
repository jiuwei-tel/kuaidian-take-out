<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-sub">按状态处理门店订单，待接单需要尽快响应</p>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
      </div>
    </div>

    <section class="panel">
      <!-- 状态分栏 -->
      <el-tabs v-model="activeTab" class="order-tabs" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :name="tab.name">
          <template #label>
            <span class="tab-label">
              {{ tab.label }}
              <em v-if="tab.badge > 0" class="tab-badge" :class="{ 'is-alert': tab.alert }">
                {{ tab.badge }}
              </em>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 筛选 -->
      <div class="filter-bar">
        <el-input
          v-model.trim="query.number"
          placeholder="订单号"
          clearable
          @keyup.enter="onSearch"
        />
        <el-input
          v-model.trim="query.phone"
          placeholder="手机号"
          clearable
          @keyup.enter="onSearch"
        />
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="下单开始时间"
          end-placeholder="下单结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="defaultTime"
          class="filter-range"
          unlink-panels
        />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        stripe
        :header-cell-style="{ padding: '10px 0' }"
      >
        <el-table-column prop="number" label="订单号" min-width="168">
          <template #default="{ row }">
            <span class="order-no">{{ row.number }}</span>
          </template>
        </el-table-column>

        <el-table-column label="收货人" min-width="130">
          <template #default="{ row }">
            <div class="cell-user">
              <span class="cell-user-name">{{ row.consignee || row.userName || '—' }}</span>
              <span class="cell-user-phone num">{{ row.phone || '—' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="收货地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || '—' }}</template>
        </el-table-column>

        <el-table-column label="订单金额" width="110" align="right">
          <template #default="{ row }">
            <span class="money num">{{ fmtMoney(row.amount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <span class="dot" :class="ORDER_STATUS_DOT[row.status]">
              {{ ORDER_STATUS_TEXT[row.status] || '未知' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">
            <span class="num muted">{{ fmtDateTime(row.orderTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="188" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" size="small" @click="openDetail(row)">
                详情
              </el-button>

              <el-button
                v-if="row.status === 2"
                link
                type="primary"
                size="small"
                @click="onConfirm(row)"
              >
                接单
              </el-button>

              <el-button
                v-if="row.status === 3"
                link
                type="primary"
                size="small"
                @click="onDelivery(row)"
              >
                派送
              </el-button>

              <el-button
                v-if="row.status === 4"
                link
                type="primary"
                size="small"
                @click="onComplete(row)"
              >
                完成
              </el-button>

              <el-button
                v-if="canCancel(row.status)"
                link
                type="danger"
                size="small"
                @click="onCancel(row)"
              >
                取消
              </el-button>

              <el-button
                v-if="row.status === 2"
                link
                type="danger"
                size="small"
                @click="onReject(row)"
              >
                拒单
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前条件下没有订单" :image-size="88" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </section>

    <!-- 订单详情 -->
    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="620px"
      :close-on-click-modal="true"
    >
      <div v-loading="detailLoading" class="detail">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              <p class="detail-label">订单号</p>
              <p class="order-no detail-no">{{ detail.number }}</p>
            </div>
            <span class="dot" :class="ORDER_STATUS_DOT[detail.status]">
              {{ ORDER_STATUS_TEXT[detail.status] || '未知' }}
            </span>
          </div>

          <hr class="perf" />

          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="下单时间">
              <span class="num">{{ fmtDateTime(detail.orderTime) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="预计送达">
              <span class="num">{{ fmtDateTime(detail.estimatedDeliveryTime) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">
              {{ PAY_METHOD_TEXT[detail.payMethod] || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              {{ PAY_STATUS_TEXT[detail.payStatus] ?? '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="收货人">
              {{ detail.consignee || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              <span class="num">{{ detail.phone || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ detail.address || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="订单备注" :span="2">
              {{ detail.remark || '无' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="2">
              {{ detail.cancelReason }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.rejectionReason" label="拒单原因" :span="2">
              {{ detail.rejectionReason }}
            </el-descriptions-item>
          </el-descriptions>

          <h4 class="detail-sub">菜品明细</h4>
          <el-table :data="detail.orderDetailList || []" size="small" border>
            <el-table-column label="菜品" min-width="170">
              <template #default="{ row }">
                <div class="dish-cell">
                  <el-image
                    v-if="row.image"
                    :src="row.image"
                    fit="cover"
                    class="dish-thumb"
                    :preview-src-list="[row.image]"
                    preview-teleported
                  >
                    <template #error>
                      <span class="dish-thumb-fallback" />
                    </template>
                  </el-image>
                  <span v-else class="dish-thumb dish-thumb--empty" />
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="口味" min-width="110">
              <template #default="{ row }">
                <span class="muted">{{ row.dishFlavor || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="70" align="right">
              <template #default="{ row }">
                <span class="num">× {{ row.number }}</span>
              </template>
            </el-table-column>
            <el-table-column label="小计" width="90" align="right">
              <template #default="{ row }">
                <span class="money num">{{ fmtMoney(row.amount) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="detail-total">
            <span>合计</span>
            <span class="money num detail-amount">{{ fmtMoney(detail.amount) }}</span>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  cancelOrder,
  completeOrder,
  conditionSearch,
  confirmOrder,
  deliveryOrder,
  orderDetail,
  orderStatistics,
  rejectionOrder,
} from '@/api/order'
import {
  ORDER_STATUS_DOT,
  ORDER_STATUS_TEXT,
  PAY_METHOD_TEXT,
  PAY_STATUS_TEXT,
} from '@/constants'
import { fmtDateTime, fmtMoney } from '@/utils/format'

const route = useRoute()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const activeTab = ref('2')

const stats = reactive({
  toBeConfirmed: 0,
  confirmed: 0,
  deliveryInProgress: 0,
})

const query = reactive({
  page: 1,
  pageSize: 10,
  number: '',
  phone: '',
})

const timeRange = ref([])
const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59),
]

// 注意「待派送」在后端是 status=3（已接单），不是单独的枚举值
const tabs = computed(() => [
  { name: 'all', label: '全部', badge: 0 },
  { name: '1', label: '待付款', badge: 0 },
  { name: '2', label: '待接单', badge: stats.toBeConfirmed, alert: true },
  { name: '3', label: '待派送', badge: stats.confirmed },
  { name: '4', label: '派送中', badge: stats.deliveryInProgress },
  { name: '5', label: '已完成', badge: 0 },
  { name: '6', label: '已取消', badge: 0 },
])

function canCancel(status) {
  return [1, 2, 3, 4].includes(status)
}

function buildParams() {
  return {
    page: query.page,
    pageSize: query.pageSize,
    number: query.number || undefined,
    phone: query.phone || undefined,
    status: activeTab.value === 'all' ? undefined : Number(activeTab.value),
    beginTime: timeRange.value?.[0] || undefined,
    endTime: timeRange.value?.[1] || undefined,
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await conditionSearch(buildParams())
    // PageResult { total, records }
    total.value = Number(data?.total || 0)
    list.value = data?.records || []
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data = await orderStatistics()
    stats.toBeConfirmed = data?.toBeConfirmed ?? 0
    stats.confirmed = data?.confirmed ?? 0
    stats.deliveryInProgress = data?.deliveryInProgress ?? 0
  } catch {
    // 角标拿不到不影响列表使用
  }
}

function refreshAll() {
  loadList()
  loadStats()
}

function onTabChange() {
  query.page = 1
  loadList()
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  query.number = ''
  query.phone = ''
  timeRange.value = []
  query.page = 1
  loadList()
}

/* ---------------- 订单动作 ---------------- */
async function onConfirm(row) {
  try {
    await ElMessageBox.confirm(
      `确认接单？订单号 ${row.number}`,
      '接单确认',
      { type: 'info', confirmButtonText: '确认接单', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    // OrdersConfirmDTO { id, status }，接单即把状态推到 3 已接单
    await confirmOrder({ id: row.id, status: 3 })
    ElMessage.success('已接单')
    refreshAll()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onDelivery(row) {
  try {
    await ElMessageBox.confirm(
      `确认派送？订单号 ${row.number}`,
      '派送确认',
      { type: 'info', confirmButtonText: '确认派送', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deliveryOrder(row.id)
    ElMessage.success('已开始派送')
    refreshAll()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onComplete(row) {
  try {
    await ElMessageBox.confirm(
      `确认订单已送达并完成？订单号 ${row.number}`,
      '完成订单',
      { type: 'info', confirmButtonText: '确认完成', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await completeOrder(row.id)
    ElMessage.success('订单已完成')
    refreshAll()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onCancel(row) {
  let value
  try {
    const result = await ElMessageBox.prompt(
      `订单号 ${row.number}`,
      '取消订单',
      {
        type: 'warning',
        confirmButtonText: '确认取消订单',
        cancelButtonText: '返回',
        inputPlaceholder: '请输入取消原因',
        inputValidator: (v) => (v && v.trim() ? true : '取消原因不能为空'),
      }
    )
    value = result.value
  } catch {
    return
  }

  try {
    await cancelOrder({ id: row.id, cancelReason: value.trim() })
    ElMessage.success('订单已取消')
    refreshAll()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onReject(row) {
  let value
  try {
    const result = await ElMessageBox.prompt(
      `订单号 ${row.number}`,
      '拒单',
      {
        type: 'warning',
        confirmButtonText: '确认拒单',
        cancelButtonText: '返回',
        inputPlaceholder: '请输入拒单原因',
        inputValidator: (v) => (v && v.trim() ? true : '拒单原因不能为空'),
      }
    )
    value = result.value
  } catch {
    return
  }

  try {
    await rejectionOrder({ id: row.id, rejectionReason: value.trim() })
    ElMessage.success('已拒单')
    refreshAll()
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 详情 ---------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await orderDetail(row.id)
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  // 支持从工作台/来单提醒带参数跳进来
  const { status, number } = route.query
  if (status !== undefined && status !== null && status !== '') {
    activeTab.value = String(status)
  }
  if (number) {
    query.number = String(number)
  }
  query.page = 1
  refreshAll()
})
</script>

<style scoped>
.order-tabs {
  padding: 4px 16px 0;
}

.order-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.order-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--edge);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-badge {
  display: inline-grid;
  place-items: center;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--edge);
  font-family: var(--font-mono);
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  line-height: 1;
  color: var(--text-2);
}

/* 待接单角标用灶火色，和页面上的「需要动作」保持同一套语言 */
.tab-badge.is-alert {
  background: var(--fire);
  color: #fff;
}

.filter-range {
  width: 380px !important;
}

.cell-user {
  display: flex;
  flex-direction: column;
  line-height: 1.45;
}

.cell-user-name {
  color: var(--text);
}

.cell-user-phone {
  font-size: 12px;
  color: var(--text-3);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 8px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* ---------------- 详情 ---------------- */
.detail {
  min-height: 120px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-label {
  margin: 0 0 2px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-3);
}

.detail-no {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
}

.detail-sub {
  margin: 20px 0 10px;
  font-size: 13px;
  font-weight: 600;
}

.dish-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dish-thumb {
  width: 30px;
  height: 30px;
  border-radius: 3px;
  flex-shrink: 0;
  background: var(--paper);
}

.dish-thumb--empty {
  display: inline-block;
  border: 1px dashed var(--edge-strong);
}

/* 图片加载失败时，把 el-image 默认的「加载失败」文案换成与「无图」一致的虚线占位 */
.dish-thumb-fallback {
  display: block;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border: 1px dashed var(--edge-strong);
}

.detail-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--edge-strong);
  font-size: 13px;
  color: var(--text-2);
}

.detail-amount {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

@media (max-width: 1100px) {
  .filter-range {
    width: 100% !important;
  }
}
</style>
