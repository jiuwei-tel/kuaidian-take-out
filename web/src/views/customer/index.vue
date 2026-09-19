<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryList, getDishList, getShopStatus } from '@/api/customer'
import { useCustomerStore } from '@/stores/customer'

const customer = useCustomerStore()

const shopOpen = ref(true)
const shopChecked = ref(false)

const categories = ref([])
const activeCategoryId = ref(null)
const dishes = ref([])
const loadingDishes = ref(false)

/* ---------------- 营业状态 ---------------- */
async function loadShopStatus() {
  try {
    const status = await getShopStatus()
    // 库里没这个 key 时后端会返回 null，按休息中处理
    shopOpen.value = status === 1
  } catch {
    // 查不到就当正常营业，别因为一个指示器把点餐页卡住
    shopOpen.value = true
  } finally {
    shopChecked.value = true
  }
}

/* ---------------- 分类与菜品 ---------------- */
async function loadCategories() {
  const list = await getCategoryList(1)
  categories.value = list || []
  if (categories.value.length) {
    selectCategory(categories.value[0].id)
  }
}

async function selectCategory(id) {
  activeCategoryId.value = id
  loadingDishes.value = true
  try {
    const list = await getDishList(id)
    dishes.value = list || []
  } catch {
    dishes.value = []
  } finally {
    loadingDishes.value = false
  }
}

/* ---------------- 口味 ----------------
   菜品没有单独的详情接口，规格就藏在列表返回的 flavors 里。
   flavor.value 多数是 JSON 字符串（形如 ["微辣","中辣"]），
   但库里也存过裸值（比如「不辣」），两种都得认。 */
function flavorsOf(dish) {
  if (!dish.flavors || !dish.flavors.length) return []
  const values = []
  dish.flavors.forEach((f) => {
    if (!f || !f.value) return
    const raw = String(f.value).trim()
    if (raw.startsWith('[')) {
      try {
        const arr = JSON.parse(raw)
        if (Array.isArray(arr)) {
          values.push(...arr)
          return
        }
      } catch {
        // 解析不了就按裸值处理，别让整页崩掉
      }
    }
    values.push(raw)
  })
  return values
}

/* ---------------- 购物车 ---------------- */
function countOfDish(dishId) {
  return customer.cartList
    .filter((item) => item.dishId === dishId)
    .reduce((sum, item) => sum + (item.number || 0), 0)
}

function firstItemOf(dishId) {
  return customer.cartList.find((item) => item.dishId === dishId)
}

const flavorDialog = ref(false)
const flavorTarget = ref(null)
const flavorOptions = ref([])
const flavorPick = ref('')

async function onAdd(dish) {
  if (!shopOpen.value) return

  const options = flavorsOf(dish)
  if (options.length) {
    // 有规格，先让顾客挑一个
    flavorTarget.value = dish
    flavorOptions.value = options
    flavorPick.value = options[0]
    flavorDialog.value = true
    return
  }

  await customer.add(dish.id, null, '')
}

async function confirmFlavor() {
  if (!flavorTarget.value || !flavorPick.value) return
  await customer.add(flavorTarget.value.id, null, flavorPick.value)
  flavorDialog.value = false
  ElMessage.success('已加入购物车')
}

async function onSub(dish) {
  const item = firstItemOf(dish.id)
  // 车里没有这条就别调减购了，后端空列表会报错
  if (!item) return
  await customer.sub(item.dishId, null, item.dishFlavor)
}

onMounted(async () => {
  await loadShopStatus()
  await loadCategories().catch(() => {
    ElMessage.error('菜单加载失败，请稍后重试')
  })
  customer.fetchCart().catch(() => {})
})
</script>

<template>
  <div class="menu-page">
    <div v-if="shopChecked && !shopOpen" class="closed-bar">
      <span class="dot dot-off" aria-hidden="true"></span>
      店铺休息中，暂时不能下单
    </div>

    <div class="menu-body">
      <!-- 左：分类 -->
      <aside class="cats">
        <ul class="cat-list">
          <li v-for="c in categories" :key="c.id">
            <button
              type="button"
              class="cat"
              :class="{ 'is-on': c.id === activeCategoryId }"
              @click="selectCategory(c.id)"
            >
              {{ c.name }}
            </button>
          </li>
        </ul>
      </aside>

      <!-- 右：菜品 -->
      <section class="dishes">
        <p v-if="loadingDishes" class="hint">正在上菜…</p>

        <template v-else-if="dishes.length">
          <article v-for="d in dishes" :key="d.id" class="dish">
            <div class="thumb-wrap">
              <el-image v-if="d.image" :src="d.image" class="thumb" fit="cover">
                <template #error>
                  <span class="thumb-fallback">{{ d.name?.slice(0, 1) }}</span>
                </template>
              </el-image>
              <span v-else class="thumb-fallback">{{ d.name?.slice(0, 1) }}</span>
            </div>

            <div class="dish-main">
              <h3 class="dish-name">{{ d.name }}</h3>
              <p v-if="d.description" class="dish-desc">{{ d.description }}</p>

              <div class="dish-foot">
                <span class="dish-price num money">{{ d.price }}</span>

                <div class="ops">
                  <template v-if="countOfDish(d.id)">
                    <button type="button" class="step" aria-label="减一份" @click="onSub(d)">−</button>
                    <span :key="countOfDish(d.id)" class="step-count num">{{ countOfDish(d.id) }}</span>
                  </template>
                  <button
                    type="button"
                    class="step step-add"
                    :disabled="!shopOpen"
                    :aria-label="flavorsOf(d).length ? '选择口味' : '加一份'"
                    @click="onAdd(d)"
                  >
                    ＋
                  </button>
                </div>
              </div>
            </div>
          </article>
        </template>

        <p v-else class="hint">这个分类暂时没有上架的菜品</p>
      </section>
    </div>

    <el-dialog
      v-model="flavorDialog"
      :title="flavorTarget?.name || '选择口味'"
      width="380px"
      align-center
    >
      <p class="flavor-label">口味</p>
      <div class="flavor-options">
        <button
          v-for="f in flavorOptions"
          :key="f"
          type="button"
          class="flavor-opt"
          :class="{ 'is-on': f === flavorPick }"
          @click="flavorPick = f"
        >
          {{ f }}
        </button>
      </div>

      <template #footer>
        <button type="button" class="btn-plain" @click="flavorDialog = false">取消</button>
        <button type="button" class="btn-primary" @click="confirmFlavor">加入购物车</button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.menu-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 22px 24px 0;
}

