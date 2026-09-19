import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken, getUserToken } from '@/utils/auth'

/**
 * 用 hash 模式：部署到 Nginx / 直接开 dist 都不需要额外配 try_files，
 * 对课设项目来说少一个坑。
 *
 * 路由分三块：
 *   /            营销首页，公开访问，不套任何外壳
 *   /manage/**   商家管理后台，员工登录后进，套 Layout（侧边栏 + 顶栏）
 *   /order/**    用户点餐端，顾客登录后进，套 OrderLayout
 *
 * 两块后台都挂在自己的一级路径下，是为了让根路径 / 留给首页。
 * 侧边栏、首页的入口都从这两个常量推路径，别再各写一份。
 *
 * 注意登录态是两套：管理端认 sky_admin_token，点餐端认 sky_user_token。
 * 守卫里按路径前缀分开校验，两边的登录态互不干扰。
 */
export const ADMIN_ROOT = '/manage'
export const ORDER_ROOT = '/order'

const routes = [
  {
    // 首页不设 meta.title，页面标题在 afterEach 里单独给品牌 slogan
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { public: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true },
  },

  /* ---------------- 点餐端 ---------------- */
  {
    // 顾客登录页，独立成页，不套点餐端的外壳
    path: `${ORDER_ROOT}/login`,
    name: 'OrderLogin',
    component: () => import('@/views/customer/login/index.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: `${ORDER_ROOT}/register`,
    name: 'OrderRegister',
    component: () => import('@/views/customer/register.vue'),
    meta: { title: '注册', public: true },
  },
  {
    path: `${ORDER_ROOT}/forgot`,
    name: 'OrderForgot',
    component: () => import('@/views/customer/forgot.vue'),
    meta: { title: '重置密码', public: true },
  },
  {
    path: ORDER_ROOT,
    component: () => import('@/layout/OrderLayout.vue'),
    children: [
      {
        // 空 path：/order 本身就是点餐主页
        path: '',
        name: 'OrderMenu',
        component: () => import('@/views/customer/index.vue'),
        meta: { title: '点餐' },
      },
      {
        path: 'checkout',
        name: 'OrderCheckout',
        component: () => import('@/views/customer/checkout.vue'),
        meta: { title: '确认订单' },
      },
      {
        path: 'vouchers',
        name: 'OrderVouchers',
        component: () => import('@/views/customer/vouchers.vue'),
        meta: { title: '领券中心' },
      },
      {
        path: 'orders',
        name: 'OrderHistory',
        component: () => import('@/views/customer/orders.vue'),
        meta: { title: '我的订单' },
      },
    ],
  },

  /* ---------------- 商家管理后台 ---------------- */
  {
    path: ADMIN_ROOT,
    component: () => import('@/layout/index.vue'),
    redirect: `${ADMIN_ROOT}/dashboard`,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' },
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', icon: 'Tickets' },
      },
      {
        path: 'dish',
        name: 'Dish',
        component: () => import('@/views/dish/index.vue'),
        meta: { title: '菜品管理', icon: 'Food' },
      },
      {
        path: 'setmeal',
        name: 'Setmeal',
        component: () => import('@/views/setmeal/index.vue'),
        meta: { title: '套餐管理', icon: 'Bowl' },
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/category/index.vue'),
        meta: { title: '分类管理', icon: 'Grid' },
      },
      {
        path: 'voucher',
        name: 'Voucher',
        component: () => import('@/views/voucher/index.vue'),
        meta: { title: '优惠券', icon: 'Ticket' },
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/employee/index.vue'),
        meta: { title: '员工管理', icon: 'UserFilled' },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '数据统计', icon: 'TrendCharts' },
      },
    ],
  },

  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

function isOrderPath(path) {
  return path === ORDER_ROOT || path.startsWith(`${ORDER_ROOT}/`)
}

router.beforeEach((to) => {
  /* ---- 点餐端：只认顾客的令牌 ---- */

  // 登录、注册、找回密码这三页不用登录就能进
  const orderPublic = [
    `${ORDER_ROOT}/login`,
    `${ORDER_ROOT}/register`,
    `${ORDER_ROOT}/forgot`,
  ]
  if (orderPublic.includes(to.path)) {
    // 已经登录了就别停在登录页
    if (to.path === `${ORDER_ROOT}/login` && getUserToken()) {
      return { path: ORDER_ROOT }
    }
    return true
  }

  if (isOrderPath(to.path)) {
    if (!getUserToken()) {
      return { path: `${ORDER_ROOT}/login`, query: { redirect: to.fullPath } }
    }
    return true
  }

  /* ---- 首页与管理后台：只认员工的令牌 ---- */
  const token = getToken()

  if (!token && !to.meta.public) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录还去登录页，直接进后台
  if (token && to.path === '/login') {
    return { path: `${ADMIN_ROOT}/dashboard` }
  }

  return true
})

router.afterEach((to) => {
  if (!to.meta.title) {
    document.title = '筷点外卖 · 今天吃什么，筷点一下'
    return
  }
  // 点餐端是给顾客看的，标题不带「商家后台」
  const suffix = isOrderPath(to.path) ? '筷点外卖' : '筷点外卖商家后台'
  document.title = `${to.meta.title} · ${suffix}`
})

export default router
