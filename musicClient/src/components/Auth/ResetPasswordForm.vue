<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { sendEmailCode, resetPassword } from '@/api/system'

const emit = defineEmits(['success', 'switch-tab'])

const loading = ref(false)
const countdown = ref(0)
const resetFormRef = ref()

const resetForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
})

const resetRules = reactive<FormRules>({
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
})

const handleSendCode = async () => {
  try {
    if (!resetForm.email) {
      ElMessage.warning('请先输入邮箱')
      return
    }
    const response = await sendEmailCode(resetForm.email)
    if (response.code === 0) {
      ElMessage.success('验证码已发送')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    } else {
      ElMessage.error(response.message)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  }
}

const handleReset = async () => {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await resetPassword(resetForm)
        if (response.code === 0) {
          ElMessage.success('密码重置成功，请登录')
          emit('switch-tab', 'login')
        } else {
          ElMessage.error(response.message)
        }
      } catch (error: any) {
        ElMessage.error(error.message || '重置密码失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const switchToLogin = () => emit('switch-tab', 'login')
</script>

<template>
  <div class="mr-form">
    <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="0">
      <el-form-item prop="email">
        <div class="mr-input-row">
          <el-input v-model="resetForm.email" placeholder="邮箱" class="mr-input" />
          <button type="button" class="mr-code-btn" :disabled="!!countdown" @click="handleSendCode">
            {{ countdown ? `${countdown}s` : '验证码' }}
          </button>
        </div>
      </el-form-item>

      <el-form-item prop="verificationCode">
        <el-input v-model="resetForm.verificationCode" placeholder="验证码" class="mr-input" />
      </el-form-item>

      <el-form-item prop="newPassword">
        <el-input v-model="resetForm.newPassword" type="password" placeholder="新密码" show-password class="mr-input" />
      </el-form-item>

      <el-form-item>
        <button type="button" class="mr-btn-primary" :disabled="loading" @click="handleReset">重置密码</button>
      </el-form-item>
    </el-form>

    <div class="mr-divider"></div>

    <div class="mr-form-footer">
      <span>想起密码了？</span>
      <a href="#" class="mr-link-highlight" @click.prevent="switchToLogin">登录</a>
    </div>
  </div>
</template>

<style scoped>
.mr-form { width: 100%; }

.mr-input :deep(.el-input__wrapper) {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  height: 40px;
  padding: 0 12px;
  box-shadow: none;
  transition: border-color 200ms ease;
}

.mr-input :deep(.el-input__wrapper:hover) { background: transparent; border-color: #727272; }
.mr-input :deep(.el-input__wrapper.is-focus) { background: transparent; border-color: #fff; }
.mr-input :deep(.el-input__inner) { color: #fff; font-size: 14px; }
.mr-input :deep(.el-input__inner::placeholder) { color: #747474; }

.mr-input-row { display: flex; gap: 8px; }
.mr-input-row .mr-input { flex: 1; }

.mr-code-btn {
  padding: 0 16px;
  height: 40px;
  background-color: #fff;
  border: none;
  border-radius: 9999px;
  color: #000;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: transform 33ms ease, background-color 200ms ease;
}

.mr-code-btn:hover:not(:disabled) {
  transform: scale(1.04);
}

.mr-code-btn:active:not(:disabled) {
  transform: scale(1);
}

.mr-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mr-btn-primary {
  width: 100%;
  height: 48px;
  background-color: #fff;
  border: none;
  border-radius: 9999px;
  color: #000;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease;
}

.mr-btn-primary:hover:not(:disabled) { transform: scale(1.04); }
.mr-btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.mr-divider { height: 1px; background-color: hsla(0, 0%, 100%, 0.1); margin: 20px 0; }
.mr-form-footer { text-align: center; }
.mr-form-footer span { font-size: 14px; color: #b3b3b3; }
.mr-link-highlight { font-size: 14px; font-weight: 600; color: #fff; text-decoration: none; margin-left: 4px; }
.mr-link-highlight:hover { text-decoration: underline; }

/* Light Theme */
:root:not(.dark) .mr-input :deep(.el-input__wrapper) {
  background: transparent;
  border: 1px solid transparent;
}
:root:not(.dark) .mr-input :deep(.el-input__wrapper:hover) {
  background: transparent;
  border-color: #d9d9d9;
}
:root:not(.dark) .mr-input :deep(.el-input__wrapper.is-focus) {
  background: transparent;
  border-color: #000;
}
:root:not(.dark) .mr-input :deep(.el-input__inner) { color: #000; }
:root:not(.dark) .mr-input :deep(.el-input__inner::placeholder) { color: #6a6a6a; }
:root:not(.dark) .mr-code-btn {
  background-color: #000;
  color: #fff;
}
:root:not(.dark) .mr-btn-primary { background-color: #000; color: #fff; }
:root:not(.dark) .mr-divider { background-color: rgba(0, 0, 0, 0.1); }
:root:not(.dark) .mr-form-footer span { color: #6a6a6a; }
:root:not(.dark) .mr-link-highlight { color: #000; }
</style>
