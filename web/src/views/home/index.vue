<template>
  <div class="home">
    <!-- ================= 顶部导航 ================= -->
    <header class="nav">
      <div class="wrap nav-inner">
        <a class="brand" href="#" @click.prevent="scrollTo('top')">
          <span class="brand-mark" aria-hidden="true">筷</span>
          <span class="brand-word">筷点外卖</span>
        </a>

        <nav class="nav-links" aria-label="页面导航">
          <button type="button" class="nav-link" @click="scrollTo('steps')">三步送到</button>
          <button type="button" class="nav-link" @click="scrollTo('menu')">今日菜单</button>
          <button type="button" class="nav-link" @click="scrollTo('why')">为什么选筷点</button>
        </nav>

        <div class="nav-actions">
          <!-- 管理工作台入口收在右上角，未登录先带去登录页 -->
          <el-dropdown trigger="click" @command="goAdmin">
            <button type="button" class="nav-admin">
              <span>管理后台</span>
              <el-icon class="nav-admin-caret"><ArrowDown /></el-icon>
            </button>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="m in adminMenus" :key="m.path" :command="m.path">
                  {{ m.title }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button type="button" class="btn btn-ink btn-sm" @click="goOrder()">
            立即点餐
          </button>
        </div>
      </div>
    </header>

    <!-- ================= 头图区 ================= -->
    <section id="top" class="hero">
      <div class="wrap hero-inner">
        <div class="hero-copy">
          <h1>今天吃什么，<br />筷点一下。</h1>

          <p class="hero-sub">
            现炒现做，平均 28 分钟送到你手上。门店超时自动赔付，配送进度全程可查，
            不用反复问「到哪了」。
          </p>

          <div class="hero-cta">
            <button type="button" class="btn btn-ink" @click="goOrder()">立即点餐</button>
            <button type="button" class="btn btn-ghost" @click="goAdmin(dashboardPath)">
              商家后台
            </button>
          </div>

          <dl class="promises">
            <div class="promise">
              <dt class="num">28 分钟</dt>
              <dd>平均送达</dd>
            </div>
            <div class="promise">
              <dt>现炒现做</dt>
              <dd>不用预制菜</dd>
            </div>
            <div class="promise">
              <dt>超时必赔</dt>
              <dd>系统自动退</dd>
            </div>
          </dl>
        </div>

        <!-- 用 CSS 画一台手机，直接演示下单到配送的过程；不放任何图片文件 -->
        <div class="hero-visual" aria-hidden="true">
          <span class="plate"></span>

          <div class="phone">
            <div class="phone-screen">
              <div class="scr-status">
                <span class="num">12:36</span>
                <span class="scr-bars"><i></i><i></i><i></i></span>
              </div>

              <div class="scr-shop">
                <span class="scr-mark">筷</span>
                <div class="scr-shop-text">
                  <p class="scr-shop-name">城南店</p>
                  <p class="scr-shop-eta num">预计 12:58 送达</p>
                </div>
              </div>

              <ol class="scr-track">
                <li class="is-done"><span class="scr-dot"></span>已下单</li>
                <li class="is-done"><span class="scr-dot"></span>已出餐</li>
                <li class="is-now"><span class="scr-dot"></span>配送中</li>
              </ol>

              <p class="scr-rider">
                <span class="ico" v-html="ICONS.scooter"></span>
                骑手已取餐，距你 <b class="num">320</b> 米
              </p>

              <ul class="scr-dishes">
                <li v-for="d in phoneDishes" :key="d.name">
                  <span class="thumb" :style="{ background: d.bg, color: d.fg }" v-html="ICONS[d.icon]"></span>
                  <span class="scr-dish-name">{{ d.name }}</span>
                  <span class="scr-dish-qty num">×{{ d.qty }}</span>
                </li>
              </ul>

              <div class="scr-foot">
                <span class="scr-total">合计<b class="num">¥ 90</b></span>
                <span class="scr-btn">去付款</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= 三步 ================= -->
    <section id="steps" class="band">
      <div class="wrap">
        <div class="sec-head">
          <h2 class="sec-title">从点单到敲门，只有三步</h2>
          <p class="sec-sub">
            订单从顾客手机直达后厨打印机，中间不经人手转抄。
          </p>
        </div>

        <ol class="steps-grid">
          <li v-for="(s, i) in steps" :key="s.title" class="step">
            <span class="step-no num">{{ String(i + 1).padStart(2, '0') }}</span>
            <h3 class="step-title">{{ s.title }}</h3>
            <p class="step-desc">{{ s.desc }}</p>
          </li>
        </ol>
      </div>
    </section>

    <!-- ================= 今日菜单 ================= -->
    <section id="menu" class="band band-white">
      <div class="wrap menu-grid">
        <div class="menu-aside">
          <h2 class="sec-title">今天想吃点什么</h2>
          <p class="sec-sub">
            门店菜单按实际供应实时更新，价格、口味、图片和商家后台里看到的完全一致。
          </p>
          <p class="menu-note">
            以下为示例菜单，实际供应以门店当日为准。点击「立即点餐」即可开始下单。
          </p>
          <button type="button" class="btn btn-brand" @click="goOrder()">立即点餐</button>
        </div>

        <ul class="menu-list">
          <li v-for="d in menu" :key="d.name" class="menu-row">
            <div class="menu-line">
              <span class="dish-name">{{ d.name }}</span>
              <span class="leader"></span>
              <span class="dish-price num">{{ d.price }}</span>
            </div>
            <p class="dish-note">{{ d.note }}</p>
          </li>
        </ul>
      </div>
    </section>

    <!-- ================= 为什么 ================= -->
    <section id="why" class="band">
      <div class="wrap">
        <div class="sec-head">
          <h2 class="sec-title">为什么是筷点</h2>
        </div>

        <div class="why-grid">
          <div v-for="w in whys" :key="w.title" class="why-item">
            <span class="why-ico" v-html="ICONS[w.icon]"></span>
            <h3 class="why-title">{{ w.title }}</h3>
            <p class="why-desc">{{ w.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ================= 结尾 ================= -->
    <section class="closing">
      <div class="wrap closing-inner">
        <div>
          <h2>饿了就别忍。</h2>
          <p>现在下单，最快 28 分钟送到门口。</p>
        </div>

        <div class="closing-cta">
          <button type="button" class="btn btn-brand" @click="goOrder()">立即点餐</button>
          <button type="button" class="btn btn-line" @click="goAdmin(dashboardPath)">
            打开管理后台
          </button>
        </div>
      </div>
    </section>

    <!-- ================= 页脚 ================= -->
    <footer class="foot">
      <div class="wrap">
        <div class="foot-grid">
          <div>
            <div class="foot-brand">
              <span class="brand-mark" aria-hidden="true">筷</span>
              <span class="foot-word">筷点外卖</span>
            </div>
            <p class="foot-slogan">现炒现做，筷点就到。</p>
          </div>

          <div>
            <h3>到店与配送</h3>
            <ul>
              <li>营业时间 10:00 – 22:00</li>
              <li>配送范围 3 公里</li>
              <li>支持微信支付</li>
            </ul>
          </div>

          <div>
            <h3>商家入口</h3>
            <ul>
              <li>
                <button type="button" class="foot-link" @click="goAdmin(dashboardPath)">
                  管理后台登录
                </button>
              </li>
              <li>
                <button type="button" class="foot-link" @click="goAdmin(`${ADMIN_ROOT}/order`)">
                  订单管理
                </button>
              </li>
              <li>
                <button type="button" class="foot-link" @click="goAdmin(`${ADMIN_ROOT}/statistics`)">
                  经营数据
                </button>
              </li>
            </ul>
          </div>
        </div>

        <div class="foot-bottom">
          <span>© 2026 筷点外卖</span>
          <span>本站为演示项目，非真实商户</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ADMIN_ROOT, ORDER_ROOT } from '@/router'
import { getToken } from '@/utils/auth'

const router = useRouter()
const dashboardPath = `${ADMIN_ROOT}/dashboard`

/* --------------------------------------------------------------------------
   管理后台入口：从路由表里长出来，和侧边栏共用一份菜单，避免两处维护
   -------------------------------------------------------------------------- */
const adminMenus = computed(() => {
  const root = router.options.routes.find((r) => r.path === ADMIN_ROOT)
  return (root?.children || []).map((child) => ({
    path: `${ADMIN_ROOT}/${child.path}`,
    title: child.meta?.title || child.path,
  }))
})

function goAdmin(path) {
  if (!path) return
  if (getToken()) {
    router.push(path)
    return
  }
  // 没登录就带着回跳地址去登录页，登录后直接落到该模块
  router.push({ path: '/login', query: { redirect: path } })
}

// 进点餐端。没登录的话，路由守卫会把人带到顾客登录页
function goOrder() {
  router.push(ORDER_ROOT)
}

/* --------------------------------------------------------------------------
   站内滚动
   注意：路由用的是 hash 模式，所以锚点只能用 JS 滚，不能用 href="#xxx"，
   否则会改写 URL 的 hash，被 vue-router 当成一次路由跳转。
   -------------------------------------------------------------------------- */
function scrollTo(id) {
  const el = document.getElementById(id)
  if (!el) return
  const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  el.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
}

/* --------------------------------------------------------------------------
   内容
   -------------------------------------------------------------------------- */
const steps = [
  { title: '选好菜，下单', desc: '菜单按门店当日供应实时更新，口味和价格一眼看清，不用来回问。' },
  { title: '后厨现炒出餐', desc: '下单直达后厨，热锅现做，出锅就打包，不让菜在窗口晾着。' },
  { title: '骑手送到门口', desc: '全程可查，超时系统自动赔付，漏送错送也不用找客服扯皮。' },
]

const menu = [
  { name: '宫保鸡丁', price: '28', note: '鸡腿肉、油酥花生、干辣椒' },
  { name: '鱼香肉丝', price: '26', note: '里脊丝、木耳、泡椒' },
  { name: '麻婆豆腐', price: '18', note: '嫩豆腐、牛肉末、花椒' },
  { name: '干煸四季豆', price: '16', note: '四季豆、肉末、芽菜' },
  { name: '蒜香排骨', price: '42', note: '肋排、蒜末、现炸' },
  { name: '酸梅汤', price: '8', note: '乌梅、山楂、冰镇' },
  { name: '米饭', price: '2', note: '东北珍珠米，一碗' },
]

const whys = [
  {
    icon: 'clock',
    title: '出餐快',
    desc: '门店覆盖 3 公里，平均 28 分钟送到，午饭不用等到下午。',
  },
  {
    icon: 'chef',
    title: '明厨亮灶',
    desc: '后厨开放，食材当天采买，现炒现做，不做预制复热。',
  },
  {
    icon: 'shield',
    title: '售后直接',
    desc: '漏送、错送、少送，符合条件一键退款，不用拍照举证。',
  },
]

// 手机屏幕上展示的订单
const phoneDishes = [
  { name: '宫保鸡丁', qty: 1, icon: 'fire', bg: '#fbe2d6', fg: '#d9480f' },
  { name: '蒜香排骨', qty: 1, icon: 'skewer', bg: '#f6e6d2', fg: '#a16207' },
  { name: '干煸四季豆', qty: 1, icon: 'leaf', bg: '#dfeee7', fg: '#1e5a4e' },
  { name: '米饭', qty: 2, icon: 'bowl', bg: '#ffe8c9', fg: '#b06a1a' },
]

/* --------------------------------------------------------------------------
   内联 SVG 图标（描边用 currentColor，颜色由 CSS 控制）
   -------------------------------------------------------------------------- */
const ICONS = {
  bowl: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 11h17"/><path d="M5 11c0 4.4 3.1 8 7 8s7-3.6 7-8"/><path d="M8.5 7.6c0-.9.9-1.3.9-2.2M12 7c0-1 .9-1.4.9-2.4M15.5 7.6c0-.9.9-1.3.9-2.2"/></svg>',
  fire: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3c1.6 2.2 1 4.4.5 6.5-.4 1.7-.2 3 1.3 3.4.4-1.5 1.2-2.4 2.2-3.1.6 1.3 1 2.6 1 4a5 5 0 0 1-10 0c0-3.6 3-5.2 5-10.8z"/></svg>',
  skewer: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4.5 19.5 20 4"/><circle cx="9.2" cy="14.8" r="2.3"/><circle cx="13" cy="11" r="2.3"/><circle cx="16.8" cy="7.2" r="2.3"/></svg>',
  leaf: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M5 19c0-7.2 4.6-12 14-12 0 8.2-4 13-11.4 13H5z"/><path d="M5 19c2.2-4 5.4-6.4 9.6-7.6"/></svg>',
  cup: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M7.4 8h9.2l-1.1 11.1a1.6 1.6 0 0 1-1.6 1.4h-3.8a1.6 1.6 0 0 1-1.6-1.4z"/><path d="M7 5.6c1.2-.9 8.8-.9 10 0"/><path d="M10.6 11.4v5.6M13.4 11.4v5.6"/></svg>',
  scooter: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="17" r="3"/><circle cx="18" cy="17" r="3"/><path d="M9 17h6"/><path d="M6 14V9.5h3.6L12.5 14"/><path d="M15.6 14 15 9h3"/></svg>',
  clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.4"/><path d="M12 7.4V12l3.2 2"/></svg>',
  chef: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M6.4 12.4a3.6 3.6 0 1 1 1.5-6.8 3.6 3.6 0 0 1 8.2 0 3.6 3.6 0 1 1 1.5 6.8"/><path d="M6.4 12.4V19h11.2v-6.6"/><path d="M6.4 15.7h11.2"/></svg>',
  shield: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.4l7 2.8v5.1c0 4.4-2.9 7.5-7 9.3-4.1-1.8-7-4.9-7-9.3V6.2z"/><path d="M9.2 12.1l2 2 3.7-3.8"/></svg>',
}
</script>

<style scoped>
/* ==========================================================================
   筷点外卖 · 首页
   借后台的「出单机」语言：发丝线分栏、点线引导符、等宽数字。
   只有头图区用品牌黄做满版铺色，其余留白，把胆量集中在一处。
   ========================================================================== */
.home {
  min-height: 100%;
  background: var(--paper);
}

.wrap {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 让浏览器把短段落断行拉匀，避免末行只剩一两个字 */
.hero-sub,
.sec-sub,
.step-desc,
.why-desc,
.menu-note,
.dish-note,
.closing p {
  text-wrap: balance;
}

/* ---------------- 按钮 ---------------- */
.btn {
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
  padding: 13px 24px;
  border: 1.5px solid transparent;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, transform 0.1s ease;
}

.btn:active {
  transform: translateY(1px);
}

.btn-sm {
  padding: 10px 16px;
  font-size: 14px;
  border-radius: 8px;
}

.btn-ink {
  background: var(--ink-900);
  color: #fff;
  box-shadow: 0 6px 18px rgba(14, 24, 23, 0.2);
}

.btn-ink:hover {
  background: var(--ink-800);
}

.btn-ghost {
  background: transparent;
  color: var(--ink-950);
  border-color: rgba(14, 24, 23, 0.3);
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.45);
  border-color: var(--ink-950);
}

.btn-brand {
  background: var(--brand);
  color: var(--ink-900);
}

.btn-brand:hover {
  background: #ffcd2b;
}

.btn-line {
  background: transparent;
  color: #e8efec;
  border-color: rgba(255, 255, 255, 0.26);
}

.btn-line:hover {
  border-color: rgba(255, 255, 255, 0.6);
}

/* ---------------- 导航 ---------------- */
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
  gap: 20px;
  height: 100%;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
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
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--ink-900);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-right: auto;
}

