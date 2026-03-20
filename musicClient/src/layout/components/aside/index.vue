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
    class="spotify-sidebar"
    :class="[
      isCollapsed ? 'spotify-sidebar-collapsed' : 'spotify-sidebar-expanded',
      isMobile ? 'spotify-sidebar-mobile' : 'spotify-sidebar-desktop'
    ]"
  >
    <div class="spotify-sidebar-content">
      <div v-if="!isMobile" class="spotify-sidebar-header" @click="toggleCollapse">
        <div class="spotify-sidebar-logo" :class="{ 'spotify-logo-centered': isCollapsed }">
          <Icon icon="mdi:library-music" class="spotify-logo-icon" />
          <span v-if="!isCollapsed" class="spotify-logo-text">音乐库</span>
        </div>
      </div>

      <div class="spotify-sidebar-main">
        <div
          v-for="(section, sIndex) in MenuData"
          :key="sIndex"
          class="spotify-nav-section"
        >
          <div v-if="sIndex > 0 && !isCollapsed" class="spotify-nav-divider"></div>
          
          <h4 v-if="!isCollapsed && section.title" class="spotify-nav-title">
            {{ section.title }}
          </h4>

          <div class="spotify-nav-items">
            <div
              v-for="(item, iIndex) in section.children"
              :key="iIndex"
              class="spotify-nav-item"
              :class="{ 'spotify-nav-item-active': route.path === item.router }"
              @click="handleMenuClick(item.router)"
              :title="isCollapsed ? item.title : ''"
            >
              <div class="spotify-nav-item-icon-wrapper">
                <Icon :icon="item.icon" class="spotify-nav-item-icon" />
              </div>
              <span v-if="!isCollapsed" class="spotify-nav-item-text">{{ item.title }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="user.isLoggedIn && !isCollapsed" class="spotify-sidebar-footer">
        <div class="spotify-footer-header">
          <Icon icon="mdi:folder-heart" class="spotify-footer-icon" />
          <span class="spotify-footer-title">收藏的歌单</span>
          <span class="spotify-footer-count">{{ favoriteStore.favoritePlaylists.length }}</span>
        </div>

        <el-scrollbar class="spotify-footer-scroll">
          <div v-if="favoriteStore.loading" class="spotify-footer-loading">
            <Icon icon="eos-icons:loading" class="text-2xl" />
          </div>
          <div v-else class="spotify-footer-list">
            <div
              v-for="item in favoriteStore.favoritePlaylists"
              :key="item.id"
              class="spotify-playlist-item"
              :class="{ 'spotify-playlist-active': route.path === `/playlist/${item.id}` }"
              @click="handlePlaylistClick(item.id)"
            >
              <el-image
                lazy
                :src="(item.coverImgUrl && item.coverImgUrl.startsWith('http')) ? item.coverImgUrl + '?param=50y50' : (item.coverImgUrl || coverImg)"
                class="spotify-playlist-cover"
                :alt="item.name"
              >
                <template #error>
                  <div class="spotify-playlist-cover-placeholder">
                    <Icon icon="ri:music-2-line" />
                  </div>
                </template>
              </el-image>
              <div class="spotify-playlist-info">
                <span class="spotify-playlist-name">{{ item.name }}</span>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div v-if="user.isLoggedIn && isCollapsed" class="spotify-sidebar-footer-collapsed">
        <div class="spotify-footer-divider"></div>
        <div
          v-for="(item, index) in favoriteStore.favoritePlaylists.slice(0, 3)"
          :key="index"
          class="spotify-mini-playlist"
          :class="{ 'spotify-playlist-active': route.path === `/playlist/${item.id}` }"
          @click="handlePlaylistClick(item.id)"
        >
          <el-image
            lazy
            :src="(item.coverImgUrl && item.coverImgUrl.startsWith('http')) ? item.coverImgUrl + '?param=50y50' : (item.coverImgUrl || coverImg)"
            class="spotify-mini-cover"
          >
            <template #error>
              <div class="spotify-playlist-cover-placeholder">
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
.spotify-sidebar {
  display: flex;
  flex-direction: column;
  background-color: #000000;
  overflow: hidden;
  transition: width 200ms ease;
}

.spotify-sidebar-desktop {
  display: none;
}

.spotify-sidebar-mobile {
  width: 100% !important;
  height: 100% !important;
}

.spotify-sidebar-expanded {
  width: 280px;
}

.spotify-sidebar-collapsed {
  width: 72px;
}

.spotify-sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px;
  gap: 8px;
}

