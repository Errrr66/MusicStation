<script setup lang="ts">
import {
  getRecommendedPlaylists,
  getRecommendedSongs,
  getBanner,
} from '@/api/system'
import coverImg from '@/assets/cover.png'
import { formatTime, replaceUrlParams, fixUrl } from '@/utils'
import { ElNotification } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
const router = useRouter()
const audio = AudioStore()
const user = UserStore()

const { loadTrack, play } = useAudioPlayer()

const bannerList = ref<{ bannerId: number; bannerUrl: string }[]>([])

const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const bannerChunks = computed(() => {
  const chunks = []
  const itemsPerSlide = isMobile.value ? 1 : 2
  for (let i = 0; i < bannerList.value.length; i += itemsPerSlide) {
    chunks.push(bannerList.value.slice(i, i + itemsPerSlide))
  }
  return chunks
})

const bannerTitles = ['今日热门', '新歌首发', '精选推荐', '热门榜单', '流行趋势', '独家放送']

const getBannerTitle = (bannerId: number) => {
  return bannerTitles[bannerId % bannerTitles.length] || '推荐内容'
}

// 推荐歌单
const recommendedPlaylist = ref([])
// 推荐歌曲
const recommendedSongList = ref([])

// 监听用户登录状态
watch(
  () => user.isLoggedIn,
  (newVal) => {
    if (newVal) {
      // 用户登录后重新获取推荐数据
      getRecommendedData()
    }
  }
)

// 获取轮播图数据
const fetchBannerData = async () => {
  try {
    const result = await getBanner()
    if (result.code === 0 && Array.isArray(result.data)) {
      bannerList.value = result.data
    } else {
      ElNotification({
        type: 'error',
        message: '获取轮播图失败',
        duration: 2000,
      })
    }
  } catch (error) {
    console.error('Error fetching banner data:', error)
    ElNotification({
      type: 'error',
      message: '获取轮播图时发生错误',
      duration: 2000,
    })
  }
}

// 获取推荐数据
const getRecommendedData = async () => {
  // 获取推荐歌单
  const result = await getRecommendedPlaylists()
  if (result.code === 0 && Array.isArray(result.data)) {
    recommendedPlaylist.value = result.data.map((item) => ({
      playlistId: item.playlistId,
      title: item.title,
      coverUrl: item.coverUrl ?? coverImg,
    }))
  } else {
    ElNotification({
      type: 'error',
      message: '获取推荐歌单失败',
      duration: 2000,
    })
  }

  // 获取推荐歌曲
  handleRefreshSongs()
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  fetchBannerData()
  getRecommendedData()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const handleRefreshSongs = async () => {
  const result = await getRecommendedSongs()
  if (result.code === 0 && Array.isArray(result.data)) {
    recommendedSongList.value = result.data.map((item) => ({
      id: item.songId,
      name: item.songName,
      artists: [
        {
          name: item.artistName,
        },
      ],
      album: {
        name: item.album,
        picUrl: item.coverUrl,
      },
      duration: item.duration,
      audioUrl: item.audioUrl,
      likeStatus: item.likeStatus || 0, // 从服务端获取收藏状态
    }))
  } else {
    ElNotification({
      type: 'error',
      message: '获取推荐歌曲失败',
      duration: 2000,
    })
  }
}

// 转换歌曲实体
const convertToTrackModel = (song: any) => {
  return {
    id: song.id.toString(),
    title: song.name,
    artist: song.artists.map((artist: any) => artist.name).join(', '),
    album: song.album.name,
    cover: song.album.picUrl || '',
    url: song.audioUrl,
    duration: song.duration,
    likeStatus: song.likeStatus || 0, // 保持收藏状态
  }
}

const handlePlaylclick = async (row: any) => {
  // 将所有推荐歌曲转换为 trackModel
  const allTracks = recommendedSongList.value
    .map((song) => convertToTrackModel(song))
    .filter((track) => track !== null)

  // 找到当前选中歌曲的索引
  const selectedIndex = recommendedSongList.value.findIndex(
    (song) => song.id === row.id
  )

  // 清空现有播放列表并添加所有歌曲
  audio.setAudioStore('trackList', allTracks)
  // 设置当前播放索引为选中的歌曲
  audio.setAudioStore('currentSongIndex', selectedIndex)

  // 播放
  await loadTrack()
  play()
}

// 判断是否是当前播放的歌曲
const isCurrentPlaying = (songId: number) => {
  const currentTrack = audio.trackList[audio.currentSongIndex]
  return currentTrack && Number(currentTrack.id) === songId
}

const dominantColor = ref('#1e3a5f')
const scrollProgress = ref(0)
const showScrollbar = ref(false)
const homeRef = ref<HTMLElement | null>(null)
const activeTab = ref<'all' | 'playlist' | 'music'>('all')
let scrollTimeout: ReturnType<typeof setTimeout> | null = null

const hexToRgb = (hex: string): string => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (result) {
    return `${parseInt(result[1], 16)}, ${parseInt(result[2], 16)}, ${parseInt(result[3], 16)}`
  }
  return '30, 58, 95'
}

