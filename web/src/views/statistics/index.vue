<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">数据统计</h2>
        <p class="page-sub">按日期区间查看营业额、用户、订单与销量排名</p>
      </div>
    </div>

    <!-- 日期区间 -->
    <section class="panel">
      <div class="range-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :clearable="false"
          unlink-panels
          @change="loadAll"
        />
        <div class="range-quick">
          <button
            v-for="preset in presets"
            :key="preset.label"
            type="button"
            class="quick-btn"
            :class="{ 'is-active': activePreset === preset.label }"
            @click="applyPreset(preset)"
          >
            {{ preset.label }}
          </button>
        </div>
      </div>
    </section>

    <!-- 营业额 -->
    <section class="panel">
      <header class="panel-head">
        <span class="panel-title">营业额统计</span>
        <span class="head-stat">
          区间合计
          <b class="money num">{{ fmtMoney(turnoverTotal) }}</b>
        </span>
      </header>
      <div class="panel-body">
        <div ref="turnoverRef" v-loading="loading" class="chart chart--lg"></div>
      </div>
    </section>

    <div class="chart-grid">
      <!-- 用户统计 -->
      <section class="panel">
        <header class="panel-head">
          <span class="panel-title">用户统计</span>
          <span class="head-stat">
            新增
            <b class="num">{{ userSummary.newTotal }}</b>
            人
          </span>
        </header>
        <div class="panel-body">
          <div ref="userRef" v-loading="loading" class="chart"></div>
        </div>
      </section>

      <!-- 订单统计 -->
      <section class="panel">
        <header class="panel-head">
          <span class="panel-title">订单统计</span>
          <span class="head-stat">
            完成率
            <b class="num">{{ fmtPercent(orderSummary.orderCompletionRate) }}</b>
          </span>
        </header>
        <div class="panel-body">
          <div class="order-mini">
            <div class="mini-cell">
              <span class="mini-label">订单总数</span>
              <span class="mini-value num">{{ orderSummary.totalOrderCount }}</span>
            </div>
            <div class="mini-cell">
              <span class="mini-label">有效订单</span>
              <span class="mini-value num">{{ orderSummary.validOrderCount }}</span>
            </div>
          </div>
          <div ref="orderRef" v-loading="loading" class="chart"></div>
        </div>
      </section>
    </div>

    <!-- 销量 Top10 -->
    <section class="panel">
      <header class="panel-head">
        <span class="panel-title">销量排名 Top10</span>
        <span class="head-stat">按销量降序</span>
      </header>
      <div class="panel-body">
        <div ref="top10Ref" v-loading="loading" class="chart chart--lg"></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import {
  ordersStatistics,
  salesTop10,
  turnoverStatistics,
  userStatistics,
} from '@/api/report'
import { fmtMoney, fmtPercent, splitList, splitNumList } from '@/utils/format'

const loading = ref(false)
const dateRange = ref([])

const presets = [
  { label: '近7天', days: 7 },
  { label: '近30天', days: 30 },
  { label: '本月', month: true },
]
const activePreset = ref('近7天')

const turnoverTotal = ref(0)
const userSummary = ref({ newTotal: 0 })
const orderSummary = ref({
  totalOrderCount: 0,
  validOrderCount: 0,
  orderCompletionRate: 0,
})

const turnoverRef = ref(null)
const userRef = ref(null)
const orderRef = ref(null)
const top10Ref = ref(null)

const charts = shallowRef({})

/* 图表配色与全站令牌一致 */
const C = {
  teal: '#1e5a4e',
  brand: '#ffc200',
  fire: '#d9480f',
  edge: '#e4e7e2',
  edgeSoft: '#f1f3ef',
  text2: '#5c6b66',
  text3: '#8a968f',
}

const axisLabel = { color: C.text3, fontSize: 11 }

function shortDates(list) {
  return list.map((d) => (d && d.length >= 10 ? d.slice(5) : d))
}

