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
type TextConverter = (value: string) => string
const openCcConverter = ref<TextConverter>((value: string) => value)
const openCcLoaded = ref(false)

const ensureOpenCcConverter = async () => {
  if (openCcLoaded.value) {
    return
  }
  try {
    const OpenCC = await import('opencc-js')
    openCcConverter.value = OpenCC.Converter({ from: 'hk', to: 'cn' })
  } catch (error) {
    console.warn('opencc-js load failed, fallback to raw text', error)
    openCcConverter.value = (value: string) => value
  } finally {
    openCcLoaded.value = true
  }
}

const handleRecognitionSuccess = async (result: any) => {
  await ensureOpenCcConverter()
  recognizedTrack.value = result.track
  showRecognizerDropdown.value = true
  localSongMatch.value = null
  isCheckingLocal.value = true

  const originalTitle = result.track.title
  const originalArtist = result.track.subtitle

  // 转换为简体中文
  const simplifiedTitle = openCcConverter.value(originalTitle)
  const simplifiedArtist = openCcConverter.value(originalArtist)

  // 提取核心标题（去除括号内容），用于匹配不同版本（如 feat. 等）
  const cleanTitle = (str: string) => {
    return str.replace(/\s*[\(\[（].*?[\)\]）]/g, '').trim()
  }
  const coreTitle = cleanTitle(originalTitle)
  const simplifiedCoreTitle = openCcConverter.value(coreTitle)

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
  <header class="spotify-navbar">
    <div class="spotify-navbar-left">
      <!-- Mobile Menu Button -->
      <button class="spotify-nav-button spotify-mobile-menu-btn" @click="toggleMobileMenu">
        <Icon icon="ri:menu-line" class="text-xl" />
      </button>

      <!-- Logo -->
      <button class="spotify-logo-btn" @click="router.push('/')">
        <img src="\logo.svg?v=1" alt="logo" class="spotify-logo" />
        <span class="spotify-logo-text">Parachutes</span>
      </button>
    </div>

    <!-- Center - Search & Actions -->
    <div class="spotify-navbar-center">
      <div class="spotify-search-wrapper">
        <Icon icon="mdi:magnify" class="spotify-search-icon" />
        <input
          v-model="searchText"
          type="text"
          class="spotify-search-input"
          placeholder="想播放什么？"
          @keyup.enter="router.push('/search?query=' + searchText)"
        />
      </div>
      <SongRecognizer @success="handleRecognitionSuccess" />
      <button class="spotify-icon-btn" @click="toggleMode">
        <Icon class="text-xl" :class="theme.isDark ? '' : 'text-orange-500'" :icon="currentIcon" />
      </button>
    </div>

    <!-- Right - User -->
    <div class="spotify-navbar-right">
      <!-- Mobile Actions -->
      <div class="spotify-mobile-actions">
        <SongRecognizer @success="handleRecognitionSuccess" />
        <button class="spotify-icon-btn" @click="toggleMobileSearch">
          <Icon icon="mdi:magnify" class="text-xl" />
        </button>
        <button class="spotify-icon-btn" @click="toggleMode">
          <Icon class="text-xl" :class="theme.isDark ? '' : 'text-orange-500'" :icon="currentIcon" />
        </button>
        <button class="spotify-icon-btn" @click="toggleMobileRightAside">
          <Icon icon="mdi:music-box-outline" class="text-xl" />
        </button>
      </div>
      <Avatar />
    </div>

    <!-- Recognition Result Dropdown -->
    <div v-if="showRecognizerDropdown" class="spotify-dropdown-overlay">
      <div class="spotify-recognition-dropdown">
        <div class="spotify-recognition-header">
          <span class="spotify-recognition-title">识别结果</span>
          <button @click="closeRecognizerDropdown" class="spotify-icon-btn-sm">
            <Icon icon="mdi:close" />
          </button>
        </div>

        <div
          v-if="recognizedTrack"
          class="spotify-recognition-result"
          :class="{'spotify-recognition-disabled': !localSongMatch && !isCheckingLocal}"
          @click="playRecognizedSong"
        >
          <div class="spotify-recognition-cover">
            <img
              :src="localSongMatch?.coverUrl || recognizedTrack.photo_url || default_album"
              class="spotify-recognition-img"
              alt="cover"
            />
            <div v-if="localSongMatch" class="spotify-recognition-play">
              <Icon icon="mdi:play" class="text-white text-xl" />
            </div>
          </div>

          <div class="spotify-recognition-info">
            <h3 class="spotify-recognition-song">{{ recognizedTrack.title }}</h3>
            <p class="spotify-recognition-artist">{{ recognizedTrack.subtitle }}</p>
            <p v-if="isCheckingLocal" class="spotify-recognition-status checking">正在查找曲库...</p>
            <p v-else-if="!localSongMatch" class="spotify-recognition-status not-found">歌曲暂未收录</p>
            <p v-else class="spotify-recognition-status found">点击播放</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Mobile Search Overlay -->
    <div v-if="isMobileSearchOpen" class="spotify-mobile-search-overlay">
      <button @click="toggleMobileSearch" class="spotify-icon-btn">
        <Icon icon="mdi:arrow-left" class="text-xl" />
      </button>
      <div class="spotify-mobile-search-wrapper">
        <Icon icon="mdi:magnify" class="spotify-search-icon" />
        <input
          v-model="searchText"
          type="text"
          class="spotify-search-input"
          placeholder="想播放什么？"
          @keyup.enter="router.push('/search?query=' + searchText); toggleMobileSearch()"
        />
      </div>
    </div>
  </header>
</template>

<style scoped>
.spotify-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background-color: #000000;
  border-radius: 8px;
  position: relative;
  min-height: 56px;
  transition: background-color 200ms ease;
}

