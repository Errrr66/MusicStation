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
  // 获取轮播图数据
  fetchBannerData()
  // 初始化时获取推荐数据
  getRecommendedData()
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
</script>
<template>
  <div class="spotify-home">
    <!-- Banner -->
    <div class="spotify-home-banner">
      <el-carousel :interval="4000" type="card" height="200px" class="spotify-carousel">
        <el-carousel-item v-for="item in bannerList" :key="item.bannerId">
          <img
            :src="fixUrl(item.bannerUrl)"
            class="spotify-banner-img"
          />
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- Recommended Playlists -->
    <section class="spotify-home-section">
      <div class="spotify-section-header">
        <h2 class="spotify-section-title">今日为你推荐</h2>
        <button @click="router.push('/playlist')" class="spotify-section-link">
          <icon-hugeicons:more class="text-lg" />
        </button>
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
              <Icon icon="mdi:play" class="text-2xl" />
            </button>
          </div>
          <h3 class="spotify-playlist-title">{{ i.title }}</h3>
        </div>
      </div>
    </section>

    <!-- Recommended Songs -->
    <section class="spotify-home-section">
      <div class="spotify-section-header">
        <h2 class="spotify-section-title">相似推荐</h2>
        <button @click="handleRefreshSongs()" class="spotify-section-link">
          <icon-tabler:refresh class="text-lg" />
        </button>
      </div>
      <div class="spotify-song-list">
        <div
          v-for="item in recommendedSongList"
          :key="item.id"
          class="spotify-song-item"
          :class="{ 'spotify-song-item-active': isCurrentPlaying(item.id) }"
          @click.stop="handlePlaylclick(item)"
        >
          <div class="spotify-song-cover">
            <el-image
              :alt="item.name"
              class="spotify-song-img"
              :src="fixUrl(item.album.picUrl) + '?param=90y90'"
            />
            <div class="spotify-song-play">
              <icon-tabler:player-play-filled class="text-lg" />
            </div>
          </div>
          <div class="spotify-song-info">
            <h3 class="spotify-song-title">{{ item.name }}</h3>
            <p class="spotify-song-artist">
              {{ item.artists.map((item) => item.name).join(' ') }}
            </p>
          </div>
          <div class="spotify-song-duration">
            {{ formatTime(item.duration) }}
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.spotify-home {
  padding: 20px;
  overflow-y: auto;
  height: 100%;
}

.spotify-home-banner {
  margin-bottom: 24px;
}

:deep(.spotify-carousel .el-carousel__item) {
  --el-carousel-item-scale: 1.2;
}

.spotify-banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.spotify-home-section {
  margin-bottom: 32px;
}

.spotify-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.spotify-section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.spotify-section-link {
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
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-section-link:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
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

.spotify-song-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.spotify-song-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-song-item:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-song-item-active {
  background-color: var(--bg-active, rgba(255, 255, 255, 0.2));
}

.spotify-song-cover {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.spotify-song-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-song-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-song-item:hover .spotify-song-play {
  opacity: 1;
}

.spotify-song-info {
  min-width: 0;
  flex: 1;
}

.spotify-song-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-song-item-active .spotify-song-title {
  color: var(--text-accent, #1db954);
}

.spotify-song-artist {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.spotify-song-duration {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
  flex-shrink: 0;
}

/* Light Theme */
:root:not(.dark) .spotify-home {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-accent: #1db954;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --bg-active: rgba(0, 0, 0, 0.12);
  --card-bg: #f0f0f0;
  --card-hover: #e0e0e0;
}

:root:not(.dark) .spotify-playlist-cover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

@media (max-width: 768px) {
  .spotify-home {
    padding: 16px;
  }
  
  .spotify-playlist-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
  
  .spotify-playlist-card {
    padding: 8px;
  }
  
  .spotify-song-list {
    grid-template-columns: 1fr;
  }
}
</style>