function baseOption(dates) {
  return {
    grid: { left: 8, right: 20, top: 28, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: C.edge } },
      backgroundColor: '#ffffff',
      borderColor: C.edge,
      textStyle: { color: '#1c2523', fontSize: 12 },
    },
    xAxis: {
      type: 'category',
      data: shortDates(dates),
      boundaryGap: false,
      axisLine: { lineStyle: { color: C.edge } },
      axisTick: { show: false },
      axisLabel,
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: C.edgeSoft } },
      axisLabel,
    },
  }
}

/** 主序列加一层很淡的面积 */
function areaSeries(name, data, color) {
  return {
    name,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 5,
    showSymbol: data.length <= 31,
    data,
    itemStyle: { color },
    lineStyle: { width: 2, color },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: hexToRgba(color, 0.22) },
        { offset: 1, color: hexToRgba(color, 0) },
      ]),
    },
  }
}

function hexToRgba(hex, alpha) {
  const n = parseInt(hex.replace('#', ''), 16)
  const r = (n >> 16) & 255
  const g = (n >> 8) & 255
  const b = n & 255
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function initCharts() {
  const map = {}
  if (turnoverRef.value) map.turnover = echarts.init(turnoverRef.value)
  if (userRef.value) map.user = echarts.init(userRef.value)
  if (orderRef.value) map.order = echarts.init(orderRef.value)
  if (top10Ref.value) map.top10 = echarts.init(top10Ref.value)
  charts.value = map
}

function resizeAll() {
  Object.values(charts.value).forEach((c) => c.resize())
}

function renderEmpty() {
  const empty = {
    title: {
      text: '暂无数据',
      left: 'center',
      top: 'middle',
      textStyle: { color: C.text3, fontSize: 13, fontWeight: 400 },
    },
    xAxis: { show: false },
    yAxis: { show: false },
  }
  charts.value.turnover?.clear()
  charts.value.turnover?.setOption(empty)
  charts.value.user?.clear()
  charts.value.user?.setOption(empty)
  charts.value.order?.clear()
  charts.value.order?.setOption(empty)
  charts.value.top10?.clear()
  charts.value.top10?.setOption(empty)
}

async function loadAll() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  const [begin, end] = dateRange.value

  loading.value = true
  try {
    const [turnover, users, orders, top10] = await Promise.all([
      turnoverStatistics(begin, end).catch(() => null),
      userStatistics(begin, end).catch(() => null),
      ordersStatistics(begin, end).catch(() => null),
      salesTop10(begin, end).catch(() => null),
    ])

    // ---- 营业额 ----
    const tDates = splitList(turnover?.dateList)
    const tValues = splitNumList(turnover?.turnoverList)
    turnoverTotal.value = tValues.reduce((a, b) => a + b, 0)

    // ---- 用户 ----
    const uDates = splitList(users?.dateList)
    const uTotal = splitNumList(users?.totalUserList)
    const uNew = splitNumList(users?.newUserList)
    userSummary.value = { newTotal: uNew.reduce((a, b) => a + b, 0) }

    // ---- 订单 ----
    const oDates = splitList(orders?.dateList)
    const oCount = splitNumList(orders?.orderCountList)
    const oValid = splitNumList(orders?.validOrderCountList)
    orderSummary.value = {
      totalOrderCount: orders?.totalOrderCount ?? 0,
      validOrderCount: orders?.validOrderCount ?? 0,
      orderCompletionRate: orders?.orderCompletionRate ?? 0,
    }

    // ---- Top10 ----
    const names = splitList(top10?.nameList)
    const numbers = splitNumList(top10?.numberList)

    await nextTick()

    if (!tDates.length && !uDates.length && !oDates.length && !names.length) {
      renderEmpty()
      return
    }

    const c = charts.value

    if (tDates.length) {
      c.turnover?.setOption(
        {
          ...baseOption(tDates),
          series: [areaSeries('营业额', tValues, C.teal)],
        },
        true
      )
    }

    if (uDates.length) {
      c.user?.setOption(
        {
          ...baseOption(uDates),
          legend: {
            right: 0,
            top: 0,
            icon: 'roundRect',
            itemWidth: 8,
            itemHeight: 8,
            textStyle: { color: C.text2, fontSize: 11 },
          },
          series: [
            areaSeries('用户总量', uTotal, C.teal),
            {
              ...areaSeries('新增用户', uNew, C.brand),
              areaStyle: { color: 'transparent' },
            },
          ],
        },
        true
      )
    }

    if (oDates.length) {
      c.order?.setOption(
        {
          ...baseOption(oDates),
          legend: {
            right: 0,
            top: 0,
            icon: 'roundRect',
            itemWidth: 8,
            itemHeight: 8,
            textStyle: { color: C.text2, fontSize: 11 },
          },
          series: [
            areaSeries('订单数', oCount, C.teal),
            {
              ...areaSeries('有效订单', oValid, C.brand),
              areaStyle: { color: 'transparent' },
            },
          ],
        },
        true
      )
    }

    if (names.length) {
      // 横向柱状图从下往上排，所以要反转，销量最高的才在最上面
      const rNames = [...names].reverse()
      const rNumbers = [...numbers].reverse()
      c.top10?.setOption(
        {
          grid: { left: 8, right: 52, top: 8, bottom: 4, containLabel: true },
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            backgroundColor: '#ffffff',
            borderColor: C.edge,
            textStyle: { color: '#1c2523', fontSize: 12 },
          },
          xAxis: {
            type: 'value',
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: C.edgeSoft } },
            axisLabel,
          },
          yAxis: {
            type: 'category',
            data: rNames,
            axisLine: { show: false },
            axisTick: { show: false },
            axisLabel: { color: C.text2, fontSize: 12 },
          },
          series: [
            {
              name: '销量',
              type: 'bar',
              data: rNumbers,
              barWidth: 13,
              itemStyle: {
                // 销量第一名用品牌黄，其余用主色
                color: (params) => (params.dataIndex === rNames.length - 1 ? C.brand : C.teal),
                borderRadius: [0, 3, 3, 0],
              },
              label: {
                show: true,
                position: 'right',
                color: C.text2,
                fontSize: 11,
                fontFamily: 'ui-monospace, Consolas, monospace',
              },
            },
          ],
        },
        true
      )
    }
  } finally {
    loading.value = false
  }
}

