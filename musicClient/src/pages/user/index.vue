<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import defaultAvatar from '@/assets/user.jpg'
import {
  updateUserInfo,
  updateUserAvatar,
  deleteUser,
  getUserInfo,
} from '@/api/system'
import { fixUrl } from '@/utils'
import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'
import { useRouter } from 'vue-router'
import AuthTabs from '@/components/Auth/AuthTabs.vue'

const router = useRouter()
const userStore = UserStore()
const loading = ref(false)
const userFormRef = ref<FormInstance>()
const cropperVisible = ref(false)
const cropperImg = ref('')
const cropper = ref<any>(null)
const authVisible = ref(false)

const userForm = reactive({
  userId: userStore.userInfo.userId,
  username: userStore.userInfo.username || '',
  phone: (userStore.userInfo as any).phone || '',
  email: (userStore.userInfo as any).email || '',
  introduction: (userStore.userInfo as any).introduction || '',
})

const userRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]{4,16}$/,
      message: '用户名格式：4-16位字符（字母、数字、下划线、连字符）',
      trigger: 'blur',
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  introduction: [
    { max: 100, message: '简介不能超过100个字符', trigger: 'blur' },
  ],
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    authVisible.value = true
  }
})

const handleAvatarClick = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = (e: Event) => {
    const target = e.target as HTMLInputElement
    const file = target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const result = e.target?.result
        if (typeof result === 'string') {
          cropperImg.value = result
          cropperVisible.value = true
        }
      }
      reader.readAsDataURL(file)
    }
  }
  input.click()
}

const reset = () => {
  if (cropper.value) {
    cropper.value.refresh()
  }
}

const changeScale = (num: number) => {
  if (cropper.value) {
    cropper.value.changeScale(num)
  }
}

const rotateLeft = () => {
  if (cropper.value) {
    cropper.value.rotateLeft()
  }
}

const rotateRight = () => {
  if (cropper.value) {
    cropper.value.rotateRight()
  }
}

const handleCropConfirm = async () => {
  if (!cropper.value) return
  cropper.value.getCropData(async (base64: string) => {
    try {
      const response = await fetch(base64)
      const blob = await response.blob()

      const formData = new FormData()
      formData.append('avatar', blob, 'avatar.png')

      const res = await updateUserAvatar(formData)

      if (res.code === 0) {
        const userInfoResponse = await getUserInfo()
        if (userInfoResponse.code === 0) {
          userStore.setUserInfo(userInfoResponse.data, userStore.userInfo.token)
          ElMessage.success('头像更新成功')
          cropperVisible.value = false
          cropperImg.value = ''
        } else {
          ElMessage.error(userInfoResponse.message || '获取用户信息失败')
        }
      } else {
        ElMessage.error(res.message || '头像更新失败')
      }
    } catch (error: any) {
      console.error('头像更新错误:', error)
      ElMessage.error(error.message || '头像更新失败')
    }
  })
}

