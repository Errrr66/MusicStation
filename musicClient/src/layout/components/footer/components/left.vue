<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import DrawerMusic from '@/components/DrawerMusic/index.vue'
import { Icon } from '@iconify/vue'
import { UserStore } from '@/stores/modules/user'
import { AudioStore } from '@/stores/modules/audio'
import { collectSong, cancelCollectSong } from '@/api/system'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { useLibraryStore } from '@/stores/modules/library'
import { useArtistStore } from '@/stores/modules/artist'
import { usePlaylistStore } from '@/stores/modules/playlist'

const { currentTrack } = useAudioPlayer()
const showDrawerMusic = ref(false)
const userStore = UserStore()
const audioStore = AudioStore()
const route = useRoute()
const libraryStore = useLibraryStore()

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
      if (song.songId === songId) {
        song.likeStatus = status
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
  <div class="flex items-center gap-4 h-full pl-4 w-96">
    <div
      class="flex items-center gap-2 cursor-pointer select-none hover:bg-hoverMenuBg transition-colors rounded-lg p-1"
      @click="showDrawerMusic = !showDrawerMusic"
    >
      <div class="w-16 h-16 flex-shrink-0">
        <img
          :src="currentTrack.cover + '?param=90y90'"
          :alt="currentTrack.title"
          class="w-full h-full object-cover rounded-lg shadow-md"
        />
      </div>
      <div class="min-w-0">
        <div
          class="text-base text-primary-foreground line-clamp-1 mb-0.5 mx-2 font-medium"
          :title="currentTrack.title"
        >
          {{ currentTrack.title }}
        </div>
        <div class="text-xs text-muted-foreground line-clamp-1 mt-0.5 mx-2">
          {{ currentTrack.artist }}
        </div>
      </div>
    </div>

    <button
      class="p-2 rounded-full hover:bg-hoverMenuBg transition text-primary-foreground/70 hover:text-primary-foreground"
      @click.stop="handleLike"
    >
      <Icon
        icon="mdi:cards-heart-outline"
        v-if="currentSongLikeStatus === 0"
        class="text-xl"
      />
      <Icon icon="mdi:cards-heart" v-else class="text-xl text-red-500" />
    </button>

    <DrawerMusic v-model="showDrawerMusic" />
  </div>
</template>