.nav-link {
  padding: 8px 12px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--text-2);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-link:hover {
  background: var(--paper);
  color: var(--ink-900);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.nav-admin {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  border: 1.5px solid var(--edge);
  border-radius: 8px;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--text);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.nav-admin:hover {
  background: var(--paper);
  border-color: var(--edge-strong);
}

.nav-admin-caret {
  font-size: 12px;
  color: var(--text-3);
}

/* ---------------- 头图区 ---------------- */
.hero {
  position: relative;
  overflow: hidden;
  background: var(--brand);
  color: var(--ink-950);
}

.hero-inner {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: clamp(28px, 5vw, 64px);
  align-items: center;
  padding-top: clamp(40px, 6.5vw, 80px);
  padding-bottom: clamp(48px, 7.5vw, 92px);
}

.hero h1 {
  margin: 0;
  font-size: clamp(30px, 5.2vw, 54px);
  font-weight: 800;
  line-height: 1.16;
  letter-spacing: -0.02em;
}

.hero-sub {
  max-width: 26em;
  margin: 20px 0 0;
  font-size: clamp(14px, 1.5vw, 16px);
  line-height: 1.85;
  color: #56430d;
}

.hero-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.promises {
  display: flex;
  margin: 36px 0 0;
}

.promise {
  padding: 0 22px;
  border-left: 1px solid rgba(14, 24, 23, 0.16);
}

.promise:first-child {
  padding-left: 0;
  border-left: 0;
}

.promise dt {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.promise dd {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: #5c4a11;
}

/* ---------------- 手机模型 ---------------- */
.hero-visual {
  position: relative;
  display: grid;
  place-items: center;
}

/* 圆盘 + 虚线圈装饰 */
.plate {
  position: absolute;
  width: min(420px, 88%);
  aspect-ratio: 1;
  border-radius: 50%;
  background: #ffd94d;
}

.plate::after {
  content: '';
  position: absolute;
  inset: -20px;
  border-radius: 50%;
  border: 1px dashed rgba(14, 24, 23, 0.2);
}

.phone {
  position: relative;
  width: 268px;
  padding: 9px;
  border-radius: 36px;
  background: var(--ink-950);
  box-shadow: 0 26px 60px rgba(14, 24, 23, 0.26);
  animation: rise 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.phone-screen {
  display: flex;
  flex-direction: column;
  height: 498px;
  border-radius: 28px;
  background: #fff;
  overflow: hidden;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.scr-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px 8px;
  font-size: 11px;
  color: var(--text-3);
}

.scr-bars {
  display: inline-flex;
  align-items: flex-end;
  gap: 2px;
}

.scr-bars i {
  width: 3px;
  border-radius: 1px;
  background: var(--text-3);
  opacity: 0.55;
}

.scr-bars i:nth-child(1) {
  height: 4px;
}
.scr-bars i:nth-child(2) {
  height: 7px;
}
.scr-bars i:nth-child(3) {
  height: 10px;
}

.scr-shop {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 16px 14px;
  border-bottom: 1px solid var(--edge);
}

.scr-mark {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--brand);
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
}

.scr-shop-text {
  min-width: 0;
}

.scr-shop-name {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-900);
}

.scr-shop-eta {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--text-3);
}

