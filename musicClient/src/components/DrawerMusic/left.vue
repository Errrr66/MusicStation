<script setup lang="ts">
import { formatTime, fixUrl } from '@/utils'
import { Icon } from '@iconify/vue'
import type { SongDetail } from '@/api/interface'
import { ref, inject, type Ref } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import vinylImg from '@/assets/vinyl.png'
import Recently from '../../layout/components/footer/components/recently.vue'

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

// 添加播放模式相关逻辑
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
  <div
    class="w-full h-full relative inset-0 px-4 flex flex-col items-center justify-center py-4 md:py-0 shrink-0"
  >
    <div class="flex flex-1 flex-col gap-4 items-center justify-center w-full">
      <!-- 封面 -->
      <div :class="` ${isPlaying ? 'is-playing' : ''}`">
        <div class="album">
          <div
            class="album-art rounded-md"
            :style="{
              backgroundImage: `url(${fixUrl(songDetail?.coverUrl) || fixUrl(currentTrack.cover)})`,
            }"
          ></div>
          <div
            class="vinyl"
            :style="{
              animationPlayState: isPlaying ? 'running' : 'paused',
              backgroundImage: `url(${vinylImg}), url(${fixUrl(songDetail?.coverUrl) || fixUrl(currentTrack.cover)})`,
            }"
          ></div>
        </div>
      </div>
      <!-- 标题类 -->
      <div class="flex flex-col items-center gap-2 mt-10">
        <h2 class="text-3xl font-bold text-primary-foreground">
          {{ songDetail?.songName || currentTrack.title }}
        </h2>
        <p class="text-xl text-inactive">
          {{ songDetail?.artistName || currentTrack.artist }}
        </p>
      </div>
      <!-- 控制区 -->
      <div class="flex gap-2 w-full items-center justify-center mt-8">
        <div class="flex items-center gap-2 w-11/12 md:w-2/4">
          <span class="text-xs w-10 text-foreground/50 text-center">{{
            formatTime(currentTime)
          }}</span>
          <el-slider
            v-model="currentTime"
            :show-tooltip="false"
            @change="seek"
            :max="duration"
            class="flex-1"
            size="small"
          />
          <span class="text-xs w-10 text-foreground/50 text-center">{{
            formatTime(duration)
          }}</span>
        </div>
      </div>
      <div class="flex items-center justify-between md:justify-center md:gap-14 w-full md:w-2/4 mt-12 px-2 md:px-0">
        <el-tooltip
          :content="playModes[currentMode].tooltip"
          placement="top"
          effect="dark"
        >
          <el-button text circle @click="togglePlayMode">
            <Icon :icon="playModes[currentMode].icon" class="text-2xl" />
          </el-button>
        </el-tooltip>
        <el-button text circle class="!p-3" @click="prevTrack">
          <icon-solar:skip-previous-bold class="text-2xl" />
        </el-button>
        <el-button text circle class="!p-3" @click="togglePlayPause">
          <Icon
            :icon="
              isPlaying
                ? 'ic:round-pause-circle'
                : 'material-symbols:play-circle'
            "
            class="text-7xl"
            :color="'#2a68fa'"
          />
        </el-button>
        <el-button text circle class="!p-3" @click="nextTrack">
          <icon-solar:skip-previous-bold class="scale-x-[-1] text-2xl" />
        </el-button>
        <el-button text circle class="scale-125 text-primary-foreground">
          <Recently />
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.album {
  box-shadow: 3px 3px 15px rgba(0, 0, 0, 0.65);
  height: 100%;
  position: relative;
  width: 100%;
  z-index: 10;
  border-radius: 8px;
}

.album-art {
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  height: 315px;
  position: relative;
  width: 325px;
  z-index: 10;
}

.vinyl {
  animation: spin 2s linear infinite;
  transition: all 500ms;
  background-position: center, center;
  background-size:
    cover,
    40% auto;
  background-repeat: no-repeat;
  border-radius: 100%;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.8);
  height: 300px;
  left: 5%;
  position: absolute;
  top: 8px;
  width: 300px;
  z-index: 5;
  will-change: transform, left;

  .is-playing & {
    left: 52%;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .album-art {
    width: 70vw;
    height: 70vw;
    max-width: 300px;
    max-height: 300px;
  }

  .vinyl {
    display: none;
  }
}
</style>
