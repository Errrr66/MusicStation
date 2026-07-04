<script setup lang="ts">
import { getFavoriteSongs } from '@/api/system'
import type { Song } from '@/api/interface'
import coverImg from '@/assets/cover.png'
import { AudioStore } from '@/stores/modules/audio'
import { useRoute } from 'vue-router'
import { fixUrl, durationToMs } from '@/utils'
import { extractDominantColor } from '@/utils/imageUtils'

const route = useRoute()
const audui = AudioStore()
const { loadTrack, play } = useAudioPlayer()

const songs = ref<Song[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(30)
const dominantColor = ref('#1e3a5f')

const playlist = ref({
  name: '我喜欢的音乐',
  coverImgUrl: coverImg,
  trackCount: 0,
  tags: [],
})

interface PageResult {
  items: Song[]
  total: number
}

const getSongs = async () => {
  const res = await getFavoriteSongs({
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    songName: searchKeyword.value,
    artistName: '',
    album: '',
  })
  if (res.code === 0 && res.data) {
    const pageData = res.data as PageResult
    songs.value = pageData.items
    playlist.value.trackCount = pageData.total
    // 使用第一首歌（最新加入）的封面作为封面图
    if (pageData.items.length > 0) {
      const firstSong = pageData.items[0]
      playlist.value.coverImgUrl = fixUrl(firstSong?.coverUrl) || coverImg
      // 提取第一首歌的封面颜色
      if (firstSong && firstSong.coverUrl) {
        const color = await extractDominantColor(firstSong.coverUrl + '?param=50y50')
        dominantColor.value = color
      }
    }
  }
}

const handleSearch = () => {
  currentPage.value = 1 // 搜索时重置页码
  getSongs()
}

const handlePlayAll = async () => {
  audui.setAudioStore('trackList', [])
  if (!songs.value.length) return

  const result = songs.value.map((song) => ({
    id: song.songId.toString(),
    title: song.songName,
    artist: song.artistName,
    album: song.album,
    cover: song.coverUrl || coverImg,
    url: song.audioUrl,
    duration: durationToMs(song.duration),
    likeStatus: song.likeStatus,
  }))

  audui.setAudioStore('trackList', result)
  audui.setAudioStore('currentSongIndex', 0)
  await loadTrack()
  play()
}

onMounted(() => {
  getSongs()
})
</script>

<template>
  <div class="mr-like-page" :style="{ '--gradient-color': dominantColor }">
    <!-- Header -->
    <div class="mr-like-header">
      <div class="mr-like-cover">
        <img
          :alt="playlist.name"
          class="mr-like-cover-img"
          :src="fixUrl(playlist?.coverImgUrl) || coverImg"
        />
      </div>
      <div class="mr-like-info">
        <span class="mr-like-type">歌单</span>
        <h1 class="mr-like-title">{{ playlist?.name }}</h1>
        <div class="mr-like-meta">
          <span>{{ playlist?.trackCount }} 首歌曲</span>
        </div>
        <div class="mr-like-actions">
          <button @click="handlePlayAll" class="mr-play-btn">
            <Icon icon="mdi:play" class="text-xl" />
            <span>播放</span>
          </button>
          <div class="mr-search-wrapper">
            <Icon icon="mdi:magnify" class="mr-search-icon" />
            <input
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
              class="mr-search-input"
              placeholder="搜索"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Songs Table -->
    <div class="mr-like-content">
      <Table :data="songs" />
    </div>
  </div>
</template>

<style scoped>
.mr-like-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 300px);
}

.mr-like-header {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 24px;
  padding-top: 48px;
}

.mr-like-cover {
  width: 232px;
  height: 232px;
  flex-shrink: 0;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.mr-like-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.mr-like-info {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  flex: 1;
  min-width: 0;
}

.mr-like-type {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-base, #fff);
  margin-bottom: 8px;
}

.mr-like-title {
  font-size: 3rem;
  font-weight: 900;
  color: var(--text-base, #fff);
  line-height: 1.1;
  margin-bottom: 16px;
}

.mr-like-meta {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 24px;
}

.mr-like-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mr-play-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  padding: 0 32px;
  background-color: var(--mr-accent);
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
}

.mr-play-btn:hover {
  transform: scale(1.04);
  background-color: var(--mr-accent-hover);
}

.mr-play-btn:active {
  transform: scale(1);
}

.mr-search-wrapper {
  position: relative;
  width: 200px;
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

.mr-like-content {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
}

/* Light Theme */
:root:not(.dark) .mr-like-page {
  --bg-surface: #f0f0f0;
  --bg-input: #e8e8e8;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-hover: #c0c0c0;
  --border-focus: #000000;
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .mr-like-cover {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

/* Responsive */
@media (max-width: 768px) {
  .mr-like-page {
    padding-bottom: 140px;
  }
  
  .mr-like-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px 16px;
  }
  
  .mr-like-cover {
    width: 180px;
    height: 180px;
  }
  
  .mr-like-title {
    font-size: 1.75rem;
  }
  
  .mr-like-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .mr-search-wrapper {
    width: 100%;
    max-width: 300px;
  }
  
  .mr-like-content {
    padding: 0 16px 16px;
  }
}
</style>
