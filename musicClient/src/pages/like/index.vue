<script setup lang="ts">
import { getFavoriteSongs } from '@/api/system'
import type { Song } from '@/api/interface'
import coverImg from '@/assets/cover.png'
import { AudioStore } from '@/stores/modules/audio'
import { useRoute } from 'vue-router'
import { fixUrl } from '@/utils'

const route = useRoute()
const audui = AudioStore()
const { loadTrack, play } = useAudioPlayer()

const songs = ref<Song[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(1000)

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
    // 使用第一首歌的封面作为封面图
    if (pageData.items.length > 0) {
      playlist.value.coverImgUrl = fixUrl(pageData.items[0].coverUrl) || coverImg
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
    duration: parseFloat(song.duration) * 1000,
    likeStatus: song.likeStatus,
  }))

  audui.setAudioStore('trackList', result)
  audui.setAudioStore('currentSongIndex', 0)
  await loadTrack()
  play()
}

// 监听当前页面歌曲列表的变化
watch(
  () => audui.currentPageSongs,
  (newSongs) => {
    if (newSongs && newSongs.length > 0) {
      // 检查是否有歌曲的收藏状态变为0（取消收藏）
      const hasUnlikedSong = newSongs.some((song) => song.likeStatus === 0)
      if (hasUnlikedSong) {
        getSongs() // 重新获取收藏列表
      }
    }
  },
  { deep: true }
)

// 监听路由变化，每次进入页面时重新获取数据
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/like') {
      getSongs()
    }
  }
)

onMounted(() => {
  getSongs()
})
</script>

<template>
  <div class="spotify-like-page">
    <!-- Header -->
    <div class="spotify-like-header">
      <div class="spotify-like-cover">
        <img
          :alt="playlist.name"
          class="spotify-like-cover-img"
          :src="fixUrl(playlist?.coverImgUrl) || coverImg"
        />
      </div>
      <div class="spotify-like-info">
        <span class="spotify-like-type">歌单</span>
        <h1 class="spotify-like-title">{{ playlist?.name }}</h1>
        <div class="spotify-like-meta">
          <span>{{ playlist?.trackCount }} 首歌曲</span>
        </div>
        <div class="spotify-like-actions">
          <button @click="handlePlayAll" class="spotify-play-btn">
            <Icon icon="mdi:play" class="text-xl" />
            <span>播放</span>
          </button>
          <div class="spotify-search-wrapper">
            <Icon icon="mdi:magnify" class="spotify-search-icon" />
            <input
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
              class="spotify-search-input"
              placeholder="搜索"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Songs Table -->
    <div class="spotify-like-content">
      <Table :data="songs" />
    </div>
  </div>
</template>

<style scoped>
.spotify-like-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 300px);
}

.spotify-like-header {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 24px;
  padding-top: 48px;
}

.spotify-like-cover {
  width: 232px;
  height: 232px;
  flex-shrink: 0;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.spotify-like-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.spotify-like-info {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  flex: 1;
  min-width: 0;
}

.spotify-like-type {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-base, #fff);
  margin-bottom: 8px;
}

.spotify-like-title {
  font-size: 3rem;
  font-weight: 900;
  color: var(--text-base, #fff);
  line-height: 1.1;
  margin-bottom: 16px;
}

.spotify-like-meta {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 24px;
}

.spotify-like-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.spotify-play-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  padding: 0 32px;
  background-color: #1db954;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
}

.spotify-play-btn:hover {
  transform: scale(1.04);
  background-color: #1ed760;
}

.spotify-play-btn:active {
  transform: scale(1);
}

.spotify-search-wrapper {
  position: relative;
  width: 200px;
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

.spotify-like-content {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
}

/* Light Theme */
:root:not(.dark) .spotify-like-page {
  --bg-surface: #ffffff;
  --bg-input: #f0f0f0;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-hover: #c0c0c0;
  --border-focus: #000000;
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .spotify-like-cover {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

/* Responsive */
@media (max-width: 768px) {
  .spotify-like-page {
    padding-bottom: 80px;
  }
  
  .spotify-like-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px 16px;
  }
  
  .spotify-like-cover {
    width: 180px;
    height: 180px;
  }
  
  .spotify-like-title {
    font-size: 1.75rem;
  }
  
  .spotify-like-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .spotify-search-wrapper {
    width: 100%;
    max-width: 300px;
  }
  
  .spotify-like-content {
    padding: 0 16px 16px;
  }
}
</style>
