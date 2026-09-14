<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">套餐管理</h2>
        <p class="page-sub">套餐是由若干菜品组合而成的售卖单元，新增后需要手动起售</p>
      </div>
      <div class="page-actions">
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :disabled="selectedIds.length === 0"
          @click="onBatchDelete"
        >
          批量删除{{ selectedIds.length ? `（${selectedIds.length}）` : '' }}
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增套餐</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="filter-bar">
        <el-input
          v-model.trim="query.name"
          placeholder="套餐名称"
          clearable
          @keyup.enter="onSearch"
        />
        <el-select
          v-model="query.categoryId"
          placeholder="套餐分类"
          clearable
          @change="onSearch"
        >
          <el-option
            v-for="c in setmealCategories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="售卖状态" clearable @change="onSearch">
          <el-option label="起售" :value="1" />
          <el-option label="停售" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        stripe
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />

        <el-table-column label="套餐名称" min-width="190">
          <template #default="{ row }">
            <div class="dish-cell">
              <el-image
                v-if="row.image"
                :src="row.image"
                fit="cover"
                class="dish-thumb"
                :preview-src-list="[row.image]"
                preview-teleported
              >
                <template #error>
                  <span class="dish-thumb-fallback" />
                </template>
              </el-image>
              <span v-else class="dish-thumb dish-thumb--empty" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="套餐分类" width="130">
          <template #default="{ row }">
            {{ row.categoryName || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="套餐价" width="110" align="right">
          <template #default="{ row }">
            <span class="money num">{{ fmtMoney(row.price) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="售卖状态" width="104">
          <template #default="{ row }">
            <span class="dot" :class="row.status === 1 ? 'dot-on' : 'dot-off'">
              {{ row.status === 1 ? '起售' : '停售' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="最后操作时间" width="150">
          <template #default="{ row }">
            <span class="num muted">{{ fmtDateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" size="small" @click="openEdit(row)">
                修改
              </el-button>
              <el-button
                link
                :type="row.status === 1 ? 'danger' : 'primary'"
                size="small"
                @click="onToggleStatus(row)"
              >
                {{ row.status === 1 ? '停售' : '起售' }}
              </el-button>
              <el-button link type="danger" size="small" @click="onDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有查到套餐" :image-size="88" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </section>

    <!-- 新增 / 修改套餐 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '修改套餐' : '新增套餐'"
      width="720px"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入套餐名称" maxlength="30" />
        </el-form-item>

        <el-form-item label="套餐分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择套餐分类" class="form-full">
            <el-option
              v-for="c in setmealCategories"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="套餐价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :max="99999"
            :precision="2"
            :step="1"
            controls-position="right"
          />
          <span class="form-hint">单位：元</span>
        </el-form-item>

        <el-form-item label="图片" prop="image">
          <ImageUpload v-model="form.image" />
        </el-form-item>

        <el-form-item label="描述信息" prop="description">
          <el-input
            v-model.trim="form.description"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="简要描述这个套餐"
          />
        </el-form-item>

        <el-form-item label="菜品明细">
          <div class="dish-picker">
            <div class="dish-picker-head">
              <span class="muted">
                已选 {{ form.setmealDishes.length }} 个菜品
              </span>
              <el-button link type="primary" :icon="Plus" @click="openDishPicker">
                添加菜品
              </el-button>
            </div>

            <el-table :data="form.setmealDishes" size="small" border>
              <el-table-column prop="name" label="菜品名称" min-width="160" />
              <el-table-column label="原价" width="100" align="right">
                <template #default="{ row }">
                  <span class="money num">{{ fmtMoney(row.price) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="份数" width="118" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.copies"
                    :min="1"
                    :max="99"
                    size="small"
                    controls-position="right"
                  />
                </template>
              </el-table-column>
              <el-table-column label="" width="56" align="center">
                <template #default="{ $index }">
                  <el-button
                    link
                    type="danger"
                    :icon="Delete"
                    @click="form.setmealDishes.splice($index, 1)"
                  />
                </template>
              </el-table-column>

              <template #empty>
                <span class="muted">还没有添加菜品，点右上角「添加菜品」</span>
              </template>
            </el-table>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 选菜弹窗 -->
    <el-dialog
      v-model="pickerVisible"
      title="添加菜品"
      width="620px"
      append-to-body
    >
      <div class="picker-bar">
        <el-select
          v-model="pickerCategoryId"
          placeholder="先选择菜品分类"
          class="form-full"
          @change="loadPickerDishes"
        >
          <el-option
            v-for="c in dishCategories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
      </div>

      <el-table
        v-loading="pickerLoading"
        :data="pickerDishes"
        row-key="id"
        height="300"
        @selection-change="onPickerSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="菜品名称" min-width="180">
          <template #default="{ row }">
            <div class="dish-cell">
              <el-image
                v-if="row.image"
                :src="row.image"
                fit="cover"
                class="dish-thumb"
                preview-teleported
              >
                <template #error>
                  <span class="dish-thumb-fallback" />
                </template>
              </el-image>
              <span v-else class="dish-thumb dish-thumb--empty" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="110" align="right">
          <template #default="{ row }">
            <span class="money num">{{ fmtMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">该分类下没有起售中的菜品</span>
        </template>
      </el-table>

      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPickDishes">
          添加{{ pickedDishes.length ? `（${pickedDishes.length}）` : '' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { categoryList } from '@/api/category'
import { dishListByCategory } from '@/api/dish'
import {
  addSetmeal,
  deleteSetmeal,
  editSetmeal,
  getSetmealById,
  pageSetmeal,
  setmealStatus,
} from '@/api/setmeal'
import { fmtDateTime, fmtMoney } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const selectedIds = ref([])
const setmealCategories = ref([])
const dishCategories = ref([])

const query = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  categoryId: undefined,
  status: undefined,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  categoryId: undefined,
  price: 0,
  image: '',
  description: '',
  setmealDishes: [],
})

const rules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择套餐分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入套餐价格', trigger: 'blur' }],
}

async function loadCategories() {
  try {
    const [setmealRes, dishRes] = await Promise.all([
      categoryList(2),
      categoryList(1),
    ])
    setmealCategories.value = setmealRes || []
    dishCategories.value = dishRes || []
  } catch {
    setmealCategories.value = []
    dishCategories.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageSetmeal({
      page: query.page,
      pageSize: query.pageSize,
      name: query.name || undefined,
      categoryId: query.categoryId,
      status: query.status,
    })
    total.value = Number(data?.total || 0)
    list.value = data?.records || []
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  query.name = ''
  query.categoryId = undefined
  query.status = undefined
  query.page = 1
  loadList()
}

/* ---------------- 主弹窗 ---------------- */
function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    categoryId: undefined,
    price: 0,
    image: '',
    description: '',
    setmealDishes: [],
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  dialogVisible.value = true
  try {
    const data = await getSetmealById(row.id)
    Object.assign(form, {
      id: data.id,
      name: data.name || '',
      categoryId: data.categoryId,
      price: Number(data.price || 0),
      image: data.image || '',
      description: data.description || '',
      setmealDishes: (data.setmealDishes || []).map((d) => ({
        dishId: d.dishId,
        name: d.name,
        price: Number(d.price || 0),
        copies: d.copies ?? 1,
      })),
    })
  } catch {
    dialogVisible.value = false
  }
}

function onDialogClosed() {
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.setmealDishes.length === 0) {
    ElMessage.warning('套餐至少需要添加一个菜品')
    return
  }

  submitting.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      categoryId: form.categoryId,
      price: form.price,
      image: form.image,
      description: form.description,
      setmealDishes: form.setmealDishes.map((d) => ({
        dishId: d.dishId,
        name: d.name,
        price: d.price,
        copies: d.copies,
      })),
    }

    if (isEdit.value) {
      await editSetmeal(payload)
      ElMessage.success('套餐已更新')
    } else {
      await addSetmeal(payload)
      // 后端的 save() 里写死了 setStatus(DISABLE)，新增出来的套餐一律是停售的
      ElMessage.success('套餐已新增，当前为停售状态，需要手动起售')
    }
    dialogVisible.value = false
    loadList()
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ---------------- 选菜 ---------------- */
const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerCategoryId = ref(undefined)
const pickerDishes = ref([])
const pickedDishes = ref([])

function openDishPicker() {
  pickerVisible.value = true
  pickerCategoryId.value = undefined
  pickerDishes.value = []
  pickedDishes.value = []
}

async function loadPickerDishes(categoryId) {
  pickerLoading.value = true
  try {
    // /admin/dish/list 只返回「起售中」的菜品
    pickerDishes.value = (await dishListByCategory(categoryId)) || []
  } catch {
    pickerDishes.value = []
  } finally {
    pickerLoading.value = false
  }
}

function onPickerSelectionChange(rows) {
  pickedDishes.value = rows
}

function confirmPickDishes() {
  if (!pickedDishes.value.length) {
    ElMessage.warning('请先勾选要加入套餐的菜品')
    return
  }

  const existingIds = new Set(form.setmealDishes.map((d) => d.dishId))
  let added = 0

  pickedDishes.value.forEach((dish) => {
    if (existingIds.has(dish.id)) return
    form.setmealDishes.push({
      dishId: dish.id,
      name: dish.name,
      price: Number(dish.price || 0),
      copies: 1,
    })
    added += 1
  })

  if (added === 0) {
    ElMessage.info('勾选的菜品都已经在套餐里了')
  } else {
    ElMessage.success(`已添加 ${added} 个菜品`)
  }

  pickerVisible.value = false
}

/* ---------------- 行操作 ---------------- */
async function onToggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const label = next === 1 ? '起售' : '停售'
  try {
    await ElMessageBox.confirm(
      `${label}套餐「${row.name}」？`,
      `${label}套餐`,
      { type: 'warning', confirmButtonText: `确认${label}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await setmealStatus(next, row.id)
    ElMessage.success(`已${label}`)
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除套餐「${row.name}」？起售中的套餐后端会拒绝删除。`,
      '删除套餐',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deleteSetmeal(row.id)
    ElMessage.success('套餐已删除')
    if (list.value.length === 1 && query.page > 1) query.page -= 1
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onBatchDelete() {
  const ids = selectedIds.value
  if (!ids.length) return

  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${ids.length} 个套餐？起售中的套餐后端会拒绝删除。`,
      '批量删除套餐',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deleteSetmeal(ids)
    ElMessage.success(`已删除 ${ids.length} 个套餐`)
    selectedIds.value = []
    query.page = 1
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(() => {
  loadCategories()
  loadList()
})
</script>

<style scoped>
.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-thumb {
  width: 34px;
  height: 34px;
  border-radius: 3px;
  flex-shrink: 0;
  background: var(--paper);
}

.dish-thumb--empty {
  display: inline-block;
  border: 1px dashed var(--edge-strong);
}

/* 图片加载失败时，把 el-image 默认的「加载失败」文案换成与「无图」一致的虚线占位 */
.dish-thumb-fallback {
  display: block;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border: 1px dashed var(--edge-strong);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 8px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.form-full {
  width: 100%;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-3);
}

/* ---------------- 菜品明细 ---------------- */
.dish-picker {
  width: 100%;
  padding: 12px;
  border: 1px dashed var(--edge-strong);
  border-radius: var(--radius-sm);
  background: var(--surface-sunken);
}

.dish-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 12.5px;
}

.picker-bar {
  margin-bottom: 12px;
}

.muted {
  color: var(--text-3);
}
</style>