.spotify-sidebar-header {
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-sidebar-header:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spotify-logo-centered {
  justify-content: center;
  width: 100%;
}

.spotify-logo-icon {
  font-size: 1.5rem;
  color: #b3b3b3;
}

.spotify-logo-text {
  font-size: 1rem;
  font-weight: 700;
  color: #b3b3b3;
}

.spotify-sidebar-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spotify-sidebar-main::-webkit-scrollbar {
  width: 4px;
}

.spotify-sidebar-main::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 20px;
}

.spotify-nav-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spotify-nav-divider {
  height: 1px;
  background-color: rgba(255, 255, 255, 0.1);
  margin: 8px 12px;
}

.spotify-nav-title {
  padding: 8px 12px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: #b3b3b3;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.spotify-nav-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spotify-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-nav-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-nav-item-active {
  background-color: rgba(255, 255, 255, 0.2);
}

.spotify-nav-item-active .spotify-nav-item-icon {
  color: #fff;
}

.spotify-nav-item-active .spotify-nav-item-text {
  color: #fff;
  font-weight: 600;
}

.spotify-nav-item-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.spotify-nav-item-icon {
  font-size: 1.5rem;
  color: #b3b3b3;
}

.spotify-nav-item-text {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #b3b3b3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spotify-sidebar-footer {
  display: flex;
  flex-direction: column;
  max-height: 240px;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.spotify-footer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.spotify-footer-icon {
  font-size: 1.125rem;
  color: #1db954;
}

.spotify-footer-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #fff;
}

.spotify-footer-count {
  font-size: 0.75rem;
  color: #b3b3b3;
  margin-left: auto;
}

.spotify-footer-scroll {
  flex: 1;
  overflow: hidden;
}

.spotify-footer-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #b3b3b3;
}

.spotify-footer-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
}

.spotify-playlist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-playlist-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-playlist-active {
  background-color: rgba(255, 255, 255, 0.15);
}

.spotify-playlist-cover {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.spotify-playlist-cover-placeholder {
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

.spotify-playlist-info {
  flex: 1;
  min-width: 0;
}

.spotify-playlist-name {
  font-size: 0.8125rem;
  color: #fff;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.spotify-sidebar-footer-collapsed {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
}

.spotify-footer-divider {
  height: 1px;
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-mini-playlist {
  display: flex;
  justify-content: center;
  padding: 4px 0;
  cursor: pointer;
}

.spotify-mini-playlist:hover .spotify-mini-cover {
  transform: scale(1.05);
}

.spotify-mini-cover {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
  transition: transform 200ms ease;
}

.spotify-sidebar-collapsed .spotify-nav-item {
  justify-content: center;
  padding: 12px 0;
}

.spotify-sidebar-collapsed .spotify-nav-item-icon {
  font-size: 1.75rem;
}

/* Light Theme */
:root:not(.dark) .spotify-sidebar {
  background-color: #f5f5f5;
}

:root:not(.dark) .spotify-sidebar-header:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-logo-icon,
:root:not(.dark) .spotify-logo-text {
  color: #000;
}

:root:not(.dark) .spotify-nav-item:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-nav-item-active {
  background-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .spotify-nav-item-icon,
:root:not(.dark) .spotify-nav-title,
:root:not(.dark) .spotify-nav-item-text {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-nav-item-active .spotify-nav-item-icon,
:root:not(.dark) .spotify-nav-item-active .spotify-nav-item-text {
  color: #000;
}

:root:not(.dark) .spotify-nav-divider {
  background-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-sidebar-footer {
  background-color: rgba(0, 0, 0, 0.05);
}

:root:not(.dark) .spotify-footer-header {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-footer-title,
:root:not(.dark) .spotify-playlist-name {
  color: #000;
}

:root:not(.dark) .spotify-playlist-item:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-playlist-active {
  background-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .spotify-playlist-cover-placeholder {
  background-color: #e0e0e0;
}

:root:not(.dark) .spotify-footer-divider {
  background-color: rgba(0, 0, 0, 0.1);
}

@media (min-width: 768px) {
  .spotify-sidebar-desktop {
    display: flex;
  }
  
  .spotify-sidebar-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .spotify-sidebar {
    border-radius: 0;
  }
}
</style>
