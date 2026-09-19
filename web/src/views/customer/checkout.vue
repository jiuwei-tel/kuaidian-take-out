<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  getAddressList,
  getDefaultAddress,
  payOrder,
  submitOrder,
} from '@/api/customer'
import { getMyVouchers } from '@/api/voucher'
import { ORDER_ROOT } from '@/router'
import { useCustomerStore } from '@/stores/customer'

const router = useRouter()
const customer = useCustomerStore()

const address = ref(null)
const remark = ref('')
const submitting = ref(false)

const lines = computed(() => customer.cartList)
const total = computed(() => customer.cartAmount)

/* ---------------- 优惠券 ----------------
   只挑能用的：没被用过、没过期、金额够门槛。
   一张能用的都没有，这一块整块不渲染。 */
const myVouchers = ref([])
const selectedId = ref(null)

const usableVouchers = computed(() =>
  myVouchers.value.filter(
    (v) => v.status === 0 && !v.expired && Number(total.value) >= Number(v.minAmount || 0)
  )
)

const selectedVoucher = computed(
  () => usableVouchers.value.find((v) => v.id === selectedId.value) || null
)

const deduct = computed(() => (selectedVoucher.value ? Number(selectedVoucher.value.value) : 0))
const payAmount = computed(() => Math.max(0, Number(total.value) - deduct.value))

function toggleVoucher(id) {
  // 再点一下就是取消，一单只用一张
  selectedId.value = selectedId.value === id ? null : id
}

async function loadVouchers() {
  try {
    myVouchers.value = (await getMyVouchers()) || []
  } catch {
    myVouchers.value = []
  }
}

async function loadAddress() {
  try {
    let addr = await getDefaultAddress()
    if (!addr) {
      // 没设默认地址就退一步取列表第一条
      const list = await getAddressList()
      addr = (list || [])[0] || null
    }
    address.value = addr
  } catch {
    address.value = null
  }
}

async function onSubmit() {
  if (!lines.value.length) {
    ElMessage.warning('购物车是空的')
    return
  }
  if (!address.value) {
    ElMessage.warning('没有可用的收货地址，没法下单')
    return
  }

  submitting.value = true
  try {
    const payload = {
      addressBookId: address.value.id,
      payMethod: 1,
      remark: remark.value,
      // 入参这一项要带秒，和接口出参的格式不一样
      estimatedDeliveryTime: dayjs().add(30, 'minute').format('YYYY-MM-DD HH:mm:ss'),
      deliveryStatus: 1,
      tablewareNumber: 1,
      tablewareStatus: 1,
      packAmount: 0,
      // 送过去的是购物车原价，券的抵扣由后端算（后端按原价校验门槛）
      amount: Number(total.value.toFixed(2)),
      // 用券就带上领取记录 id；不用券不带这个字段
      voucherId: selectedVoucher.value ? selectedVoucher.value.id : undefined,
    }

    const order = await submitOrder(payload)

    // 后端这边是模拟支付，调完订单直接变「待接单」
    await payOrder({ orderNumber: order.orderNumber, payMethod: 1 })

    // 下单成功后端已经把购物车清空了，本地跟着清一下就行
    customer.clearLocalCart()

    await ElMessageBox.alert(`订单号 ${order.orderNumber}`, '下单成功，商家已收到', {
      confirmButtonText: '去看订单',
      showClose: false,
      type: 'success',
    }).catch(() => {})

    router.replace(`${ORDER_ROOT}/orders`)
  } catch {
    // 失败原因由 axios 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAddress()
  loadVouchers()
})
</script>

