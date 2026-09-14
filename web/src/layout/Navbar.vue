<template>
  <header class="navbar">
    <div class="navbar-left">
      <button
        type="button"
        class="nav-icon-btn"
        :title="collapsed ? '展开菜单' : '收起菜单'"
        @click="$emit('toggle')"
      >
        <el-icon>
          <component :is="collapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </button>

      <button type="button" class="nav-home" title="返回官网首页" @click="goHome">
        <el-icon><HomeFilled /></el-icon>
        <span>返回首页</span>
      </button>

      <h1 class="nav-title">{{ pageTitle }}</h1>
    </div>

    <div class="navbar-right">
      <span class="nav-today">
        今天 {{ todayText }}
      </span>

      <span class="nav-divider" aria-hidden="true"></span>

      <!-- 营业状态：改状态是要影响生意的，所以一律弹确认 -->
      <!-- 后端连不上 Redis 时该接口会 500，此处降级为「状态未知」并禁掉开关 -->
      <el-tooltip
        :disabled="shopStore.available !== false"
        :content="shopStore.unavailableReason"
        placement="bottom"
      >
        <div
          class="shop-status"
          :class="{ 'is-unknown': shopStore.available === false }"
        >
          <span class="shop-lamp" :class="lampClass" aria-hidden="true"></span>
          <span class="shop-label">{{ statusLabel }}</span>
          <el-switch
            :model-value="shopStore.status"
            :active-value="1"
            :inactive-value="0"
            :loading="shopStore.loading"
            :disabled="shopStore.available === false"
            :before-change="beforeStatusChange"
            size="small"
          />
        </div>
      </el-tooltip>

      <span class="nav-divider" aria-hidden="true"></span>

      <el-dropdown trigger="click" @command="onCommand">
        <button type="button" class="nav-user">
          <el-icon class="nav-user-icon"><UserFilled /></el-icon>
          <span>{{ displayName }}</span>
          <el-icon class="nav-user-caret"><ArrowDown /></el-icon>
        </button>

        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              账号：{{ userStore.userInfo?.userName || '—' }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { useShopStore } from '@/stores/shop'
import { useUserStore } from '@/stores/user'

defineProps({
  collapsed: { type: Boolean, default: false },
})
defineEmits(['toggle'])

dayjs.locale('zh-cn')

const route = useRoute()
const router = useRouter()
const shopStore = useShopStore()
const userStore = useUserStore()

const pageTitle = computed(() => route.meta?.title || '工作台')
const todayText = computed(() => dayjs().format('M月D日 dddd'))
const displayName = computed(
  () => userStore.userInfo?.name || userStore.userInfo?.userName || '未登录'
)

const lampClass = computed(() => {
  if (shopStore.available === false) return 'is-unknown'
  return shopStore.isOpen ? 'is-open' : 'is-closed'
})

const statusLabel = computed(() => {
  if (shopStore.available === false) return '状态未知'
  return shopStore.isOpen ? '营业中' : '打烊中'
})

/** el-switch 的 before-change：返回 Promise，resolve 才真的切换 */
function beforeStatusChange() {
  const next = shopStore.status === 1 ? 0 : 1
  const isOpen = next === 1
  const title = isOpen ? '开始营业' : '打烊'
  const tip = isOpen
    ? '开始营业后，顾客端将可以正常下单。'
    : '打烊后顾客端将无法下单，已下单的订单不受影响。'

  return ElMessageBox.confirm(tip, `确认${title}？`, {
    type: isOpen ? 'info' : 'warning',
    confirmButtonText: `确认${title}`,
    cancelButtonText: '再想想',
  }).then(async () => {
    await shopStore.updateStatus(next)
    ElMessage.success(isOpen ? '已开始营业' : '已打烊')
    return true
  })
}

function goHome() {
  router.push('/')
}

async function onCommand(command) {
  if (command !== 'logout') return

  try {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  await userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: var(--navbar-h);
  padding: 0 20px 0 12px;
  flex-shrink: 0;
  background: var(--surface);
  border-bottom: 1px solid var(--edge);
}

.navbar-left,
.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.nav-icon-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-2);
  font-size: 17px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-icon-btn:hover {
  background: var(--paper);
  color: var(--text);
}

/* 回官网的入口：做成带字按钮而不是光秃秃的图标，否则容易被当成装饰忽略掉 */
.nav-home {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 10px;
  flex-shrink: 0;
  border: 1px solid var(--edge);
  border-radius: var(--radius-sm);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--text-2);
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.nav-home:hover {
  color: var(--brand-deep);
  border-color: var(--brand);
  background: rgba(255, 194, 0, 0.12);
}

.nav-home .el-icon {
  font-size: 14px;
}

/* 窄屏收成纯图标，把横向空间让给页面标题 */
@media (max-width: 1000px) {
  .nav-home span {
    display: none;
  }

  .nav-home {
    padding: 0 8px;
  }
}

.nav-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.nav-today {
  font-size: 12.5px;
  color: var(--text-3);
  white-space: nowrap;
}

.nav-divider {
  width: 1px;
  height: 18px;
  background: var(--edge);
}

/* ---------------- 营业状态 ---------------- */
.shop-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-status.is-unknown {
  cursor: not-allowed;
}

.shop-lamp {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.shop-lamp.is-open {
  background: var(--brand-deep);
  box-shadow: 0 0 0 3px rgba(255, 194, 0, 0.22);
}

.shop-lamp.is-closed {
  background: var(--st-off);
  box-shadow: 0 0 0 3px rgba(138, 150, 143, 0.16);
}

/* 状态取不到时刻意做得「熄灭」而不是「打烊」，避免误读成生意已关 */
.shop-lamp.is-unknown {
  background: transparent;
  border: 1px solid var(--st-off);
  box-shadow: none;
  opacity: 0.7;
}

.shop-label {
  font-size: 13px;
  color: var(--text-2);
  white-space: nowrap;
}

.shop-status.is-unknown .shop-label {
  color: var(--text-3);
}

/* ---------------- 用户 ---------------- */
.nav-user {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.nav-user:hover {
  background: var(--paper);
  border-color: var(--edge);
}

.nav-user-icon {
  font-size: 15px;
  color: var(--text-2);
}

.nav-user-caret {
  font-size: 12px;
  color: var(--text-3);
}

@media (max-width: 720px) {
  .nav-today,
  .shop-label {
    display: none;
  }
}
</style>