/* 三段进度条 */
.scr-track {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin: 0;
  padding: 16px 18px 14px;
  list-style: none;
  font-size: 10.5px;
  color: var(--text-3);
}

.scr-track::before {
  content: '';
  position: absolute;
  left: 49px;
  right: 49px;
  top: 21px;
  height: 2px;
  background: var(--edge);
}

/* 进度填充用伪元素画，<ol> 里只允许放 <li> */
.scr-track::after {
  content: '';
  position: absolute;
  left: 49px;
  right: 49px;
  top: 21px;
  height: 2px;
  background: var(--teal);
  transform-origin: left center;
  animation: grow 0.9s 0.3s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

@keyframes grow {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(0.66);
  }
}

.scr-track li {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 6px;
  width: 62px;
}

.scr-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--edge-strong);
}

.is-done {
  color: var(--text-2);
}

.is-done .scr-dot {
  background: var(--teal);
  border-color: var(--teal);
}

.is-now {
  color: var(--ink-900);
  font-weight: 600;
}

.is-now .scr-dot {
  background: var(--brand);
  border-color: var(--brand-deep);
  box-shadow: 0 0 0 4px rgba(255, 194, 0, 0.3);
}

.scr-rider {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 16px 12px;
  padding: 9px 11px;
  border-radius: 8px;
  background: var(--teal-soft);
  color: var(--teal-dark);
  font-size: 11.5px;
}

