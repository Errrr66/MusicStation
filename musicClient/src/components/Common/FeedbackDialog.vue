<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { addFeedback } from '@/api/system'
import { Icon } from '@iconify/vue'

const dialogVisible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  content: '',
})

const rules = reactive<FormRules>({
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 10, message: '反馈内容不能少于 10 个字符', trigger: 'blur' },
    { max: 200, message: '反馈内容不能超过 200 个字符', trigger: 'blur' },
  ],
})

const openDialog = () => {
  formData.content = ''
  formRef.value?.resetFields()
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await addFeedback({ content: formData.content })
        if (res.code === 0) {
          ElMessage.success('反馈提交成功，感谢您的意见！')
          closeDialog()
        } else {
          ElMessage.error(res.message || '提交失败，请稍后再试')
        }
      } catch (error: any) {
        console.error('Feedback submission error:', error)
        ElMessage.error(error.message || '提交反馈时发生错误')
      } finally {
        loading.value = false
      }
    }
  })
}

defineExpose({ openDialog })
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    width="480px"
    :close-on-click-modal="false"
    :show-close="false"
    class="spotify-feedback-dialog"
    @close="closeDialog"
  >
    <template #header>
      <div class="spotify-dialog-header">
        <div class="spotify-dialog-title-wrapper">
          <Icon icon="mdi:message-text-outline" class="spotify-dialog-icon" />
          <span class="spotify-dialog-title">意见反馈</span>
        </div>
        <button class="spotify-dialog-close" @click="closeDialog">
          <Icon icon="mdi:close" />
        </button>
      </div>
    </template>

    <div class="spotify-dialog-content">
      <p class="spotify-dialog-desc">我们非常重视您的意见，请告诉我们您的想法和建议。</p>
      
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="0px">
        <el-form-item prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="6"
            placeholder="请输入您的宝贵意见或建议（10-200字）"
            maxlength="200"
            show-word-limit
            class="spotify-textarea"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="spotify-dialog-footer">
        <button class="spotify-btn-cancel" @click="closeDialog">取消</button>
        <button
          class="spotify-btn-submit"
          :disabled="loading"
          @click="handleSubmit"
        >
          <Icon v-if="loading" icon="eos-icons:loading" class="text-lg" />
          <span v-else>提交反馈</span>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.spotify-feedback-dialog :deep(.el-dialog) {
  background-color: #282828;
  border-radius: 12px;
  overflow: hidden;
}

.spotify-feedback-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.spotify-feedback-dialog :deep(.el-dialog__body) {
  padding: 0 24px;
}

.spotify-feedback-dialog :deep(.el-dialog__footer) {
  padding: 0 24px 24px;
}

.spotify-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.spotify-dialog-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.spotify-dialog-icon {
  font-size: 1.5rem;
  color: #1db954;
}

.spotify-dialog-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
}

.spotify-dialog-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #b3b3b3;
  font-size: 1.25rem;
  cursor: pointer;
  transition: background-color 200ms ease, color 200ms ease;
}

.spotify-dialog-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.spotify-dialog-content {
  padding-top: 16px;
}

.spotify-dialog-desc {
  font-size: 0.875rem;
  color: #b3b3b3;
  margin-bottom: 16px;
  line-height: 1.5;
}

.spotify-textarea :deep(.el-textarea__inner) {
  background-color: #3e3e3e;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 12px 16px;
  color: #fff;
  font-size: 0.9375rem;
  line-height: 1.5;
  resize: none;
  transition: border-color 200ms ease, box-shadow 200ms ease;
}

.spotify-textarea :deep(.el-textarea__inner::placeholder) {
  color: #6a6a6a;
}

.spotify-textarea :deep(.el-textarea__inner:hover) {
  border-color: rgba(255, 255, 255, 0.2);
}

.spotify-textarea :deep(.el-textarea__inner:focus) {
  border-color: #1db954;
  box-shadow: 0 0 0 2px rgba(29, 185, 84, 0.2);
}

.spotify-textarea :deep(.el-input__count) {
  background: transparent;
  color: #6a6a6a;
}

.spotify-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.spotify-btn-cancel {
  padding: 12px 24px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 500px;
  color: #fff;
  font-size: 0.9375rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-btn-cancel:hover {
  border-color: #fff;
  transform: scale(1.02);
}

.spotify-btn-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 32px;
  background-color: #1db954;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-btn-submit:hover:not(:disabled) {
  background-color: #1ed760;
  transform: scale(1.02);
}

.spotify-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Light Theme */
:root:not(.dark) .spotify-feedback-dialog :deep(.el-dialog) {
  background-color: #fff;
}

:root:not(.dark) .spotify-dialog-header {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-dialog-title {
  color: #000;
}

:root:not(.dark) .spotify-dialog-close {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-dialog-close:hover {
  background-color: rgba(0, 0, 0, 0.08);
  color: #000;
}

:root:not(.dark) .spotify-dialog-desc {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-textarea :deep(.el-textarea__inner) {
  background-color: #f5f5f5;
  color: #000;
}

:root:not(.dark) .spotify-textarea :deep(.el-textarea__inner::placeholder) {
  color: #9a9a9a;
}

:root:not(.dark) .spotify-textarea :deep(.el-textarea__inner:hover) {
  border-color: rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .spotify-textarea :deep(.el-input__count) {
  color: #9a9a9a;
}

:root:not(.dark) .spotify-btn-cancel {
  border-color: rgba(0, 0, 0, 0.2);
  color: #000;
}

:root:not(.dark) .spotify-btn-cancel:hover {
  border-color: #000;
}
</style>
