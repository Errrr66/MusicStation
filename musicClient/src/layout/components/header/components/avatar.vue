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
    class="mr-avatar-dropdown"
    trigger="click"
  >
    <div class="mr-avatar-trigger">
      <el-avatar
        :src="fixUrl(user.userInfo.avatarUrl) || defaultAvatar"
        class="mr-avatar"
        shape="circle"
        :size="32"
      />
    </div>

    <template #dropdown>
      <div class="mr-dropdown-menu">
        <div class="mr-dropdown-actions">
          <div class="mr-dropdown-action" @click="router.push('/user')" title="个人中心">
            <Icon icon="mdi:account-outline" class="mr-dropdown-action-icon" />
          </div>
          <div class="mr-dropdown-action" @click="openFeedbackDialog" title="意见反馈">
            <Icon icon="mdi:message-text-outline" class="mr-dropdown-action-icon" />
          </div>
          <div class="mr-dropdown-action mr-dropdown-action-logout" @click="handleLogout" title="退出登录">
            <Icon icon="mdi:logout" class="mr-dropdown-action-icon" />
          </div>
        </div>
      </div>
    </template>
  </el-dropdown>
  <button
    v-else
    class="mr-login-btn"
    @click="showLogin = true"
  >
    <Icon icon="mdi:account-outline" class="text-lg" />
    <span>登录</span>
  </button>
  <AuthTabs v-if="showLogin" v-model="showLogin" />
  <FeedbackDialog ref="feedbackDialogRef" />
</template>

<style scoped>
.mr-avatar-dropdown {
  cursor: pointer;
}

.mr-avatar-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #3e3e3e;
  padding: 4px;
  transition: background-color 200ms ease;
  overflow: hidden;
}

.mr-avatar-trigger:hover {
  background-color: #4e4e4e;
}

.mr-avatar {
  transition: transform 200ms ease;
}

.mr-avatar-trigger:hover .mr-avatar {
  transform: scale(1.05);
}

.mr-dropdown-menu {
  background-color: #000000;
  border-radius: 8px;
  padding: 8px;
  min-width: 200px;
  box-shadow: 0 16px 24px rgba(0, 0, 0, 0.3), 0 6px 8px rgba(0, 0, 0, 0.2);
}

.mr-dropdown-actions {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 8px 12px;
}

.mr-dropdown-action {
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

.mr-dropdown-action:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.mr-dropdown-action-icon {
  font-size: 1.25rem;
}

.mr-dropdown-action-logout {
  color: #b3b3b3;
}

.mr-dropdown-action-logout:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.mr-login-btn {
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

.mr-login-btn:hover {
  transform: scale(1.04);
}

/* Light Theme */
:root:not(.dark) .mr-avatar-trigger {
  background-color: #f0f0f0;
}

:root:not(.dark) .mr-avatar-trigger:hover {
  background-color: #e0e0e0;
}

:root:not(.dark) .mr-dropdown-menu {
  background-color: #ffffff;
}

:root:not(.dark) .mr-dropdown-action {
  color: #6a6a6a;
}

:root:not(.dark) .mr-dropdown-action:hover {
  background-color: rgba(0, 0, 0, 0.1);
  color: #000000;
}

:root:not(.dark) .mr-dropdown-action-logout {
  color: #6a6a6a;
}

:root:not(.dark) .mr-dropdown-action-logout:hover {
  background-color: rgba(0, 0, 0, 0.1);
  color: #000000;
}

:root:not(.dark) .mr-login-btn {
  background-color: #000;
  color: #fff;
}
</style>
