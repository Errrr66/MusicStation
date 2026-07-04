<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { MenuStore } from '@/stores/modules/menu'
import { computed } from 'vue'

const menuStore = MenuStore()

const {
  isPlaying,
  nextTrack,
  prevTrack,
  togglePlayPause,
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
  return map[playMode.value] ?? 'ri:order-play-line'
})

const modeTitle = computed(() => {
  const map = {
    order: '顺序播放',
    loop: '列表循环',
    single: '单曲循环',
    shuffle: '随机播放'
  }
  return map[playMode.value] ?? '顺序播放'
})

const openQueue = () => {
  menuStore.setPlaylistOpen(true)
}
</script>

<template>
  <div class="mr-playing-bar-center">
    <div class="mr-playing-controls">
      <button
        @click="togglePlayMode"
        class="mr-control-btn mr-control-btn-mode"
        :title="modeTitle"
      >
        <Icon :icon="modeIcon" class="mr-control-icon" />
      </button>

      <button
        @click="prevTrack"
        class="mr-control-btn"
      >
        <Icon icon="mdi:skip-previous" class="mr-control-icon-lg" />
      </button>

      <button
        @click="togglePlayPause"
        class="mr-play-btn"
      >
        <Icon
          :icon="isPlaying ? 'mdi:pause' : 'mdi:play'"
          class="mr-play-icon"
        />
      </button>

      <button
        @click="nextTrack"
        class="mr-control-btn"
      >
        <Icon icon="mdi:skip-next" class="mr-control-icon-lg" />
      </button>

      <button
        @click="openQueue"
        class="mr-control-btn"
        title="播放队列"
      >
        <Icon icon="ri:play-list-2-fill" class="mr-control-icon" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.mr-playing-bar-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  max-width: 722px;
}

.mr-playing-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mr-control-btn {
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

.mr-control-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.mr-control-btn:active {
  transform: scale(1);
}

.mr-control-btn-mode {
  position: relative;
}

.mr-control-btn-mode.active::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background-color: var(--text-accent, var(--mr-accent));
  border-radius: 50%;
}

.mr-control-icon {
  font-size: 1rem;
}

.mr-control-icon-lg {
  font-size: 1.5rem;
}

.mr-play-btn {
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

.mr-play-btn:hover {
  transform: scale(1.06);
}

.mr-play-btn:active {
  transform: scale(1);
}

.mr-play-icon {
  font-size: 1.25rem;
  margin-left: 2px;
}

:root:not(.dark) .mr-playing-bar-center {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-accent: var(--mr-accent);
  --btn-bg: #000000;
  --btn-text: #ffffff;
}

@media (max-width: 768px) {
  .mr-playing-bar-center {
    display: none;
  }
}
</style>
