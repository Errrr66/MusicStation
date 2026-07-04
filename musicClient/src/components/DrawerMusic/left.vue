<script setup lang="ts">
import { formatTime } from '@/utils'
import { Icon } from '@iconify/vue'
import type { SongDetail } from '@/api/interface'
import { computed, ref, inject, type Ref } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { MenuStore } from '@/stores/modules/menu'
import { UserStore } from '@/stores/modules/user'
import { AudioStore } from '@/stores/modules/audio'
import { collectSong, cancelCollectSong } from '@/api/system'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useLibraryStore } from '@/stores/modules/library'
import { useArtistStore } from '@/stores/modules/artist'
import { usePlaylistStore } from '@/stores/modules/playlist'

import vinylImg from '@/assets/vinyl.png'
import defaultAlbum from '@/assets/default_album.jpg'

const menuStore = MenuStore()
const userStore = UserStore()
const audioStore = AudioStore()
const route = useRoute()
const libraryStore = useLibraryStore()

const {
  currentTrack,
  isPlaying,
  currentTime,
  duration,
  nextTrack,
  prevTrack,
  togglePlayPause,
  seek,
  setPlayMode,
} = useAudioPlayer()

const songDetail = inject<Ref<SongDetail | null>>('songDetail')
const drawerCover = inject<Ref<string>>('drawerCover')
const switchRightTab = inject<(tab: 'lyric' | 'comment') => void>('switchRightTab')

const isCurrentTrackDetail = computed(() => {
  const detailSongId = Number(songDetail?.value?.songId)
  const playingSongId = Number(currentTrack.value.id)
  return detailSongId > 0 && detailSongId === playingSongId
})

const displayCover = computed(() => {
  return drawerCover?.value || defaultAlbum
})

const displaySongName = computed(() => {
  if (isCurrentTrackDetail.value && songDetail?.value?.songName) {
    return songDetail.value.songName
  }
  return currentTrack.value.title
})

const displayArtistName = computed(() => {
  if (isCurrentTrackDetail.value && songDetail?.value?.artistName) {
    return songDetail.value.artistName
  }
  return currentTrack.value.artist
})

const playModes = {
  order: {
    icon: 'ri:order-play-line',
    next: 'shuffle',
    tooltip: '顺序播放',
  },
  shuffle: {
    icon: 'ri:shuffle-line',
    next: 'loop',
    tooltip: '随机播放',
  },
  loop: {
    icon: 'ri:repeat-2-line',
    next: 'single',
    tooltip: '列表循环',
  },
  single: {
    icon: 'ri:repeat-one-line',
    next: 'order',
    tooltip: '单曲循环',
  },
}

const currentMode = ref('order')

const togglePlayMode = () => {
  const nextMode = playModes[currentMode.value].next
  currentMode.value = nextMode
  setPlayMode(nextMode)
}

// 获取当前播放歌曲的喜欢状态
const currentSongLikeStatus = computed(() => {
  const track = audioStore.trackList[audioStore.currentSongIndex]
  return track?.likeStatus || 0
})

// 更新所有相同歌曲的喜欢状态
const updateAllSongLikeStatus = (songId: number, status: number) => {
  audioStore.trackList.forEach((track) => {
    if (Number(track.id) === songId) {
      track.likeStatus = status
    }
  })

  if (audioStore.currentPageSongs) {
    audioStore.currentPageSongs.forEach((song) => {
      if ((song as any).songId === songId) {
        ;(song as any).likeStatus = status
      }
    })
  }

  if (route.path === '/library' && libraryStore.tableData?.items) {
    const song = libraryStore.tableData.items.find(
      (song) => song.songId === songId
    )
    if (song) {
      song.likeStatus = status
    }
  }

  if (route.path.startsWith('/artist/')) {
    const artistStore = useArtistStore()
    if (artistStore.artistInfo?.songs) {
      const song = artistStore.artistInfo.songs.find(
        (song) => song.songId === songId
      )
      if (song) {
        song.likeStatus = status
      }
    }
  }

  if (route.path.startsWith('/playlist/')) {
    const playlistStore = usePlaylistStore()
    if (playlistStore.songs) {
      const song = playlistStore.songs.find((song) => song.songId === songId)
      if (song) {
        song.likeStatus = status
      }
    }
  }
}

