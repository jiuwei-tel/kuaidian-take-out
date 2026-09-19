<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { ORDER_ROOT } from '@/router'
import { useCustomerStore } from '@/stores/customer'

const route = useRoute()
const router = useRouter()
const customer = useCustomerStore()

// 吸底购物车条只在点餐主页出现，确认订单和订单列表不需要
const showCartBar = computed(() => route.path === ORDER_ROOT)

const panelOpen = ref(false)
watch(
  () => route.path,
  () => {
    panelOpen.value = false
  }
)

onMounted(() => {
  // 进来先把车里的东西拉回来，刷新页面也不丢
  if (customer.token) {
    customer.fetchCart().catch(() => {})
  }
})

function goCheckout() {
  if (!customer.cartCount) return
  panelOpen.value = false
  router.push(`${ORDER_ROOT}/checkout`)
}

async function onLogout() {
  try {
    await ElMessageBox.confirm('退出后要重新登录才能点餐', '要退出吗', {
      confirmButtonText: '退出',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  customer.logout()
  router.push(`${ORDER_ROOT}/login`)
}

// 购物车图标，内联画出来，不引图片
const CART_ICON = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="20" r="1.4"/><circle cx="18" cy="20" r="1.4"/><path d="M2 3h2.2l2.4 11.2a1.6 1.6 0 0 0 1.6 1.3h8.7a1.6 1.6 0 0 0 1.6-1.3L20 7H5"/></svg>`
</script>

<template>
  <div class="shell">
    <header class="nav">
      <div class="wrap nav-inner">
        <router-link class="brand" to="/">
          <span class="brand-mark" aria-hidden="true">筷</span>
          <span class="brand-word">筷点外卖</span>
        </router-link>

        <nav class="nav-links" aria-label="点餐导航">
          <router-link class="nav-link" :to="ORDER_ROOT" exact-active-class="is-on">菜单</router-link>
          <router-link class="nav-link" :to="`${ORDER_ROOT}/vouchers`" active-class="is-on">领券</router-link>
          <router-link class="nav-link" :to="`${ORDER_ROOT}/orders`" active-class="is-on">我的订单</router-link>
        </nav>

        <el-dropdown trigger="click" @command="onLogout">
          <button type="button" class="who">
            <span>{{ customer.userInfo?.username || '顾客' }}</span>
            <el-icon class="caret"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="shell-main" :class="{ 'has-cart-bar': showCartBar }">
      <router-view />
    </main>

    <div v-if="showCartBar" class="cart-dock">
      <transition name="panel">
        <div v-if="panelOpen && customer.cartList.length" class="cart-panel">
          <div class="wrap">
            <div class="cart-panel-head">
              <span class="panel-label">已选</span>
              <button type="button" class="link-btn" @click="customer.clearLocalCart()">清空</button>
            </div>
            <ul class="cart-lines">
              <li v-for="item in customer.cartList" :key="item.id" class="cart-line">
                <span class="line-name">{{ item.name }}</span>
                <span v-if="item.dishFlavor" class="line-flavor">{{ item.dishFlavor }}</span>
                <span class="leader" aria-hidden="true"></span>
                <span class="line-times num">×{{ item.number }}</span>
                <span class="line-sum num money">{{ (Number(item.amount) * item.number).toFixed(2) }}</span>
              </li>
            </ul>
          </div>
        </div>
      </transition>

      <div class="cart-bar">
        <div class="wrap cart-inner">
          <button
            type="button"
            class="cart-toggle"
            :disabled="!customer.cartList.length"
            @click="panelOpen = !panelOpen"
          >
            <span class="cart-icon" v-html="CART_ICON" aria-hidden="true"></span>
            <span v-if="customer.cartCount" class="cart-badge num">{{ customer.cartCount }}</span>
            <span class="cart-text">
              <template v-if="customer.cartCount">
                共 {{ customer.cartCount }} 件
                <b class="num money">{{ customer.cartAmount.toFixed(2) }}</b>
              </template>
              <template v-else>还没选菜</template>
            </span>
          </button>

          <button type="button" class="btn-checkout" :disabled="!customer.cartCount" @click="goCheckout">
            去结算
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--paper);
}

.wrap {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 顶栏沿用首页那条白底玻璃条 */
.nav {
  position: sticky;
  top: 0;
  z-index: 60;
  height: 62px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: saturate(1.5) blur(8px);
  border-bottom: 1px solid var(--edge);
}

.nav-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--text);
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--brand);
  color: var(--ink-900);
  font-size: 16px;
  font-weight: 800;
  line-height: 1;
}

.brand-word {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-right: auto;
}

.nav-link {
  padding: 7px 12px;
  border-radius: 7px;
  font-size: 14px;
  color: var(--text-2);
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-link:hover {
  background: var(--surface-sunken);
  color: var(--text);
}

.nav-link.is-on {
  background: var(--teal-soft);
  color: var(--teal);
  font-weight: 600;
}

.who {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 10px;
  border: 1px solid var(--edge);
  border-radius: 7px;
  background: var(--surface);
  color: var(--text-2);
  font-family: var(--font-sans);
  font-size: 14px;
  cursor: pointer;
}

.who:hover {
  border-color: var(--edge-strong);
  color: var(--text);
}

.caret {
  font-size: 12px;
}

.shell-main {
  flex: 1;
}

.shell-main.has-cart-bar {
  padding-bottom: 92px;
}

/* ---------------- 吸底购物车 ---------------- */
.cart-dock {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 55;
}

.cart-bar {
  height: 64px;
  background: var(--ink-900);
  color: var(--text-invert);
}

.cart-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 100%;
}

.cart-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 4px;
  border: 0;
  background: none;
  color: inherit;
  font-family: var(--font-sans);
  cursor: pointer;
}

.cart-toggle:disabled {
  cursor: default;
  opacity: 0.55;
}

.cart-icon {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
}

.cart-badge {
  display: inline-grid;
  place-items: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--brand);
  color: var(--ink-900);
  font-size: 12px;
  font-weight: 700;
}

.cart-text {
  font-size: 14px;
}

.cart-text b {
  margin-left: 2px;
  font-size: 17px;
  font-weight: 700;
}

.btn-checkout {
  height: 40px;
  margin-left: auto;
  padding: 0 26px;
  border: 0;
  border-radius: 8px;
  background: var(--teal);
  color: #fff;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-checkout:hover:not(:disabled) {
  background: var(--teal-dark);
}

.btn-checkout:disabled {
  background: var(--ink-700);
  color: var(--text-3);
  cursor: not-allowed;
}

.cart-panel {
  max-height: 42vh;
  overflow-y: auto;
  padding: 14px 0 16px;
  border-top: 1px solid var(--edge);
  background: var(--surface);
  box-shadow: var(--shadow-pop);
}

.cart-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--edge);
}

.panel-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
}

.link-btn {
  border: 0;
  background: none;
  color: var(--text-3);
  font-family: var(--font-sans);
  font-size: 13px;
  cursor: pointer;
}

.link-btn:hover {
  color: var(--fire);
}

.cart-lines {
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 菜名和价钱之间拉一条点线，和首页菜单区一个手法 */
.cart-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 9px 0;
}

.cart-line + .cart-line {
  border-top: 1px solid var(--edge);
}

.line-name {
  font-size: 14px;
  color: var(--text);
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
  font-size: 13px;
  color: var(--text-3);
}

.line-sum {
  min-width: 62px;
  font-size: 14px;
  text-align: right;
  color: var(--text);
}

.panel-enter-active,
.panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 860px) {
  .wrap {
    padding: 0 16px;
  }

  .brand-word {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .panel-enter-active,
  .panel-leave-active {
    transition: none;
  }
}
</style>
