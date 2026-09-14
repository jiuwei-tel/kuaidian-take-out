<template>
  <div class="uploader">
    <div class="uploader-box" :class="{ 'is-loading': uploading }">
      <template v-if="modelValue">
        <el-image
          :src="modelValue"
          fit="cover"
          class="uploader-img"
          :preview-src-list="[modelValue]"
          preview-teleported
        />
        <div class="uploader-mask">
          <button type="button" @click="pick">替换</button>
          <button type="button" @click="clear">删除</button>
        </div>
      </template>

      <button v-else type="button" class="uploader-empty" @click="pick">
        <el-icon><Plus /></el-icon>
        <span>上传图片</span>
      </button>

      <div v-if="uploading" class="uploader-progress">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>上传中…</span>
      </div>
    </div>

    <div class="uploader-tip">
      <button type="button" class="uploader-link" @click="manualOpen = !manualOpen">
        手填图片地址
      </button>
      <span class="muted">建议 200×200</span>
    </div>

    <el-input
      v-if="manualOpen"
      :model-value="modelValue"
      size="small"
      clearable
      placeholder="https://..."
      @update:model-value="emit('update:modelValue', $event)"
    />

    <input
      ref="inputRef"
      type="file"
      accept="image/png,image/jpeg,image/jpg,image/webp,image/gif"
      hidden
      @change="onFileChange"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/common'

const props = defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const inputRef = ref(null)
const uploading = ref(false)
const manualOpen = ref(false)

const MAX_SIZE = 5 * 1024 * 1024

function pick() {
  inputRef.value?.click()
}

function clear() {
  emit('update:modelValue', '')
}

async function onFileChange(event) {
  const file = event.target.files?.[0]
  // 同一个文件连续选两次也要能触发 change
  event.target.value = ''
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }

  if (file.size > MAX_SIZE) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    const url = await uploadFile(file)
    if (url && typeof url === 'string') {
      emit('update:modelValue', url)
      ElMessage.success('图片上传成功')
    } else {
      manualOpen.value = true
      ElMessage.warning('上传接口没有返回图片地址，请手动填写')
    }
  } catch {
    // 常见原因：alioss 的 access-key 已失效。降级成手填地址，不阻塞你继续录数据
    manualOpen.value = true
  } finally {
    uploading.value = false
  }
}

defineExpose({ pick })
</script>

<style scoped>
.uploader {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 148px;
}

.uploader-box {
  position: relative;
  width: 148px;
  height: 148px;
  border: 1px dashed var(--edge-strong);
  border-radius: var(--radius-sm);
  background: var(--surface-sunken);
  overflow: hidden;
}

.uploader-box:hover .uploader-mask {
  opacity: 1;
}

.uploader-img {
  width: 100%;
  height: 100%;
  display: block;
}

.uploader-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 100%;
  border: 0;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.15s ease;
}

.uploader-empty:hover {
  color: var(--teal);
}

.uploader-empty .el-icon {
  font-size: 22px;
}

.uploader-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  background: rgba(22, 33, 31, 0.62);
  opacity: 0;
  transition: opacity 0.16s ease;
}

.uploader-mask button {
  border: 0;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 12.5px;
  color: #fff;
  cursor: pointer;
}

.uploader-mask button:hover {
  color: var(--brand);
}

.uploader-progress {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  color: var(--text-2);
}

.uploader-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
}

.uploader-link {
  padding: 0;
  border: 0;
  background: transparent;
  font-family: var(--font-sans);
  font-size: 11.5px;
  color: var(--teal);
  cursor: pointer;
}

.uploader-link:hover {
  text-decoration: underline;
}
</style>
