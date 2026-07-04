<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { computed } from 'vue'

const { volume, setVolume } = useAudioPlayer()

const isMuted = computed(() => volume.value === 0)

const toggleVolume = () => {
  setVolume(isMuted.value ? 50 : 0)
}

const volumeIcon = computed(() => {
  if (isMuted.value || volume.value === 0) return 'mdi:volume-off'
  if (volume.value < 30) return 'mdi:volume-low'
  if (volume.value < 70) return 'mdi:volume-medium'
  return 'mdi:volume-high'
})
</script>
<template>
  <div class="mr-playing-bar-right">
    <div class="mr-volume-control">
      <button
        class="mr-extra-btn"
        @click="toggleVolume"
      >
        <Icon :icon="volumeIcon" class="mr-extra-icon" />
      </button>
      <div class="mr-volume-wrapper">
        <div class="mr-volume-bar">
          <div 
            class="mr-volume-fill" 
            :style="{ width: volume + '%' }"
          >
            <div class="mr-volume-handle"></div>
          </div>
        </div>
        <input
          type="range"
          class="mr-volume-input"
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
.mr-playing-bar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  min-width: 180px;
  width: 30%;
}

.mr-extra-btn {
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

.mr-extra-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.mr-extra-btn:active {
  transform: scale(1);
}

.mr-extra-icon {
  font-size: 1rem;
}

.mr-volume-control {
  display: flex;
  align-items: center;
  gap: 4px;
}

.mr-volume-wrapper {
  position: relative;
  width: 120px;
  height: 24px;
  display: flex;
  align-items: center;
}

.mr-volume-bar {
  width: 100%;
  height: 6px;
  background-color: var(--volume-bg, #4d4d4d);
  border-radius: 3px;
  overflow: hidden;
}

.mr-volume-fill {
  height: 100%;
  background-color: var(--volume-fill, #fff);
  border-radius: 2px;
  position: relative;
  transition: background-color 200ms ease;
}

.mr-volume-handle {
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

.mr-volume-wrapper:hover .mr-volume-fill {
  background-color: var(--volume-hover, var(--mr-accent));
}

.mr-volume-wrapper:hover .mr-volume-handle {
  opacity: 1;
}

.mr-volume-input {
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
:root:not(.dark) .mr-playing-bar-right {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --volume-bg: #c0c0c0;
  --volume-fill: #000000;
  --volume-hover: var(--mr-accent);
}

@media (max-width: 768px) {
  .mr-playing-bar-right {
    display: none;
  }
}
</style>
