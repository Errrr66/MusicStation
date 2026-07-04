<script setup lang="ts">
import Left from './components/left.vue'
import Center from './components/center.vue'
import Right from './components/right.vue'
import Recently from './components/recently.vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { formatTime } from '@/utils'
import { computed } from 'vue'

const {
  currentTime,
  duration,
  seek,
} = useAudioPlayer()

const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const handleClick = (e: MouseEvent) => {
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  const newTime = percent * duration.value
  seek(newTime)
}
</script>
<template>
  <footer class="mr-playing-bar">
    <div 
      class="mr-progress-top"
      @click="handleClick"
    >
      <div class="mr-progress-top-fill" :style="{ width: progressPercent + '%' }">
        <div class="mr-progress-top-handle">
          <div class="mr-progress-tooltip">
            {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
          </div>
        </div>
      </div>
    </div>
    <div class="mr-playing-bar-content">
      <Left />
      <Center />
      <Right />
    </div>
    <Recently />
  </footer>
</template>

<style scoped>
.mr-playing-bar {
  display: flex;
  flex-direction: column;
  background-color: #000000;
  flex-shrink: 0;
  transition: background-color 200ms ease;
  position: relative;
  height: 90px;
}

.mr-playing-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  padding-top: 12px;
  height: 90px;
}

.mr-progress-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  z-index: 10;
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-progress-top-fill {
  height: 4px;
  background-color: #fff;
  border-radius: 2px;
  position: relative;
  transition: background-color 200ms ease, height 200ms ease;
}

.mr-progress-top:hover .mr-progress-top-fill {
  background-color: var(--mr-accent);
  height: 6px;
}

.mr-progress-top-handle {
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background-color: #fff;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 200ms ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.mr-progress-top:hover .mr-progress-top-handle {
  opacity: 1;
}

.mr-progress-tooltip {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #000;
  color: #fff;
  font-size: 0.75rem;
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  opacity: 0;
  transition: opacity 200ms ease;
}

.mr-progress-tooltip::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 4px solid #000;
}

.mr-progress-top:hover .mr-progress-tooltip {
  opacity: 1;
}

:root:not(.dark) .mr-progress-tooltip {
  background-color: #fff;
  color: #000;
}

:root:not(.dark) .mr-progress-tooltip::after {
  border-top-color: #fff;
}

:root:not(.dark) .mr-playing-bar {
  background-color: #ffffff;
}

:root:not(.dark) .mr-progress-top {
  background-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-progress-top-fill {
  background-color: #000000;
}

:root:not(.dark) .mr-progress-top:hover .mr-progress-top-fill {
  background-color: var(--mr-accent);
}

:root:not(.dark) .mr-progress-top-handle {
  background-color: #000000;
}

@media (max-width: 768px) {
  .mr-playing-bar {
    height: auto;
  }
  
  .mr-playing-bar-content {
    display: none;
  }
  
  .mr-progress-top {
    position: relative;
    height: 4px;
  }
  
  .mr-progress-top-fill {
    height: 4px;
  }
  
  .mr-progress-top:hover .mr-progress-top-fill {
    height: 4px;
  }
  
  .mr-progress-top-handle {
    display: none;
  }
  
  .mr-progress-tooltip {
    display: none;
  }
}
</style>
