<template>
  <div class="login">
    <!-- 和顾客登录页同一套版式，左侧品牌黄，右侧表单 -->
    <aside class="login-brand">
      <div class="brand-row">
        <span class="brand-mark" aria-hidden="true">筷</span>
        <div class="brand-text">
          <strong>筷点外卖</strong>
          <span>顾客注册</span>
        </div>
      </div>

      <div class="brand-copy">
        <h1>先注册，再点餐。</h1>
        <p>顺手把收货信息填了，注册完就能直接下单，不用再来回补。</p>
      </div>

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

    <main class="login-form-side">
      <div class="login-box">
        <h2>注册新账号</h2>
        <p class="login-sub">账号用于登录，收货信息会存成默认地址</p>

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
              placeholder="至少 3 位，登录时用"
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
              placeholder="至少 6 位"
              autocomplete="new-password"
              show-password
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirm">
            <el-input
              v-model.trim="form.confirm"
              type="password"
              placeholder="再输一次"
              autocomplete="new-password"
              show-password
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <p class="group-title">收货信息</p>

          <el-form-item label="收货人" prop="name">
            <el-input v-model.trim="form.name" placeholder="怎么称呼您" clearable>
              <template #prefix><el-icon><UserFilled /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model.trim="form.phone"
              placeholder="11 位手机号，忘了密码时要用"
              maxlength="11"
              clearable
            >
              <template #prefix><el-icon><Phone /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="收货地址" prop="address">
            <el-input
              v-model.trim="form.address"
              type="textarea"
              :rows="2"
              maxlength="100"
              show-word-limit
              placeholder="写详细点，比如：广东省清远市清城区演示大厦 A 座 505"
            />
          </el-form-item>

          <el-button
            type="primary"
            class="login-submit"
            size="large"
            :loading="loading"
            @click="onSubmit"
          >
            {{ loading ? '正在注册…' : '注册' }}
          </el-button>
        </el-form>

        <router-link class="back-home" :to="`${ORDER_ROOT}/login`">
          已经有账号了？去登录
        </router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Phone, User, UserFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { register } from '@/api/customer'
import { ORDER_ROOT } from '@/router'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  name: '',
  phone: '',
  address: '',
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, message: '账号至少 3 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请再输一次密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一样'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  name: [{ required: true, message: '请填收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不对', trigger: 'blur' },
  ],
  address: [{ required: true, message: '请填收货地址', trigger: 'blur' }],
}

const today = dayjs().format('MM / DD')

const sampleMenu = [
  { name: '招牌小炒肉', price: '28.00' },
  { name: '青椒炒鸡蛋', price: '16.00' },
  { name: '西红柿鸡蛋汤', price: '12.00' },
]

async function onSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      name: form.name,
      phone: form.phone,
      address: form.address,
    })
    ElMessage.success('注册成功，去登录吧')
    // 注册完不直接发登录态，回登录页登一次
    router.replace(`${ORDER_ROOT}/login`)
  } catch {
    // 失败提示由 axios 拦截器给出
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

.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 42%;
  min-width: 380px;
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
  margin-top: 44px;
  max-width: 18em;
}

.brand-copy h1 {
  margin: 0 0 14px;
  font-size: 26px;
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

.card-wrap {
  display: flex;
  flex: 1;
  align-items: flex-end;
  min-height: 200px;
  margin-top: 32px;
  padding-bottom: 44px;
}

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
  padding: 36px 24px;
}

.login-box {
  width: 100%;
  max-width: 372px;
}

.login-box h2 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 600;
}

.login-sub {
  margin: 0 0 22px;
  font-size: 13px;
  color: var(--text-3);
}

/* 把收货信息那几项和账号信息分开 */
.group-title {
  margin: 18px 0 14px;
  padding-top: 16px;
  border-top: 1px solid var(--edge);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
  letter-spacing: 0.06em;
}

.login-submit {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 0.08em;
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
