<script setup lang="ts">
import { ref, computed } from 'vue'
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'
import ResetPasswordForm from './ResetPasswordForm.vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits(['update:modelValue'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const activeTab = ref('login')

const handleSuccess = () => {
  dialogVisible.value = false
}

const handleClose = () => {
  activeTab.value = 'login'
}

const handleSwitchTab = (tab: string) => {
  activeTab.value = tab
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :show-close="false"
    width="340px"
    class="spotify-auth-dialog"
    @close="handleClose"
    >
    <div class="spotify-auth-container">
      <h2 class="spotify-auth-title">
        {{ activeTab === 'login' ? '登录' : activeTab === 'register' ? '注册' : '重置密码' }}
      </h2>

      <div class="spotify-auth-content">
        <Transition name="fade" mode="out-in">
          <LoginForm
            v-if="activeTab === 'login'"
            key="login"
            @success="handleSuccess"
            @switch-tab="handleSwitchTab"
          />
          <RegisterForm
            v-else-if="activeTab === 'register'"
            key="register"
            @success="handleSuccess"
            @switch-tab="handleSwitchTab"
          />
          <ResetPasswordForm
            v-else
            key="reset"
            @success="handleSuccess"
            @switch-tab="handleSwitchTab"
          />
        </Transition>
      </div>

      <button class="spotify-auth-close" @click="dialogVisible = false">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
          <path d="M19 6.41L17.59 5L12 10.59L6.41 5L5 6.41L10.59 12L5 17.59L6.41 19L12 13.41L17.59 19L19 17.59L13.41 12z"/>
        </svg>
      </button>
    </div>
  </el-dialog>
</template>

<style scoped>
.spotify-auth-dialog :deep(.el-dialog) {
  background-color: #282828;
  border-radius: 8px;
  overflow: hidden;
  max-width: 170px;
  width: 170px !important;
}

.spotify-auth-dialog :deep(.el-dialog__header) {
  display: none;
}

.spotify-auth-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.spotify-auth-container {
  padding: 24px;
  position: relative;
}

.spotify-auth-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 20px;
  text-align: center;
}

.spotify-auth-content {
  width: 100%;
}

.spotify-auth-close {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #b3b3b3;
  cursor: pointer;
  transition: color 200ms ease;
}

.spotify-auth-close:hover {
  color: #fff;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 150ms ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Light Theme */
:root:not(.dark) .spotify-auth-dialog :deep(.el-dialog) {
  background-color: #fff;
}

:root:not(.dark) .spotify-auth-title {
  color: #000;
}

:root:not(.dark) .spotify-auth-close {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-auth-close:hover {
  color: #000;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .spotify-auth-dialog :deep(.el-dialog) {
    width: 100% !important;
    max-width: 100% !important;
    border-radius: 0;
    margin: 0;
    min-height: 100vh;
  }
  
  .spotify-auth-container {
    padding: 20px;
  }
  
  .spotify-auth-title {
    font-size: 1.125rem;
    margin-bottom: 16px;
  }
}
</style>
