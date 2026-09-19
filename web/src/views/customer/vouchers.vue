<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyVouchers, getVoucherList, seckillVoucher } from '@/api/voucher'

const available = ref([])
const mine = ref([])
const seckilling = ref(null) // 正在抢的那张券 id，防连点

/* 能抢的条件：没抢过、还有库存 */
function canSeckill(v) {
  return !v.got && Number(v.stock) > 0
}

function btnText(v) {
  if (v.got) return '已抢过'
  if (Number(v.stock) <= 0) return '已抢完'
  return seckilling.value === v.id ? '抢…' : '立即抢'
}

/* 我的券分三种状态：已使用 / 已过期 / 未使用 */
function mineState(v) {
  if (v.status === 1) return { text: '已使用', cls: 'is-used' }
  if (v.expired) return { text: '已过期', cls: 'is-expired' }
  return { text: '未使用', cls: 'is-unused' }
}

/* 后端给的时间是 yyyy-MM-dd HH:mm，列表里只留月日时分 */
function shortTime(s) {
  return s ? s.slice(5) : ''
}

const unusedCount = computed(
  () => mine.value.filter((v) => v.status === 0 && !v.expired).length
)

async function loadAll() {
  try {
    const [a, m] = await Promise.all([getVoucherList(), getMyVouchers()])
    available.value = a || []
    mine.value = m || []
  } catch {
    // 失败原因由请求层统一提示
  }
}

async function onSeckill(voucher) {
  if (!canSeckill(voucher) || seckilling.value) return
  seckilling.value = voucher.id
  try {
    await seckillVoucher(voucher.id)
    ElMessage.success('抢到了，下单时可以用')
    await loadAll()
  } catch {
    // 已抢过 / 已抢完 / 不在活动时间，后端会给具体文案，拦截器已提示
  } finally {
    seckilling.value = null
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="voucher-page">
    <h1 class="page-h">领券中心</h1>
    <p class="page-tip">
      券统一减 30 元，订单金额满 30 元可用。抢到之后在提交订单那一步选用。
      <template v-if="unusedCount">你有 <b class="num">{{ unusedCount }}</b> 张还没用。</template>
    </p>

    <section class="block">
      <h2 class="block-h">可抢的券</h2>

      <ul v-if="available.length" class="cards">
        <li
          v-for="v in available"
          :key="v.id"
          class="card"
          :class="{ 'is-out': !canSeckill(v) }"
        >
          <div class="face">
            <span class="face-num num">{{ Number(v.value).toFixed(0) }}</span>
            <span class="face-unit">元</span>
          </div>

          <div class="body">
            <p class="name">{{ v.name }}</p>
            <p class="cond">
              满 {{ Number(v.minAmount).toFixed(0) }} 元可用 · 剩余
              <b class="num">{{ v.stock }}</b> 张
            </p>
            <p class="time num">
              {{ shortTime(v.beginTime) }} 至 {{ shortTime(v.endTime) }}
            </p>
          </div>

          <div class="action">
            <button
              type="button"
              class="btn-take"
              :disabled="!canSeckill(v) || seckilling === v.id"
              @click="onSeckill(v)"
            >
              {{ btnText(v) }}
            </button>
          </div>
        </li>
      </ul>

      <p v-else class="empty">暂时没有可抢的券</p>
    </section>

    <section class="block">
      <h2 class="block-h">我的券</h2>

      <ul v-if="mine.length" class="cards">
        <li v-for="v in mine" :key="v.id" class="card is-mine" :class="mineState(v).cls">
          <div class="face">
            <span class="face-num num">{{ Number(v.value).toFixed(0) }}</span>
            <span class="face-unit">元</span>
          </div>

          <div class="body">
            <p class="name">{{ v.name }}</p>
            <p class="cond">满 {{ Number(v.minAmount).toFixed(0) }} 元可用</p>
            <p class="time num">
              领取于 {{ shortTime(v.createTime) }}
              <template v-if="v.usedTime"> · 用于 {{ shortTime(v.usedTime) }}</template>
            </p>
          </div>

          <div class="action">
            <span class="state">{{ mineState(v).text }}</span>
          </div>
        </li>
      </ul>

      <p v-else class="empty">还没抢过券，上面挑一张试试</p>
    </section>
  </div>
</template>

<style scoped>
.voucher-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 26px 24px 48px;
}

.page-h {
  margin: 0 0 8px;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.page-tip {
  margin: 0 0 26px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-2);
  text-wrap: balance;
}

.block + .block {
  margin-top: 30px;
}

.block-h {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  letter-spacing: 0.06em;
}

.cards {
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 券卡：左边面额、中间信息、右边动作 */
.card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--edge);
  border-radius: 8px;
  background: var(--surface);
  transition: border-color 0.15s ease;
}

.card + .card {
  margin-top: 10px;
}

.card:hover {
  border-color: var(--edge-strong);
}

.card.is-out {
  opacity: 0.55;
}

.face {
  display: flex;
  align-items: baseline;
  gap: 2px;
  min-width: 74px;
  padding-right: 16px;
  border-right: 1px dashed var(--edge-strong);
  color: var(--teal);
}

.face-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.face-unit {
  font-size: 12px;
}

.body {
  flex: 1;
  min-width: 0;
}

.name {
  margin: 0 0 4px;
  font-size: 14.5px;
  font-weight: 600;
}

.cond {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-2);
}

.time {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--text-3);
}

.action {
  flex-shrink: 0;
}

.btn-take {
  padding: 8px 18px;
  border: 1px solid var(--teal);
  border-radius: 6px;
  background: var(--teal);
  color: #fff;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-take:hover:not(:disabled) {
  background: var(--teal-dark);
  border-color: var(--teal-dark);
}

.btn-take:disabled {
  border-color: var(--edge);
  background: transparent;
  color: var(--text-3);
  cursor: not-allowed;
}

/* 我的券：状态用文字标出，不用彩色块 */
.state {
  font-size: 12.5px;
  color: var(--text-3);
}

.is-unused .state {
  color: var(--teal);
  font-weight: 600;
}

/* 用掉的和过期的整体压暗，一眼能和能用的区分开 */
.is-used,
.is-expired {
  background: transparent;
}

.is-used .face,
.is-expired .face {
  color: var(--text-3);
}

.is-used .name,
.is-expired .name {
  color: var(--text-2);
  font-weight: 500;
}

.empty {
  margin: 0;
  padding: 18px 0;
  font-size: 13px;
  color: var(--text-3);
}
</style>
