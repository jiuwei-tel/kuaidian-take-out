<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">菜品管理</h2>
        <p class="page-sub">新增的菜品默认是起售状态，可直接在列表里调整</p>
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
        <el-button type="primary" :icon="Plus" @click="openCreate">新增菜品</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="filter-bar">
        <el-input
          v-model.trim="query.name"
          placeholder="菜品名称"
          clearable
          @keyup.enter="onSearch"
        />
        <el-select
          v-model="query.categoryId"
          placeholder="菜品分类"
          clearable
          @change="onSearch"
        >
          <el-option
            v-for="c in dishCategories"
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

        <el-table-column label="菜品名称" min-width="190">
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
              <span class="dish-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="菜品分类" width="130">
          <template #default="{ row }">
            {{ row.categoryName || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="售价" width="110" align="right">
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
          <el-empty description="没有查到菜品" :image-size="88" />
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

    <!-- 新增 / 修改 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '修改菜品' : '新增菜品'"
      width="640px"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入菜品名称" maxlength="30" />
        </el-form-item>

        <el-form-item label="菜品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择菜品分类" class="form-full">
            <el-option
              v-for="c in dishCategories"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="菜品价格" prop="price">
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
            placeholder="简要描述这道菜"
          />
        </el-form-item>

        <el-form-item label="口味做法">
          <div class="flavor-editor">
            <div v-for="(flavor, index) in form.flavors" :key="index" class="flavor-row">
              <el-input
                v-model.trim="flavor.name"
                placeholder="口味名，如 辣度"
                class="flavor-name"
                maxlength="10"
              />
              <el-select
                v-model="flavor.values"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车可新增，如 不辣 / 微辣"
                class="flavor-values"
              />
              <el-button
                link
                type="danger"
                :icon="Delete"
                @click="removeFlavor(index)"
              />
            </div>

            <el-button link type="primary" :icon="Plus" @click="addFlavor">
              添加口味
            </el-button>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { categoryList } from '@/api/category'
import {
  addDish,
  deleteDish,
  dishStatus,
  editDish,
  getDishById,
  pageDish,
} from '@/api/dish'
import { fmtDateTime, fmtMoney, splitList } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const selectedIds = ref([])
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
  flavors: [],
})

const rules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择菜品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入菜品价格', trigger: 'blur' }],
}

async function loadCategories() {
  try {
    // type=1 是菜品分类
    dishCategories.value = (await categoryList(1)) || []
  } catch {
    dishCategories.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageDish({
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

/* ---------------- 口味编辑 ---------------- */
function addFlavor() {
  form.flavors.push({ name: '', values: [] })
}

function removeFlavor(index) {
  form.flavors.splice(index, 1)
}

/** 表单里 values 是数组，后端存的是 "不辣,微辣" 这种字符串 */
function buildFlavorsPayload() {
  return form.flavors
    .filter((f) => f.name && f.name.trim())
    .map((f) => ({
      name: f.name.trim(),
      // 后端 DishFlavor.value 是 String，用逗号串起来
      value: (f.values || []).join(','),
    }))
}

/* ---------------- 弹窗 ---------------- */
function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    categoryId: undefined,
    price: 0,
    image: '',
    description: '',
    flavors: [],
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  dialogVisible.value = true
  try {
    const data = await getDishById(row.id)
    Object.assign(form, {
      id: data.id,
      name: data.name || '',
      categoryId: data.categoryId,
      price: Number(data.price || 0),
      image: data.image || '',
      description: data.description || '',
      flavors: (data.flavors || []).map((f) => ({
        name: f.name || '',
        values: splitList(f.value),
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

  const flavors = buildFlavorsPayload()
  const invalid = flavors.find((f) => !f.value)
  if (invalid) {
    ElMessage.warning(`口味「${invalid.name}」还没有设置可选值`)
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
      flavors,
    }
    if (isEdit.value) {
      await editDish(payload)
      ElMessage.success('菜品已更新')
    } else {
      await addDish(payload)
      ElMessage.success('菜品已新增，当前为起售状态')
    }
    dialogVisible.value = false
    loadList()
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

async function onToggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const label = next === 1 ? '起售' : '停售'
  try {
    await ElMessageBox.confirm(
      `${label}菜品「${row.name}」？`,
      `${label}菜品`,
      { type: 'warning', confirmButtonText: `确认${label}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await dishStatus(next, row.id)
    ElMessage.success(`已${label}`)
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除菜品「${row.name}」？起售中的菜品后端会拒绝删除。`,
      '删除菜品',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deleteDish(row.id)
    ElMessage.success('菜品已删除')
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
      `确认删除选中的 ${ids.length} 个菜品？起售中的菜品后端会拒绝删除。`,
      '批量删除菜品',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deleteDish(ids)
    ElMessage.success(`已删除 ${ids.length} 个菜品`)
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

.dish-name {
  color: var(--text);
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

/* ---------------- 口味编辑 ---------------- */
.flavor-editor {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 1px dashed var(--edge-strong);
  border-radius: var(--radius-sm);
  background: var(--surface-sunken);
}

.flavor-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.flavor-name {
  width: 138px;
  flex-shrink: 0;
}

.flavor-values {
  flex: 1;
  min-width: 0;
}
</style>
