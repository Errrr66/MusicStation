<script setup lang="ts">
import { formatTime, fixUrl } from '@/utils'
import { Icon } from '@iconify/vue'
import type { SongDetail } from '@/api/interface'
import { computed, ref, inject, type Ref } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { MenuStore } from '@/stores/modules/menu'
import vinylImg from '@/assets/vinyl.png'

const menuStore = MenuStore()

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

const isCurrentTrackDetail = computed(() => {
  const detailSongId = Number(songDetail?.value?.songId)
  const playingSongId = Number(currentTrack.value.id)
  return detailSongId > 0 && detailSongId === playingSongId
})

const displayCover = computed(() => {
  if (isCurrentTrackDetail.value && songDetail?.value?.coverUrl) {
    return fixUrl(songDetail.value.coverUrl)
  }
  return fixUrl(currentTrack.value.cover)
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
</script>

<template>
  <div class="spotify-player-full">
    <div class="spotify-player-content">
      <div class="spotify-album-container" :class="{ 'is-playing': isPlaying }">
        <div class="spotify-album">
          <div
            class="spotify-album-cover"
            :style="{
              backgroundImage: `url(${displayCover})`,
            }"
          ></div>
          <div
            class="spotify-vinyl"
            :style="{
              animationPlayState: isPlaying ? 'running' : 'paused',
              backgroundImage: `url(${vinylImg}), url(${displayCover})`,
            }"
          ></div>
        </div>
      </div>

      <div class="spotify-song-header">
        <h2 class="spotify-song-title">
          {{ displaySongName }}
        </h2>
        <p class="spotify-song-artist">
          {{ displayArtistName }}
        </p>
      </div>

      <div class="spotify-progress-section">
        <span class="spotify-time">{{ formatTime(currentTime) }}</span>
        <div class="spotify-slider-wrapper">
          <el-slider
            v-model="currentTime"
            :show-tooltip="false"
            @change="seek"
            :max="duration"
            size="small"
            class="spotify-slider"
          />
        </div>
        <span class="spotify-time">{{ formatTime(duration) }}</span>
      </div>

      <div class="spotify-controls">
        <el-tooltip
          :content="playModes[currentMode].tooltip"
          placement="top"
          effect="dark"
        >
          <button class="spotify-control-btn" @click="togglePlayMode">
            <Icon :icon="playModes[currentMode].icon" />
          </button>
        </el-tooltip>
        <button class="spotify-control-btn spotify-control-btn-lg" @click="prevTrack">
          <Icon icon="mdi:skip-previous" />
        </button>
        <button class="spotify-play-btn" @click="togglePlayPause">
          <Icon :icon="isPlaying ? 'mdi:pause' : 'mdi:play'" />
        </button>
        <button class="spotify-control-btn spotify-control-btn-lg" @click="nextTrack">
          <Icon icon="mdi:skip-next" />
        </button>
        <button class="spotify-control-btn" @click="menuStore.setPlaylistOpen(true)">
          <Icon icon="ri:play-list-2-fill" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spotify-player-full {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.spotify-player-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  width: 100%;
  max-width: 400px;
}

.spotify-album-container {
  position: relative;
  width: 100%;
  display: flex;
  justify-content: center;
}

.spotify-album {
  position: relative;
  width: 280px;
  height: 280px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  border-radius: 8px;
}

.spotify-album-cover {
  position: relative;
  width: 280px;
  height: 280px;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  border-radius: 8px;
  z-index: 10;
}

.spotify-vinyl {
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

.is-playing .spotify-vinyl {
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

.spotify-song-header {
  text-align: center;
}

.spotify-song-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
}

.spotify-song-artist {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.7);
}

.spotify-progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.spotify-time {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.5);
  min-width: 40px;
  text-align: center;
}

.spotify-slider-wrapper {
  flex: 1;
}

.spotify-slider :deep(.el-slider__runway) {
  background-color: rgba(255, 255, 255, 0.2);
  height: 4px;
  border-radius: 2px;
}

.spotify-slider :deep(.el-slider__bar) {
  background-color: #1db954;
  height: 4px;
  border-radius: 2px;
}

.spotify-slider :deep(.el-slider__button) {
  width: 12px;
  height: 12px;
  border: none;
  background-color: #fff;
}

.spotify-slider:hover :deep(.el-slider__bar) {
  background-color: #1ed760;
}

.spotify-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.spotify-control-btn {
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

.spotify-control-btn:hover {
  color: #fff;
  transform: scale(1.05);
}

.spotify-control-btn-lg {
  font-size: 2rem;
}

.spotify-play-btn {
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

.spotify-play-btn:hover {
  transform: scale(1.06);
  background-color: #f0f0f0;
}

@media (max-width: 768px) {
  .spotify-album {
    width: 200px;
    height: 200px;
  }

  .spotify-album-cover {
    width: 200px;
    height: 200px;
  }

  .spotify-vinyl {
    display: none;
  }

  .spotify-song-title {
    font-size: 1.25rem;
  }

  .spotify-song-artist {
    font-size: 0.875rem;
  }

  .spotify-controls {
    gap: 16px;
  }

  .spotify-play-btn {
    width: 56px;
    height: 56px;
    font-size: 1.75rem;
  }
}
</style>
