<script setup lang="ts">
import { computed } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import DrawerMusic from '@/components/DrawerMusic/index.vue'
import { Icon } from '@iconify/vue'
import { MenuStore } from '@/stores/modules/menu'
import { UserStore } from '@/stores/modules/user'
import defaultAlbum from '@/assets/default_album.jpg'
import { appendImageParam, fixUrl } from '@/utils'

const { currentTrack, isPlaying, togglePlayPause, currentTime, duration } = useAudioPlayer()
const userStore = UserStore()
const menuStore = MenuStore()
const router = useRouter()
const route = useRoute()
const showDrawerMusic = computed({
  get: () => menuStore.isSongDrawerOpen,
  set: (value: boolean) => menuStore.setSongDrawerOpen(value),
})

const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const ringRadius = 20
const ringCircumference = 2 * Math.PI * ringRadius
const ringStyle = computed(() => ({
  strokeDasharray: `${ringCircumference} ${ringCircumference}`,
  strokeDashoffset: ringCircumference - (progressPercent.value / 100) * ringCircumference,
}))

const openQueue = () => {
  menuStore.setPlaylistOpen(true)
}

const openNowPlaying = () => {
  menuStore.setRightAsideOpen(true)
}

const navItems = [
  { label: '首页', icon: 'mdi:home-outline', path: '/' },
  { label: 'AI', icon: 'mdi:chat-processing-outline', path: '/chat' },
  { label: '发现', icon: 'mdi:compass-outline', path: '/search' },
  { label: '我的', icon: 'mdi:account-outline', path: () => (userStore.userInfo?.userId ? `/profile/${userStore.userInfo.userId}` : '/login') },
]

const getNavPath = (path: string | (() => string)) => typeof path === 'function' ? path() : path

const isNavActive = (path: string | (() => string)) => route.path === getNavPath(path)

const goNav = (path: string | (() => string)) => {
  const target = getNavPath(path)
  if (target === route.path) return
  router.push(target)
}
</script>

<template>
  <div class="mr-mobile-player">
    <div class="mr-mobile-player-content" @click="showDrawerMusic = true">
      <div class="mr-mobile-player-cover" @click.stop="openNowPlaying">
        <img
          :src="appendImageParam(currentTrack.cover, '60y60') || fixUrl(currentTrack.cover) || defaultAlbum"
          :alt="currentTrack.title"
          class="mr-mobile-player-img"
        />
      </div>

      <div class="mr-mobile-player-info">
        <div class="mr-mobile-player-title">{{ currentTrack.title }}</div>
        <div class="mr-mobile-player-artist">{{ currentTrack.artist }}</div>
      </div>

      <div class="mr-mobile-player-actions" @click.stop>
        <div class="mr-play-progress" @click.stop="togglePlayPause">
          <svg class="mr-progress-ring" viewBox="0 0 44 44">
            <circle class="mr-progress-ring-track" cx="22" cy="22" :r="ringRadius" />
            <circle
              class="mr-progress-ring-bar"
              cx="22"
              cy="22"
              :r="ringRadius"
              :style="ringStyle"
            />
          </svg>
          <button class="mr-mobile-player-btn mr-mobile-player-btn-play">
            <Icon :icon="isPlaying ? 'mdi:pause' : 'mdi:play'" />
          </button>
        </div>
        <button class="mr-mobile-player-btn mr-queue-btn" @click.stop="openQueue" title="播放队列">
          <Icon icon="ri:play-list-2-fill" />
        </button>
      </div>
    </div>

    <nav class="mr-mobile-player-nav" @click.stop>
      <button
        v-for="item in navItems"
        :key="item.label"
        class="mr-nav-item"
        :class="{ 'mr-nav-item-active': isNavActive(item.path) }"
        @click="goNav(item.path)"
      >
        <Icon :icon="item.icon" class="mr-nav-icon" />
      </button>
    </nav>

    <DrawerMusic v-model="showDrawerMusic" />
  </div>
</template>

<style scoped>
.mr-mobile-player {
  display: none;
  flex-direction: column;
  background-color: var(--bg-playing-bar, #181818);
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.mr-mobile-player-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px;
  margin: 0 12px 4px;
  border-radius: 12px;
  cursor: pointer;
  background: var(--bg-playing-bar, #181818);
}

.mr-mobile-player-cover {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.mr-mobile-player-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.mr-mobile-player-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--bg-elevated, #242424);
}

.mr-mobile-player-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  max-width: 45%;
}

.mr-mobile-player-artist {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  max-width: 45%;
}

.mr-mobile-player-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.mr-play-progress {
  position: relative;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.mr-progress-ring {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.mr-progress-ring circle {
  fill: none;
  stroke-width: 2;
}

.mr-progress-ring-track {
  stroke: rgba(128, 128, 128, 0.3);
}

.mr-progress-ring-bar {
  stroke: var(--text-base, #fff);
  stroke-linecap: round;
  transition: stroke-dashoffset 200ms linear;
}

.mr-mobile-player-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.5rem;
  cursor: pointer;
  transition: color 200ms ease, transform 33ms ease;
}

.mr-mobile-player-btn:hover {
  color: var(--text-base, #fff);
}

.mr-mobile-player-btn:active {
  transform: scale(0.95);
}

.mr-mobile-player-btn-play {
  font-size: 1.75rem;
}

.mr-mobile-player-btn-sm {
  width: 36px;
  height: 36px;
  font-size: 1.25rem;
}

.mr-queue-btn {
  width: 40px;
  height: 40px;
  font-size: 1.5rem;
}

.mr-mobile-player-nav {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 8px 0 calc(8px + env(safe-area-inset-bottom));
  background: var(--bg-playing-bar, #181818);
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.mr-nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: transparent;
  border: none;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: color 200ms ease;
}

.mr-nav-item:hover {
  color: var(--text-base, #fff);
}

.mr-nav-item-active {
  color: var(--text-base, #fff);
}

.mr-nav-icon {
  font-size: 1.5rem;
}

/* Dark Theme - pure black for player & nav */
.dark .mr-mobile-player {
  --bg-playing-bar: #000000;
  --border-color: rgba(255, 255, 255, 0.1);
}

/* Light Theme */
:root:not(.dark) .mr-mobile-player {
  --bg-playing-bar: #f0f0f0;
  --bg-elevated: #ffffff;
  --border-color: rgba(0, 0, 0, 0.1);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
}

@media (max-width: 768px) {
  .mr-mobile-player {
    display: flex;
  }
}
</style>
