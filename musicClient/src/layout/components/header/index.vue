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

const toggleMobileMenu = () => {
  menuStore.setMobileMenuOpen(!menuStore.isMobileMenuOpen)
}

const toggleMobileRightAside = () => {
  menuStore.setRightAsideOpen(!menuStore.isRightAsideOpen)
}

const toggleMobileSearch = () => {
  menuStore.toggleMobileSearch()
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
  <header class="mr-navbar">
    <div class="mr-navbar-left">
      <!-- Mobile Menu Button -->
      <button class="mr-nav-button mr-mobile-menu-btn" @click="toggleMobileMenu">
        <Icon icon="mdi:menu" class="text-xl mr-menu-icon" />
      </button>

      <!-- Logo -->
      <button class="mr-logo-btn" @click="router.push('/')">
        <img src="\logo.svg?v=1" alt="logo" class="mr-logo" />
        <span class="mr-logo-text">Parachutes</span>
      </button>
    </div>

    <!-- Center - Search & Actions -->
    <div class="mr-navbar-center">
      <div class="mr-search-wrapper">
        <Icon icon="mdi:magnify" class="mr-search-icon" />
        <input
          v-model="searchText"
          type="text"
          class="mr-search-input"
          placeholder="想播放什么？"
          @keyup.enter="router.push('/search?query=' + searchText)"
        />
      </div>
      <SongRecognizer @success="handleRecognitionSuccess" />
      <button class="mr-icon-btn" @click="toggleMode">
        <Icon class="text-xl mr-theme-icon" :icon="currentIcon" />
      </button>
    </div>

    <!-- Right - User -->
    <div class="mr-navbar-right">
      <!-- Mobile Actions -->
      <div class="mr-mobile-actions">
        <SongRecognizer @success="handleRecognitionSuccess" />
        <button class="mr-icon-btn" @click="toggleMobileSearch">
          <Icon icon="mdi:magnify" class="text-xl" />
        </button>
        <button class="mr-icon-btn" @click="toggleMode">
          <Icon class="text-xl mr-theme-icon" :icon="currentIcon" />
        </button>
        <button class="mr-icon-btn mr-mobile-rightaside-btn" @click="toggleMobileRightAside">
          <Icon icon="mdi:music-box-outline" class="text-xl" />
        </button>
      </div>
      <Avatar />
    </div>

    <!-- Recognition Result Dropdown -->
    <div v-if="showRecognizerDropdown" class="mr-dropdown-overlay">
      <div class="mr-recognition-dropdown">
        <div class="mr-recognition-header">
          <span class="mr-recognition-title">识别结果</span>
          <button @click="closeRecognizerDropdown" class="mr-icon-btn-sm">
            <Icon icon="mdi:close" />
          </button>
        </div>

        <div
          v-if="recognizedTrack"
          class="mr-recognition-result"
          :class="{'mr-recognition-disabled': !localSongMatch && !isCheckingLocal}"
          @click="playRecognizedSong"
        >
          <div class="mr-recognition-cover">
            <img
              :src="localSongMatch?.coverUrl || recognizedTrack.photo_url || default_album"
              class="mr-recognition-img"
              alt="cover"
            />
            <div v-if="localSongMatch" class="mr-recognition-play">
              <Icon icon="mdi:play" class="text-white text-xl" />
            </div>
          </div>

          <div class="mr-recognition-info">
            <h3 class="mr-recognition-song">{{ recognizedTrack.title }}</h3>
            <p class="mr-recognition-artist">{{ recognizedTrack.subtitle }}</p>
            <p v-if="isCheckingLocal" class="mr-recognition-status checking">正在查找曲库...</p>
            <p v-else-if="!localSongMatch" class="mr-recognition-status not-found">歌曲暂未收录</p>
            <p v-else class="mr-recognition-status found">点击播放</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Mobile Search Overlay -->
    <div v-if="menuStore.isMobileSearchOpen" class="mr-mobile-search-overlay">
      <button @click="toggleMobileSearch" class="mr-icon-btn">
        <Icon icon="mdi:arrow-left" class="text-xl" />
      </button>
      <div class="mr-mobile-search-wrapper">
        <Icon icon="mdi:magnify" class="mr-search-icon" />
        <input
          v-model="searchText"
          type="text"
          class="mr-search-input"
          placeholder="想播放什么？"
          @keyup.enter="router.push('/search?query=' + searchText); toggleMobileSearch()"
        />
      </div>
    </div>
  </header>
</template>

<style scoped>
.mr-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background-color: var(--bg-surface, #000);
  border-radius: 8px;
  position: relative;
  min-height: 56px;
  transition: background-color 200ms ease;
}

.mr-navbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.mr-mobile-menu-btn {
  display: none;
}

.mr-menu-icon {
  color: var(--text-base, #fff);
}

.mr-logo-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
}

.mr-logo {
  width: 32px;
  height: 32px;
  filter: brightness(0) invert(1);
  transition: filter 200ms ease;
}

.mr-logo-btn:hover .mr-logo {
  filter: brightness(0) invert(0.85);
}

.mr-logo-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  display: none;
}