// 处理喜欢/取消喜欢
const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  const track = audioStore.trackList[audioStore.currentSongIndex]
  if (!track) return

  try {
    const songId = Number(track.id)
    if (currentSongLikeStatus.value === 0) {
      const res = await collectSong(songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(songId, 1)
        ElMessage.success('已添加到我的喜欢')
      } else {
        ElMessage.error(res.message || '添加到我的喜欢失败')
      }
    } else {
      const res = await cancelCollectSong(songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(songId, 0)
        ElMessage.success('已取消喜欢')
      } else {
        ElMessage.error(res.message || '取消喜欢失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}
</script>

<template>
  <div class="mr-player-full">
    <div class="mr-player-content">
      <div class="mr-album-container" :class="{ 'is-playing': isPlaying }">
        <div class="mr-album">
          <div
            class="mr-album-cover"
            :style="{
              backgroundImage: `url('${displayCover}')`,
            }"
          ></div>
          <div
            class="mr-vinyl"
            :style="{
              animationPlayState: isPlaying ? 'running' : 'paused',
              backgroundImage: `url('${vinylImg}'), url('${displayCover}')`,
            }"
          ></div>
        </div>
      </div>

      <div class="mr-song-header">
        <h2 class="mr-song-title">
          {{ displaySongName }}
        </h2>
        <p class="mr-song-artist">
          {{ displayArtistName }}
        </p>
      </div>

      <div class="mr-song-actions">
        <button class="mr-action-btn" @click="handleLike">
          <Icon
            :icon="currentSongLikeStatus === 0 ? 'mdi:cards-heart-outline' : 'mdi:cards-heart'"
            :class="{ 'mr-like-active': currentSongLikeStatus === 1 }"
          />
        </button>
        <button class="mr-action-btn" @click="switchRightTab?.('comment')">
          <Icon icon="mdi:message-text-outline" />
        </button>
      </div>

      <div class="mr-progress-section">
        <span class="mr-time">{{ formatTime(currentTime) }}</span>
        <div class="mr-slider-wrapper">
          <el-slider
            v-model="currentTime"
            :show-tooltip="false"
            @change="seek"
            :max="duration"
            size="small"
            class="mr-slider"
          />
        </div>
        <span class="mr-time">{{ formatTime(duration) }}</span>
      </div>

      <div class="mr-controls">
        <el-tooltip
          :content="playModes[currentMode].tooltip"
          placement="top"
          effect="dark"
        >
          <button class="mr-control-btn" @click="togglePlayMode">
            <Icon :icon="playModes[currentMode].icon" />
          </button>
        </el-tooltip>
        <button class="mr-control-btn mr-control-btn-lg" @click="prevTrack">
          <Icon icon="mdi:skip-previous" />
        </button>
        <button class="mr-play-btn" @click="togglePlayPause">
          <Icon :icon="isPlaying ? 'mdi:pause' : 'mdi:play'" />
        </button>
        <button class="mr-control-btn mr-control-btn-lg" @click="nextTrack">
          <Icon icon="mdi:skip-next" />
        </button>
        <button class="mr-control-btn" @click="menuStore.setPlaylistOpen(true)">
          <Icon icon="ri:play-list-2-fill" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mr-player-full {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  position: relative;
}

.mr-player-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  width: 100%;
  max-width: 400px;
}

.mr-album-container {
  position: relative;
  width: 100%;
  display: flex;
  justify-content: center;
}

.mr-album {
  position: relative;
  width: 280px;
  height: 280px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  border-radius: 8px;
}

.mr-album-cover {
  position: relative;
  width: 280px;
  height: 280px;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  border-radius: 8px;
  z-index: 10;
}

.mr-vinyl {
  position: absolute;
  top: 8px;
  left: 5%;
  width: 264px;
  height: 264px;
  border-radius: 50%;
  background-position: center, center;
  background-size: cover, 40% auto;
  background-repeat: no-repeat;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
  animation: spin 3s linear infinite;
  z-index: 5;
  transition: left 0.5s ease;
}

.is-playing .mr-vinyl {
  left: 52%;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.mr-song-header {
  text-align: center;
}

.mr-song-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.mr-song-artist {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
}

.mr-song-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

.mr-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.7);
  font-size: 1.25rem;
  cursor: pointer;
  transition: color 200ms ease, transform 33ms ease;
}

.mr-action-btn:hover {
  color: #fff;
  transform: scale(1.05);
}

.mr-like-active {
  color: var(--mr-accent);
}

.mr-progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.mr-time {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.5);
  min-width: 40px;
  text-align: center;
}

.mr-slider-wrapper {
  flex: 1;
}

.mr-slider :deep(.el-slider__runway) {
  background-color: rgba(255, 255, 255, 0.2);
  height: 4px;
  border-radius: 2px;
}

.mr-slider :deep(.el-slider__bar) {
  background-color: var(--mr-accent);
  height: 4px;
  border-radius: 2px;
}

.mr-slider :deep(.el-slider__button) {
  width: 12px;
  height: 12px;
  border: none;
  background-color: #fff;
}

.mr-slider:hover :deep(.el-slider__bar) {
  background-color: var(--mr-accent-hover);
}

.mr-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.mr-control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.7);
  font-size: 1.5rem;
  cursor: pointer;
  transition: color 200ms ease, transform 33ms ease;
}

.mr-control-btn:hover {
  color: #fff;
  transform: scale(1.05);
}

.mr-control-btn-lg {
  font-size: 2rem;
}

.mr-play-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: #fff;
  border: none;
  border-radius: 50%;
  color: #000;
  font-size: 2rem;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
}

.mr-play-btn:hover {
  transform: scale(1.06);
  background-color: #f0f0f0;
}

@media (max-width: 768px) {
  .mr-album {
    width: 200px;
    height: 200px;
  }

  .mr-album-cover {
    width: 200px;
    height: 200px;
  }

  .mr-vinyl {
    display: none;
  }

  .mr-song-title {
    font-size: 1.25rem;
  }

  .mr-song-artist {
    font-size: 0.875rem;
  }

  .mr-controls {
    gap: 16px;
  }

  .mr-play-btn {
    width: 56px;
    height: 56px;
    font-size: 1.75rem;
  }
}
</style>
