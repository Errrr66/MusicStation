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
const { loadTrack, play } = useAudioPlayer()

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

const currentTrackId = computed(() => audioStore.trackList[audioStore.currentSongIndex]?.id)

const handleTrackClick = async (_track: trackModel, index: number) => {
  await loadTrack(index)
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
    class="mr-queue-drawer"
  >
    <div class="mr-queue">
      <div class="mr-queue-header">
        <h3 class="mr-queue-title">播放队列</h3>
        <span class="mr-queue-count">{{ audioStore.trackList.length }} 首歌曲</span>
        <button class="mr-queue-close" @click="closeQueue">
          <Icon icon="mdi:close" />
        </button>
      </div>

      <div class="mr-queue-content">
        <div v-if="audioStore.trackList.length === 0" class="mr-queue-empty">
          <Icon icon="mdi:music-note-off" class="mr-empty-icon" />
          <p class="mr-empty-text">队列中没有歌曲</p>
          <p class="mr-empty-hint">播放一首歌曲开始收听</p>
        </div>

        <div
          v-else
          class="mr-queue-list"
        >
          <div
            v-for="(item, index) in audioStore.trackList"
            :key="item.id"
            class="mr-queue-item"
            :class="{ 'mr-queue-item-active': currentTrackId === item.id }"
            @click="handleTrackClick(item, index)"
            @mouseenter="mouseOverIndex = index"
            @mouseleave="mouseOverIndex = -1"
          >
            <div class="mr-queue-cover-wrapper">
              <img
                :src="fixUrl(item.cover) || defaultCover"
                :alt="item.title"
                class="mr-queue-cover"
              />
              <div
                v-if="currentTrackId === item.id"
                class="mr-queue-playing-indicator"
              >
                <Icon icon="mdi:volume-high" class="mr-playing-icon" />
              </div>
              <div
                v-else-if="mouseOverIndex === index"
                class="mr-queue-play-overlay"
              >
                <Icon icon="mdi:play" class="mr-play-overlay-icon" />
              </div>
            </div>

            <div class="mr-queue-info">
              <span class="mr-queue-title-text" :title="item.title">{{ item.title }}</span>
              <span class="mr-queue-artist" :title="item.artist">{{ item.artist }}</span>
            </div>

            <div class="mr-queue-duration">
              {{ formatMillisecondsToTime(Number(item.duration) * 1000) }}
            </div>

            <button
              v-show="mouseOverIndex === index"
              class="mr-queue-delete"
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
.mr-queue-drawer :deep(.el-drawer) {
  background-color: #121212;
}

.mr-queue-drawer :deep(.el-drawer__body) {
  padding: 0;
  height: 100%;
}

.mr-queue {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mr-queue-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.mr-queue-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.mr-queue-count {
  font-size: 0.75rem;
  color: #b3b3b3;
  margin-left: 12px;
}

.mr-queue-close {
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

.mr-queue-close:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-queue-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.mr-queue-content::-webkit-scrollbar {
  width: 8px;
}

.mr-queue-content::-webkit-scrollbar-track {
  background: transparent;
}

.mr-queue-content::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.mr-queue-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
  text-align: center;
}

.mr-empty-icon {
  font-size: 4rem;
  color: #535353;
  margin-bottom: 16px;
}

.mr-empty-text {
  font-size: 1rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px;
}

.mr-empty-hint {
  font-size: 0.875rem;
  color: #b3b3b3;
  margin: 0;
}

.mr-queue-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mr-queue-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-queue-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-queue-item-active {
  background-color: rgba(255, 255, 255, 0.2);
}

.mr-queue-item-active:hover {
  background-color: rgba(255, 255, 255, 0.25);
}

.mr-queue-cover-wrapper {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.mr-queue-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.mr-queue-playing-indicator,
.mr-queue-play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
}

.mr-playing-icon {
  font-size: 1.25rem;
  color: var(--mr-accent);
}

.mr-play-overlay-icon {
  font-size: 1.25rem;
  color: #fff;
}

.mr-queue-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mr-queue-title-text {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mr-queue-item-active .mr-queue-title-text {
  color: var(--mr-accent);
}

.mr-queue-artist {
  font-size: 0.8125rem;
  color: #b3b3b3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mr-queue-duration {
  font-size: 0.75rem;
  color: #b3b3b3;
  flex-shrink: 0;
}

.mr-queue-delete {
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

.mr-queue-delete:hover {
  color: #e91429;
  background-color: rgba(233, 20, 41, 0.1);
}

/* Light Theme */
:root:not(.dark) .mr-queue-drawer :deep(.el-drawer) {
  background-color: #fff;
}

:root:not(.dark) .mr-queue-header {
  border-bottom-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-queue-title {
  color: #000;
}

:root:not(.dark) .mr-queue-close {
  color: #6a6a6a;
}

:root:not(.dark) .mr-queue-close:hover {
  color: #000;
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-empty-icon {
  color: #d0d0d0;
}

:root:not(.dark) .mr-empty-text {
  color: #000;
}

:root:not(.dark) .mr-empty-hint {
  color: #6a6a6a;
}

:root:not(.dark) .mr-queue-item:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-queue-item-active {
  background-color: rgba(0, 0, 0, 0.12);
}

:root:not(.dark) .mr-queue-item-active:hover {
  background-color: rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .mr-queue-title-text {
  color: #000;
}

:root:not(.dark) .mr-queue-item-active .mr-queue-title-text {
  color: var(--mr-accent);
}

:root:not(.dark) .mr-queue-artist,
:root:not(.dark) .mr-queue-duration {
  color: #6a6a6a;
}

:root:not(.dark) .mr-queue-delete {
  color: #6a6a6a;
}

:root:not(.dark) .mr-queue-delete:hover {
  color: #e91429;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .mr-queue-drawer :deep(.el-drawer) {
    width: 100% !important;
    max-width: 100% !important;
  }
  
  .mr-queue-header {
    padding: 12px 16px;
  }
  
  .mr-queue-title {
    font-size: 1rem;
  }
  
  .mr-queue-content {
    padding: 4px;
  }
  
  .mr-queue-item {
    padding: 6px;
    gap: 10px;
  }
  
  .mr-queue-cover-wrapper {
    width: 44px;
    height: 44px;
  }
  
  .mr-queue-title-text {
    font-size: 0.875rem;
  }
  
  .mr-queue-artist {
    font-size: 0.75rem;
  }
  
  .mr-queue-duration {
    display: none;
  }
  
  .mr-queue-delete {
    width: 36px;
    height: 36px;
  }
}
</style>
