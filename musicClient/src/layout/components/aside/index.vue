<script setup lang="ts">
import { MenuData } from './data'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'
import { ref, watch } from 'vue'
import { UserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import AuthTabs from '@/components/Auth/AuthTabs.vue'
import { useFavoriteStore } from '@/stores/modules/favorite'
import coverImg from '@/assets/cover.png'

const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const router = useRouter()
const user = UserStore()
const favoriteStore = useFavoriteStore()
const authVisible = ref(false)
const isCollapsed = ref(!props.isMobile)

const handleProtectedRoute = (path: string) => {
  if (!user.isLoggedIn && (path === '/like' || path === '/user')) {
    ElMessage.warning('请先登录')
    authVisible.value = true
    return false
  }
  return true
}


const emit = defineEmits(['close'])

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

watch(
  () => user.isLoggedIn,
  (newVal) => {
    if (newVal) {
      favoriteStore.getFavoritePlaylists()
    } else {
      favoriteStore.clearFavoritePlaylists()
    }
  },
  { immediate: true }
)

const handleMenuClick = (path: string) => {
  if (handleProtectedRoute(path)) {
    router.push(path)
    if (props.isMobile) emit('close')
  }
}

const handlePlaylistClick = (id: number) => {
  router.push(`/playlist/${id}`)
  if (props.isMobile) emit('close')
}
</script>

<template>
  <aside
    class="mr-sidebar"
    :class="[
      isCollapsed ? 'mr-sidebar-collapsed' : 'mr-sidebar-expanded',
      isMobile ? 'mr-sidebar-mobile' : 'mr-sidebar-desktop'
    ]"
  >
    <div class="mr-sidebar-content">
      <div class="mr-sidebar-header" @click="!isMobile && toggleCollapse()">
        <div class="mr-sidebar-logo" :class="{ 'mr-logo-centered': isCollapsed && !isMobile }">
          <Icon icon="mdi:library-music" class="mr-logo-icon" />
          <span v-if="!isCollapsed && !isMobile" class="mr-logo-text">音乐库</span>
        </div>
        <button v-if="isMobile" class="mr-mobile-close" @click="$emit('close')">
          <Icon icon="mdi:close" />
        </button>
      </div>

      <div class="mr-sidebar-main">
        <div
          v-for="(section, sIndex) in MenuData"
          :key="sIndex"
          class="mr-nav-section"
        >
          <div v-if="sIndex > 0 && !isCollapsed" class="mr-nav-divider"></div>
          
          <h4 v-if="!isCollapsed && section.title" class="mr-nav-title">
            {{ section.title }}
          </h4>

          <div class="mr-nav-items">
            <div
              v-for="(item, iIndex) in section.children"
              :key="iIndex"
              class="mr-nav-item"
              :class="{ 'mr-nav-item-active': route.path === item.router }"
              @click="handleMenuClick(item.router)"
              :title="isCollapsed ? item.title : ''"
            >
              <div class="mr-nav-item-icon-wrapper">
                <Icon :icon="item.icon" class="mr-nav-item-icon" />
              </div>
              <span v-if="!isCollapsed" class="mr-nav-item-text">{{ item.title }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="user.isLoggedIn && !isCollapsed" class="mr-sidebar-footer">
        <div class="mr-footer-header">
          <Icon icon="mdi:folder-heart" class="mr-footer-icon" />
          <span class="mr-footer-title">收藏的歌单</span>
          <span class="mr-footer-count">{{ favoriteStore.favoritePlaylists.length }}</span>
        </div>

        <el-scrollbar class="mr-footer-scroll">
          <div v-if="favoriteStore.loading" class="mr-footer-loading">
            <Icon icon="eos-icons:loading" class="text-2xl" />
          </div>
          <div v-else class="mr-footer-list">
            <div
              v-for="item in favoriteStore.favoritePlaylists"
              :key="item.id"
              class="mr-playlist-item"
              :class="{ 'mr-playlist-active': route.path === `/playlist/${item.id}` }"
              @click="handlePlaylistClick(item.id)"
            >
              <el-image
                lazy
                :src="(item.coverImgUrl && item.coverImgUrl.startsWith('http')) ? item.coverImgUrl + '?param=50y50' : (item.coverImgUrl || coverImg)"
                class="mr-playlist-cover"
                :alt="item.name"
              >
                <template #error>
                  <div class="mr-playlist-cover-placeholder">
                    <Icon icon="ri:music-2-line" />
                  </div>
                </template>
              </el-image>
              <div class="mr-playlist-info">
                <span class="mr-playlist-name">{{ item.name }}</span>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div v-if="user.isLoggedIn && isCollapsed" class="mr-sidebar-footer-collapsed">
        <div class="mr-footer-divider"></div>
        <div
          v-for="(item, index) in favoriteStore.favoritePlaylists.slice(0, 3)"
          :key="index"
          class="mr-mini-playlist"
          :class="{ 'mr-playlist-active': route.path === `/playlist/${item.id}` }"
          @click="handlePlaylistClick(item.id)"
        >
          <el-image
            lazy
            :src="(item.coverImgUrl && item.coverImgUrl.startsWith('http')) ? item.coverImgUrl + '?param=50y50' : (item.coverImgUrl || coverImg)"
            class="mr-mini-cover"
          >
            <template #error>
              <div class="mr-playlist-cover-placeholder">
                <Icon icon="ri:music-2-line" />
              </div>
            </template>
          </el-image>
        </div>
      </div>
    </div>

    <AuthTabs v-model="authVisible" />
  </aside>
</template>

<style scoped>
.mr-sidebar {
  display: flex;
  flex-direction: column;
  background-color: #121212;
  overflow: hidden;
  transition: width 200ms ease;
}

.mr-sidebar-desktop {
  display: none;
}

.mr-sidebar-mobile {
  width: 100% !important;
  height: 100% !important;
}

.mr-sidebar-expanded {
  width: 280px;
}

.mr-sidebar-collapsed {
  width: 72px;
}

.mr-sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px;
  gap: 8px;
}