const extractDominantColor = (imageUrl: string): Promise<string> => {
  return new Promise((resolve) => {
    const img = new Image()
    img.crossOrigin = 'Anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        resolve('#1e3a5f')
        return
      }
      canvas.width = 50
      canvas.height = 50
      ctx.drawImage(img, 0, 0, 50, 50)
      const imageData = ctx.getImageData(0, 0, 50, 50).data
      let r = 0, g = 0, b = 0, count = 0
      for (let i = 0; i < imageData.length; i += 4) {
        r += imageData[i]
        g += imageData[i + 1]
        b += imageData[i + 2]
        count++
      }
      r = Math.floor(r / count)
      g = Math.floor(g / count)
      b = Math.floor(b / count)
      const hex = '#' + [r, g, b].map(x => x.toString(16).padStart(2, '0')).join('')
      resolve(hex)
    }
    img.onerror = () => resolve('#1e3a5f')
    img.src = imageUrl
  })
}

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  const scrollTop = target.scrollTop
  const maxScroll = 400
  scrollProgress.value = Math.min(scrollTop / maxScroll, 1)
  
  showScrollbar.value = true
  if (scrollTimeout) {
    clearTimeout(scrollTimeout)
  }
  scrollTimeout = setTimeout(() => {
    showScrollbar.value = false
  }, 1500)
}

