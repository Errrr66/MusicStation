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
      '--track-cover-url': `url(${currentTrack.cover})`,
    }"
    v-model="showDrawer"
    direction="btt"
    size="100%"
    :modal="false"
    :showClose="false"
    class="drawer-bg backdrop-filter backdrop-blur-md"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div
          class="flex items-center justify-center gap-2 text-primary-foreground"
        >
          <el-button text circle @click="showDrawer = false">
            <icon-uiw:down />
          </el-button>
        </div>
      </div>
    </template>
    <main class="flex h-full">
      <div class="flex w-full flex-1">
        <div class="w-1/2">
          <Left />
        </div>
        <div class="w-1/2 relative">
          <Right />
        </div>
      </div>
    </main>
    <template #footer>
      <div class="flex justify-end gap-2 pr-4 pb-4">
        <button @click="toggleMode" class="text-primary-foreground hover:bg-black/10 dark:hover:bg-white/10 p-2 rounded-full transition-colors">
          <Icon class="text-xl" :icon="currentIcon" />
        </button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
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
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(20px);
  z-index: -1;
}
</style>
