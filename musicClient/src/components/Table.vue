<script setup lang="ts">
import type { Song } from '@/api/interface'
import { AudioStore } from '@/stores/modules/audio'
import { defaultSong } from '@/mock'
import { trackModel } from '@/stores/interface'
import { formatMillisecondsToTime, fixUrl } from '@/utils'
import { collectSong, cancelCollectSong } from '@/api/system'
import { ElMessage } from 'element-plus'
import default_album from '@/assets/default_album.jpg'
import { UserStore } from '@/stores/modules/user'

const userStore = UserStore()
const audio = AudioStore()
const { loadTrack, play } = useAudioPlayer()

const props = defineProps({
  data: {
    type: Array as PropType<Song[]>,
    default: () => [],
  },
})

// 监听数据变化，更新当前页面的歌曲列表
watch(
  () => props.data,
  (newData) => {
    audio.setCurrentPageSongs(newData)
  },
  { immediate: true }
)

// 转换歌曲实体
const convertToTrackModel = (song: Song) => {
  // console.log('原始歌曲数据:', song)
  if (!song.songId || !song.songName || !song.audioUrl) {
    console.error('歌曲数据不完整:', song)
    return null
  }
  return {
    id: song.songId.toString(),
    title: song.songName,
    artist: song.artistName,
    album: song.album,
    cover: fixUrl(song.coverUrl) || default_album,
    url: fixUrl(song.audioUrl),
    duration: Number(song.duration) || 0,
    likeStatus: song.likeStatus || 0,
  }
}

// 播放音乐
const handlePlay = async (row: Song) => {
  // 先将所有表格数据转换为 trackModel
  const allTracks = props.data
    .map((song) => convertToTrackModel(song))
    .filter((track) => track !== null)

  // 找到当前选中歌曲的索引
  const selectedIndex = props.data.findIndex(
    (song) => song.songId === row.songId
  )

  // 清空现有播放列表并添加所有歌曲
  audio.setAudioStore('trackList', allTracks)
  // 设置当前播放索引为选中的歌曲
  audio.setAudioStore('currentSongIndex', selectedIndex)

  // 加载并播放选中的歌曲
  await loadTrack()
  play()
}

// 更新所有相同歌曲的喜欢状态
const updateAllSongLikeStatus = (songId: number, status: number) => {
  // 更新播放列表中的状态
  audio.trackList.forEach((track) => {
    if (Number(track.id) === songId) {
      track.likeStatus = status
    }
  })

  // 更新当前页面的歌曲列表状态
  if (audio.currentPageSongs) {
    audio.currentPageSongs.forEach((song) => {
      if (song.songId === songId) {
        song.likeStatus = status
      }
    })
  }

  // 更新原始数据
  if (props.data) {
    const song = props.data.find((song) => song.songId === songId)
    if (song) {
      song.likeStatus = status
    }
  }
}