.mr-sidebar-header {
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-sidebar-header:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mr-logo-centered {
  justify-content: center;
  width: 100%;
}

.mr-logo-icon {
  font-size: 1.5rem;
  color: #b3b3b3;
}

.mr-logo-text {
  font-size: 1rem;
  font-weight: 700;
  color: #b3b3b3;
}

.mr-mobile-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #b3b3b3;
  font-size: 1.5rem;
  cursor: pointer;
  margin-left: auto;
  transition: color 200ms ease, background-color 200ms ease;
}

.mr-mobile-close:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-sidebar-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mr-sidebar-main::-webkit-scrollbar {
  width: 4px;
}

.mr-sidebar-main::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 20px;
}

.mr-nav-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mr-nav-divider {
  height: 1px;
  background-color: rgba(255, 255, 255, 0.1);
  margin: 8px 12px;
}

.mr-nav-title {
  padding: 8px 12px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: #b3b3b3;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.mr-nav-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mr-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-nav-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-nav-item-active {
  background-color: rgba(255, 255, 255, 0.2);
}

.mr-nav-item-active .mr-nav-item-icon {
  color: #fff;
}

.mr-nav-item-active .mr-nav-item-text {
  color: #fff;
  font-weight: 600;
}

.mr-nav-item-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.mr-nav-item-icon {
  font-size: 1.5rem;
  color: #b3b3b3;
}

.mr-nav-item-text {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #b3b3b3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mr-sidebar-footer {
  display: flex;
  flex-direction: column;
  max-height: 240px;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.mr-footer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.mr-footer-icon {
  font-size: 1.125rem;
  color: var(--mr-accent);
}

.mr-footer-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #fff;
}

.mr-footer-count {
  font-size: 0.75rem;
  color: #b3b3b3;
  margin-left: auto;
}