.scr-rider .ico {
  display: inline-flex;
  width: 14px;
  height: 14px;
}

.ico svg {
  width: 100%;
  height: 100%;
  display: block;
}

.scr-dishes {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 0 16px;
  list-style: none;
  overflow: hidden;
}

.scr-dishes li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--edge);
  font-size: 12.5px;
}

.scr-dishes li:last-child {
  border-bottom: 0;
}

.thumb {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
}

.thumb svg {
  width: 19px;
  height: 19px;
  display: block;
}

.scr-dish-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text);
}

.scr-dish-qty {
  font-size: 11.5px;
  color: var(--text-3);
}

.scr-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px 16px;
  border-top: 1px solid var(--edge);
  background: var(--surface-sunken);
}

.scr-total {
  display: grid;
  font-size: 11px;
  color: var(--text-3);
}

.scr-total b {
  font-size: 17px;
  font-weight: 700;
  color: var(--ink-900);
}

.scr-btn {
  padding: 9px 14px;
  border-radius: 8px;
  background: var(--brand);
  color: var(--ink-900);
  font-size: 12.5px;
  font-weight: 700;
}

/* ---------------- 通用区块 ---------------- */
.band {
  padding: clamp(52px, 7.5vw, 88px) 0;
}

.band-white {
  background: var(--surface);
  border-top: 1px solid var(--edge);
  border-bottom: 1px solid var(--edge);
}