// 处理喜欢/取消喜欢
const handleLike = async (row: Song, e: Event) => {
  e.stopPropagation() // 阻止事件冒泡

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    if (row.likeStatus === 0) {
      // 收藏歌曲
      const res = await collectSong(row.songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(row.songId, 1)
        ElMessage.success('已添加到我的喜欢')
      } else {
        ElMessage.error(res.message || '添加到我的喜欢失败')
      }
    } else {
      // 取消收藏
      const res = await cancelCollectSong(row.songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(row.songId, 0)
        ElMessage.success('已取消喜欢')
      } else {
        ElMessage.error(res.message || '取消喜欢失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const downLoadMusic = (row: Song, e: Event) => {
  e.stopPropagation() // 阻止事件冒泡
  const link = document.createElement('a')
  link.href = fixUrl(row.audioUrl)
  link.setAttribute('download', `${row.songName} - ${row.artistName}`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 判断是否是当前播放的歌曲
const isCurrentPlaying = (songId: number) => {
  const currentTrack = audio.trackList[audio.currentSongIndex]
  return currentTrack && Number(currentTrack.id) === songId
}
</script>

<template>
  <div class="spotify-song-table">
    <!-- Table Header -->
    <div class="spotify-table-header">
      <div class="spotify-table-header-content">
        <span class="spotify-table-num">#</span>
        <span class="spotify-table-title">标题</span>
        <span class="spotify-table-album">专辑</span>
        <span class="spotify-table-date">发布日期</span>
        <span class="spotify-table-duration">
          <Icon icon="mdi:clock-outline" />
        </span>
      </div>
    </div>

    <!-- Table Body -->
    <div class="spotify-table-body">
      <div
        v-for="(row, index) in data"
        :key="row.songId"
        class="spotify-table-row"
        :class="{ 'spotify-table-row-active': isCurrentPlaying(row.songId) }"
        @click="handlePlay(row)"
      >
        <div class="spotify-table-cell spotify-table-num">
          <span class="spotify-row-index">{{ index + 1 }}</span>
          <div class="spotify-row-play">
            <Icon icon="mdi:play" />
          </div>
        </div>

        <div class="spotify-table-cell spotify-table-title-cell">
          <div class="spotify-song-cover">
            <el-image
              :src="fixUrl(row.coverUrl) + '?param=50y50'"
              fit="cover"
              lazy
              :alt="row.songName"
              class="spotify-song-img"
            >
              <template #error>
                <div class="spotify-song-img-placeholder">
                  <Icon icon="mdi:music-note" />
                </div>
              </template>
            </el-image>
          </div>
          <div class="spotify-song-info">
            <div class="spotify-song-name" :title="row.songName">
              {{ row.songName }}
            </div>
            <div class="spotify-song-artist" :title="row.artistName">
              {{ row.artistName }}
            </div>
          </div>
        </div>

        <div class="spotify-table-cell spotify-table-album">
          <span :title="row.album">{{ row.album }}</span>
        </div>

        <div class="spotify-table-cell spotify-table-date">
          <span>{{ row.releaseTime || '-' }}</span>
        </div>

        <div class="spotify-table-cell spotify-table-duration-cell">
          <div class="spotify-row-actions">
            <button
              class="spotify-action-btn"
              @click="handleLike(row, $event)"
              :title="row.likeStatus === 1 ? '取消喜欢' : '喜欢'"
            >
              <Icon
                :icon="row.likeStatus === 1 ? 'mdi:cards-heart' : 'mdi:cards-heart-outline'"
                :class="{ 'spotify-like-active': row.likeStatus === 1 }"
              />
            </button>
            <span class="spotify-duration">
              {{ formatMillisecondsToTime(Number(row.duration) * 1000) }}
            </span>
            <button
              class="spotify-action-btn"
              @click.stop="downLoadMusic(row, $event)"
              title="下载"
            >
              <Icon icon="mdi:download-outline" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spotify-song-table {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

.spotify-table-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: var(--bg-surface, #121212);
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  padding: 0 16px;
}

.spotify-table-header-content {
  display: grid;
  grid-template-columns: 16px 4fr 2fr 1fr minmax(120px, 1fr);
  gap: 16px;
  align-items: center;
  height: 36px;
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--text-subdued, #b3b3b3);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.spotify-table-body {
  padding: 0 16px;
}

.spotify-table-row {
  display: grid;
  grid-template-columns: 16px 4fr 2fr 1fr minmax(120px, 1fr);
  gap: 16px;
  align-items: center;
  height: 56px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-table-row:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-table-row:hover .spotify-row-index {
  display: none;
}

.spotify-table-row:hover .spotify-row-play {
  display: flex;
}

.spotify-table-row:hover .spotify-row-actions {
  opacity: 1;
}

.spotify-table-row-active {
  background-color: var(--bg-active, rgba(255, 255, 255, 0.2));
}

.spotify-table-row-active .spotify-song-name {
  color: var(--text-accent, #1db954);
}

.spotify-table-row-active .spotify-row-index {
  color: var(--text-accent, #1db954);
}

.spotify-table-cell {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-table-num {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-row-index {
  display: flex;
}

.spotify-row-play {
  display: none;
  align-items: center;
  justify-content: center;
  color: var(--text-base, #fff);
}

.spotify-table-title-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.spotify-song-cover {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
}

.spotify-song-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-song-img-placeholder {
  width: 100%;
  height: 100%;
  background-color: var(--bg-elevated, #282828);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-song-info {
  flex: 1;
  min-width: 0;
}

.spotify-song-name {
  font-size: 0.9375rem;
  font-weight: 400;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-song-artist {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.spotify-song-artist:hover {
  color: var(--text-base, #fff);
  text-decoration: underline;
}

.spotify-table-album,
.spotify-table-date {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-table-album:hover,
.spotify-table-date:hover {
  color: var(--text-base, #fff);
  text-decoration: underline;
}

.spotify-table-duration-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.spotify-row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-action-btn {
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

.spotify-action-btn:hover {
  color: var(--text-base, #fff);
  transform: scale(1.1);
}

.spotify-like-active {
  color: var(--text-accent, #1db954) !important;
}

.spotify-duration {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  min-width: 40px;
  text-align: right;
}

/* Light Theme */
:root:not(.dark) .spotify-song-table {
  --bg-surface: #f0f0f0;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --bg-active: rgba(0, 0, 0, 0.12);
  --bg-elevated: #e8e8e8;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-accent: #1db954;
  --border-color: rgba(0, 0, 0, 0.1);
}

/* Responsive */
@media (max-width: 768px) {
  .spotify-table-header {
    display: none;
  }
  
  .spotify-table-body {
    padding: 0 8px;
  }
  
  .spotify-table-row {
    display: flex;
    align-items: center;
    gap: 12px;
    height: auto;
    padding: 10px 0;
    border-radius: 8px;
    position: relative;
  }
  
  .spotify-table-num {
    display: none;
  }
  
  .spotify-table-title-cell {
    flex: 1;
    min-width: 0;
    gap: 12px;
  }
  
  .spotify-song-cover {
    width: 48px;
    height: 48px;
  }
  
  .spotify-song-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  
  .spotify-song-name {
    font-size: 0.9375rem;
    display: block;
  }
  
  .spotify-song-artist {
    font-size: 0.8125rem;
    display: block;
  }
  
  .spotify-table-album,
  .spotify-table-date {
    display: none;
  }
  
  .spotify-table-duration-cell {
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
  }
  
  .spotify-row-actions {
    opacity: 1;
    gap: 4px;
  }
  
  .spotify-action-btn {
    width: 36px;
    height: 36px;
  }
  
  .spotify-duration {
    display: none;
  }
}
</style>