watch(
  () => audio.trackList[audio.currentSongIndex]?.cover,
  async (newCover) => {
    if (newCover) {
      const color = await extractDominantColor(newCover + '?param=50y50')
      dominantColor.value = color
    }
  },
  { immediate: true }
)
</script>
<template>
  <div class="spotify-home-wrapper" :style="{ '--gradient-color': dominantColor }">
    <div 
      class="spotify-gradient-bg"
      :style="{ opacity: 1 - scrollProgress }"
    ></div>
    <div 
      class="spotify-home-header"
      :style="{ 
        backgroundColor: `rgba(${hexToRgb(dominantColor)}, ${scrollProgress})`,
        backdropFilter: scrollProgress > 0.5 ? 'blur(10px)' : 'none'
      }"
    >
      <div class="spotify-header-tabs">
        <button 
          class="spotify-tab-btn" 
          :class="{ active: activeTab === 'all' }"
          @click="activeTab = 'all'"
        >全部</button>
        <button 
          class="spotify-tab-btn" 
          :class="{ active: activeTab === 'playlist' }"
          @click="activeTab = 'playlist'"
        >歌单</button>
        <button 
          class="spotify-tab-btn" 
          :class="{ active: activeTab === 'music' }"
          @click="activeTab = 'music'"
        >音乐</button>
      </div>
    </div>
    <div 
      ref="homeRef"
      class="spotify-home"
      :class="{ 'show-scrollbar': showScrollbar }"
      @scroll="handleScroll"
    >
    <div class="spotify-home-content">
    
    <!-- Banner -->
    <div class="spotify-home-banner" v-show="activeTab === 'all'">
      <h1 class="spotify-page-title">新发现</h1>
      <el-carousel :interval="4000" height="320px" class="spotify-carousel" arrow="hover">
        <el-carousel-item v-for="(chunk, index) in bannerChunks" :key="index">
          <div class="spotify-banner-group">
            <div class="spotify-banner-item" v-for="item in chunk" :key="item.bannerId">
              <h3 class="spotify-banner-title">{{ getBannerTitle(item.bannerId) }}</h3>
              <img :src="fixUrl(item.bannerUrl)" class="spotify-banner-img" />
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- Recommended Playlists -->
    <section class="spotify-home-section" v-show="activeTab === 'all' || activeTab === 'playlist'">
      <div class="spotify-section-header">
        <a @click="router.push('/playlist')" class="spotify-section-link">
          <span>歌单已更新</span>
          <svg class="spotify-section-chevron" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
            <path d="M19.817 61.863c1.48 0 2.672-.515 3.702-1.546l24.243-23.63c1.352-1.385 1.996-2.737 2.028-4.443 0-1.674-.644-3.09-2.028-4.443L23.519 4.138c-1.03-.998-2.253-1.513-3.702-1.513-2.994 0-5.409 2.382-5.409 5.344 0 1.481.612 2.833 1.739 3.96l20.99 20.347-20.99 20.283c-1.127 1.126-1.739 2.478-1.739 3.96 0 2.93 2.415 5.344 5.409 5.344Z"></path>
          </svg>
        </a>
      </div>
      <div class="spotify-playlist-grid">
        <div
          class="spotify-playlist-card"
          v-for="i in recommendedPlaylist.slice(0, 6)"
          :key="i.playlistId"
          @click="router.push(`/playlist/${i.playlistId}`)"
        >
          <div class="spotify-playlist-cover">
            <img
              :alt="i.title"
              loading="lazy"
              class="spotify-playlist-img"
              :src="replaceUrlParams(fixUrl(i.coverUrl) ?? coverImg, 'param=350y350')"
            />
            <button class="spotify-playlist-play-btn">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
                <path fill="currentColor" d="M8 5v14l11-7z"/>
              </svg>
            </button>
          </div>
          <h3 class="spotify-playlist-title">{{ i.title }}</h3>
        </div>
      </div>
    </section>

    <!-- Recommended Songs -->
    <section class="spotify-home-section" v-show="activeTab === 'all' || activeTab === 'music'">
      <div class="spotify-section-header">
        <a @click="handleRefreshSongs()" class="spotify-section-link">
          <span>正在流行中</span>
          <svg class="spotify-section-chevron" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
            <path d="M19.817 61.863c1.48 0 2.672-.515 3.702-1.546l24.243-23.63c1.352-1.385 1.996-2.737 2.028-4.443 0-1.674-.644-3.09-2.028-4.443L23.519 4.138c-1.03-.998-2.253-1.513-3.702-1.513-2.994 0-5.409 2.382-5.409 5.344 0 1.481.612 2.833 1.739 3.96l20.99 20.347-20.99 20.283c-1.127 1.126-1.739 2.478-1.739 3.96 0 2.93 2.415 5.344 5.409 5.344Z"></path>
          </svg>
        </a>
      </div>
      <div class="spotify-song-table">
        <div class="spotify-song-header-row">
          <span class="spotify-song-header-num">#</span>
          <span class="spotify-song-header-title">标题</span>
          <span class="spotify-song-header-album">专辑</span>
          <span class="spotify-song-header-duration">
            <Icon icon="mdi:clock-outline" />
          </span>
        </div>
        <div
          v-for="(item, index) in recommendedSongList"
          :key="item.id"
          class="spotify-song-row"
          :class="{ 'spotify-song-row-active': isCurrentPlaying(item.id) }"
          @click.stop="handlePlaylclick(item)"
        >
          <div class="spotify-song-num">
            <span v-if="!isCurrentPlaying(item.id)" class="spotify-song-index">{{ index + 1 }}</span>
            <Icon v-else icon="mdi:volume-high" class="spotify-song-playing-icon" />
            <button class="spotify-song-row-play">
              <Icon icon="mdi:play" />
            </button>
          </div>
          <div class="spotify-song-main">
            <el-image
              :alt="item.name"
              class="spotify-song-row-img"
              :src="fixUrl(item.album.picUrl) + '?param=50y50'"
            />
            <div class="spotify-song-row-info">
              <span class="spotify-song-row-title">{{ item.name }}</span>
              <span class="spotify-song-row-artist">
                {{ item.artists.map((a) => a.name).join(', ') }}
              </span>
            </div>
          </div>
          <div class="spotify-song-row-album">{{ item.album.name }}</div>
          <div class="spotify-song-row-duration">{{ formatTime(item.duration) }}</div>
        </div>
      </div>
    </section>
    </div>
  </div>
</div>
</template>

<style scoped>
.spotify-home-wrapper {
  position: relative;
  height: 100%;
  background: var(--bg-surface, #121212);
}

.spotify-gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400px;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 100%);
  pointer-events: none;
  z-index: 1;
}

.spotify-home {
  position: relative;
  overflow-y: auto;
  height: 100%;
  z-index: 2;
}