.closed-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: var(--radius);
  background: var(--fire-soft);
  font-size: 13px;
  color: var(--fire);
}

.menu-body {
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

/* ---------------- 左侧分类 ---------------- */
.cats {
  position: sticky;
  top: 84px;
  max-height: calc(100vh - 84px - 100px);
  overflow-y: auto;
}

.cat-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.cat {
  position: relative;
  display: block;
  width: 100%;
  padding: 12px 12px 12px 16px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--text-2);
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.cat:hover {
  background: var(--surface-sunken);
  color: var(--text);
}

/* 选中态用一条主色竖标，和侧边栏一个做法 */
.cat.is-on {
  background: var(--teal-soft);
  color: var(--teal);
  font-weight: 600;
}

.cat.is-on::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background: var(--teal);
  transform: translateY(-50%);
}

/* ---------------- 右侧菜品 ---------------- */
.dishes {
  display: flex;
  flex-direction: column;
}

.dish {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 0;
}

.dish + .dish {
  border-top: 1px solid var(--edge);
}

.thumb-wrap {
  flex: none;
  width: 56px;
  height: 56px;
  overflow: hidden;
  border-radius: 8px;
}

.thumb,
.thumb-fallback {
  width: 100%;
  height: 100%;
}

/* 教学用的图床早就失效了，大多数菜图会走到这里 */
.thumb-fallback {
  display: grid;
  place-items: center;
  border: 1px dashed var(--edge-strong);
  border-radius: 8px;
  background: var(--surface-sunken);
  font-size: 20px;
  font-weight: 700;
  color: var(--text-3);
}

.dish-main {
  flex: 1;
  min-width: 0;
}

.dish-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.dish-desc {
  margin: 5px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--text-3);
  text-wrap: balance;
}

.dish-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
}

.dish-price {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.ops {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--edge-strong);
  border-radius: 50%;
  background: var(--surface);
  color: var(--text-2);
  font-family: var(--font-sans);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
}

.step:hover:not(:disabled) {
  border-color: var(--teal);
  color: var(--teal);
}

.step-add {
  border-color: var(--teal);
  background: var(--teal);
  color: #fff;
}

.step-add:hover:not(:disabled) {
  background: var(--teal-dark);
  border-color: var(--teal-dark);
  color: #fff;
}

.step:disabled {
  border-color: var(--edge);
  background: var(--surface-sunken);
  color: var(--text-3);
  cursor: not-allowed;
}

.step-count {
  min-width: 18px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  text-align: center;
  animation: step-pop 0.18s ease;
}

@keyframes step-pop {
  from {
    transform: scale(0.75);
  }
  to {
    transform: scale(1);
  }
}

.hint {
  margin: 40px 0;
  font-size: 13.5px;
  color: var(--text-3);
  text-align: center;
}

/* ---------------- 口味弹窗 ---------------- */
.flavor-label {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text-2);
}

.flavor-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.flavor-opt {
  padding: 7px 16px;
  border: 1px solid var(--edge-strong);
  border-radius: 999px;
  background: var(--surface);
  font-family: var(--font-sans);
  font-size: 13px;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.15s ease;
}

.flavor-opt:hover {
  border-color: var(--teal);
  color: var(--teal);
}

.flavor-opt.is-on {
  border-color: var(--teal);
  background: var(--teal-soft);
  color: var(--teal);
  font-weight: 600;
}

.btn-plain,
.btn-primary {
  padding: 9px 20px;
  border-radius: var(--radius-sm);
  font-family: var(--font-sans);
  font-size: 14px;
  cursor: pointer;
}

.btn-plain {
  border: 1px solid var(--edge-strong);
  background: var(--surface);
  color: var(--text-2);
}

.btn-plain:hover {
  border-color: var(--teal);
  color: var(--teal);
}

.btn-primary {
  border: 1px solid var(--teal);
  background: var(--teal);
  color: #fff;
  font-weight: 600;
}

.btn-primary:hover {
  background: var(--teal-dark);
  border-color: var(--teal-dark);
}

@media (max-width: 860px) {
  .menu-page {
    padding: 16px 16px 0;
  }

  .menu-body {
    grid-template-columns: 108px minmax(0, 1fr);
    gap: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .step-count {
    animation: none;
  }
}
</style>
