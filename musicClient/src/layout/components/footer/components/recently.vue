<script setup lang="ts">
import { formatMillisecondsToTime, fixUrl } from '@/utils'
import { trackModel } from '@/stores/interface'
import { AudioStore } from '@/stores/modules/audio'
import { MenuStore } from '@/stores/modules/menu'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import defaultCover from '@/assets/default_album.jpg'

const props = defineProps({
  isGlobal: {
    type: Boolean,
    default: true
  }
})

const menuStore = MenuStore()
const audioStore = AudioStore()
const { loadTrack, play, audioElement } = useAudioPlayer()

const mouseOverIndex = ref(-1)
const localIsPlaylistOpen = ref(false)

const isPlaylistOpen = computed({
  get() {
    return props.isGlobal ? menuStore.isPlaylistOpen : localIsPlaylistOpen.value
  },
  set(value: boolean) {
    if (props.isGlobal) {
      menuStore.setPlaylistOpen(value)
    } else {
      localIsPlaylistOpen.value = value
    }
  }
})

const currentTrackId = computed(() => audioStore.currentSong?.id)

const handleTrackClick = (track: trackModel, index: number) => {
  loadTrack(index)
  play()
}

const handleDelete = (trackId: string | number) => {
  audioStore.removeTrackFromQueue(trackId)
}

const closeQueue = () => {
  isPlaylistOpen.value = false
}
</script>

<template>
  <el-drawer
    v-model="isPlaylistOpen"
    direction="rtl"
    size="360px"
    :with-header="false"
    class="spotify-queue-drawer"
  >
    <div class="spotify-queue">
      <div class="spotify-queue-header">
        <h3 class="spotify-queue-title">播放队列</h3>
        <span class="spotify-queue-count">{{ audioStore.trackList.length }} 首歌曲</span>
        <button class="spotify-queue-close" @click="closeQueue">
          <Icon icon="mdi:close" />
        </button>
      </div>

      <div class="spotify-queue-content">
        <div v-if="audioStore.trackList.length === 0" class="spotify-queue-empty">
          <Icon icon="mdi:music-note-off" class="spotify-empty-icon" />
          <p class="spotify-empty-text">队列中没有歌曲</p>
          <p class="spotify-empty-hint">播放一首歌曲开始收听</p>
        </div>

        <div
          v-else
          class="spotify-queue-list"
        >
          <div
            v-for="(item, index) in audioStore.trackList"
            :key="item.id"
            class="spotify-queue-item"
            :class="{ 'spotify-queue-item-active': currentTrackId === item.id }"
            @click="handleTrackClick(item, index)"
            @mouseenter="mouseOverIndex = index"
            @mouseleave="mouseOverIndex = -1"
          >
            <div class="spotify-queue-cover-wrapper">
              <img
                :src="fixUrl(item.cover) || defaultCover"
                :alt="item.title"
                class="spotify-queue-cover"
              />
              <div
                v-if="currentTrackId === item.id"
                class="spotify-queue-playing-indicator"
              >
                <Icon icon="mdi:volume-high" class="spotify-playing-icon" />
              </div>
              <div
                v-else-if="mouseOverIndex === index"
                class="spotify-queue-play-overlay"
              >
                <Icon icon="mdi:play" class="spotify-play-overlay-icon" />
              </div>
            </div>

            <div class="spotify-queue-info">
              <span class="spotify-queue-title-text" :title="item.title">{{ item.title }}</span>
              <span class="spotify-queue-artist" :title="item.artist">{{ item.artist }}</span>
            </div>

            <div class="spotify-queue-duration">
              {{ formatMillisecondsToTime(Number(item.duration) * 1000) }}
            </div>

            <button
              v-show="mouseOverIndex === index"
              class="spotify-queue-delete"
              @click.stop="handleDelete(item.id)"
              title="从队列中移除"
            >
              <Icon icon="mdi:delete-outline" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.spotify-queue-drawer :deep(.el-drawer) {
  background-color: #121212;
}

