<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { formatTime } from '@/utils'
import { AudioStore } from '@/stores/modules/audio'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'

const {
  isPlaying,
  currentTime,
  duration,
  nextTrack,
  prevTrack,
  togglePlayPause,
  seek,
} = useAudioPlayer()
</script>

<template>
  <div class="flex flex-col items-center justify-center flex-1 h-full">
    <!-- Controls -->
    <div class="flex items-center justify-center space-x-6 mb-2">
      <div class="flex items-center space-x-4">
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
    <div class="w-1/2 flex items-center space-x-3">
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
