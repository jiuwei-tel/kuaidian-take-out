import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

/**
 * 用 hash 模式：部署到 Nginx / 直接开 dist 都不需要额外配 try_files，
 * 对课设项目来说少一个坑。
 *
 * 路由分两块：
 *   /            营销首页，公开访问，不套后台外壳
 *   /manage/**   商家管理后台，需要登录，套 Layout（侧边栏 + 顶栏）
 *
 * 后台统一挂在 /manage 下，是为了让根路径 / 能留给首页。
 * 侧边栏、首页的「管理后台」下拉都从这个常量推路径，别再各写一份。
 */
export const ADMIN_ROOT = '/manage'

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

router.beforeEach((to) => {
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
  document.title = to.meta.title
    ? `${to.meta.title} · 筷点外卖商家后台`
    : '筷点外卖 · 今天吃什么，筷点一下'
})

export default router