.spotify-queue-drawer :deep(.el-drawer__body) {
  padding: 0;
  height: 100%;
}

.spotify-queue {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.spotify-queue-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.spotify-queue-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.spotify-queue-count {
  font-size: 0.75rem;
  color: #b3b3b3;
  margin-left: 12px;
}

.spotify-queue-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #b3b3b3;
  font-size: 1.25rem;
  cursor: pointer;
  margin-left: auto;
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-queue-close:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-queue-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.spotify-queue-content::-webkit-scrollbar {
  width: 8px;
}

.spotify-queue-content::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-queue-content::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.spotify-queue-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
  text-align: center;
}

.spotify-empty-icon {
  font-size: 4rem;
  color: #535353;
  margin-bottom: 16px;
}

.spotify-empty-text {
  font-size: 1rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px;
}

.spotify-empty-hint {
  font-size: 0.875rem;
  color: #b3b3b3;
  margin: 0;
}

.spotify-queue-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spotify-queue-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-queue-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-queue-item-active {
  background-color: rgba(255, 255, 255, 0.2);
}

.spotify-queue-item-active:hover {
  background-color: rgba(255, 255, 255, 0.25);
}

.spotify-queue-cover-wrapper {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.spotify-queue-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.spotify-queue-playing-indicator,
.spotify-queue-play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
}

.spotify-playing-icon {
  font-size: 1.25rem;
  color: #1db954;
}

.spotify-play-overlay-icon {
  font-size: 1.25rem;
  color: #fff;
}

.spotify-queue-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.spotify-queue-title-text {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-queue-item-active .spotify-queue-title-text {
  color: #1db954;
}

.spotify-queue-artist {
  font-size: 0.8125rem;
  color: #b3b3b3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-queue-duration {
  font-size: 0.75rem;
  color: #b3b3b3;
  flex-shrink: 0;
}

.spotify-queue-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #b3b3b3;
  font-size: 1.125rem;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-queue-delete:hover {
  color: #e91429;
  background-color: rgba(233, 20, 41, 0.1);
}

/* Light Theme */
:root:not(.dark) .spotify-queue-drawer :deep(.el-drawer) {
  background-color: #fff;
}

:root:not(.dark) .spotify-queue-header {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-queue-title {
  color: #000;
}

:root:not(.dark) .spotify-queue-close {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-queue-close:hover {
  color: #000;
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-empty-icon {
  color: #d0d0d0;
}

:root:not(.dark) .spotify-empty-text {
  color: #000;
}

:root:not(.dark) .spotify-empty-hint {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-queue-item:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-queue-item-active {
  background-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .spotify-queue-item-active:hover {
  background-color: rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-queue-title-text {
  color: #000;
}

:root:not(.dark) .spotify-queue-item-active .spotify-queue-title-text {
  color: #1db954;
}

:root:not(.dark) .spotify-queue-artist,
:root:not(.dark) .spotify-queue-duration {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-queue-delete {
  color: #6a6a6a;
}

:root:not(.dark) .spotify-queue-delete:hover {
  color: #e91429;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .spotify-queue-drawer :deep(.el-drawer) {
    width: 100% !important;
    max-width: 100% !important;
  }
  
  .spotify-queue-header {
    padding: 12px 16px;
  }
  
  .spotify-queue-title {
    font-size: 1rem;
  }
  
  .spotify-queue-content {
    padding: 4px;
  }
  
  .spotify-queue-item {
    padding: 6px;
    gap: 10px;
  }
  
  .spotify-queue-cover-wrapper {
    width: 44px;
    height: 44px;
  }
  
  .spotify-queue-title-text {
    font-size: 0.875rem;
  }
  
  .spotify-queue-artist {
    font-size: 0.75rem;
  }
  
  .spotify-queue-duration {
    display: none;
  }
  
  .spotify-queue-delete {
    width: 36px;
    height: 36px;
  }
}
</style>
