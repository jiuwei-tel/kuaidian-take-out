<template>
  <div class="login">
    <!-- 左：品牌面板。不做渐变堆叠，只放一张出单小票作为唯一的视觉记忆点 -->
    <aside class="login-brand">
      <div class="brand-row">
        <span class="brand-mark">筷</span>
        <div class="brand-text">
          <strong>筷点外卖</strong>
          <span>商家管理后台</span>
        </div>
      </div>

      <div class="brand-copy">
        <h1>每一单，都从这里开始。</h1>
        <p>
          接单、出餐、派送、结算，以及菜品与套餐的日常维护，
          都在同一个后台里完成。
        </p>
      </div>

      <!-- 出单小票 -->
      <div class="ticket-wrap" aria-hidden="true">
        <div class="ticket receipt">
          <div class="ticket-head">
            <span class="ticket-brand">筷点外卖</span>
            <span class="ticket-tag">堂食 / 外送</span>
          </div>
          <hr class="perf" />
          <div class="ticket-line ticket-line--no">
            <span>订单号</span>
            <span class="num">20260913&nbsp;0017</span>
          </div>
          <hr class="perf" />
          <div class="ticket-line"><span>宫保鸡丁</span><span class="num">× 1</span></div>
          <div class="ticket-line"><span>米饭</span><span class="num">× 2</span></div>
          <div class="ticket-line"><span>酸梅汤</span><span class="num">× 1</span></div>
          <hr class="perf" />
          <div class="ticket-line ticket-line--total">
            <span>合计</span>
            <span class="num">¥ 62.00</span>
          </div>
          <p class="ticket-foot">请及时接单 · 超时将自动取消</p>
        </div>
      </div>
    </aside>

    <!-- 右：登录表单 -->
    <main class="login-form-side">
      <div class="login-box">
        <h2>员工登录</h2>
        <p class="login-sub">请使用门店分配的员工账号登录</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          label-position="top"
          @keyup.enter="onSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="请输入用户名"
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
            {{ loading ? '正在登录…' : '登录' }}
          </el-button>
        </el-form>

        <button type="button" class="login-hint" @click="fillDemo">
          课设默认账号 <b>admin / 123456</b> · 点此填入
        </button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function fillDemo() {
  form.username = 'admin'
  form.password = '123456'
}

async function onSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect ? redirect : '/manage/dashboard')
  } catch {
    // 失败原因已由 axios 拦截器统一提示
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
  background: var(--ink-900);
  color: var(--text-invert);
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
  background: var(--brand);
  color: var(--ink-900);
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
  font-weight: 600;
  letter-spacing: 0.02em;
}

.brand-text span {
  font-size: 11px;
  color: #8fa39c;
  letter-spacing: 0.06em;
}

.brand-copy {
  margin-top: 56px;
  max-width: 22em;
}

.brand-copy h1 {
  margin: 0 0 14px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: 0.01em;
}

.brand-copy p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.85;
  color: #9db0a9;
}

/* ---------------- 小票卡片 ---------------- */
.ticket-wrap {
  position: relative;
  flex: 1;
  min-height: 220px;
  margin-top: 40px;
}

.ticket {
  position: absolute;
  left: 8px;
  bottom: -36px;
  width: 268px;
  padding: 18px 20px 22px;
  transform: rotate(-3.2deg);
  color: var(--text);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.38);
}

.ticket-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ticket-brand {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.ticket-tag {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-3);
  letter-spacing: 0.06em;
}

.ticket-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  font-size: 12.5px;
  line-height: 2.1;
  color: var(--text-2);
}

.ticket-line .num {
  color: var(--text);
}

.ticket-line--no {
  font-size: 12px;
}

.ticket-line--no .num {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.ticket-line--total {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.ticket-line--total .num {
  font-size: 16px;
  font-weight: 700;
}

.ticket-foot {
  margin: 14px 0 0;
  padding-top: 12px;
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

@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
}
</style>
