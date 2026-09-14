<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">员工管理</h2>
        <p class="page-sub">新增员工的初始密码由后端统一分配，请提醒其首次登录后修改</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="filter-bar">
        <el-input
          v-model.trim="query.name"
          placeholder="员工姓名"
          clearable
          @keyup.enter="onSearch"
        />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" row-key="id" stripe>
        <el-table-column prop="name" label="姓名" min-width="120" />

        <el-table-column prop="username" label="账号" min-width="140">
          <template #default="{ row }">
            <span class="order-no">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column label="手机号" width="140">
          <template #default="{ row }">
            <span class="num">{{ row.phone || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            {{ SEX_TEXT[row.sex] || '—' }}
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="104">
          <template #default="{ row }">
            <span class="dot" :class="row.status === 1 ? 'dot-on' : 'dot-off'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="最后操作时间" width="150">
          <template #default="{ row }">
            <span class="num muted">{{ fmtDateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
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
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有查到员工" :image-size="88" />
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
      :title="isEdit ? '修改员工' : '新增员工'"
      width="460px"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="82px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入姓名" maxlength="20" />
        </el-form-item>

        <el-form-item label="账号" prop="username">
          <el-input
            v-model.trim="form.username"
            placeholder="登录用的用户名"
            maxlength="30"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>

        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio value="1">男</el-radio>
            <el-radio value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model.trim="form.idNumber" placeholder="18 位身份证号" maxlength="18" />
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
  addEmployee,
  editEmployee,
  enableOrDisableEmployee,
  pageEmployee,
} from '@/api/employee'
import { fmtDateTime } from '@/utils/format'

const SEX_TEXT = { 1: '男', 0: '女' }

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10,
  name: '',
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  username: '',
  phone: '',
  sex: '1',
  idNumber: '',
})

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度 2~20 个字符', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 30, message: '账号长度 3~30 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idNumber: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    {
      pattern: /^\d{17}[\dXx]$/,
      message: '身份证号格式不正确',
      trigger: 'blur',
    },
  ],
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageEmployee({
      page: query.page,
      pageSize: query.pageSize,
      name: query.name || undefined,
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
  query.page = 1
  loadList()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    username: '',
    phone: '',
    sex: '1',
    idNumber: '',
  })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    username: row.username || '',
    phone: row.phone || '',
    sex: row.sex === '0' ? '0' : '1',
    idNumber: row.idNumber || '',
  })
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
      username: form.username,
      phone: form.phone,
      sex: form.sex,
      idNumber: form.idNumber,
    }
    if (isEdit.value) {
      await editEmployee(payload)
      ElMessage.success('员工信息已更新')
    } else {
      await addEmployee(payload)
      ElMessage.success('员工已新增')
    }
    dialogVisible.value = false
    loadList()
  } catch {
    /* 拦截器已提示，重名等错误也走这里 */
  } finally {
    submitting.value = false
  }
}

async function onToggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const label = next === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `${label}员工「${row.name}」的账号？`,
      `${label}账号`,
      { type: 'warning', confirmButtonText: `确认${label}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await enableOrDisableEmployee(next, row.id)
    ElMessage.success(`已${label}`)
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
</style>
