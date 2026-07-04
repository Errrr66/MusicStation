<script setup lang="ts">
import { computed } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import DrawerMusic from '@/components/DrawerMusic/index.vue'
import AudioVisualizer from '@/components/AudioVisualizer.vue'
import { Icon } from '@iconify/vue'
import { UserStore } from '@/stores/modules/user'
import { AudioStore } from '@/stores/modules/audio'
import { collectSong, cancelCollectSong } from '@/api/system'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useLibraryStore } from '@/stores/modules/library'
import { useArtistStore } from '@/stores/modules/artist'
import { usePlaylistStore } from '@/stores/modules/playlist'
import { MenuStore } from '@/stores/modules/menu'
import defaultAlbum from '@/assets/default_album.jpg'
import { appendImageParam, fixUrl } from '@/utils'

const { currentTrack } = useAudioPlayer()
const userStore = UserStore()
const audioStore = AudioStore()
const route = useRoute()
const libraryStore = useLibraryStore()
const menuStore = MenuStore()
const showDrawerMusic = computed({
  get: () => menuStore.isSongDrawerOpen,
  set: (value: boolean) => menuStore.setSongDrawerOpen(value),
})

// 获取当前播放歌曲的喜欢状态
const currentSongLikeStatus = computed(() => {
  const currentTrack = audioStore.trackList[audioStore.currentSongIndex]
  return currentTrack?.likeStatus || 0
})

// 更新所有相同歌曲的喜欢状态
const updateAllSongLikeStatus = (songId: number, status: number) => {
  // 更新播放列表中的状态
  audioStore.trackList.forEach((track) => {
    if (Number(track.id) === songId) {
      track.likeStatus = status
    }
  })

  // 更新当前页面的歌曲列表状态
  if (audioStore.currentPageSongs) {
    audioStore.currentPageSongs.forEach((song) => {
      if ((song as any).songId === songId) {
        ;(song as any).likeStatus = status
      }
    })
  }

  // 更新曲库页面的数据
  if (route.path === '/library' && libraryStore.tableData?.items) {
    const song = libraryStore.tableData.items.find(
      (song) => song.songId === songId
    )
    if (song) {
      song.likeStatus = status
    }
  }

  // 更新歌手详情页的数据
  if (route.path.startsWith('/artist/')) {
    const artistStore = useArtistStore()
    if (artistStore.artistInfo?.songs) {
      const song = artistStore.artistInfo.songs.find(
        (song) => song.songId === songId
      )
      if (song) {
        song.likeStatus = status
      }
    }
  }

  // 更新歌单详情页的数据
  if (route.path.startsWith('/playlist/')) {
    const playlistStore = usePlaylistStore()
    if (playlistStore.songs) {
      const song = playlistStore.songs.find((song) => song.songId === songId)
      if (song) {
        song.likeStatus = status
      }
    }
  }
}

// 处理喜欢/取消喜欢
const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  const currentTrack = audioStore.trackList[audioStore.currentSongIndex]
  if (!currentTrack) return

  try {
    const songId = Number(currentTrack.id)
    if (currentSongLikeStatus.value === 0) {
      // 收藏歌曲
      const res = await collectSong(songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(songId, 1)
        ElMessage.success('已添加到我的喜欢')
      } else {
        ElMessage.error(res.message || '添加到我的喜欢失败')
      }
    } else {
      // 取消收藏
      const res = await cancelCollectSong(songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(songId, 0)
        ElMessage.success('已取消喜欢')
      } else {
        ElMessage.error(res.message || '取消喜欢失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}
</script>

<template>
  <div class="mr-playing-bar-left">
    <div
      class="mr-playing-bar-track"
      @click="showDrawerMusic = !showDrawerMusic"
    >
      <div class="mr-playing-bar-cover">
        <img
          :src="appendImageParam(currentTrack.cover, '90y90') || fixUrl(currentTrack.cover) || defaultAlbum"
          :alt="currentTrack.title"
          class="mr-playing-bar-img"
        />
      </div>
      <div class="mr-playing-bar-info">
        <div class="mr-playing-bar-title" :title="currentTrack.title">
          {{ currentTrack.title }}
        </div>
        <div class="mr-playing-bar-artist">
          {{ currentTrack.artist }}
        </div>
      </div>
    </div>

    <button
      class="mr-playing-bar-like"
      @click.stop="handleLike"
    >
      <Icon
        icon="mdi:cards-heart-outline"
        v-if="currentSongLikeStatus === 0"
        class="mr-like-icon"
      />
      <Icon icon="mdi:cards-heart" v-else class="mr-like-icon mr-like-active" />
    </button>

    <AudioVisualizer 
      :bar-count="18" 
      :min-height="2" 
      :max-height="40"
      :bar-width="4"
      :gap="4"
      class="left-visualizer"
    />

    <DrawerMusic v-model="showDrawerMusic" />
  </div>
</template>

<style scoped>
.mr-playing-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 180px;
  width: 30%;
}

.mr-playing-bar-track {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-playing-bar-track:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-playing-bar-cover {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  position: relative;
}

.mr-playing-bar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.mr-playing-bar-info {
  min-width: 0;
  flex: 1;
}

.mr-playing-bar-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mr-playing-bar-artist {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.mr-playing-bar-like {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 33ms ease;
}

.mr-playing-bar-like:hover {
  transform: scale(1.1);
}

.mr-playing-bar-like:active {
  transform: scale(1);
}

.mr-like-icon {
  font-size: 1.25rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-playing-bar-like:hover .mr-like-icon {
  color: var(--text-base, #fff);
}

.mr-like-active {
  color: var(--text-accent, var(--mr-accent));
}

.mr-playing-bar-like:hover .mr-like-active {
  color: var(--text-accent-hover, var(--mr-accent-hover));
}

.left-visualizer {
  height: 50px;
  flex-shrink: 0;
}

/* Light Theme */
:root:not(.dark) .mr-playing-bar-left {
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-accent: var(--mr-accent);
  --text-accent-hover: var(--mr-accent-hover);
}
</style>
