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
  phone: userStore.userInfo.phone || '',
  email: userStore.userInfo.email || '',
  introduction: userStore.userInfo.introduction || '',
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
  <div class="spotify-profile-page">
    <div class="spotify-profile-header">
      <div class="spotify-profile-avatar" @click="handleAvatarClick">
        <el-avatar
          :src="fixUrl(userStore.userInfo.avatarUrl) || defaultAvatar"
          :size="180"
          class="spotify-avatar-img"
        />
        <div class="spotify-avatar-hover">
          <Icon icon="mdi:camera-edit" class="text-3xl" />
          <span>更换头像</span>
        </div>
      </div>
      <div class="spotify-profile-info">
        <h1 class="spotify-profile-name">{{ userStore.userInfo.username || '用户' }}</h1>
        <p class="spotify-profile-email">{{ userStore.userInfo.email || '未设置邮箱' }}</p>
        <div class="spotify-profile-stats">
          <div class="spotify-stat-item">
            <span class="spotify-stat-value">{{ userStore.userInfo.phone ? '已绑定' : '未绑定' }}</span>
            <span class="spotify-stat-label">手机</span>
          </div>
          <div class="spotify-stat-divider"></div>
          <div class="spotify-stat-item">
            <span class="spotify-stat-value">{{ userStore.userInfo.introduction ? '已设置' : '未设置' }}</span>
            <span class="spotify-stat-label">简介</span>
          </div>
        </div>
      </div>
    </div>

    <div class="spotify-profile-content">
      <div class="spotify-profile-section">
        <h2 class="spotify-section-title">账户设置</h2>
        
        <el-form
          ref="userFormRef"
          :model="userForm"
          :rules="userRules"
          label-width="0"
          class="spotify-profile-form"
        >
          <div class="spotify-form-grid">
            <div class="spotify-form-item">
              <label class="spotify-form-label">用户名</label>
              <el-form-item prop="username">
                <el-input v-model="userForm.username" placeholder="请输入用户名" class="spotify-input" />
              </el-form-item>
            </div>

            <div class="spotify-form-item">
              <label class="spotify-form-label">邮箱地址</label>
              <el-form-item prop="email">
                <el-input v-model="userForm.email" placeholder="请输入邮箱" class="spotify-input" />
              </el-form-item>
            </div>

            <div class="spotify-form-item">
              <label class="spotify-form-label">联系电话</label>
              <el-form-item prop="phone">
                <el-input v-model="userForm.phone" placeholder="请输入联系电话" class="spotify-input" />
              </el-form-item>
            </div>
          </div>

          <div class="spotify-form-full">
            <label class="spotify-form-label">个人简介</label>
            <el-form-item prop="introduction">
              <el-input
                v-model="userForm.introduction"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己吧..."
                maxlength="100"
                show-word-limit
                class="spotify-textarea"
              />
            </el-form-item>
          </div>

          <div class="spotify-form-actions">
            <button
              type="button"
              :disabled="loading"
              @click="handleSubmit"
              class="spotify-btn-save"
            >
              <Icon v-if="loading" icon="eos-icons:loading" class="text-lg" />
              <span>{{ loading ? '保存中...' : '保存更改' }}</span>
            </button>
          </div>
        </el-form>
      </div>

      <div class="spotify-profile-section spotify-danger-zone">
        <h2 class="spotify-section-title">危险区域</h2>
        <div class="spotify-danger-content">
          <div class="spotify-danger-info">
            <h3>注销账号</h3>
            <p>注销后，您的所有数据将被永久删除且无法恢复。</p>
          </div>
          <button
            type="button"
            :disabled="loading"
            @click="handleDelete"
            class="spotify-btn-danger"
          >
            注销账号
          </button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="cropperVisible"
      title="裁剪头像"
      align-center
      width="90%"
      class="spotify-cropper-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="spotify-cropper-container">
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
        <div class="spotify-cropper-footer">
          <div class="spotify-cropper-tools">
            <button type="button" class="spotify-tool-btn" @click="reset">
              <Icon icon="mdi:refresh" />
            </button>
            <button type="button" class="spotify-tool-btn" @click="changeScale(1)">
              <Icon icon="mdi:magnify-plus" />
            </button>
            <button type="button" class="spotify-tool-btn" @click="changeScale(-1)">
              <Icon icon="mdi:magnify-minus" />
            </button>
            <button type="button" class="spotify-tool-btn" @click="rotateLeft">
              <Icon icon="mdi:rotate-left" />
            </button>
            <button type="button" class="spotify-tool-btn" @click="rotateRight">
              <Icon icon="mdi:rotate-right" />
            </button>
          </div>
          <div class="spotify-cropper-actions">
            <button type="button" class="spotify-btn-cancel" @click="cropperVisible = false">取消</button>
            <button type="button" class="spotify-btn-confirm" @click="handleCropConfirm">确认</button>
          </div>
        </div>
      </template>
    </el-dialog>

    <AuthTabs v-model="authVisible" />
  </div>
