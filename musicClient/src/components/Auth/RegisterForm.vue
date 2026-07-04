<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { sendEmailCode, register } from '@/api/system'

const emit = defineEmits(['success', 'switch-tab'])

const loading = ref(false)
const countdown = ref(0)
const registerFormRef = ref()

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  verificationCode: '',
})

const registerRules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
})

const handleSendCode = async () => {
  try {
    if (!registerForm.email) {
      ElMessage.warning('请先输入邮箱')
      return
    }
    const response = await sendEmailCode(registerForm.email)
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

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await register(registerForm)
        if (response.code === 0) {
          ElMessage.success('注册成功，请登录')
          emit('switch-tab', 'login')
        } else {
          ElMessage.error(response.message)
        }
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败')
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
    <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0">
      <el-form-item prop="username">
        <el-input v-model="registerForm.username" placeholder="用户名" class="mr-input" />
      </el-form-item>

      <el-form-item prop="email">
        <div class="mr-input-row">
          <el-input v-model="registerForm.email" placeholder="邮箱" class="mr-input" />
          <button type="button" class="mr-code-btn" :disabled="!!countdown" @click="handleSendCode">
            {{ countdown ? `${countdown}s` : '验证码' }}
          </button>
        </div>
      </el-form-item>

      <el-form-item prop="verificationCode">
        <el-input v-model="registerForm.verificationCode" placeholder="验证码" class="mr-input" />
      </el-form-item>

      <el-form-item prop="password">
        <el-input v-model="registerForm.password" type="password" placeholder="密码" show-password class="mr-input" />
      </el-form-item>

      <el-form-item>
        <button type="button" class="mr-btn-primary" :disabled="loading" @click="handleRegister">注册</button>
      </el-form-item>
    </el-form>

    <div class="mr-divider"></div>

    <div class="mr-form-footer">
      <span>已有账户？</span>
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