.sec-title {
  margin: 0;
  font-size: clamp(22px, 3vw, 30px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--ink-900);
}

.sec-sub {
  max-width: 34em;
  margin: 12px 0 0;
  font-size: 14.5px;
  line-height: 1.85;
  color: var(--text-2);
}

.sec-head {
  max-width: 640px;
}

/* ---------------- 三步 ---------------- */
.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: clamp(20px, 3.4vw, 44px);
  margin: 46px 0 0;
  padding: 0;
  list-style: none;
}

.step {
  padding-top: 18px;
  border-top: 2px solid var(--ink-900);
}

.step-no {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--teal);
}

.step-title {
  margin: 12px 0 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink-900);
}

.step-desc {
  margin: 9px 0 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: var(--text-2);
}

/* ---------------- 菜单 ---------------- */
#menu {
  scroll-margin-top: 62px;
}

.menu-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.15fr);
  gap: clamp(28px, 5vw, 76px);
  align-items: start;
}

.menu-note {
  max-width: 30em;
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px dashed var(--edge-strong);
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--text-2);
}

.menu-aside .btn {
  margin-top: 24px;
}

.menu-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.menu-row {
  padding: 15px 12px;
  margin: 0 -12px;
  border-bottom: 1px solid var(--edge);
  border-radius: 8px;
  transition: background 0.15s ease;
}