</template>

<style scoped>
.spotify-profile-page {
  min-height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.spotify-profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 32px;
  background: linear-gradient(135deg, rgba(29, 185, 84, 0.15) 0%, rgba(29, 185, 84, 0.05) 100%);
  border-radius: 12px;
  margin-bottom: 24px;
}

.spotify-profile-avatar {
  position: relative;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  transition: transform 200ms ease, box-shadow 200ms ease;
}

.spotify-profile-avatar:hover {
  transform: scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}

.spotify-avatar-img {
  width: 100%;
  height: 100%;
}

.spotify-avatar-hover {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-profile-avatar:hover .spotify-avatar-hover {
  opacity: 1;
}

.spotify-profile-info {
  text-align: center;
}

.spotify-profile-name {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 4px;
}

.spotify-profile-email {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 16px;
}

.spotify-profile-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.spotify-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.spotify-stat-value {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-base, #fff);
}

.spotify-stat-label {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.spotify-profile-section {
  background-color: var(--card-bg, #181818);
  border-radius: 12px;
  padding: 24px;
}

.spotify-section-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-profile-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.spotify-form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.spotify-form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spotify-form-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-base, #fff);
}

.spotify-input :deep(.el-input__wrapper) {
  background-color: var(--bg-elevated, #242424);
  border: 1px solid transparent;
  border-radius: 8px;
  box-shadow: none;
  padding: 12px 16px;
  transition: all 200ms ease;
}

.spotify-input :deep(.el-input) {
  --el-input-bg-color: var(--bg-elevated, #242424);
}

.spotify-input :deep(.el-input__inner) {
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
}

.spotify-input :deep(.el-input__inner::placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-input :deep(.el-input__wrapper:hover) {
  border-color: var(--border-hover, rgba(255, 255, 255, 0.2));
}

.spotify-input :deep(.el-input__wrapper.is-focus) {
  border-color: #1db954;
  box-shadow: 0 0 0 2px rgba(29, 185, 84, 0.2);
}

.spotify-form-full {
  margin-bottom: 24px;
}

.spotify-textarea :deep(.el-textarea__inner) {
  background-color: var(--bg-elevated, #242424);
  border: 1px solid transparent;
  border-radius: 8px;
  box-shadow: none;
  padding: 12px 16px;
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
  resize: none;
  transition: all 200ms ease;
}

.spotify-textarea :deep(.el-textarea__inner::placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-textarea :deep(.el-textarea__inner:hover) {
  border-color: var(--border-hover, rgba(255, 255, 255, 0.2));
}

.spotify-textarea :deep(.el-textarea__inner:focus) {
  border-color: #1db954;
  box-shadow: 0 0 0 2px rgba(29, 185, 84, 0.2);
}

.spotify-textarea :deep(.el-input__count) {
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-form-actions {
  display: flex;
  justify-content: flex-end;
}

.spotify-btn-save {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  background-color: #1db954;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-btn-save:hover:not(:disabled) {
  background-color: #1ed760;
  transform: scale(1.02);
}

.spotify-btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spotify-danger-zone {
  border: 1px solid rgba(241, 94, 108, 0.3);
}

.spotify-danger-zone .spotify-section-title {
  color: #f15e6c;
}

.spotify-danger-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
}

.spotify-danger-info h3 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-base, #fff);
  margin-bottom: 4px;
}

.spotify-danger-info p {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-btn-danger {
  padding: 10px 24px;
  background-color: transparent;
  border: 1px solid #f15e6c;
  border-radius: 500px;
  color: #f15e6c;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-btn-danger:hover:not(:disabled) {
  background-color: rgba(241, 94, 108, 0.1);
}

.spotify-btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spotify-cropper-dialog :deep(.el-dialog) {
  background-color: var(--bg-surface, #121212);
  border-radius: 12px;
  overflow: hidden;
  max-width: 500px;
}

.spotify-cropper-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-cropper-dialog :deep(.el-dialog__title) {
  color: var(--text-base, #fff);
  font-weight: 700;
}

.spotify-cropper-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.spotify-cropper-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-cropper-container {
  width: 100%;
  height: 300px;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
}

.spotify-cropper-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 16px;
}

.spotify-cropper-tools {
  display: flex;
  gap: 8px;
}

.spotify-tool-btn {
  width: 36px;
  height: 36px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 50%;
  color: var(--text-base, #fff);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
  transition: background-color 200ms ease;
}

.spotify-tool-btn:hover {
  background-color: var(--bg-elevated-hover, #2a2a2a);
}

.spotify-cropper-actions {
  display: flex;
  gap: 12px;
}

.spotify-btn-cancel {
  padding: 10px 20px;
  background-color: transparent;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.3));
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-btn-cancel:hover {
  border-color: var(--text-base, #fff);
}

.spotify-btn-confirm {
  padding: 10px 24px;
  background-color: #1db954;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-btn-confirm:hover {
  background-color: #1ed760;
}

/* Light Theme */
:root:not(.dark) .spotify-profile-page {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --bg-surface: #ffffff;
  --bg-elevated: #f5f5f5;
  --bg-elevated-hover: #e8e8e8;
  --card-bg: #f8f8f8;
  --border-color: rgba(0, 0, 0, 0.1);
  --border-hover: rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .spotify-profile-header {
  background: linear-gradient(135deg, rgba(29, 185, 84, 0.1) 0%, rgba(29, 185, 84, 0.05) 100%);
}

:root:not(.dark) .spotify-profile-avatar {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-profile-avatar:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .spotify-cropper-dialog :deep(.el-dialog) {
  background-color: #ffffff;
}

:root:not(.dark) .spotify-tool-btn {
  background-color: #f5f5f5;
  color: #000;
}

:root:not(.dark) .spotify-tool-btn:hover {
  background-color: #e8e8e8;
}

:root:not(.dark) .spotify-profile-page .spotify-input {
  background-color: transparent !important;
}

:root:not(.dark) .spotify-profile-page .spotify-input :deep(.el-input__wrapper) {
  background-color: #f5f5f5 !important;
  border-color: transparent;
}

:root:not(.dark) .spotify-profile-page .spotify-input :deep(.el-input__inner) {
  color: #000000;
}

:root:not(.dark) .spotify-profile-page .spotify-input :deep(.el-input__wrapper:hover) {
  background-color: #e8e8e8 !important;
  border-color: rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-profile-page .spotify-input :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff !important;
  border-color: #1db954;
}

:root:not(.dark) .spotify-profile-page .spotify-textarea :deep(.el-textarea__inner) {
  background-color: #f5f5f5 !important;
  border-color: transparent;
  color: #000000;
}

:root:not(.dark) .spotify-profile-page .spotify-textarea :deep(.el-textarea__inner:hover) {
  background-color: #e8e8e8 !important;
  border-color: rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-profile-page .spotify-textarea :deep(.el-textarea__inner:focus) {
  background-color: #ffffff !important;
  border-color: #1db954;
}

:root:not(.dark) .spotify-btn-cancel {
  border-color: rgba(0, 0, 0, 0.2);
  color: #000;
}

:root:not(.dark) .spotify-btn-cancel:hover {
  border-color: #000;
}

@media (min-width: 768px) {
  .spotify-profile-header {
    flex-direction: row;
    padding: 40px;
  }

  .spotify-profile-info {
    text-align: left;
  }

  .spotify-profile-stats {
    justify-content: flex-start;
  }

  .spotify-form-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .spotify-danger-content {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

@media (min-width: 1024px) {
  .spotify-profile-page {
    max-width: 900px;
    margin: 0 auto;
  }
}
</style>