.spotify-navbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.spotify-mobile-menu-btn {
  display: none;
}

.spotify-logo-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
}

.spotify-logo {
  width: 32px;
  height: 32px;
  filter: brightness(0) invert(1);
  transition: filter 200ms ease;
}

.spotify-logo-btn:hover .spotify-logo {
  filter: brightness(0) invert(0.85);
}

.spotify-logo-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  display: none;
}

.spotify-navbar-center {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  max-width: 600px;
}

.spotify-search-wrapper {
  position: relative;
  width: 100%;
  max-width: 400px;
}

.spotify-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.spotify-search-input {
  width: 100%;
  padding: 10px 12px 10px 44px;
  background-color: var(--bg-input, #242424);
  border: none;
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
  line-height: 16px;
  transition: box-shadow 200ms ease, background-color 200ms ease;
}

.spotify-search-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-search-input:hover {
  box-shadow: 0 0 0 1px var(--border-hover, #535353);
}

.spotify-search-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--border-focus, #fff);
}

.spotify-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: color 200ms ease, transform 33ms ease;
}

.spotify-icon-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.spotify-icon-btn:active {
  transform: scale(1);
}

.spotify-navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.spotify-mobile-actions {
  display: none;
  align-items: center;
  gap: 4px;
}

.spotify-dropdown-overlay {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  width: 95%;
  max-width: 400px;
  z-index: 100;
}

.spotify-recognition-dropdown {
  background-color: var(--bg-dropdown, #282828);
  border-radius: 8px;
  box-shadow: 0 16px 24px rgba(0, 0, 0, 0.3), 0 6px 8px rgba(0, 0, 0, 0.2);
  padding: 8px;
}

.spotify-recognition-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  margin-bottom: 8px;
}

.spotify-recognition-title {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-icon-btn-sm {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: color 200ms ease;
}

.spotify-icon-btn-sm:hover {
  color: var(--text-base, #fff);
}

.spotify-recognition-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-recognition-result:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-recognition-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spotify-recognition-cover {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.spotify-recognition-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.spotify-recognition-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-recognition-cover:hover .spotify-recognition-play {
  opacity: 1;
}

.spotify-recognition-info {
  flex: 1;
  min-width: 0;
}

.spotify-recognition-song {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-recognition-artist {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-recognition-status {
  font-size: 0.75rem;
  margin-top: 4px;
}

.spotify-recognition-status.checking {
  color: #1db954;
}

.spotify-recognition-status.not-found {
  color: #f15e6c;
}

.spotify-recognition-status.found {
  color: #1db954;
}

.spotify-mobile-search-overlay {
  position: absolute;
  inset: 0;
  background-color: var(--bg-surface, #121212);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  z-index: 50;
}

.spotify-mobile-search-wrapper {
  position: relative;
  flex: 1;
}

/* Light Theme */
:root:not(.dark) .spotify-navbar {
  background-color: #ffffff;
  --bg-input: #f0f0f0;
  --bg-surface: #ffffff;
  --bg-dropdown: #ffffff;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-hover: #c0c0c0;
  --border-focus: #000000;
}

:root:not(.dark) .spotify-recognition-dropdown {
  box-shadow: 0 16px 24px rgba(0, 0, 0, 0.15), 0 6px 8px rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-logo {
  filter: brightness(0) invert(0);
}

:root:not(.dark) .spotify-logo-btn:hover .spotify-logo {
  filter: brightness(0) invert(0.2);
}

@media (min-width: 768px) {
  .spotify-logo-text {
    display: block;
  }
  
  .spotify-navbar-center {
    display: flex;
  }
  
  .spotify-mobile-actions {
    display: none;
  }
}

@media (max-width: 768px) {
  .spotify-navbar {
    border-radius: 0;
    padding: 8px 12px;
  }
  
  .spotify-mobile-menu-btn {
    display: flex;
  }
  
  .spotify-navbar-center {
    display: none;
  }
  
  .spotify-mobile-actions {
    display: flex;
  }
  
  .spotify-logo-text {
    display: none;
  }
}
</style>
