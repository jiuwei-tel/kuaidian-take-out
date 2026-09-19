<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">优惠券</h2>
        <p class="page-sub">
          顾客在点餐端的领券中心抢券，提交订单时选用；一张券只能用一次，用掉后变「已使用」
        </p>
      </div>
      <div class="page-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增优惠券</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="filter-bar">
        <el-input
          v-model.trim="query.name"
          placeholder="券名称"
          clearable
          @keyup.enter="onSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable class="filter-select">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="list" row-key="id" stripe>
        <el-table-column prop="name" label="券名称" min-width="140" />

        <el-table-column label="抵扣金额" width="110">
          <template #default="{ row }">
            <span class="money num">¥{{ Number(row.value).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="使用门槛" width="120">
          <template #default="{ row }">
            <span class="num muted">满 {{ Number(row.minAmount).toFixed(0) }} 元</span>
          </template>
        </el-table-column>

        <el-table-column label="剩余库存" width="100">
          <template #default="{ row }">
            <span class="num">{{ row.stock }}</span>
          </template>
        </el-table-column>

        <el-table-column label="已领取" width="90">
          <template #default="{ row }">
            <span class="num">{{ row.receivedCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="活动时间" width="170">
          <template #default="{ row }">
            <div class="num muted time-line">{{ row.beginTime }}</div>
            <div class="num muted time-line">至 {{ row.endTime }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <span class="dot" :class="row.status === 1 ? 'dot-on' : 'dot-off'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" size="small" @click="openEdit(row)">修改</el-button>
              <el-button
                link
                :type="row.status === 1 ? 'warning' : 'primary'"
                size="small"
                @click="onToggleStatus(row)"
              >
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
              <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="没有查到优惠券" :image-size="88" />
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
      :title="isEdit ? '修改优惠券' : '新增优惠券'"
      width="480px"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="给顾客看的名字" maxlength="30" />
        </el-form-item>

        <el-form-item label="抵扣金额" prop="value">
          <el-input-number v-model="form.value" :min="1" :max="9999" :precision="2" />
          <span class="form-tip">元</span>
        </el-form-item>

        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0" :max="9999" :precision="2" />
          <span class="form-tip">订单金额满这个数才能用</span>
        </el-form-item>

        <el-form-item label="发放数量" prop="stock">
          <el-input-number v-model="form.stock" :min="1" :max="99999" :step="10" />
          <span class="form-tip">张</span>
        </el-form-item>

        <el-form-item label="活动时间" prop="range">
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm"
            format="YYYY-MM-DD HH:mm"
            :teleported="false"
          />
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
  addVoucher,
  deleteVoucher,
  getVoucherPage,
  setVoucherStatus,
  updateVoucher,
} from '@/api/voucher'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  status: null,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  value: 30,
  minAmount: 30,
  stock: 100,
  range: [],
})

const rules = {
  name: [
    { required: true, message: '请输入券名称', trigger: 'blur' },
    { min: 2, max: 30, message: '名称长度 2~30 个字符', trigger: 'blur' },
  ],
  value: [{ required: true, message: '请输入抵扣金额', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入使用门槛', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入发放数量', trigger: 'blur' }],
  range: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
}

async function loadList() {
  loading.value = true
  try {
    const data = await getVoucherPage({
      page: query.page,
      pageSize: query.pageSize,
      name: query.name || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status,
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
  query.status = null
  query.page = 1
  loadList()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    value: 30,
    minAmount: 30,
    stock: 100,
    range: [],
  })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    value: Number(row.value || 0),
    minAmount: Number(row.minAmount || 0),
    stock: Number(row.stock || 0),
    // 后端给的是 yyyy-MM-dd HH:mm，正好和选择器的 value-format 对得上
    range: row.beginTime && row.endTime ? [row.beginTime, row.endTime] : [],
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
      value: form.value,
      minAmount: form.minAmount,
      stock: form.stock,
      beginTime: form.range?.[0],
      endTime: form.range?.[1],
    }
    if (isEdit.value) {
      await updateVoucher(payload)
      ElMessage.success('优惠券已更新')
    } else {
      await addVoucher(payload)
      ElMessage.success('优惠券已新增')
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
  const label = next === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(
      `${label}「${row.name}」？${next === 0 ? '下架后顾客在领券中心就看不到它了。' : ''}`,
      `${label}优惠券`,
      { type: 'warning', confirmButtonText: `确认${label}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await setVoucherStatus(row.id, next)
    ElMessage.success(`已${label}`)
    loadList()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.name}」？已经领到的顾客手里那张也会一起失效。`,
      '删除优惠券',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  try {
    await deleteVoucher(row.id)
    ElMessage.success('已删除')
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

.filter-select {
  width: 130px;
}

.time-line {
  font-size: 12px;
  line-height: 1.6;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-3);
}
</style>
