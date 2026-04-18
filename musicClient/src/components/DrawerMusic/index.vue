<script setup lang="ts">
import Left from './left.vue'
import Right from './right.vue'
import { getSongDetail } from '@/api/system'
import type { SongDetail } from '@/api/interface'
import { ref, provide, watch } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { Icon } from '@iconify/vue'
import { fixUrl } from '@/utils'

const showDrawer = defineModel<boolean>()
const songDetail = ref<SongDetail | null>(null)
let latestDetailRequestId = 0

const { currentTrack } = useAudioPlayer()

watch(
  () => currentTrack.value.id,
  async (newId) => {
    songDetail.value = null
    const currentSongId = Number(newId)
    if (!currentSongId) {
      return
    }

    const requestId = ++latestDetailRequestId

    try {
      const res = await getSongDetail(currentSongId)
      if (requestId !== latestDetailRequestId) {
        return
      }
      if (res.code === 0 && res.data) {
        const songData = res.data as unknown as SongDetail
        if (
          'songId' in songData &&
          'songName' in songData &&
          'artistName' in songData &&
          'album' in songData &&
          Number(songData.songId) === currentSongId
        ) {
          songDetail.value = songData
        } else {
          console.error('歌曲详情数据格式不正确或歌曲ID不匹配')
        }
      }
    } catch (error) {
      if (requestId === latestDetailRequestId) {
        console.error('获取歌曲详情失败:', error)
      }
    }
  },
  { immediate: true }
)

provide('songDetail', songDetail)
</script>

<template>
  <el-drawer
    :style="{
      '--track-cover-url': `url(${fixUrl(currentTrack.cover)})`,
    }"
    v-model="showDrawer"
    direction="btt"
    size="100%"
    :modal="false"
    :showClose="false"
    class="spotify-drawer"
    :with-header="false"
  >
    <div class="spotify-drawer-content">
      <div class="spotify-drawer-header">
        <button @click="showDrawer = false" class="spotify-drawer-close">
          <Icon icon="mdi:chevron-down" />
        </button>
        <div class="spotify-drawer-title">
          <span class="spotify-drawer-song">{{ currentTrack.title }}</span>
          <span class="spotify-drawer-artist">{{ currentTrack.artist }}</span>
        </div>
        <div class="w-10"></div>
      </div>

      <main class="spotify-drawer-main">
        <div class="spotify-drawer-scroll">
          <div class="spotify-drawer-left">
            <Left />
          </div>
          <div class="spotify-drawer-right">
            <Right />
          </div>
        </div>
      </main>
    </div>
  </el-drawer>
</template>

<style scoped>
.spotify-drawer :deep(.el-drawer) {
  overflow: hidden;
  margin: 0 !important;
  width: 100% !important;
  height: 100% !important;
  max-width: 100% !important;
  max-height: 100% !important;
}

.spotify-drawer :deep(.el-overlay) {
  background-color: transparent;
}

.spotify-drawer :deep(.el-drawer__body) {
  padding: 0;
  margin: 0;
  height: 100%;
  overflow: hidden;
}

.spotify-drawer-content {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.3) 0%, rgba(0, 0, 0, 0.8) 100%);
  backdrop-filter: blur(60px) saturate(180%);
  overflow: hidden;
  position: relative;
}

.spotify-drawer-content::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--track-cover-url);
  background-size: cover;
  background-position: center;
  filter: blur(80px) saturate(1.5);
  transform: scale(1.5);
  z-index: -1;
}

.spotify-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  flex-shrink: 0;
}

.spotify-drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 1.5rem;
  cursor: pointer;
  transition: background-color 200ms ease, transform 33ms ease;
}

.spotify-drawer-close:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
}

.spotify-drawer-title {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.spotify-drawer-song {
  font-size: 0.875rem;
  font-weight: 700;
  color: #fff;
}

.spotify-drawer-artist {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
}

.spotify-drawer-main {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.spotify-drawer-scroll {
  height: 100%;
  width: 100%;
  display: flex;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.spotify-drawer-scroll::-webkit-scrollbar {
  display: none;
}

.spotify-drawer-left {
  width: 100%;
  flex-shrink: 0;
  scroll-snap-align: center;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
}

.spotify-drawer-right {
  width: 100%;
  flex-shrink: 0;
  scroll-snap-align: center;
  height: 100%;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
}

@media (min-width: 768px) {
  .spotify-drawer-scroll {
    overflow-x: hidden;
  }
  
  .spotify-drawer-left {
    width: 50%;
    padding: 0 48px;
  }
  
  .spotify-drawer-right {
    width: 50%;
    padding: 0 48px;
  }
}
</style>