.menu-row:last-child {
  border-bottom: 0;
}

.menu-row:hover {
  background: #fffaea;
}

.menu-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.dish-name {
  font-size: 15.5px;
  font-weight: 600;
  color: var(--ink-900);
  white-space: nowrap;
}

/* 菜名和价格之间用点线连接 */
.leader {
  flex: 1;
  min-width: 20px;
  margin-bottom: 5px;
  border-bottom: 1px dotted var(--edge-strong);
}

.dish-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-900);
}

.dish-price::before {
  content: '¥';
  margin-right: 2px;
  font-size: 0.72em;
  color: var(--text-2);
}

.dish-note {
  margin: 5px 0 0;
  font-size: 12.5px;
  color: var(--text-2);
}

/* ---------------- 为什么 ---------------- */
.why-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: clamp(24px, 4vw, 52px);
  margin-top: 44px;
}

.why-item {
  padding-left: 22px;
  border-left: 1px solid var(--edge);
}

.why-item:first-child {
  padding-left: 0;
  border-left: 0;
}

.why-ico {
  display: block;
  width: 30px;
  height: 30px;
  color: var(--teal);
}

.why-title {
  margin: 16px 0 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-900);
}

.why-desc {
  margin: 9px 0 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: var(--text-2);
}

/* ---------------- 结尾 ---------------- */
.closing {
  padding: clamp(44px, 6vw, 68px) 0;
  background: var(--ink-800);
  color: #fff;
}

