<script setup lang="ts">
import { Icon } from '@iconify/vue'
import Avatar from './components/avatar.vue'
import SongRecognizer from '@/components/SongRecognizer.vue'
import { getAllSongs } from '@/api/system'
import { Song } from '@/api/interface'
import { AudioStore } from '@/stores/modules/audio'
import { MenuStore } from '@/stores/modules/menu'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import default_album from '@/assets/default_album.jpg'
// @ts-ignore
import * as OpenCC from 'opencc-js'

const route = useRoute()
const router = useRouter()
const currentIcon = ref('material-symbols:wb-sunny-outline-rounded')
const theme = themeStore()
const menuStore = MenuStore()
import { useDark, useToggle } from '@vueuse/core'

const searchText = ref('')
const isMobileSearchOpen = ref(false)

const toggleMobileMenu = () => {
  menuStore.setMobileMenuOpen(!menuStore.isMobileMenuOpen)
}

const toggleMobileRightAside = () => {
  menuStore.setRightAsideOpen(!menuStore.isRightAsideOpen)
}

const toggleMobileSearch = () => {
  isMobileSearchOpen.value = !isMobileSearchOpen.value
}

// 听歌识曲相关
const showRecognizerDropdown = ref(false)
const recognizedTrack = ref<any>(null)
const localSongMatch = ref<Song | null>(null)
const isCheckingLocal = ref(false)
const audioStore = AudioStore()
const { loadTrack, play } = useAudioPlayer()

const converter = OpenCC.Converter({ from: 'hk', to: 'cn' })

const handleRecognitionSuccess = async (result: any) => {
  recognizedTrack.value = result.track
  showRecognizerDropdown.value = true
  localSongMatch.value = null
  isCheckingLocal.value = true

  const originalTitle = result.track.title
  const originalArtist = result.track.subtitle

  // 转换为简体中文
  const simplifiedTitle = converter(originalTitle)
  const simplifiedArtist = converter(originalArtist)

  // 提取核心标题（去除括号内容），用于匹配不同版本（如 feat. 等）
  const cleanTitle = (str: string) => {
    return str.replace(/\s*[\(\[（].*?[\)\]）]/g, '').trim()
  }
  const coreTitle = cleanTitle(originalTitle)
  const simplifiedCoreTitle = converter(coreTitle)

  try {
    // 1. 优先使用简体中文搜索本地曲库
    let res = await getAllSongs({
      songName: simplifiedTitle,
      pageNum: 1,
      pageSize: 20 // 增加搜索范围
    })

    // 2. 如果无结果且标题不同，尝试原标题搜索
    if ((!res.data || !res.data.items || res.data.items.length === 0) && originalTitle !== simplifiedTitle) {
      res = await getAllSongs({
        songName: originalTitle,
        pageNum: 1,
        pageSize: 20
      })
    }

    // 3. 如果还是无结果，且核心标题与原标题不同（说明有括号内容），尝试使用核心标题搜索
    // 这能解决如：识别出 "Song (feat. X)" 但本地只有 "Song" 的情况
    if ((!res.data || !res.data.items || res.data.items.length === 0) && coreTitle.length > 0 && coreTitle !== originalTitle) {
      res = await getAllSongs({
        songName: simplifiedCoreTitle,
        pageNum: 1,
        pageSize: 20
      })
    }

    if (res.code === 0 && res.data && res.data.items) {
       // 尝试找到匹配的歌曲
       // 优先匹配 title 和 artist
       const matches = res.data.items.filter((item: Song) => {
          const itemTitle = item.songName.toLowerCase()
          const searchTitleSimp = simplifiedTitle.toLowerCase()
          const searchTitleOrig = originalTitle.toLowerCase()
          const searchCoreSimp = simplifiedCoreTitle.toLowerCase()

          return itemTitle.includes(searchTitleSimp) ||
                 itemTitle.includes(searchTitleOrig) ||
                 searchTitleSimp.includes(itemTitle) ||
                 (searchCoreSimp.length > 1 && itemTitle.includes(searchCoreSimp))
       })

       if (matches.length > 0) {
         // 如果有多个匹配，尝试匹配艺术家 (也需考虑简繁)
         const artistMatch = matches.find((item: Song) => {
            const itemArtist = item.artistName.toLowerCase()
            const searchArtistSimp = simplifiedArtist.toLowerCase()
            const searchArtistOrig = originalArtist.toLowerCase()

            return itemArtist.includes(searchArtistSimp) ||
                   itemArtist.includes(searchArtistOrig) ||
                   searchArtistSimp.includes(itemArtist)
         })
         localSongMatch.value = artistMatch || matches[0]
       }
    }
  } catch (e) {
    console.error("查找本地歌曲失败", e)
  } finally {
    isCheckingLocal.value = false
  }
}

const playRecognizedSong = async () => {
   if (localSongMatch.value) {
     const song = localSongMatch.value
      const track = {
        id: song.songId.toString(),
        title: song.songName,
        artist: song.artistName,
        album: song.album || recognizedTrack.value.title, // fallback
        cover: song.coverUrl || default_album,
        url: song.audioUrl,
        duration: Number(song.duration) || 0,
        likeStatus: song.likeStatus || 0,
      }

      // 添加到播放列表并播放
      audioStore.addTracks(track)
      // addTracks 会自动设置 currentSongIndex 如果 ID 已存在或者添加到末尾
      // 但我们需要确保加载并播放

      await loadTrack()
      play()
      showRecognizerDropdown.value = false
   }
}

const closeRecognizerDropdown = () => {
  showRecognizerDropdown.value = false
}

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