.mr-footer-scroll {
  flex: 1;
  overflow: hidden;
}

.mr-footer-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #b3b3b3;
}

.mr-footer-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
}

.mr-playlist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-playlist-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-playlist-active {
  background-color: rgba(255, 255, 255, 0.15);
}

.mr-playlist-cover {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.mr-playlist-cover-placeholder {
  width: 100%;
  height: 100%;
  background-color: #282828;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #b3b3b3;
  font-size: 1.25rem;
}

.mr-playlist-info {
  flex: 1;
  min-width: 0;
}

.mr-playlist-name {
  font-size: 0.8125rem;
  color: #fff;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.mr-sidebar-footer-collapsed {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
}

.mr-footer-divider {
  height: 1px;
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-mini-playlist {
  display: flex;
  justify-content: center;
  padding: 4px 0;
  cursor: pointer;
}

.mr-mini-playlist:hover .mr-mini-cover {
  transform: scale(1.05);
}

.mr-mini-cover {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
  transition: transform 200ms ease;
}

.mr-sidebar-collapsed .mr-nav-item {
  justify-content: center;
  padding: 12px 0;
}

.mr-sidebar-collapsed .mr-nav-item-icon {
  font-size: 1.75rem;
}

/* Light Theme */
:root:not(.dark) .mr-sidebar {
  background-color: #f0f0f0;
}

:root:not(.dark) .mr-sidebar-header:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-logo-icon,
:root:not(.dark) .mr-logo-text {
  color: #000;
}

:root:not(.dark) .mr-nav-item:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-nav-item-active {
  background-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .mr-nav-item-icon,
:root:not(.dark) .mr-nav-title,
:root:not(.dark) .mr-nav-item-text {
  color: #6a6a6a;
}

:root:not(.dark) .mr-nav-item-active .mr-nav-item-icon,
:root:not(.dark) .mr-nav-item-active .mr-nav-item-text {
  color: #000;
}

:root:not(.dark) .mr-nav-divider {
  background-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-sidebar-footer {
  background-color: rgba(0, 0, 0, 0.05);
}

:root:not(.dark) .mr-footer-header {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-footer-title,
:root:not(.dark) .mr-playlist-name {
  color: #000;
}

:root:not(.dark) .mr-playlist-item:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-playlist-active {
  background-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .mr-playlist-cover-placeholder {
  background-color: #e0e0e0;
}

:root:not(.dark) .mr-footer-divider {
  background-color: rgba(0, 0, 0, 0.1);
}

@media (min-width: 768px) {
  .mr-sidebar-desktop {
    display: flex;
  }
  
  .mr-sidebar-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .mr-sidebar {
    border-radius: 0;
  }
  
  .mr-sidebar-content {
    padding: 8px;
    gap: 8px;
  }
  
  .mr-sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px;
    cursor: default;
  }
  
  .mr-sidebar-header:hover {
    background-color: transparent;
  }
  
  .mr-sidebar-main {
    gap: 4px;
  }
  
  .mr-nav-item {
    padding: 12px;
    border-radius: 8px;
  }
  
  .mr-nav-item-icon {
    font-size: 1.5rem;
  }
  
  .mr-nav-item-text {
    font-size: 0.9375rem;
    font-weight: 500;
  }
  
  .mr-nav-title {
    padding: 8px 12px;
    font-size: 0.6875rem;
    letter-spacing: 0.1em;
  }
  
  .mr-sidebar-footer {
    max-height: 240px;
    border-radius: 8px;
  }
  
  .mr-footer-header {
    padding: 12px 16px;
  }
  
  .mr-footer-title {
    font-size: 0.8125rem;
  }
  
  .mr-playlist-item {
    padding: 8px;
    border-radius: 6px;
  }
  
  .mr-playlist-cover {
    width: 40px;
    height: 40px;
    border-radius: 4px;
  }
  
  .mr-playlist-name {
    font-size: 0.8125rem;
  }
}
</style>
