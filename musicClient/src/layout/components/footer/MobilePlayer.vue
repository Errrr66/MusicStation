<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import DrawerMusic from '@/components/DrawerMusic/index.vue'
import { Icon } from '@iconify/vue'
import { UserStore } from '@/stores/modules/user'
import { AudioStore } from '@/stores/modules/audio'
import { collectSong, cancelCollectSong } from '@/api/system'
import { ElMessage } from 'element-plus'

const { currentTrack, isPlaying, togglePlayPause, nextTrack, prevTrack, currentTime, duration, playMode, togglePlayMode } = useAudioPlayer()
const showDrawerMusic = ref(false)
const userStore = UserStore()
const audioStore = AudioStore()

const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const modeIcon = computed(() => {
  const map = {
    order: 'ri:order-play-line',
    shuffle: 'ri:shuffle-line',
    loop: 'ri:repeat-2-line',
    single: 'ri:repeat-one-line'
  }
  return map[playMode.value]
})

const currentSongLikeStatus = computed(() => {
  const currentTrack = audioStore.trackList[audioStore.currentSongIndex]
  return currentTrack?.likeStatus || 0
})

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  const currentTrack = audioStore.trackList[audioStore.currentSongIndex]
  if (!currentTrack) return

  try {
    const songId = Number(currentTrack.id)
    if (currentSongLikeStatus.value === 0) {
      const res = await collectSong(songId)
      if (res.code === 0) {
        currentTrack.likeStatus = 1
        ElMessage.success('已添加到我的喜欢')
      }
    } else {
      const res = await cancelCollectSong(songId)
      if (res.code === 0) {
        currentTrack.likeStatus = 0
        ElMessage.success('已取消喜欢')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}
</script>

<template>
  <div class="spotify-mobile-player">
    <div class="spotify-mobile-player-progress">
      <div class="spotify-mobile-player-progress-bar" :style="{ width: progressPercent + '%' }"></div>
    </div>
    
    <div class="spotify-mobile-player-content" @click="showDrawerMusic = true">
      <div class="spotify-mobile-player-cover">
        <img
          :src="currentTrack.cover + '?param=60y60'"
          :alt="currentTrack.title"
          class="spotify-mobile-player-img"
        />
      </div>
      
      <div class="spotify-mobile-player-info">
        <div class="spotify-mobile-player-title">{{ currentTrack.title }}</div>
        <div class="spotify-mobile-player-artist">{{ currentTrack.artist }}</div>
      </div>
      
      <div class="spotify-mobile-player-actions" @click.stop>
        <button class="spotify-mobile-player-btn spotify-mobile-player-btn-sm" @click="togglePlayMode" :title="playMode">
          <Icon :icon="modeIcon" />
        </button>
        <button class="spotify-mobile-player-btn" @click="prevTrack">
          <Icon icon="mdi:skip-previous" />
        </button>
        <button class="spotify-mobile-player-btn spotify-mobile-player-btn-play" @click="togglePlayPause">
          <Icon :icon="isPlaying ? 'mdi:pause' : 'mdi:play'" />
        </button>
        <button class="spotify-mobile-player-btn" @click="nextTrack">
          <Icon icon="mdi:skip-next" />
        </button>
        <button class="spotify-mobile-player-btn spotify-mobile-player-btn-sm" @click="handleLike">
          <Icon
            :icon="currentSongLikeStatus === 0 ? 'mdi:cards-heart-outline' : 'mdi:cards-heart'"
            :class="{ 'spotify-like-active': currentSongLikeStatus !== 0 }"
          />
        </button>
      </div>
    </div>
    
    <DrawerMusic v-model="showDrawerMusic" />
  </div>
</template>

<style scoped>
.spotify-mobile-player {
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

.spotify-mobile-player-progress {
  height: 2px;
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-mobile-player-progress-bar {
  height: 100%;
  background-color: #1db954;
}

.spotify-mobile-player-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  cursor: pointer;
}

.spotify-mobile-player-cover {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.spotify-mobile-player-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.spotify-mobile-player-info {
  flex: 1;
  min-width: 0;
}

.spotify-mobile-player-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-mobile-player-artist {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.spotify-mobile-player-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.spotify-mobile-player-btn {
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

.spotify-mobile-player-btn:hover {
  color: var(--text-base, #fff);
}

.spotify-mobile-player-btn:active {
  transform: scale(0.95);
}

.spotify-mobile-player-btn-play {
  font-size: 1.75rem;
}

.spotify-mobile-player-btn-sm {
  width: 36px;
  height: 36px;
  font-size: 1.25rem;
}

.spotify-like-active {
  color: #1db954;
}

/* Light Theme */
:root:not(.dark) .spotify-mobile-player {
  --bg-playing-bar: #ffffff;
  --border-color: rgba(0, 0, 0, 0.1);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
}

@media (max-width: 768px) {
  .spotify-mobile-player {
    display: flex;
  }
}
</style>
