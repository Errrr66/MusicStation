<script setup lang="ts">
import Left from './left.vue'
import Right from './right.vue'
import { useDark, useToggle } from '@vueuse/core'
import { getSongDetail } from '@/api/system'
import type { SongDetail } from '@/api/interface'
import { ref, provide, watch } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { themeStore } from '@/stores/modules/theme'
import { Icon } from '@iconify/vue'
import { fixUrl } from '@/utils'

const theme = themeStore()
const showDrawer = defineModel<boolean>()
const songDetail = ref<SongDetail | null>(null)

const isDark = useDark({
  selector: 'html',
  attribute: 'class',
  valueDark: 'dark',
  valueLight: 'light',
})
const toggleDark = useToggle(isDark)
const toggleMode = () => {
  theme.setDark(!isDark.value)
  toggleDark()
}
const { currentTrack } = useAudioPlayer()

const currentIcon = ref('material-symbols:wb-sunny-outline-rounded')

watch(
  () => theme.isDark,
  (newValue) => {
    currentIcon.value = newValue
      ? 'mdi:weather-night'
      : 'material-symbols:wb-sunny-outline-rounded'
  },
  { immediate: true }
)

// 监听 currentTrack 的变化，获取歌曲详情
watch(
  () => currentTrack.value.id,
  async (newId) => {
    if (newId) {
      try {
        const res = await getSongDetail(Number(newId))
        if (res.code === 0 && res.data) {
          // 确保返回的数据符合 SongDetail 接口
          const songData = res.data as unknown as SongDetail
          if (
            'songId' in songData &&
            'songName' in songData &&
            'artistName' in songData &&
            'album' in songData
          ) {
            songDetail.value = songData
          } else {
            console.error('歌曲详情数据格式不正确')
          }
        }
      } catch (error) {
        console.error('获取歌曲详情失败:', error)
      }
    }
  },
  { immediate: true }
)

// 提供 songDetail 给子组件
provide('songDetail', songDetail)
</script>
<template>
  <el-drawer
    :style="{
      '--track-cover-url': `url(${fixUrl(currentTrack.cover)})`,
    }"
    v-model="showDrawer"
    direction="btt"
    size="100%"
    :modal="false"
    :showClose="false"
    class="drawer-bg !p-0 !m-0 rounded-t-3xl overflow-hidden"
    :with-header="false"
  >
    <div class="h-full w-full flex flex-col backdrop-filter backdrop-blur-2xl bg-black/40 rounded-3xl overflow-hidden">
      <!-- Header -->
      <div class="flex items-center justify-between p-4 px-6">
        <el-button text circle @click="showDrawer = false" class="!bg-white/10 hover:!bg-white/20 !text-white !p-2">
           <Icon icon="solar:alt-arrow-down-linear" class="text-2xl" />
        </el-button>
        <div class="flex-1 text-center mx-4">
             <!-- Title can go here -->
        </div>
        <button @click="toggleMode" class="text-white hover:bg-white/10 p-2 rounded-full transition-colors">
          <Icon class="text-xl" :icon="currentIcon" />
        </button>
      </div>

      <main class="flex-1 overflow-hidden relative">
        <div class="h-full w-full flex flex-row overflow-x-auto snap-x snap-mandatory md:overflow-hidden no-scrollbar">
          <!-- Left: Cover & Info -->
          <div class="w-full md:w-1/2 flex-shrink-0 snap-center h-full flex items-center justify-center p-0 md:p-12 overflow-hidden">
            <Left />
          </div>
          <!-- Right: Lyrics -->
          <div class="w-full md:w-1/2 flex-shrink-0 snap-center h-full p-0 md:p-10 overflow-hidden">
            <Right />
          </div>
        </div>
      </main>
   </div>
  </el-drawer>
</template>

<style scoped>
:deep(.el-drawer) {
    border-top-left-radius: 1.5rem !important; /* rounded-3xl approx */
    border-top-right-radius: 1.5rem !important;
    overflow: hidden !important;
}

@media (max-width: 768px) {
  :deep(.el-drawer) {
    height: calc(100% - 20px) !important;
    margin-bottom: 10px !important;
    border-radius: 1.5rem !important;
    width:calc(100% - 20px) !important;
    margin-left: 10px !important;
  }
}

:deep(.el-drawer__body) {
    padding: 0 !important;
    height: 100% !important;
}

.drawer-bg {
  background-image: var(--track-cover-url);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.drawer-bg::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.8));
  backdrop-filter: blur(60px) saturate(180%) contrast(1.1);
  z-index: -1;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
