<template>
  <div class="login">
    <!-- 左侧：顾客入口用品牌黄，和管理端那块深墨分开，
         进来一眼就知道自己走的是哪道门 -->
    <aside class="login-brand">
      <div class="brand-row">
        <span class="brand-mark" aria-hidden="true">筷</span>
        <div class="brand-text">
          <strong>筷点外卖</strong>
          <span>顾客点餐</span>
        </div>
      </div>

      <div class="brand-copy">
        <h1>登录之后，慢慢挑。</h1>
        <p>浏览菜单、加购物车、下单，订单做到哪一步随时都能查到。</p>
      </div>

      <!-- 一张菜单卡当视觉记忆点，菜名和价钱之间拉点线的手法取自首页 -->
      <div class="card-wrap" aria-hidden="true">
        <div class="menu-card">
          <div class="card-head">
            <span class="card-title">今日菜单</span>
            <span class="card-tag num">{{ today }}</span>
          </div>
          <ul class="card-lines">
            <li v-for="d in sampleMenu" :key="d.name" class="card-line">
              <span class="card-name">{{ d.name }}</span>
              <span class="leader"></span>
              <span class="card-price num money">{{ d.price }}</span>
            </li>
          </ul>
          <p class="card-foot">现炒现送 · 半小时内送达</p>
        </div>
      </div>
    </aside>

    <!-- 右侧：登录表单 -->
    <main class="login-form-side">
      <div class="login-box">
        <h2>顾客登录</h2>
        <p class="login-sub">登录后即可浏览菜单并下单</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          label-position="top"
          @keyup.enter="onSubmit"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="请输入账号"
              autocomplete="username"
              clearable
            >
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model.trim="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-button
            type="primary"
            class="login-submit"
            size="large"
            :loading="loading"
            @click="onSubmit"
          >
            {{ loading ? '正在登录…' : '登录并点餐' }}
          </el-button>
        </el-form>

        <button type="button" class="login-hint" @click="fillDemo">
          演示账号 <b>user01 / 123456</b> · 点此填入
        </button>

        <div class="login-links">
          <router-link class="text-link" :to="`${ORDER_ROOT}/register`">注册新账号</router-link>
          <span class="link-sep" aria-hidden="true">·</span>
          <router-link class="text-link" :to="`${ORDER_ROOT}/forgot`">忘记密码</router-link>
        </div>

        <router-link class="back-home" to="/">← 回到筷点外卖首页</router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { ORDER_ROOT } from '@/router'
import { useCustomerStore } from '@/stores/customer'

const router = useRouter()
const route = useRoute()
const customer = useCustomerStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const today = dayjs().format('MM / DD')

// 左侧卡片上摆的示例，纯装饰
const sampleMenu = [
  { name: '招牌小炒肉', price: '28.00' },
  { name: '青椒炒鸡蛋', price: '16.00' },
  { name: '西红柿鸡蛋汤', price: '12.00' },
]

function fillDemo() {
  form.username = 'user01'
  form.password = '123456'
}

async function onSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await customer.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(
      typeof redirect === 'string' && redirect ? redirect : ORDER_ROOT
    )
  } catch {
    // 失败原因由 axios 拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  display: flex;
  min-height: 100vh;
  background: var(--surface);
}

/* ---------------- 左侧品牌面板 ---------------- */
.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 46%;
  min-width: 420px;
  padding: 32px 40px 0;
  background: var(--brand);
  color: var(--ink-950);
  overflow: hidden;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: var(--ink-900);
  color: var(--brand);
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-text strong {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.brand-text span {
  font-size: 11px;
  color: rgba(14, 24, 23, 0.62);
  letter-spacing: 0.06em;
}

.brand-copy {
  margin-top: 52px;
  max-width: 20em;
}

.brand-copy h1 {
  margin: 0 0 14px;
  font-size: 27px;
  font-weight: 700;
  line-height: 1.42;
  letter-spacing: -0.01em;
}

.brand-copy p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: rgba(14, 24, 23, 0.72);
  text-wrap: balance;
}

/* ---------------- 菜单卡 ---------------- */
.card-wrap {
  display: flex;
  flex: 1;
  align-items: flex-end;
  min-height: 230px;
  margin-top: 40px;
  padding-bottom: 44px;
}

/* 菜单卡靠底摆着。原先用的是绝对定位加负 bottom，
   窗口一矮卡片就被切掉半截，改成跟着文档流走就不会了 */
.menu-card {
  width: 272px;
  padding: 17px 20px 18px;
  border-radius: 10px;
  background: var(--surface);
  color: var(--text);
  box-shadow: 0 16px 38px rgba(14, 24, 23, 0.22);
  transform: rotate(-2.4deg);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 11px;
  border-bottom: 1px solid var(--edge);
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.card-tag {
  font-size: 11px;
  color: var(--text-3);
}

.card-lines {
  margin: 0;
  padding: 0;
  list-style: none;
}

.card-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 8px 0;
}

.card-name {
  font-size: 13px;
  color: var(--text);
}

.leader {
  flex: 1;
  border-bottom: 1px dotted var(--edge-strong);
  transform: translateY(-3px);
}

.card-price {
  font-size: 13px;
  color: var(--text);
}

.card-foot {
  margin: 12px 0 0;
  padding-top: 10px;
  border-top: 1px dashed var(--edge-strong);
  font-size: 11px;
  color: var(--text-3);
  text-align: center;
  letter-spacing: 0.04em;
}

/* ---------------- 右侧表单 ---------------- */
.login-form-side {
  display: grid;
  place-items: center;
  flex: 1;
  padding: 40px 24px;
}

.login-box {
  width: 100%;
  max-width: 344px;
}

.login-box h2 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.login-sub {
  margin: 0 0 28px;
  font-size: 13px;
  color: var(--text-3);
}

.login-submit {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 0.08em;
}

.login-hint {
  display: block;
  width: 100%;
  margin-top: 20px;
  padding: 10px 12px;
  border: 1px dashed var(--edge-strong);
  border-radius: var(--radius-sm);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--text-3);
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.login-hint:hover {
  border-color: var(--teal);
  color: var(--teal);
}

.login-hint b {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--text-2);
}

.login-hint:hover b {
  color: var(--teal);
}

.login-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

.text-link {
  font-size: 13px;
  color: var(--teal);
  text-decoration: none;
}

.text-link:hover {
  text-decoration: underline;
}

.link-sep {
  color: var(--edge-strong);
}

.back-home {
  display: block;
  margin-top: 18px;
  font-size: 12.5px;
  color: var(--text-3);
  text-align: center;
  text-decoration: none;
}

.back-home:hover {
  color: var(--teal);
}

@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
}
</style>
