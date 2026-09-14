<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">分类管理</h2>
        <p class="page-sub">菜品分类用于菜品归类，套餐分类用于套餐归类</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="filter-bar">
        <el-input
          v-model.trim="query.name"
          placeholder="分类名称"
          clearable
          @keyup.enter="onSearch"
        />
        <el-select v-model="query.type" placeholder="分类类型" clearable @change="onSearch">
          <el-option label="菜品分类" :value="1" />
          <el-option label="套餐分类" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" row-key="id" stripe>
        <el-table-column prop="name" label="分类名称" min-width="160" />

        <el-table-column label="分类类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'warning'" effect="plain" size="small">
              {{ CATEGORY_TYPE_TEXT[row.type] || '—' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="排序" width="80" align="right">
          <template #default="{ row }">
            <span class="num">{{ row.sort ?? 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <span class="dot" :class="row.status === 1 ? 'dot-on' : 'dot-off'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作时间" width="150">
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
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" size="small" @click="onDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="还没有分类，先新增一个" :image-size="88" />
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '修改分类' : '新增分类'"
      width="440px"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入分类名称" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="分类类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">菜品分类</el-radio>
            <el-radio :value="2">套餐分类</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
          <span class="form-hint">数字越小越靠前</span>
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
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  addCategory,
  deleteCategory,
  editCategory,
  enableOrDisableCategory,
  pageCategory,
} from '@/api/category'
import { CATEGORY_TYPE_TEXT } from '@/constants'
import { fmtDateTime } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  type: 1,
  sort: 0,
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageCategory({
      page: query.page,
      pageSize: query.pageSize,
      name: query.name || undefined,
      type: query.type,
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

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  query.name = ''
  query.type = undefined
  query.page = 1
  loadList()
}

function openCreate() {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.type = 1
  form.sort = 0
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.type = row.type
  form.sort = row.sort ?? 0
  dialogVisible.value = true
}

function onDialogClosed() {
  formRef.value?.clearValidate()
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      type: form.type,
      sort: form.sort,
    }
    if (isEdit.value) {
      await editCategory(payload)
      ElMessage.success('分类已更新')
    } else {
      await addCategory(payload)
      ElMessage.success('分类已新增')
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
  const label = next === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `${label}分类「${row.name}」？`,
      `${label}分类`,
      { type: 'warning', confirmButtonText: `确认${label}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await enableOrDisableCategory(next, row.id)
    ElMessage.success(`已${label}`)
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${row.name}」？该分类下如果还有菜品或套餐，后端会拒绝删除。`,
      '删除分类',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    // 删掉当前页最后一条时要回退一页
    if (list.value.length === 1 && query.page > 1) query.page -= 1
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(loadList)
</script>

<style scoped>
.row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 8px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-3);
}
</style>