.mr-navbar-center {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  max-width: 600px;
}

.mr-search-wrapper {
  position: relative;
  width: 100%;
  max-width: 400px;
}

.mr-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.mr-search-input {
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

.mr-search-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.mr-search-input:hover {
  box-shadow: 0 0 0 1px var(--border-hover, #535353);
}

.mr-search-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--border-focus, #fff);
}

.mr-icon-btn {
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

.mr-icon-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.mr-icon-btn:active {
  transform: scale(1);
}

.mr-theme-icon {
  color: var(--text-base, #fff);
}

.mr-navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.mr-mobile-actions {
  display: none;
  align-items: center;
  gap: 4px;
}

.mr-dropdown-overlay {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  width: 95%;
  max-width: 400px;
  z-index: 100;
}

.mr-recognition-dropdown {
  background-color: var(--bg-dropdown, #282828);
  border-radius: 8px;
  box-shadow: 0 16px 24px rgba(0, 0, 0, 0.3), 0 6px 8px rgba(0, 0, 0, 0.2);
  padding: 8px;
}

.mr-recognition-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  margin-bottom: 8px;
}

.mr-recognition-title {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-subdued, #b3b3b3);
}

.mr-icon-btn-sm {
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

.mr-icon-btn-sm:hover {
  color: var(--text-base, #fff);
}

.mr-recognition-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-recognition-result:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-recognition-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mr-recognition-cover {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.mr-recognition-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.mr-recognition-play {
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

.mr-recognition-cover:hover .mr-recognition-play {
  opacity: 1;
}

.mr-recognition-info {
  flex: 1;
  min-width: 0;
}

.mr-recognition-song {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mr-recognition-artist {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mr-recognition-status {
  font-size: 0.75rem;
  margin-top: 4px;
}

.mr-recognition-status.checking {
  color: var(--mr-accent);
}

.mr-recognition-status.not-found {
  color: #f15e6c;
}

.mr-recognition-status.found {
  color: var(--mr-accent);
}

.mr-mobile-search-overlay {
  position: absolute;
  inset: 0;
  background-color: var(--bg-surface, #121212);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  z-index: 50;
}

.mr-mobile-search-wrapper {
  position: relative;
  flex: 1;
}

/* Light Theme */
:root:not(.dark) .mr-navbar {
  background-color: var(--bg-surface, #fff);
  --bg-input: #f0f0f0;
  --bg-surface: #ffffff;
  --bg-dropdown: #ffffff;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-hover: #c0c0c0;
  --border-focus: #000000;
}

:root:not(.dark) .mr-recognition-dropdown {
  box-shadow: 0 16px 24px rgba(0, 0, 0, 0.15), 0 6px 8px rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-logo {
  filter: brightness(0) invert(0);
}

:root:not(.dark) .mr-logo-btn:hover .mr-logo {
  filter: brightness(0) invert(0.2);
}

@media (min-width: 768px) {
  .mr-logo-text {
    display: block;
  }
  
  .mr-navbar-center {
    display: flex;
  }
  
  .mr-mobile-actions {
    display: none;
  }
}

@media (max-width: 768px) {
  .mr-navbar {
    border-radius: 0;
    padding: 8px 12px;
    gap: 8px;
    min-height: 52px;
  }

  .mr-navbar-left {
    gap: 6px;
  }

  .mr-mobile-menu-btn {
    display: flex;
    width: 36px;
    height: 36px;
    align-items: center;
    justify-content: center;
  }

  .mr-logo {
    width: 28px;
    height: 28px;
  }

  .mr-navbar-center {
    display: none;
  }

  .mr-mobile-actions {
    display: flex;
    align-items: center;
    gap: 0;
  }

  .mr-mobile-actions .mr-icon-btn,
  .mr-mobile-actions .mr-recognizer-btn {
    width: 32px;
    height: 32px;
    padding: 0;
    justify-content: center;
  }

  /* 精简移动端 header 右侧图标 */
  .mr-mobile-actions .mr-recognizer-btn,
  .mr-mobile-actions .mr-mobile-rightaside-btn {
    display: none;
  }

  .mr-mobile-actions .mr-recognizer-text {
    display: none;
  }

  .mr-avatar {
    --el-avatar-size: 32px;
  }

  .mr-logo-text {
    display: none;
  }
}
</style>
