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
  <div class="flex flex-col items-center justify-center flex-none md:flex-1 h-full px-2 md:px-0">
    <!-- Controls -->
    <div class="flex items-center justify-center space-x-2 md:space-x-6 md:mb-2">
      <div class="flex items-center space-x-2 md:space-x-4">
        <!-- Play Mode Button -->
        <button
          @click="togglePlayMode"
          class="p-2 rounded-full hover:bg-hoverMenuBg transition md:hidden"
          :title="modeTitle"
        >
           <Icon :icon="modeIcon" class="text-xl md:text-2xl text-gray-500 hover:text-primary" />
        </button>

        <button
          @click="prevTrack"
          class="p-2 rounded-full hover:bg-hoverMenuBg transition"
        >
          <Icon icon="solar:skip-previous-bold" class="text-2xl" />
        </button>

        <button
          @click="togglePlayPause"
          class="p-2 rounded-full hover:bg-hoverMenuBg transition transform hover:scale-110"
        >
          <Icon
            :icon="
              isPlaying
                ? 'ic:round-pause-circle'
                : 'material-symbols:play-circle'
            "
            class="text-5xl"
            :color="'#2a68fa'"
          />
        </button>

        <button
          @click="nextTrack"
          class="p-2 rounded-full hover:bg-hoverMenuBg transition"
        >
          <Icon
            icon="solar:skip-previous-bold"
            class="transform scale-x-[-1] text-2xl"
          />
        </button>
      </div>
    </div>

    <!-- Progress Bar -->
    <div class="hidden md:flex w-1/2 items-center space-x-3">
      <span class="text-xs w-10 text-right">{{ formatTime(currentTime) }}</span>
      <el-slider
        v-model="currentTime"
        :step="1"
        :show-tooltip="false"
        @change="seek"
        :max="duration"
        class="flex-1"
        size="small"
      />
      <span class="text-xs w-10">{{ formatTime(duration) }}</span>
    </div>
  </div>
</template>
