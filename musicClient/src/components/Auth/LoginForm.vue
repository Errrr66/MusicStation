<script setup lang="ts">
import { ref, reactive } from 'vue'
import { UserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import type { FormRules } from 'element-plus'

const userStore = UserStore()
const emit = defineEmits(['success', 'switch-tab'])

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
})

const loginRules = reactive<FormRules>({
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await userStore.userLogin(loginForm)
        if (result.success) {
          ElMessage.success(result.message)
          emit('success')
        } else {
          ElMessage.error(result.message)
        }
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const switchToRegister = () => {
  emit('switch-tab', 'register')
}

const switchToReset = () => {
  emit('switch-tab', 'reset')
}
</script>

<template>
  <div class="spotify-form">
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      label-width="0"
      @keyup.enter="handleLogin"
    >
      <el-form-item prop="email">
        <el-input
          v-model="loginForm.email"
          placeholder="邮箱"
          class="spotify-input"
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          placeholder="密码"
          show-password
          class="spotify-input"
        />
      </el-form-item>

      <div class="spotify-form-options">
        <label class="spotify-checkbox">
          <input type="checkbox" />
          <span>记住我</span>
        </label>
        <a href="#" class="spotify-link" @click.prevent="switchToReset">忘记密码？</a>
      </div>

      <el-form-item>
        <button
          type="button"
          class="spotify-btn-primary"
          :disabled="loading"
          @click="handleLogin"
        >
          登录
        </button>
      </el-form-item>
    </el-form>

    <div class="spotify-divider"></div>

    <div class="spotify-form-footer">
      <span>还没有账户？</span>
      <a href="#" class="spotify-link-highlight" @click.prevent="switchToRegister">注册</a>
    </div>
  </div>
</template>

<style scoped>
.spotify-form {
  width: 100%;
}

.spotify-input :deep(.el-input__wrapper) {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  height: 40px;
  padding: 0 12px;
  box-shadow: none;
  transition: background-color 200ms ease, border-color 200ms ease;
}

.spotify-input :deep(.el-input__wrapper:hover) {
  background: transparent;
  border-color: #727272;
}

.spotify-input :deep(.el-input__wrapper.is-focus) {
  background: transparent;
  border-color: #fff;
}

.spotify-input :deep(.el-input__inner) {
  color: #fff;
  font-size: 14px;
}

.spotify-input :deep(.el-input__inner::placeholder) {
  color: #747474;
}

.spotify-form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0 16px;
}

.spotify-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #b3b3b3;
  cursor: pointer;
}

.spotify-checkbox input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #1db954;
}

.spotify-link {
  font-size: 13px;
  color: #b3b3b3;
  text-decoration: none;
  transition: color 200ms ease;
}

.spotify-link:hover {
  color: #fff;
  text-decoration: underline;
}

.spotify-btn-primary {
  width: 100%;
  height: 48px;
  background-color: #fff;
  border: none;
  border-radius: 9999px;
  color: #000;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
}

.spotify-btn-primary:hover:not(:disabled) {
  transform: scale(1.04);
}

.spotify-btn-primary:active:not(:disabled) {
  transform: scale(1);
}

.spotify-btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spotify-divider {
  height: 1px;
  background-color: hsla(0, 0%, 100%, 0.1);
  margin: 20px 0;
}

.spotify-form-footer {
  text-align: center;
}

.spotify-form-footer span {
  font-size: 14px;
  color: #b3b3b3;
}

.spotify-link-highlight {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  margin-left: 4px;
}

.spotify-link-highlight:hover {
  text-decoration: underline;
}

/* Light Theme */
:root:not(.dark) .spotify-input :deep(.el-input__wrapper) {
  background: transparent;
  border: 1px solid transparent;
}

:root:not(.dark) .spotify-input :deep(.el-input__wrapper:hover) {
  background: transparent;
  border-color: #d9d9d9;
}

:root:not(.dark) .spotify-input :deep(.el-input__wrapper.is-focus) {
  background: transparent;
  border-color: #000;
}

:root:not(.dark) .spotify-input :deep(.el-input__inner) {
  color: #000;
}

:root:not(.dark) .spotify-input :deep(.el-input__inner::placeholder) {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-checkbox {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-link {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-link:hover {
  color: #000;
}

:root:not(.dark) .spotify-btn-primary {
  background-color: #000;
  color: #fff;
}

:root:not(.dark) .spotify-divider {
  background-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-form-footer span {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-link-highlight {
  color: #000;
}
</style>
