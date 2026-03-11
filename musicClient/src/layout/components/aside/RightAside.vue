<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { AudioStore } from '@/stores/modules/audio'
import { UserStore } from '@/stores/modules/user'
import { MenuStore } from '@/stores/modules/menu'
import {
  collectSong,
  cancelCollectSong,
  getAllArtists,
  getArtistDetail,
} from '@/api/system'
import { ElMessage } from 'element-plus'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { useRouter } from 'vue-router'
import defaultSongCover from '@/assets/default_album.jpg'
import defaultUserAvatar from '@/assets/user.jpg'

const isCollapsed = ref(false)
const audioStore = AudioStore()
const userStore = UserStore()
const menuStore = MenuStore()
const router = useRouter()
const artistInfo = ref<any>(null)

// Access audio player capabilities
const { currentTrack, nextTrack: playNext } = useAudioPlayer()

// Computed: Current Song Like Status
const currentSongLikeStatus = computed(() => {
  return currentTrack.value?.likeStatus || 0
})

// Watch current artist to fetch info
watch(
  () => currentTrack.value.artist,
  async (newArtist) => {
    if (!newArtist || newArtist === '未知歌手') {
      artistInfo.value = null
      return
    }

    try {
      // 1. Search for artist by name
      const searchRes = await getAllArtists({
        artistName: newArtist,
        pageNum: 1,
        pageSize: 1,
      })
      if (searchRes.code === 0 && searchRes.data?.items?.length) {
        const artist = searchRes.data.items[0]
        // 2. Get detailed info including introduction
        const detailRes = await getArtistDetail(artist.artistId)
        if (detailRes.code === 0 && detailRes.data) {
          artistInfo.value = detailRes.data
        } else {
          // Fallback to search result if detail fails (though detail has intro)
          artistInfo.value = artist
        }
      } else {
        artistInfo.value = null
      }
    } catch (error) {
      console.error('Failed to fetch artist info:', error)
      artistInfo.value = null
    }
  },
  { immediate: true }
)

// Computed: Next Track Info
const nextTrackInfo = computed(() => {
  if (!audioStore.trackList.length) return null
  const nextIndex =
    (audioStore.currentSongIndex + 1) % audioStore.trackList.length
  return audioStore.trackList[nextIndex]
})

