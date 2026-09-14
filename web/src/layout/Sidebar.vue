<template>
  <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
    <!-- 品牌区兼作回官网的入口，点 Logo 回首页是后台的通用习惯 -->
    <router-link to="/" class="sidebar-brand" title="返回官网首页">
      <span class="brand-mark">筷</span>
      <span v-show="!collapsed" class="brand-name">筷点外卖</span>
      <el-icon v-show="!collapsed" class="brand-back"><HomeFilled /></el-icon>
    </router-link>

    <nav class="sidebar-nav">
      <el-menu
        :default-active="activePath"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="sky-menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </nav>

    <div v-show="!collapsed" class="sidebar-foot">
      <span class="num">{{ version }}</span>
      <span>管理后台</span>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ADMIN_ROOT } from '@/router'

defineProps({
  collapsed: { type: Boolean, default: false },
})

const version = 'v1.0.0'
const route = useRoute()
const router = useRouter()

// 菜单直接从路由表里长出来，避免两处维护
const menus = computed(() => {
  const root = router.options.routes.find((r) => r.path === ADMIN_ROOT)
  return (root?.children || []).map((child) => ({
    path: `${ADMIN_ROOT}/${child.path}`,
    title: child.meta?.title || child.path,
    icon: child.meta?.icon || 'Menu',
  }))
})

const activePath = computed(() => route.path)
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--sidebar-w);
  height: 100%;
  background: var(--ink-900);
  transition: width 0.18s ease;
  overflow: hidden;
}

.sidebar.is-collapsed {
  width: var(--sidebar-w-collapsed);
}

/* ---------------- 品牌（兼作回官网的入口） ---------------- */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  height: var(--navbar-h);
  padding: 0 16px;
  flex-shrink: 0;
  text-decoration: none;
  transition: background 0.15s ease;
}

.sidebar-brand:hover {
  background: rgba(255, 255, 255, 0.05);
}

/* 平时收着，hover 才浮出来，不和菜单抢注意力 */
.brand-back {
  margin-left: auto;
  font-size: 13px;
  color: #6f827b;
  opacity: 0;
  transform: translateX(-3px);
  transition: opacity 0.15s ease, transform 0.15s ease, color 0.15s ease;
}

.sidebar-brand:hover .brand-back {
  opacity: 1;
  transform: translateX(0);
  color: var(--brand);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 5px;
  background: var(--brand);
  color: var(--ink-900);
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.brand-name {
  color: var(--text-invert);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

/* ---------------- 菜单 ---------------- */
.sidebar-nav {
  flex: 1;
  min-height: 0;
  padding-top: 6px;
  overflow-y: auto;
}

.sky-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #a9b7b1;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-item-height: 42px;
  border-right: none;
}

.sky-menu :deep(.el-menu-item) {
  position: relative;
  margin: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 13.5px;
  letter-spacing: 0.02em;
}

/* 选中态：一条品牌黄的立标，其余保持安静 */
.sky-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.08);
  font-weight: 500;
}

.sky-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 9px;
  bottom: 9px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: var(--brand);
}

.sky-menu :deep(.el-menu-item .el-icon) {
  font-size: 17px;
}

.sky-menu:not(.el-menu--collapse) :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  width: 17px;
}

/* 折叠状态下选中项的背景块换个形状 */
.sky-menu.el-menu--collapse :deep(.el-menu-item) {
  margin: 2px 6px;
  padding: 0 !important;
  justify-content: center;
}

/* ---------------- 页脚 ---------------- */
.sidebar-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  font-size: 11px;
  color: #6f827b;
  letter-spacing: 0.04em;
  white-space: nowrap;
}
</style>
