<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { computed } from 'vue'

const { volume, setVolume } = useAudioPlayer()

const isMuted = computed(() => volume.value === 0)

const toggleVolume = () => {
  setVolume(isMuted.value ? 0.5 : 0)
}

const volumeIcon = computed(() => {
  if (isMuted.value || volume.value === 0) return 'mdi:volume-off'
  if (volume.value < 30) return 'mdi:volume-low'
  if (volume.value < 70) return 'mdi:volume-medium'
  return 'mdi:volume-high'
})
</script>
<template>
  <div class="spotify-playing-bar-right">
    <div class="spotify-volume-control">
      <button
        class="spotify-extra-btn"
        @click="toggleVolume"
      >
        <Icon :icon="volumeIcon" class="spotify-extra-icon" />
      </button>
      <div class="spotify-volume-wrapper">
        <div class="spotify-volume-bar">
          <div 
            class="spotify-volume-fill" 
            :style="{ width: volume + '%' }"
          >
            <div class="spotify-volume-handle"></div>
          </div>
        </div>
        <input
          type="range"
          class="spotify-volume-input"
          :value="volume"
          max="100"
          step="1"
          @input="setVolume(Number(($event.target as HTMLInputElement).value))"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.spotify-playing-bar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  min-width: 180px;
  width: 30%;
}

.spotify-extra-btn {
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

.spotify-extra-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.spotify-extra-btn:active {
  transform: scale(1);
}

.spotify-extra-icon {
  font-size: 1rem;
}

.spotify-volume-control {
  display: flex;
  align-items: center;
  gap: 4px;
}

.spotify-volume-wrapper {
  position: relative;
  width: 120px;
  height: 24px;
  display: flex;
  align-items: center;
}

.spotify-volume-bar {
  width: 100%;
  height: 6px;
  background-color: var(--volume-bg, #4d4d4d);
  border-radius: 3px;
  overflow: hidden;
}

.spotify-volume-fill {
  height: 100%;
  background-color: var(--volume-fill, #fff);
  border-radius: 2px;
  position: relative;
  transition: background-color 200ms ease;
}

.spotify-volume-handle {
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background-color: var(--volume-fill, #fff);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 200ms ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.spotify-volume-wrapper:hover .spotify-volume-fill {
  background-color: var(--volume-hover, #1db954);
}

.spotify-volume-wrapper:hover .spotify-volume-handle {
  opacity: 1;
}

.spotify-volume-input {
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
:root:not(.dark) .spotify-playing-bar-right {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --volume-bg: #c0c0c0;
  --volume-fill: #000000;
  --volume-hover: #1db954;
}

@media (max-width: 768px) {
  .spotify-playing-bar-right {
    display: none;
  }
}
</style>