// Toggle Sidebar
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// Handle Like/Unlike
const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  const track = currentTrack.value
  if (!track || !track.id) return

  try {
    const songId = Number(track.id)
    if (currentSongLikeStatus.value === 0) {
      const res = await collectSong(songId)
      if (res.code === 0) {
        track.likeStatus = 1
        ElMessage.success('已添加到我的喜欢')
      } else {
        ElMessage.error(res.message || '添加到我的喜欢失败')
      }
    } else {
      const res = await cancelCollectSong(songId)
      if (res.code === 0) {
        track.likeStatus = 0
        ElMessage.success('已取消喜欢')
      } else {
        ElMessage.error(res.message || '取消喜欢失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// Play Next Song (via list click)
const playNextTrack = () => {
  playNext()
}

const openQueue = () => {
  menuStore.setPlaylistOpen(true)
}
</script>

<template>
  <aside
    class="sidebar-card hidden h-[calc(100%-1rem)] overflow-hidden md:block shadow-xl transition-all duration-300 m-2 rounded-2xl dark:bg-[#121212] bg-gray-100"
    :class="[isCollapsed ? 'w-[120px]' : 'w-96']"
  >
    <nav class="flex flex-col p-4 h-full box-border">
      <!-- Header: Collapse Button -->
      <div
        class="flex items-center mb-4 transition-all duration-300"
        :class="isCollapsed ? 'justify-center' : 'justify-start'"
      >
        <button
          class="p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
          @click="toggleCollapse"
          :title="isCollapsed ? '展开' : '收起'"
        >
          <Icon
            :icon="isCollapsed ? 'ri:menu-fold-line' : 'ri:menu-unfold-line'"
            class="text-xl text-primary-foreground"
          />
        </button>
        <span
          v-if="!isCollapsed"
          class="ml-2 font-bold text-lg dark:text-white"
        >
          Now Playing
        </span>
      </div>

      <!-- Main Content (Hidden when collapsed) -->
      <div
        v-if="!isCollapsed"
        class="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar space-y-6"
      >
        <!-- Section 1: Current Song Info -->
        <div class="w-full">
          <!-- Cover -->
          <div
            class="w-full aspect-square rounded-xl shadow-lg overflow-hidden mb-4 relative group"
          >
            <img
              :src="currentTrack.cover || defaultSongCover"
              alt="Cover"
              class="w-full h-full object-cover"
            />
          </div>

          <!-- Info & Like -->
          <div class="flex items-start justify-between">
            <div class="flex-1 min-w-0 mr-4">
              <div
                class="text-2xl font-bold text-primary-foreground truncate hover:underline cursor-pointer"
                :title="currentTrack.title"
              >
                {{ currentTrack.title || '未知歌曲' }}
              </div>
              <div
                class="text-base text-gray-500 dark:text-gray-400 truncate hover:underline cursor-pointer mt-1"
                :title="currentTrack.artist"
              >
                {{ currentTrack.artist || '未知艺人' }}
              </div>
            </div>
            <button
              class="p-2 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors flex-shrink-0"
              @click="handleLike"
            >
              <Icon
                icon="mdi:cards-heart-outline"
                v-if="currentSongLikeStatus === 0"
                class="text-2xl text-primary-foreground"
              />
              <Icon
                icon="mdi:cards-heart"
                v-else
                class="text-2xl text-red-500"
              />
            </button>
          </div>
        </div>

        <!-- Section 2: About Artist -->
        <div class="p-4 bg-white/50 dark:bg-[#1e1e1e] rounded-xl shadow-sm">
          <h3 class="font-bold text-base mb-3 text-primary-foreground">
            关于艺人
          </h3>
          <div class="flex flex-col gap-3" v-if="artistInfo">
            <div
              class="flex items-center gap-3 cursor-pointer hover:bg-black/5 dark:hover:bg-white/5 p-2 rounded-lg transition-colors"
              @click="router.push(`/artist/${artistInfo.artistId}`)"
            >
              <img
                :src="artistInfo.avatar || defaultUserAvatar"
                alt="Artist Avatar"
                class="w-12 h-12 rounded-full object-cover shadow-sm"
              />
              <div class="font-semibold text-primary-foreground">
                {{ artistInfo.artistName || currentTrack.artist }}
              </div>
            </div>
            <div
              class="text-sm text-gray-500 dark:text-gray-400 line-clamp-3 leading-relaxed px-2"
            >
              {{
                artistInfo.introduction
                  ? artistInfo.introduction.replace(/<[^>]+>/g, '')
                  : '这位艺人暂无详细介绍。快去听听TA的其他作品吧！'
              }}
            </div>
          </div>

          <!-- Fallback if no artist info found but we have a name -->
          <div
            class="flex flex-col gap-3"
            v-else-if="
              currentTrack.artist && currentTrack.artist !== '未知歌手'
            "
          >
            <div class="flex items-center gap-3 p-2">
              <img
                :src="defaultUserAvatar"
                alt="Default Avatar"
                class="w-12 h-12 rounded-full object-cover shadow-sm"
              />
              <div class="font-semibold text-primary-foreground">
                {{ currentTrack.artist }}
              </div>
            </div>
            <div class="text-sm text-gray-500 dark:text-gray-400 px-2">
              暂无详细介绍
            </div>
          </div>

          <div v-else class="text-sm text-gray-500 dark:text-gray-400 px-2">
            暂无艺人信息
          </div>
        </div>

        <!-- Section 3: Next in Queue -->
        <div
          class="p-4 bg-white/50 dark:bg-[#1e1e1e] rounded-xl shadow-sm"
          v-if="nextTrackInfo"
        >
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-bold text-base text-primary-foreground">
              队列中的下一首
            </h3>
            <span
              class="text-xs text-gray-400 cursor-pointer hover:text-primary"
              @click="openQueue"
            >
              打开队列
            </span>
          </div>

          <div
            class="flex items-center gap-3 p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/5 cursor-pointer transition-colors group"
            @click="playNextTrack"
          >
            <div class="relative w-12 h-12 flex-shrink-0">
              <img
                :src="nextTrackInfo.cover || defaultSongCover"
                alt="Next Song Cover"
                class="w-full h-full rounded-md object-cover"
              />
              <div
                class="absolute inset-0 flex items-center justify-center bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity rounded-md"
              >
                <Icon icon="solar:play-bold" class="text-white" />
              </div>
            </div>
            <div class="min-w-0">
              <div class="font-medium text-primary-foreground truncate">
                {{ nextTrackInfo.title }}
              </div>
              <div
                class="text-xs text-gray-500 dark:text-gray-400 truncate"
              >
                {{ nextTrackInfo.artist }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Collapsed View (Icons only?) -->
      <div v-else class="flex flex-col items-center mt-4">
        <!-- Maybe show mini cover or nothing -->
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.5);
  border-radius: 20px;
}
</style>