.spotify-home::-webkit-scrollbar {
  width: 12px;
}

.spotify-home::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-home::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 6px;
  border: 3px solid transparent;
  background-clip: content-box;
  transition: background 300ms ease;
}

.spotify-home.show-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
}

.spotify-home::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.spotify-home-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  z-index: 100;
  transition: backdrop-filter 200ms ease;
}

.spotify-header-tabs {
  display: flex;
  gap: 8px;
}

.spotify-tab-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-tab-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.spotify-tab-btn.active {
  background: #ffffff;
  color: #000000;
}

.spotify-home-content {
  padding: 20px;
  padding-top: 64px;
}

.spotify-page-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-highlight, #f5f5f7);
  margin-bottom: 24px;
}

.spotify-home-banner {
  margin-bottom: 24px;
  position: relative;
}

.spotify-home-banner .spotify-page-title {
  margin-bottom: 16px;
}

.spotify-carousel :deep(.el-carousel__container) {
  height: 320px;
}

.spotify-carousel :deep(.el-carousel__item) {
  border-radius: 8px;
  overflow: hidden;
}

.spotify-carousel :deep(.el-carousel__arrow) {
  width: 32px;
  height: 32px;
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  transition: all 200ms ease;
}

.spotify-carousel :deep(.el-carousel__arrow:hover) {
  background-color: rgba(0, 0, 0, 0.9);
  transform: scale(1.1);
}

.spotify-carousel :deep(.el-carousel__arrow--left) {
  left: 16px;
}

.spotify-carousel :deep(.el-carousel__arrow--right) {
  right: 16px;
}

.spotify-carousel :deep(.el-carousel__indicators) {
  bottom: 12px;
}

.spotify-carousel :deep(.el-carousel__indicator--horizontal .el-carousel__button) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.5);
  transition: all 200ms ease;
}

.spotify-carousel :deep(.el-carousel__indicator--horizontal.is-active .el-carousel__button) {
  background-color: #fff;
  width: 8px;
}

.spotify-banner-group {
  display: flex;
  gap: 16px;
  height: 100%;
  padding: 0 4px;
}

.spotify-banner-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.spotify-banner-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-highlight, #f5f5f7);
  margin: 0;
  padding-left: 4px;
  flex-shrink: 0;
}

.spotify-banner-img {
  width: 100%;
  height: 240px;
  object-fit: cover;
  border-radius: 8px;
}

.spotify-home-section {
  margin-bottom: 32px;
}

.spotify-section-header {
  margin-bottom: 16px;
}

.spotify-section-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-highlight, #f5f5f7);
  text-decoration: none;
  cursor: pointer;
  transition: color 200ms ease;
}

.spotify-section-link span {
  color: inherit;
}

.spotify-section-link:hover {
  color: var(--text-base, #fff);
}

.spotify-section-chevron {
  width: 16px;
  height: 16px;
  fill: currentColor;
  transition: transform 200ms ease;
}

.spotify-section-link:hover .spotify-section-chevron {
  transform: translateX(4px);
}

.spotify-playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 24px;
}

