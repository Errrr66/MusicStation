<script setup lang="ts">
import Left from './left.vue'
import Right from './right.vue'
import { getSongDetail } from '@/api/system'
import type { SongDetail } from '@/api/interface'
import { ref, provide, watch, computed, type Ref } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { Icon } from '@iconify/vue'
import { fixUrl } from '@/utils'
import { AudioStore } from '@/stores/modules/audio'
import defaultAlbum from '@/assets/default_album.jpg'

const showDrawer = defineModel<boolean>()
const songDetail = ref<SongDetail | null>(null)
const activeRightTab = ref<'lyric' | 'comment'>('lyric')
const drawerScrollRef = ref<HTMLElement | null>(null)
let latestDetailRequestId = 0

const { currentTrack } = useAudioPlayer()
const audioStore = AudioStore()
const drawerCover = computed(() => fixUrl(currentTrack.value.cover) || defaultAlbum)
const stableDrawerCover = ref(defaultAlbum)
let latestCoverProbeId = 0

provide('songDetail', songDetail)
provide('drawerCover', stableDrawerCover)
provide('activeRightTab', activeRightTab)
provide('switchRightTab', (tab: 'lyric' | 'comment') => {
  activeRightTab.value = tab
  if (drawerScrollRef.value) {
    drawerScrollRef.value.scrollTo({
      left: drawerScrollRef.value.clientWidth,
      behavior: 'smooth',
    })
  }
})
provide('goBackToLeft', () => {
  if (drawerScrollRef.value) {
    drawerScrollRef.value.scrollTo({
      left: 0,
      behavior: 'smooth',
    })
  }
})
provide('closeDrawer', () => {
  showDrawer.value = false
})

const probeImage = (url: string): Promise<boolean> => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}

const syncTrackMetaFromDetail = (detail: SongDetail, songId: number) => {
  const targetIndex = audioStore.currentSongIndex
  const targetTrack = audioStore.trackList[targetIndex] as any
  if (!targetTrack || Number(targetTrack.id) !== songId) return

  // 仅同步文案字段，封面统一以当前播放列表数据为准，避免歌词页与播放栏封面不一致
  if (detail.songName) {
    targetTrack.title = detail.songName
  }
  if (detail.artistName) {
    targetTrack.artist = detail.artistName
  }
}

watch(
  () => drawerCover.value,
  async (newCover) => {
    const probeId = ++latestCoverProbeId
    if (!newCover) {
      stableDrawerCover.value = defaultAlbum
      return
    }

    const ok = await probeImage(newCover)
    if (probeId !== latestCoverProbeId) return

    if (ok) {
      stableDrawerCover.value = newCover
      return
    }

    // 当前歌曲封面不可用时，直接回退默认封面，避免沿用上一首封面造成错位
    stableDrawerCover.value = defaultAlbum
  },
  { immediate: true }
)

watch(
  () => currentTrack.value.id,
  async (newId) => {
    // 切歌先重置为默认封面，等待新封面探测结果，避免旧封面残留
    stableDrawerCover.value = defaultAlbum
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
          syncTrackMetaFromDetail(songData, currentSongId)
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
</script>

<template>
  <el-drawer
    :style="{
      '--track-cover-url': `url('${stableDrawerCover}')`,
    }"
    v-model="showDrawer"
    direction="btt"
    size="100%"
    :modal="false"
    :showClose="false"
    class="mr-drawer"
    :with-header="false"
  >
    <div class="mr-drawer-content">
      <div class="mr-drawer-header">
        <button @click="showDrawer = false" class="mr-drawer-close">
          <Icon icon="mdi:chevron-down" />
        </button>
        <div class="mr-drawer-title">
          <span class="mr-drawer-song">{{ currentTrack.title }}</span>
          <span class="mr-drawer-artist">{{ currentTrack.artist }}</span>
        </div>
        <div class="w-10"></div>
      </div>

      <main class="mr-drawer-main">
        <div ref="drawerScrollRef" class="mr-drawer-scroll">
          <div class="mr-drawer-left">
            <Left />
          </div>
          <div class="mr-drawer-right">
            <Right />
          </div>
        </div>
      </main>
    </div>
  </el-drawer>
</template>

<style scoped>
.mr-drawer :deep(.el-drawer) {
  overflow: hidden;
  margin: 0 !important;
  width: 100% !important;
  height: 100% !important;
  max-width: 100% !important;
  max-height: 100% !important;
}

.mr-drawer :deep(.el-overlay) {
  background-color: transparent;
}

.mr-drawer :deep(.el-drawer__body) {
  padding: 0;
  margin: 0;
  height: 100%;
  overflow: hidden;
}

.mr-drawer-content {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.3) 0%, rgba(0, 0, 0, 0.8) 100%);
  backdrop-filter: blur(60px) saturate(180%);
  overflow: hidden;
  position: relative;
}

.mr-drawer-content::before {
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

.mr-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  flex-shrink: 0;
}

.mr-drawer-close {
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

.mr-drawer-close:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
}

.mr-drawer-title {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mr-drawer-song {
  font-size: 0.875rem;
  font-weight: 700;
  color: #fff;
}

.mr-drawer-artist {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
}

.mr-drawer-main {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.mr-drawer-scroll {
  height: 100%;
  width: 100%;
  display: flex;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.mr-drawer-scroll::-webkit-scrollbar {
  display: none;
}

.mr-drawer-left {
  width: 100%;
  flex-shrink: 0;
  scroll-snap-align: center;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
}

.mr-drawer-right {
  width: 100%;
  flex-shrink: 0;
  scroll-snap-align: center;
  height: 100%;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
}

@media (min-width: 768px) {
  .mr-drawer-scroll {
    overflow-x: hidden;
  }
  
  .mr-drawer-left {
    width: 50%;
    padding: 0 48px;
  }
  
  .mr-drawer-right {
    width: 50%;
    padding: 0 48px;
  }
}
</style>
