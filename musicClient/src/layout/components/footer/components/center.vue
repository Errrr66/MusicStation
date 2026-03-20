<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { formatTime } from '@/utils'
import { AudioStore } from '@/stores/modules/audio'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { computed } from 'vue'

const {
  isPlaying,
  currentTime,
  duration,
  nextTrack,
  prevTrack,
  togglePlayPause,
  seek,
  playMode,
  togglePlayMode
} = useAudioPlayer()

const modeIcon = computed(() => {
  const map = {
    order: 'ri:order-play-line',
    shuffle: 'ri:shuffle-line',
    loop: 'ri:repeat-2-line',
    single: 'ri:repeat-one-line'
  }
  return map[playMode.value]
})

const modeTitle = computed(() => {
  const map = {
    order: '顺序播放',
    loop: '列表循环',
    single: '单曲循环',
    shuffle: '随机播放'
  }
  return map[playMode.value]
})
</script>

<template>
  <div class="spotify-playing-bar-center">
    <!-- Controls -->
    <div class="spotify-playing-controls">
      <!-- Play Mode Button -->
      <button
        @click="togglePlayMode"
        class="spotify-control-btn spotify-control-btn-mode"
        :title="modeTitle"
      >
        <Icon :icon="modeIcon" class="spotify-control-icon" />
      </button>

      <!-- Previous Button -->
      <button
        @click="prevTrack"
        class="spotify-control-btn"
      >
        <Icon icon="mdi:skip-previous" class="spotify-control-icon-lg" />
      </button>

      <!-- Play/Pause Button -->
      <button
        @click="togglePlayPause"
        class="spotify-play-btn"
      >
        <Icon
          :icon="isPlaying ? 'mdi:pause' : 'mdi:play'"
          class="spotify-play-icon"
        />
      </button>

      <!-- Next Button -->
      <button
        @click="nextTrack"
        class="spotify-control-btn"
      >
        <Icon icon="mdi:skip-next" class="spotify-control-icon-lg" />
      </button>
    </div>

    <!-- Progress Bar -->
    <div class="spotify-progress-container">
      <span class="spotify-time">{{ formatTime(currentTime) }}</span>
      <div class="spotify-progress-wrapper">
        <div class="spotify-progress-bar">
          <div 
            class="spotify-progress-fill" 
            :style="{ width: (currentTime / duration * 100) + '%' }"
          >
            <div class="spotify-progress-handle"></div>
          </div>
        </div>
        <input
          type="range"
          class="spotify-progress-input"
          :value="currentTime"
          :max="duration"
          step="1"
          @input="seek($event.target.value)"
        />
      </div>
      <span class="spotify-time">{{ formatTime(duration) }}</span>
    </div>
  </div>
</template>

<style scoped>
.spotify-playing-bar-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  max-width: 722px;
  gap: 8px;
}

.spotify-playing-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spotify-control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--text-subdued, #b3b3b3);
  transition: color 200ms ease, transform 33ms ease;
}

.spotify-control-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.spotify-control-btn:active {
  transform: scale(1);
}

.spotify-control-btn-mode {
  position: relative;
}

.spotify-control-btn-mode.active::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background-color: var(--text-accent, #1db954);
  border-radius: 50%;
}

.spotify-control-icon {
  font-size: 1rem;
}

.spotify-control-icon-lg {
  font-size: 1.5rem;
}

.spotify-play-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background-color: var(--btn-bg, #fff);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--btn-text, #000);
  transition: transform 33ms ease, background-color 200ms ease;
}

.spotify-play-btn:hover {
  transform: scale(1.06);
}

.spotify-play-btn:active {
  transform: scale(1);
}

.spotify-play-icon {
  font-size: 1.25rem;
  margin-left: 2px;
}

.spotify-progress-container {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.spotify-time {
  font-size: 0.6875rem;
  font-weight: 400;
  color: var(--text-subdued, #b3b3b3);
  min-width: 40px;
  text-align: center;
}

.spotify-progress-wrapper {
  position: relative;
  flex: 1;
  height: 12px;
  display: flex;
  align-items: center;
}

.spotify-progress-bar {
  width: 100%;
  height: 4px;
  background-color: var(--progress-bg, #4d4d4d);
  border-radius: 2px;
  overflow: hidden;
}

.spotify-progress-fill {
  height: 100%;
  background-color: var(--progress-fill, #fff);
  border-radius: 2px;
  position: relative;
  transition: background-color 200ms ease;
}

.spotify-progress-handle {
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background-color: var(--progress-fill, #fff);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 200ms ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.spotify-progress-wrapper:hover .spotify-progress-fill {
  background-color: var(--progress-hover, #1db954);
}

.spotify-progress-wrapper:hover .spotify-progress-handle {
  opacity: 1;
}

.spotify-progress-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  margin: 0;
}

/* Light Theme */
:root:not(.dark) .spotify-playing-bar-center {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-accent: #1db954;
  --btn-bg: #000000;
  --btn-text: #ffffff;
  --progress-bg: #c0c0c0;
  --progress-fill: #000000;
  --progress-hover: #1db954;
}

@media (max-width: 768px) {
  .spotify-playing-bar-center {
    display: none;
  }
}
</style>