.spotify-playlist-card {
  background-color: var(--card-bg, #181818);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-playlist-card:hover {
  background-color: var(--card-hover, #282828);
}

.spotify-playlist-card:hover .spotify-playlist-play-btn {
  opacity: 1;
  transform: translateY(0);
}

.spotify-playlist-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.spotify-playlist-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-playlist-play-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 48px;
  height: 48px;
  background-color: #1db954;
  border: none;
  border-radius: 50%;
  color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: translateY(8px);
  transition: all 200ms ease;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
}

.spotify-playlist-play-btn:hover {
  transform: translateY(0) scale(1.04);
  background-color: #1ed760;
}

.spotify-playlist-play-btn svg {
  color: #000;
  fill: #000;
}

.spotify-playlist-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.spotify-song-table {
  width: 100%;
}

.spotify-song-header-row {
  display: grid;
  grid-template-columns: 40px 1fr 1fr 80px;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  margin-bottom: 8px;
}

.spotify-song-header-num,
.spotify-song-header-duration {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  font-weight: 400;
}

.spotify-song-header-title {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  font-weight: 400;
}

.spotify-song-header-album {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  font-weight: 400;
}

.spotify-song-row {
  display: grid;
  grid-template-columns: 40px 1fr 1fr 80px;
  align-items: center;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-song-row:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-song-row-active {
  background-color: var(--bg-active, rgba(255, 255, 255, 0.2));
}

.spotify-song-row-active:hover {
  background-color: var(--bg-active, rgba(255, 255, 255, 0.25));
}

.spotify-song-num {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
}

.spotify-song-index {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-song-playing-icon {
  font-size: 1rem;
  color: var(--text-accent, #1db954);
}

.spotify-song-row-play {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: transparent;
  border: none;
  color: var(--text-base, #fff);
  cursor: pointer;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-song-row:hover .spotify-song-row-play {
  opacity: 1;
}

.spotify-song-row:hover .spotify-song-index,
.spotify-song-row:hover .spotify-song-playing-icon {
  opacity: 0;
}

.spotify-song-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.spotify-song-row-img {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.spotify-song-row-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.spotify-song-row-title {
  font-size: 0.9375rem;
  font-weight: 400;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-song-row-active .spotify-song-row-title {
  color: var(--text-accent, #1db954);
}

.spotify-song-row-artist {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-song-row-artist:hover {
  color: var(--text-base, #fff);
  text-decoration: underline;
}

.spotify-song-row-album {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-song-row-album:hover {
  color: var(--text-base, #fff);
  text-decoration: underline;
}

.spotify-song-row-duration {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  text-align: right;
}

/* Light Theme */
:root:not(.dark) .spotify-home {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-highlight: #1d1d1f;
  --text-accent: #1db954;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --bg-active: rgba(0, 0, 0, 0.12);
  --bg-surface: #ffffff;
  --gradient-color: #e8f4f8;
  --card-bg: #f0f0f0;
  --card-hover: #e0e0e0;
  --border-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-playlist-cover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-home-header {
  background-color: transparent;
}

:root:not(.dark) .spotify-tab-btn {
  background: rgba(0, 0, 0, 0.1);
  color: #000000;
}

:root:not(.dark) .spotify-tab-btn:hover {
  background: rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .spotify-tab-btn.active {
  background: #000000;
  color: #ffffff;
}

:root:not(.dark) .spotify-home.show-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .spotify-home::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.4);
}

@media (max-width: 768px) {
  .spotify-home {
    padding: 12px;
    padding-bottom: 80px;
  }
  
  .spotify-page-title {
    font-size: 1.375rem;
    margin-bottom: 16px;
  }
  
  .spotify-home-banner {
    margin-bottom: 16px;
  }
  
  .spotify-carousel :deep(.el-carousel__container) {
    height: 200px;
  }
  
  .spotify-carousel :deep(.el-carousel__item) {
    height: 200px;
  }
  
  .spotify-carousel :deep(.el-carousel__arrow) {
    width: 28px;
    height: 28px;
  }
  
  .spotify-carousel :deep(.el-carousel__arrow--left) {
    left: 8px;
  }
  
  .spotify-carousel :deep(.el-carousel__arrow--right) {
    right: 8px;
  }
  
  .spotify-banner-group {
    gap: 8px;
    padding: 0;
  }
  
  .spotify-banner-item {
    width: 100%;
  }
  
  .spotify-banner-title {
    font-size: 0.75rem;
  }
  
  .spotify-banner-img {
    height: 160px;
  }
  
  .spotify-section-link {
    font-size: 0.875rem;
  }
  
  .spotify-section-chevron {
    width: 10px;
    height: 10px;
  }
  
  .spotify-playlist-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .spotify-playlist-card {
    padding: 8px;
  }
  
  .spotify-playlist-cover {
    margin-bottom: 8px;
  }
  
  .spotify-playlist-play-btn {
    width: 36px;
    height: 36px;
    opacity: 1;
    transform: translateY(0);
  }
  
  .spotify-playlist-title {
    font-size: 0.8125rem;
  }
  
  .spotify-song-header-row {
    display: none;
  }
  
  .spotify-song-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 4px;
    border-radius: 8px;
  }
  
  .spotify-song-num {
    display: none;
  }
  
  .spotify-song-main {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    min-width: 0;
  }
  
  .spotify-song-row-img {
    width: 48px;
    height: 48px;
    flex-shrink: 0;
  }
  
  .spotify-song-row-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }
  
  .spotify-song-row-title {
    font-size: 0.9375rem;
    display: block;
  }
  
  .spotify-song-row-artist {
    font-size: 0.75rem;
    display: block;
  }
  
  .spotify-song-row-album {
    display: none;
  }
  
  .spotify-song-row-duration {
    display: none;
  }
}
</style>