function setPresetRange(preset) {
  activePreset.value = preset.label
  if (preset.month) {
    dateRange.value = [
      dayjs().startOf('month').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ]
  } else {
    dateRange.value = [
      dayjs().subtract(preset.days - 1, 'day').format('YYYY-MM-DD'),
      dayjs().format('YYYY-MM-DD'),
    ]
  }
}

function applyPreset(preset) {
  setPresetRange(preset)
  loadAll()
}

onMounted(async () => {
  // 先把日期定下来，但此时图表容器还没 init，所以不在这里发请求
  setPresetRange(presets[0])
  await nextTick()
  initCharts()
  loadAll()
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  Object.values(charts.value).forEach((c) => c.dispose())
  charts.value = {}
})
</script>

<style scoped>
.range-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.range-quick {
  display: flex;
  gap: 6px;
}

.quick-btn {
  padding: 5px 12px;
  border: 1px solid var(--edge);
  border-radius: var(--radius-sm);
  background: var(--surface);
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--text-2);
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.quick-btn:hover {
  border-color: var(--teal);
  color: var(--teal);
}

.quick-btn.is-active {
  border-color: var(--teal);
  background: var(--teal);
  color: #fff;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.chart-grid .panel + .panel {
  margin-top: 0;
}

.chart-grid + .panel,
.chart-grid ~ .panel {
  margin-top: 16px;
}

.chart {
  width: 100%;
  height: 236px;
}

.chart--lg {
  height: 288px;
}

.head-stat {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  font-size: 12.5px;
  color: var(--text-3);
}

.head-stat b {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.head-stat b.money::before {
  content: '¥';
  font-size: 0.8em;
  margin-right: 1px;
  opacity: 0.7;
}

.order-mini {
  display: flex;
  gap: 28px;
  margin-bottom: 14px;
}

.mini-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-label {
  font-size: 12px;
  color: var(--text-3);
}

.mini-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

@media (max-width: 1100px) {
  .chart-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