.closing-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
}

.closing h2 {
  margin: 0;
  font-size: clamp(22px, 3vw, 30px);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.closing p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #93a5a0;
}

.closing-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

/* ---------------- 页脚 ---------------- */
.foot {
  background: var(--ink-950);
  color: #8fa19b;
  font-size: 13px;
}

.foot-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr;
  gap: 32px;
  padding: 46px 0 34px;
}

.foot-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.foot-word {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #e8efec;
}

.foot-slogan {
  margin: 12px 0 0;
  color: #7f938c;
}

.foot h3 {
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 600;
  color: #e8efec;
}

.foot ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 9px;
}

.foot-link {
  padding: 0;
  border: 0;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 13px;
  color: #b9c9c3;
  text-decoration: underline;
  text-decoration-color: rgba(255, 255, 255, 0.22);
  text-underline-offset: 3px;
  cursor: pointer;
  transition: color 0.15s ease, text-decoration-color 0.15s ease;
}

.foot-link:hover {
  color: var(--brand);
  text-decoration-color: currentColor;
}

.foot-bottom {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 0 28px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: #7f938c;
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1020px) {
  .hero-inner {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    margin-top: 12px;
  }

  .menu-grid {
    grid-template-columns: 1fr;
  }

  .steps-grid,
  .why-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .step {
    margin-bottom: 22px;
  }

  .step:last-child {
    margin-bottom: 0;
  }

  .why-item {
    padding: 20px 0 0;
    border-left: 0;
    border-top: 1px solid var(--edge);
  }

  .why-item:first-child {
    padding-top: 0;
    border-top: 0;
  }
}

@media (max-width: 860px) {
  .nav-links {
    display: none;
  }

  .foot-grid {
    grid-template-columns: 1fr 1fr;
    gap: 28px;
  }
}

@media (max-width: 560px) {
  .hero h1 {
    font-size: 32px;
  }

  .promises {
    flex-direction: column;
    gap: 14px;
  }

  .promise {
    padding: 12px 0 0;
    border-left: 0;
    border-top: 1px solid rgba(14, 24, 23, 0.14);
  }

  .promise:first-child {
    padding-top: 0;
    border-top: 0;
  }

  .hero-cta .btn {
    flex: 1;
    min-width: 140px;
  }

  .phone {
    width: 244px;
  }

  .phone-screen {
    height: 452px;
  }

  .foot-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 420px) {
  .nav .btn-sm {
    display: none;
  }
}
</style>

<!-- 下拉菜单被挂到 body 上，作用域样式管不到，单独开一个非 scoped 块 -->
<style>
.home-admin-popper {
  min-width: 160px;
}
</style>