<template>
  <div class="checkout-page">
    <h1 class="page-h">确认订单</h1>

    <div class="sheet-wrap">
      <div class="sheet receipt">
        <section class="block">
          <p class="block-label">送至</p>
          <template v-if="address">
            <p class="addr-line">
              <span class="addr-name">{{ address.consignee }}</span>
              <span class="addr-phone num">{{ address.phone }}</span>
            </p>
            <p class="addr-detail">
              {{ address.provinceName }}{{ address.cityName }}{{ address.districtName }}{{ address.detail }}
            </p>
          </template>
          <p v-else class="addr-empty">暂时没有可用的收货地址</p>
        </section>

        <hr class="perf" />

        <section class="block">
          <p class="block-label">明细</p>
          <ul class="lines">
            <li v-for="item in lines" :key="item.id" class="line">
              <span class="line-name">{{ item.name }}</span>
              <span v-if="item.dishFlavor" class="line-flavor">{{ item.dishFlavor }}</span>
              <span class="leader" aria-hidden="true"></span>
              <span class="line-times num">×{{ item.number }}</span>
              <span class="line-sum num money">{{ (Number(item.amount) * item.number).toFixed(2) }}</span>
            </li>
          </ul>
          <p v-if="!lines.length" class="empty">购物车是空的，先回菜单挑两个菜</p>
        </section>

        <template v-if="usableVouchers.length">
          <hr class="perf" />

          <section class="block">
            <p class="block-label">优惠券</p>
            <ul class="vouchers">
              <li
                v-for="v in usableVouchers"
                :key="v.id"
                class="voucher"
                :class="{ 'is-on': selectedId === v.id }"
                @click="toggleVoucher(v.id)"
              >
                <span class="voucher-value num">-¥{{ Number(v.value).toFixed(2) }}</span>
                <span class="voucher-name">{{ v.name }}</span>
                <span class="voucher-cond">满 {{ Number(v.minAmount).toFixed(0) }} 元可用</span>
                <span class="voucher-pick">{{ selectedId === v.id ? '已选' : '选用' }}</span>
              </li>
            </ul>
          </section>
        </template>

        <hr class="perf" />

        <section class="block">
          <p class="block-label">备注</p>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            maxlength="80"
            show-word-limit
            placeholder="口味、餐具之类的说明（选填）"
          />
        </section>

        <hr class="perf" />

        <div class="total-row">
          <span class="total-label">商品金额</span>
          <span class="num money">{{ total.toFixed(2) }}</span>
        </div>

        <div v-if="selectedVoucher" class="total-row is-deduct">
          <span class="total-label">优惠券</span>
          <span class="num deduct-num">-¥{{ deduct.toFixed(2) }}</span>
        </div>

        <div class="total-row is-final">
          <span class="total-label">实付</span>
          <b class="total-num num money">{{ payAmount.toFixed(2) }}</b>
        </div>
      </div>
    </div>

    <div class="actions">
      <button type="button" class="btn-ghost" @click="router.back()">再改改</button>
      <button
        type="button"
        class="btn-primary"
        :disabled="submitting || !lines.length || !address"
        @click="onSubmit"
      >
        {{ submitting ? '提交中…' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.checkout-page {
  max-width: 620px;
  margin: 0 auto;
  padding: 26px 24px 40px;
}

.page-h {
  margin: 0 0 20px;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

/* 小票上下有锯齿，外面留出空间 */
.sheet-wrap {
  padding: 10px 0;
}

.sheet {
  padding: 22px 24px;
}

.block-label {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--text-3);
  letter-spacing: 0.06em;
}

.addr-line {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 0;
}

.addr-name {
  font-size: 15px;
  font-weight: 600;
}

.addr-phone {
  font-size: 13px;
  color: var(--text-2);
}

.addr-detail {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-2);
  text-wrap: balance;
}

.addr-empty {
  margin: 0;
  font-size: 13px;
  color: var(--fire);
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
  padding: 8px 0;
}

.line-name {
  font-size: 14px;
  color: var(--text);
}

.line-flavor {
  font-size: 12px;
  color: var(--text-3);
}

/* 菜名和价钱之间的点线，沿用首页菜单区的手法 */
.leader {
  flex: 1;
  min-width: 20px;
  border-bottom: 1px dotted var(--edge-strong);
  transform: translateY(-3px);
}

.line-times {
  font-size: 13px;
  color: var(--text-3);
}

.line-sum {
  min-width: 64px;
  font-size: 14px;
  text-align: right;
}

.empty {
  margin: 4px 0;
  font-size: 13px;
  color: var(--text-3);
}

.total-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.total-row + .total-row {
  margin-top: 8px;
}

/* 券抵扣和实付单独标出来，别和商品金额糊在一起 */
.is-deduct .total-label,
.deduct-num {
  color: var(--teal);
}

.is-final {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--edge);
}

.total-label {
  font-size: 14px;
  font-weight: 600;
}

.total-num {
  font-size: 22px;
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-ghost,
.btn-primary {
  padding: 12px 26px;
  border-radius: 8px;
  font-family: var(--font-sans);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-ghost {
  border: 1px solid var(--edge-strong);
  background: var(--surface);
  color: var(--text-2);
}

.btn-ghost:hover {
  border-color: var(--teal);
  color: var(--teal);
}

.btn-primary {
  border: 1px solid var(--teal);
  background: var(--teal);
  color: #fff;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  background: var(--teal-dark);
  border-color: var(--teal-dark);
}

.btn-primary:disabled {
  border-color: var(--edge);
  background: var(--edge);
  color: var(--text-3);
  cursor: not-allowed;
}

/* ---------------- 优惠券 ---------------- */

.vouchers {
  margin: 0;
  padding: 0;
  list-style: none;
}

.voucher {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 9px 12px;
  border: 1px dashed var(--edge-strong);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.voucher + .voucher {
  margin-top: 8px;
}

.voucher:hover {
  border-color: var(--teal);
}

.voucher.is-on {
  border-style: solid;
  border-color: var(--teal);
  background: rgba(30, 90, 78, 0.05);
}

.voucher-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--teal);
}

.voucher-name {
  font-size: 13.5px;
}

.voucher-cond {
  flex: 1;
  font-size: 12px;
  color: var(--text-3);
}

.voucher-pick {
  font-size: 12px;
  color: var(--teal);
}

.is-on .voucher-pick {
  font-weight: 600;
}

:deep(.perf) {
  margin: 18px 0;
}
</style>