// 初始化时根据 store 设置图标
watch(
  () => theme.isDark,
  (newValue) => {
    currentIcon.value = newValue
      ? 'mdi:weather-night'
      : 'material-symbols:wb-sunny-outline-rounded'
  },
  { immediate: true }
)

// 赋值到搜索框
watch(
  () => route.query,
  (newValue) => {
    if (newValue.query) {
      searchText.value = newValue.query as string
    }
  },
  { immediate: true }
)
</script>
<template>
  <header class="px-4 py-2 border-b flex items-center justify-between relative">
    <div class="flex items-center gap-2 w-60">
      <!-- 移动端菜单按钮 -->
      <button class="md:hidden p-1 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700" @click="toggleMobileMenu">
        <Icon icon="ri:menu-line" class="text-2xl" />
      </button>

      <button class="flex relative items-center" @click="router.push('/')">
        <img src="\logo.svg?v=1" alt="logo" class="w-10 h-10 ml-2" />
        <span class="ml-3 text-2xl font-bold hidden md:block">Parachutes</span>
      </button>
    </div>

    <!-- 输入框和头像 -->
    <div class="hidden md:flex items-center justify-center gap-3 flex-1 relative">
      <div class="relative mr-6">
        <Icon
          icon="mdi:magnify"
          class="absolute left-2 top-1/2 transform -translate-y-1/2 text-gray-500 text-xl"
        />
        <input
          v-model="searchText"
          type="text"
          class="mt-0.5 w-96 text-sm pl-8 pr-2 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary transition-all duration-300 focus:w-80 search-bg"
          placeholder="想播放什么？"
          @keyup.enter="router.push('/library?query=' + searchText)"
        />

        <!-- 听歌识曲结果下拉框已移动到外层 -->
      </div>
      <SongRecognizer @success="handleRecognitionSuccess" />
      <button @click="toggleMode">
        <Icon class="text-xl" :class="theme.isDark ? '' : 'text-orange-500'" :icon="currentIcon" />
      </button>
    </div>
    <div class="md:w-60 flex justify-end items-center gap-3 ml-auto">
       <!-- 移动端搜索和主题控制 -->
       <div class="flex items-center gap-3 md:hidden">
        <SongRecognizer @success="handleRecognitionSuccess" />
        <button @click="toggleMobileSearch">
          <Icon icon="mdi:magnify" class="text-xl" />
        </button>
        <button @click="toggleMode">
          <Icon class="text-xl" :class="theme.isDark ? '' : 'text-orange-500'" :icon="currentIcon" />
        </button>
        <button @click="toggleMobileRightAside">
          <Icon icon="mdi:music-box-outline" class="text-xl" />
        </button>
      </div>
      <Avatar />
    </div>

    <!-- 听歌识曲结果下拉框 (Global) -->
    <div
      v-if="showRecognizerDropdown"
      class="absolute top-[calc(100%+0.5rem)] left-1/2 -translate-x-1/2 w-[95%] md:w-[400px] bg-white dark:bg-gray-800 rounded-lg shadow-xl z-[100] border border-gray-200 dark:border-gray-700 p-2"
    >
      <div class="flex justify-between items-center mb-2 px-1">
          <span class="text-xs text-gray-500 font-bold">识别结果</span>
          <button @click="closeRecognizerDropdown" class="text-gray-400 hover:text-gray-600">
            <Icon icon="mdi:close" />
          </button>
      </div>

      <div
        v-if="recognizedTrack"
        class="flex items-center gap-3 p-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer transition-colors"
        :class="{'opacity-50 cursor-not-allowed': !localSongMatch && !isCheckingLocal}"
        @click="playRecognizedSong"
      >
          <!-- 封面 -->
          <div class="relative w-12 h-12 flex-shrink-0">
            <img
              :src="localSongMatch?.coverUrl || recognizedTrack.photo_url || default_album"
              class="w-full h-full object-cover rounded-md"
              alt="cover"
            />
            <div v-if="localSongMatch" class="absolute inset-0 flex items-center justify-center bg-black/30 rounded-md opacity-0 hover:opacity-100 transition-opacity">
              <Icon icon="mdi:play" class="text-white text-xl" />
            </div>
          </div>

          <!-- 信息 -->
          <div class="flex-1 min-w-0">
            <h3 class="font-medium text-sm truncate text-gray-900 dark:text-gray-100">{{ recognizedTrack.title }}</h3>
            <p class="text-xs text-gray-500 truncate">{{ recognizedTrack.subtitle }}</p>
            <p v-if="isCheckingLocal" class="text-xs text-blue-500 mt-1">正在查找曲库...</p>
            <p v-else-if="!localSongMatch" class="text-xs text-red-500 mt-1">歌曲暂未收录</p>
            <p v-else class="text-xs text-green-500 mt-1">点击播放</p>
          </div>
      </div>
    </div>

    <!-- 移动端搜索栏覆盖层 -->
    <div v-if="isMobileSearchOpen" class="absolute inset-0 bg-white dark:bg-[#121212] flex items-center px-4 z-50 md:hidden">
       <button @click="toggleMobileSearch" class="mr-2">
         <Icon icon="mdi:arrow-left" class="text-xl" />
       </button>
       <div class="relative flex-1">
         <Icon
            icon="mdi:magnify"
            class="absolute left-2 top-1/2 transform -translate-y-1/2 text-gray-500 text-xl"
          />
         <input
            v-model="searchText"
            type="text"
            class="w-full text-sm pl-8 pr-2 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary transition-all duration-300 search-bg"
            placeholder="想播放什么？"
            @keyup.enter="router.push('/library?query=' + searchText); toggleMobileSearch()"
          />
       </div>
    </div>
  </header>
</template>

<style scoped>
.search-bg {
  background-color: #e3e3e3;
}
</style>
