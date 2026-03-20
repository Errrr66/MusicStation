<script setup lang="ts">
import { ref } from 'vue'
import { UserStore } from '@/stores/modules/user'
import AuthTabs from '@/components/Auth/AuthTabs.vue'
import FeedbackDialog from '@/components/Common/FeedbackDialog.vue'
import defaultAvatar from '@/assets/user.jpg'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/system'
import { useRouter } from 'vue-router'
import { fixUrl } from '@/utils'
import { Icon } from '@iconify/vue'

const showLogin = ref(false)
const user = UserStore()
const router = useRouter()
const feedbackDialogRef = ref<InstanceType<typeof FeedbackDialog> | null>(null)

const handleLogout = async () => {
  try {
    const response = await logout()
    if (response.code === 0) {
      user.clearUserInfo()
      ElMessage.success('退出登录成功')
    } else {
      ElMessage.error(response.message || '退出失败')
    }
  } catch (error: any) {
    console.error('退出登录错误:', error)
    ElMessage.error(error.message || '退出失败')
    user.clearUserInfo()
  }
}

const openFeedbackDialog = () => {
  feedbackDialogRef.value?.openDialog()
}
</script>

<template>
  <el-dropdown
    v-if="user.userInfo && user.userInfo.userId"
    class="spotify-avatar-dropdown"
    trigger="click"
  >
    <div class="spotify-avatar-trigger">
      <el-avatar
        :src="fixUrl(user.userInfo.avatarUrl) || defaultAvatar"
        class="spotify-avatar"
        shape="circle"
        :size="32"
      />
    </div>

    <template #dropdown>
      <div class="spotify-dropdown-menu">
        <div class="spotify-dropdown-header">
          <el-avatar
            :src="fixUrl(user.userInfo.avatarUrl) || defaultAvatar"
            :size="48"
            class="spotify-dropdown-avatar"
          />
          <div class="spotify-dropdown-user-info">
            <span class="spotify-dropdown-username">{{ user.userInfo.username }}</span>
            <span class="spotify-dropdown-email">{{ user.userInfo.email || '未设置邮箱' }}</span>
          </div>
        </div>
        <div class="spotify-dropdown-actions">
          <div class="spotify-dropdown-action" @click="router.push('/user')" title="个人中心">
            <Icon icon="mdi:account-outline" class="spotify-dropdown-action-icon" />
          </div>
          <div class="spotify-dropdown-action" @click="openFeedbackDialog" title="意见反馈">
            <Icon icon="mdi:message-text-outline" class="spotify-dropdown-action-icon" />
          </div>
          <div class="spotify-dropdown-action spotify-dropdown-action-logout" @click="handleLogout" title="退出登录">
            <Icon icon="mdi:logout" class="spotify-dropdown-action-icon" />
          </div>
        </div>
      </div>
    </template>
  </el-dropdown>
  <button
    v-else
    class="spotify-login-btn"
    @click="showLogin = true"
  >
    <Icon icon="mdi:account-outline" class="text-lg" />
    <span>登录</span>
  </button>
  <AuthTabs v-if="showLogin" v-model="showLogin" />
  <FeedbackDialog ref="feedbackDialogRef" />
</template>

<style scoped>
.spotify-avatar-dropdown {
  cursor: pointer;
}

.spotify-avatar-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.7);
  transition: background-color 200ms ease;
  overflow: hidden;
}

.spotify-avatar-trigger:hover {
  background-color: rgba(0, 0, 0, 0.9);
}

.spotify-avatar {
  transition: transform 200ms ease;
}

.spotify-avatar-trigger:hover .spotify-avatar {
  transform: scale(1.05);
}

.spotify-dropdown-menu {
  background-color: #121212;
  border-radius: 8px;
  padding: 8px;
  min-width: 200px;
  box-shadow: 0 16px 24px rgba(0, 0, 0, 0.3), 0 6px 8px rgba(0, 0, 0, 0.2);
}

.spotify-dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.spotify-dropdown-avatar {
  flex-shrink: 0;
}

.spotify-dropdown-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.spotify-dropdown-username {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-dropdown-email {
  font-size: 0.75rem;
  color: #b3b3b3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-dropdown-actions {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 8px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.spotify-dropdown-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #b3b3b3;
  cursor: pointer;
  transition: background-color 200ms ease, color 200ms ease;
}

.spotify-dropdown-action:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.spotify-dropdown-action-icon {
  font-size: 1.25rem;
}

.spotify-dropdown-action-logout {
  color: #e91429;
}

.spotify-dropdown-action-logout:hover {
  background-color: rgba(233, 20, 41, 0.1);
  color: #e91429;
}

.spotify-login-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background-color: #fff;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease;
}

.spotify-login-btn:hover {
  transform: scale(1.04);
}

/* Light Theme */
:root:not(.dark) .spotify-avatar-trigger {
  background-color: rgba(255, 255, 255, 0.9);
}

:root:not(.dark) .spotify-avatar-trigger:hover {
  background-color: #fff;
}

:root:not(.dark) .spotify-dropdown-actions {
  border-top-color: rgba(255, 255, 255, 0.1);
}

:root:not(.dark) .spotify-dropdown-action {
  color: #b3b3b3;
}

:root:not(.dark) .spotify-dropdown-action:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

:root:not(.dark) .spotify-login-btn {
  background-color: #000;
  color: #fff;
}
</style>