const handleSubmit = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await updateUserInfo(userForm)
        if (response.code === 0) {
          const userInfoResponse = await getUserInfo()
          userStore.setUserInfo(userInfoResponse.data, userStore.userInfo.token)
          ElMessage.success('更新成功')
        } else {
          ElMessage.error(response.message || '更新失败')
        }
      } catch (error: any) {
        ElMessage.error(error.message || '更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '注销账号后，所有数据将被清除且无法恢复，是否确认注销？',
      '警告',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    loading.value = true
    const response = await deleteUser()
    if (response.code === 0) {
      userStore.clearUserInfo()
      ElMessage.success('账号已注销')
      router.push('/')
    } else {
      ElMessage.error(response.message || '注销失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '注销失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mr-profile-page">
    <h1 class="mr-page-title">账户设置</h1>
    
    <div class="mr-profile-card">
      <div class="mr-profile-header">
        <div class="mr-profile-avatar" @click="handleAvatarClick">
          <el-avatar
            :src="fixUrl(userStore.userInfo.avatarUrl) || defaultAvatar"
            :size="80"
            class="mr-avatar-img"
          />
          <div class="mr-avatar-hover">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20">
              <path fill="currentColor" d="M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z"/>
              <path fill="currentColor" d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/>
            </svg>
          </div>
        </div>
        <div class="mr-profile-info">
          <h2 class="mr-profile-name">{{ userStore.userInfo.username || '用户' }}</h2>
        </div>
      </div>
    </div>

    <div class="mr-profile-section">
      <h3 class="mr-section-title">个人信息</h3>
      
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="0"
        class="mr-profile-form"
      >
        <div class="mr-form-row">
          <label class="mr-form-label">用户名</label>
          <el-form-item prop="username">
            <el-input v-model="userForm.username" placeholder="请输入用户名" class="mr-input" />
          </el-form-item>
        </div>

        <div class="mr-form-row">
          <label class="mr-form-label">邮箱地址</label>
          <el-form-item prop="email">
            <el-input v-model="userForm.email" placeholder="请输入邮箱" class="mr-input" />
          </el-form-item>
        </div>

        <div class="mr-form-row">
          <label class="mr-form-label">联系电话</label>
          <el-form-item prop="phone">
            <el-input v-model="userForm.phone" placeholder="请输入联系电话" class="mr-input" />
          </el-form-item>
        </div>

        <div class="mr-form-row">
          <label class="mr-form-label">个人简介</label>
          <el-form-item prop="introduction">
            <el-input
              v-model="userForm.introduction"
              type="textarea"
              :rows="3"
              placeholder="介绍一下自己吧..."
              maxlength="100"
              show-word-limit
              class="mr-textarea"
            />
          </el-form-item>
        </div>

        <div class="mr-form-actions">
          <button
            type="button"
            :disabled="loading"
            @click="handleSubmit"
            class="mr-btn-save"
          >
            <svg v-if="loading" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16" class="animate-spin">
              <path fill="currentColor" d="M12 4V2A10 10 0 0 0 2 12h2a8 8 0 0 1 8-8z"/>
            </svg>
            <span>{{ loading ? '保存中...' : '保存更改' }}</span>
          </button>
        </div>
      </el-form>
    </div>

    <div class="mr-profile-section mr-danger-zone">
      <h3 class="mr-section-title">危险区域</h3>
      <div class="mr-danger-content">
        <div class="mr-danger-info">
          <span>注销账号</span>
          <p>注销后，您的所有数据将被永久删除且无法恢复。</p>
        </div>
        <button
          type="button"
          :disabled="loading"
          @click="handleDelete"
          class="mr-btn-danger"
        >
          注销账号
        </button>
      </div>
    </div>

    <el-dialog
      v-model="cropperVisible"
      title="裁剪头像"
      align-center
      width="90%"
      class="mr-cropper-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="mr-cropper-container">
        <vue-cropper
          ref="cropper"
          :img="cropperImg"
          :info="true"
          :canScale="true"
          :autoCrop="true"
          :fixedBox="true"
          :canMove="true"
          :canMoveBox="true"
          :centerBox="true"
          :infoTrue="true"
          :fixed="true"
          :fixedNumber="[1, 1]"
          :high="true"
          mode="cover"
          :round="true"
        />
      </div>
      <template #footer>
        <div class="mr-cropper-footer">
          <div class="mr-cropper-tools">
            <button type="button" class="mr-tool-btn" @click="reset">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor" d="M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-8 3.58-8 8s3.58 8 8 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
              </svg>
            </button>
            <button type="button" class="mr-tool-btn" @click="changeScale(1)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
              </svg>
            </button>
            <button type="button" class="mr-tool-btn" @click="changeScale(-1)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor" d="M19 13H5v-2h14v2z"/>
              </svg>
            </button>
            <button type="button" class="mr-tool-btn" @click="rotateLeft">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor" d="M7.11 8.53L5.7 7.11C4.8 8.27 4.24 9.61 4.07 11h2.02c.14-.87.49-1.72 1.02-2.47zM6.09 13H4.07c.17 1.39.72 2.73 1.62 3.89l1.41-1.42c-.52-.75-.87-1.59-1.01-2.47zm1.01 5.32c1.16.9 2.51 1.44 3.9 1.61V17.9c-.87-.15-1.71-.49-2.46-1.03L7.1 18.32zM13 4.07V1L8.45 5.55 13 10V6.09c2.84.48 5 2.94 5 5.91s-2.16 5.43-5 5.91v2.02c3.95-.49 7-3.85 7-7.93s-3.05-7.44-7-7.93z"/>
              </svg>
            </button>
            <button type="button" class="mr-tool-btn" @click="rotateRight">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                <path fill="currentColor" d="M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02c-2.84-.48-5-2.94-5-5.91s2.16-5.43 5-5.91V10l4.55-4.45zM19.93 11c-.17-1.39-.72-2.73-1.62-3.89l-1.42 1.42c.54.75.88 1.6 1.02 2.47h2.02zM13 17.9v2.02c1.39-.17 2.74-.71 3.9-1.61l-1.44-1.44c-.75.54-1.59.89-2.46 1.03zm3.89-2.42l1.42 1.41c.9-1.16 1.45-2.5 1.62-3.89h-2.02c-.14.87-.48 1.72-1.02 2.48z"/>
              </svg>
            </button>
          </div>
          <div class="mr-cropper-actions">
            <button type="button" class="mr-btn-cancel" @click="cropperVisible = false">取消</button>
            <button type="button" class="mr-btn-confirm" @click="handleCropConfirm">确认</button>
          </div>
        </div>
      </template>
    </el-dialog>

    <AuthTabs v-model="authVisible" />
  </div>
</template>

<style scoped>
.mr-profile-page {
  min-height: 100%;
  padding: 24px;
  overflow-y: auto;
  max-width: 680px;
  margin: 0 auto;
}

.mr-page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-highlight, #f5f5f7);
  margin-bottom: 20px;
}

.mr-profile-card {
  background-color: var(--card-bg, #181818);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 12px;
}

.mr-profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mr-profile-avatar {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 200ms ease;
  background-color: #282828;
  padding: 8px;
}

.mr-profile-avatar:hover {
  transform: scale(1.03);
}

.mr-avatar-img {
  width: 100%;
  height: 100%;
}

.mr-avatar-hover {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 200ms ease;
}

.mr-profile-avatar:hover .mr-avatar-hover {
  opacity: 1;
}

.mr-profile-info {
  flex: 1;
  min-width: 0;
}

.mr-profile-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-base, #fff);
  margin-bottom: 4px;
}

.mr-profile-section {
  background-color: var(--card-bg, #181818);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 12px;
}

.mr-section-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.mr-profile-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.mr-form-row {
  margin-bottom: 16px;
}

.mr-form-row:last-of-type {
  margin-bottom: 20px;
}

.mr-form-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-highlight, #f5f5f7);
  margin-bottom: 6px;
}

.mr-input :deep(.el-input__wrapper) {
  background-color: var(--bg-elevated, #242424);
  border: 1px solid transparent;
  border-radius: 6px;
  box-shadow: none;
  padding: 10px 14px;
  transition: all 200ms ease;
}

.mr-input :deep(.el-input__inner) {
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
}

.mr-input :deep(.el-input__inner::placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.mr-input :deep(.el-input__wrapper:hover) {
  border-color: var(--border-hover, rgba(255, 255, 255, 0.15));
}

.mr-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--mr-accent);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--text-base, #fff) 15%, transparent);
}

.mr-textarea :deep(.el-textarea__inner) {
  background-color: var(--bg-elevated, #242424);
  border: 1px solid transparent;
  border-radius: 6px;
  box-shadow: none;
  padding: 10px 14px;
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
  resize: none;
  transition: all 200ms ease;
}

.mr-textarea :deep(.el-textarea__inner::placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.mr-textarea :deep(.el-textarea__inner:hover) {
  border-color: var(--border-hover, rgba(255, 255, 255, 0.15));
}

.mr-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--mr-accent);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--text-base, #fff) 15%, transparent);
}

.mr-textarea :deep(.el-input__count) {
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
}

.mr-form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.mr-btn-save {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background-color: #ffffff;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.mr-btn-save:hover:not(:disabled) {
  background-color: #e0e0e0;
  transform: scale(1.02);
}

.mr-btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mr-danger-zone {
  border: 1px solid rgba(241, 94, 108, 0.25);
}

.mr-danger-zone .mr-section-title {
  color: #f15e6c;
}

.mr-danger-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.mr-danger-info span {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  display: block;
  margin-bottom: 2px;
}

.mr-danger-info p {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-btn-danger {
  padding: 8px 20px;
  background-color: transparent;
  border: 1px solid #f15e6c;
  border-radius: 500px;
  color: #f15e6c;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
  white-space: nowrap;
}

.mr-btn-danger:hover:not(:disabled) {
  background-color: rgba(241, 94, 108, 0.1);
}

.mr-btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mr-cropper-dialog :deep(.el-dialog) {
  background-color: var(--bg-surface, #121212);
  border-radius: 12px;
  overflow: hidden;
  max-width: 480px;
}

.mr-cropper-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.mr-cropper-dialog :deep(.el-dialog__title) {
  color: var(--text-base, #fff);
  font-weight: 600;
  font-size: 1rem;
}

.mr-cropper-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.mr-cropper-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.mr-cropper-container {
  width: 100%;
  height: 280px;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
}

.mr-cropper-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 16px;
}

.mr-cropper-tools {
  display: flex;
  gap: 6px;
}

.mr-tool-btn {
  width: 32px;
  height: 32px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 50%;
  color: var(--text-base, #fff);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 200ms ease;
}

.mr-tool-btn:hover {
  background-color: var(--bg-elevated-hover, #2a2a2a);
}

.mr-cropper-actions {
  display: flex;
  gap: 10px;
}

.mr-btn-cancel {
  padding: 8px 16px;
  background-color: transparent;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.25));
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.mr-btn-cancel:hover {
  border-color: var(--text-base, #fff);
}

.mr-btn-confirm {
  padding: 8px 20px;
  background-color: var(--mr-accent);
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.mr-btn-confirm:hover {
  background-color: var(--mr-accent-hover);
}

/* Light Theme */
:root:not(.dark) .mr-profile-page {
  --text-base: #1d1d1f;
  --text-subdued: #86868b;
  --text-highlight: #1d1d1f;
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --bg-elevated-hover: #d8d8d8;
  --card-bg: #f0f0f0;
  --border-color: rgba(0, 0, 0, 0.08);
  --border-hover: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .mr-profile-card,
:root:not(.dark) .mr-profile-section {
  background-color: #f0f0f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

:root:not(.dark) .mr-profile-avatar {
  background-color: #f0f0f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-cropper-dialog :deep(.el-dialog) {
  background-color: #ffffff;
}

:root:not(.dark) .mr-tool-btn {
  background-color: #f5f5f7;
  color: #1d1d1f;
}

:root:not(.dark) .mr-tool-btn:hover {
  background-color: #e8e8ed;
}

:root:not(.dark) .mr-input :deep(.el-input__wrapper) {
  background-color: #f5f5f7;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-input :deep(.el-input__inner) {
  color: #1d1d1f;
}

:root:not(.dark) .mr-input :deep(.el-input__wrapper:hover) {
  background-color: #f0f0f0;
  border-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .mr-input :deep(.el-input__wrapper.is-focus) {
  background-color: #f5f5f7;
  border-color: var(--mr-accent);
}

:root:not(.dark) .mr-textarea :deep(.el-textarea__inner) {
  background-color: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  color: #1d1d1f;
}

:root:not(.dark) .mr-textarea :deep(.el-textarea__inner:hover) {
  border-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .mr-textarea :deep(.el-textarea__inner:focus) {
  background-color: #ffffff;
  border-color: var(--mr-accent);
}

:root:not(.dark) .mr-btn-cancel {
  border-color: rgba(0, 0, 0, 0.15);
  color: #1d1d1f;
}

:root:not(.dark) .mr-btn-cancel:hover {
  border-color: #1d1d1f;
}

:root:not(.dark) .mr-btn-save {
  background-color: #1d1d1f;
  color: #ffffff;
}

:root:not(.dark) .mr-btn-save:hover:not(:disabled) {
  background-color: #333333;
}

@media (max-width: 768px) {
  .mr-profile-page {
    padding: 16px;
    padding-bottom: 140px;
  }
  
  .mr-page-title {
    font-size: 1.25rem;
    margin-bottom: 16px;
  }
  
  .mr-profile-card {
    padding: 16px;
  }
  
  .mr-profile-avatar {
    width: 64px;
    height: 64px;
  }
  
  .mr-profile-name {
    font-size: 1.125rem;
  }
  
  .mr-profile-section {
    padding: 16px;
  }
  
  .mr-section-title {
    font-size: 0.75rem;
    margin-bottom: 12px;
  }
  
  .mr-form-row {
    margin-bottom: 14px;
  }
  
  .mr-form-label {
    font-size: 0.75rem;
  }
  
  .mr-btn-save {
    width: 100%;
    justify-content: center;
  }
  
  .mr-danger-content {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }
  
  .mr-danger-info {
    text-align: center;
    margin-bottom: 8px;
  }
  
  .mr-btn-danger {
    width: 100%;
  }
  
  .mr-cropper-container {
    height: 240px;
  }
  
  .mr-cropper-footer {
    flex-direction: column;
    gap: 12px;
  }
  
  .mr-cropper-tools {
    width: 100%;
    justify-content: center;
  }
  
  .mr-cropper-actions {
    width: 100%;
  }
  
  .mr-btn-cancel,
  .mr-btn-confirm {
    flex: 1;
  }
}
</style>
