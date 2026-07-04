<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { addFeedback } from '@/api/system'
import { Icon } from '@iconify/vue'

const dialogVisible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

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
    :width="isMobile ? '90%' : '480px'"
    :close-on-click-modal="false"
    :show-close="false"
    class="mr-feedback-dialog"
    @close="closeDialog"
  >
    <template #header>
      <div class="mr-dialog-header">
        <div class="mr-dialog-title-wrapper">
          <Icon icon="mdi:message-text-outline" class="mr-dialog-icon" />
          <span class="mr-dialog-title">意见反馈</span>
        </div>
        <button class="mr-dialog-close" @click="closeDialog">
          <Icon icon="mdi:close" />
        </button>
      </div>
    </template>

    <div class="mr-dialog-content">
      <p class="mr-dialog-desc">我们非常重视您的意见，请告诉我们您的想法和建议。</p>
      
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="0px">
        <el-form-item prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="isMobile ? 4 : 6"
            placeholder="请输入您的宝贵意见或建议（10-200字）"
            maxlength="200"
            show-word-limit
            class="mr-textarea"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="mr-dialog-footer">
        <button class="mr-btn-cancel" @click="closeDialog">取消</button>
        <button
          class="mr-btn-submit"
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
.mr-feedback-dialog :deep(.el-dialog) {
  background-color: #282828;
  border-radius: 12px;
  overflow: hidden;
}

.mr-feedback-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.mr-feedback-dialog :deep(.el-dialog__body) {
  padding: 0 24px;
}

.mr-feedback-dialog :deep(.el-dialog__footer) {
  padding: 0 24px 24px;
}

.mr-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.mr-dialog-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mr-dialog-icon {
  font-size: 1.5rem;
  color: var(--mr-accent);
}

.mr-dialog-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
}

.mr-dialog-close {
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

.mr-dialog-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.mr-dialog-content {
  padding-top: 16px;
}

.mr-dialog-desc {
  font-size: 0.875rem;
  color: #b3b3b3;
  margin-bottom: 16px;
  line-height: 1.5;
}

.mr-textarea :deep(.el-textarea__inner) {
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

.mr-textarea :deep(.el-textarea__inner::placeholder) {
  color: #6a6a6a;
}

.mr-textarea :deep(.el-textarea__inner:hover) {
  border-color: rgba(255, 255, 255, 0.2);
}

.mr-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--mr-accent);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--text-base, #fff) 20%, transparent);
}

.mr-textarea :deep(.el-input__count) {
  background: transparent;
  color: #6a6a6a;
}

.mr-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.mr-btn-cancel {
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

.mr-btn-cancel:hover {
  border-color: #fff;
  transform: scale(1.02);
}

.mr-btn-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 32px;
  background-color: var(--mr-accent);
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 200ms ease;
}

.mr-btn-submit:hover:not(:disabled) {
  background-color: var(--mr-accent-hover);
  transform: scale(1.02);
}

.mr-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Light Theme */
:root:not(.dark) .mr-feedback-dialog :deep(.el-dialog) {
  background-color: #fff;
}

:root:not(.dark) .mr-dialog-header {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-dialog-title {
  color: #000;
}

:root:not(.dark) .mr-dialog-close {
  color: #6a6a6a;
}

:root:not(.dark) .mr-dialog-close:hover {
  background-color: rgba(0, 0, 0, 0.08);
  color: #000;
}

:root:not(.dark) .mr-dialog-desc {
  color: #6a6a6a;
}

:root:not(.dark) .mr-textarea :deep(.el-textarea__inner) {
  background-color: #f5f5f5;
  color: #000;
}

:root:not(.dark) .mr-textarea :deep(.el-textarea__inner::placeholder) {
  color: #9a9a9a;
}

:root:not(.dark) .mr-textarea :deep(.el-textarea__inner:hover) {
  border-color: rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .mr-textarea :deep(.el-input__count) {
  color: #9a9a9a;
}

:root:not(.dark) .mr-btn-cancel {
  border-color: rgba(0, 0, 0, 0.2);
  color: #000;
}

:root:not(.dark) .mr-btn-cancel:hover {
  border-color: #000;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .mr-feedback-dialog :deep(.el-dialog) {
    max-height: 85vh;
    margin: 5vh auto 0;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
  }
  
  .mr-dialog-header {
    padding: 16px 20px;
    flex-shrink: 0;
  }
  
  .mr-dialog-title {
    font-size: 1rem;
  }
  
  .mr-dialog-icon {
    font-size: 1.25rem;
  }
  
  .mr-feedback-dialog :deep(.el-dialog__body) {
    padding: 0 16px;
    overflow-y: auto;
  }
  
  .mr-feedback-dialog :deep(.el-dialog__footer) {
    padding: 0 16px calc(16px + env(safe-area-inset-bottom));
    flex-shrink: 0;
  }
  
  .mr-dialog-desc {
    font-size: 0.8125rem;
  }
  
  .mr-textarea :deep(.el-textarea__inner) {
    padding: 10px 12px;
    font-size: 0.875rem;
  }
  
  .mr-dialog-footer {
    flex-direction: column;
  }
  
  .mr-btn-cancel,
  .mr-btn-submit {
    width: 100%;
    justify-content: center;
  }
}
</style>
