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
import { fixUrl } from '@/utils'

const props = defineProps<{
  isMobile?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const isCollapsed = ref(false)
const audioStore = AudioStore()
const userStore = UserStore()
const menuStore = MenuStore()
const router = useRouter()
const artistInfo = ref<any>(null)

const { currentTrack, nextTrack: playNext } = useAudioPlayer()

const currentSongLikeStatus = computed(() => {
  return currentTrack.value?.likeStatus || 0
})

watch(
  () => currentTrack.value.artist,
  async (newArtist) => {
    if (!newArtist || newArtist === '未知歌手') {
      artistInfo.value = null
      return
    }

    try {
      const searchRes = await getAllArtists({
        artistName: newArtist,
        pageNum: 1,
        pageSize: 1,
      })
      if (searchRes.code === 0 && searchRes.data?.items?.length) {
        const artist = searchRes.data.items[0]
        const detailRes = await getArtistDetail(artist.artistId)
        if (detailRes.code === 0 && detailRes.data) {
          artistInfo.value = detailRes.data
        } else {
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

const nextTrackInfo = computed(() => {
  if (!audioStore.trackList.length) return null
  const nextIndex =
    (audioStore.currentSongIndex + 1) % audioStore.trackList.length
  return audioStore.trackList[nextIndex]
})

const toggleCollapse = () => {
  if (props.isMobile) {
    emit('close')
  } else {
    isCollapsed.value = !isCollapsed.value
  }
}

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

const playNextTrack = () => {
  playNext()
}

const openQueue = () => {
  menuStore.setPlaylistOpen(true)
}
</script>

<template>
  <aside
    class="spotify-now-playing"
    :class="[
      isMobile ? 'spotify-now-playing-mobile' : 'spotify-now-playing-desktop',
      isCollapsed ? 'spotify-now-playing-collapsed' : ''
    ]"
  >
    <nav class="spotify-now-playing-nav">
      <div v-if="!isCollapsed || isMobile" class="spotify-now-playing-header">
        <button
          class="spotify-now-playing-toggle"
          @click="toggleCollapse"
          :title="isMobile ? '关闭' : '收起'"
        >
          <Icon
            :icon="isMobile ? 'mdi:close' : 'mdi:chevron-right'"
            class="spotify-toggle-icon"
          />
        </button>
        <span v-if="!isMobile" class="spotify-now-playing-title">
          {{ currentTrack.artist || '未知艺人' }}
        </span>
        <div v-if="isMobile" class="w-8"></div>
      </div>

      <div
        v-if="!isCollapsed || isMobile"
        class="spotify-now-playing-content"
      >
        <div class="spotify-now-playing-cover">
          <img
            :src="fixUrl(currentTrack.cover) || defaultSongCover"
            alt="Cover"
            class="spotify-now-playing-img"
          />
        </div>

        <div class="spotify-now-playing-info">
          <div class="spotify-now-playing-song">
            <div class="spotify-now-playing-song-title" :title="currentTrack.title">
              {{ currentTrack.title || '未知歌曲' }}
            </div>
            <div class="spotify-now-playing-song-artist" :title="currentTrack.artist">
              {{ currentTrack.artist || '未知艺人' }}
            </div>
          </div>
          <button class="spotify-now-playing-like" @click="handleLike">
            <Icon
              icon="mdi:cards-heart-outline"
              v-if="currentSongLikeStatus === 0"
              class="spotify-like-icon"
            />
            <Icon icon="mdi:cards-heart" v-else class="spotify-like-icon spotify-like-active" />
          </button>
        </div>

        <div class="spotify-now-playing-section">
          <h3 class="spotify-now-playing-section-title">关于艺人</h3>
          <div class="spotify-now-playing-artist" v-if="artistInfo">
            <div
              class="spotify-now-playing-artist-info"
              @click="router.push(`/artist/${artistInfo.artistId}`)"
            >
              <img
                :src="fixUrl(artistInfo.avatar) || defaultUserAvatar"
                alt="Artist Avatar"
                class="spotify-now-playing-artist-avatar"
              />
              <div class="spotify-now-playing-artist-name">
                {{ artistInfo.artistName || currentTrack.artist }}
              </div>
            </div>
            <div class="spotify-now-playing-artist-bio">
              {{
                artistInfo.introduction
                  ? artistInfo.introduction.replace(/<[^>]+>/g, '')
                  : '这位艺人暂无详细介绍。快去听听TA的其他作品吧！'
              }}
            </div>
          </div>

          <div
            class="spotify-now-playing-artist"
            v-else-if="currentTrack.artist && currentTrack.artist !== '未知歌手'"
          >
            <div class="spotify-now-playing-artist-info">
              <img
                :src="defaultUserAvatar"
                alt="Default Avatar"
                class="spotify-now-playing-artist-avatar"
              />
              <div class="spotify-now-playing-artist-name">
                {{ currentTrack.artist }}
              </div>
            </div>
            <div class="spotify-now-playing-artist-bio">
              暂无详细介绍
            </div>
          </div>

          <div v-else class="spotify-now-playing-empty">
            暂无艺人信息
          </div>
        </div>

        <div class="spotify-now-playing-section" v-if="nextTrackInfo">
          <div class="spotify-now-playing-queue-header">
            <h3 class="spotify-now-playing-section-title">队列中的下一首</h3>
            <span class="spotify-now-playing-queue-link" @click="openQueue">
              打开队列
            </span>
          </div>

          <div class="spotify-now-playing-next" @click="playNextTrack">
            <div class="spotify-now-playing-next-cover">
              <img
                :src="fixUrl(nextTrackInfo.cover) || defaultSongCover"
                alt="Next Song Cover"
                class="spotify-now-playing-next-img"
              />
              <div class="spotify-now-playing-next-play">
                <Icon icon="mdi:play" class="text-white" />
              </div>
            </div>
            <div class="spotify-now-playing-next-info">
              <div class="spotify-now-playing-next-title">
                {{ nextTrackInfo.title }}
              </div>
              <div class="spotify-now-playing-next-artist">
                {{ nextTrackInfo.artist }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="spotify-now-playing-collapsed-content">
        <div class="spotify-collapsed-top">
          <img
            :src="fixUrl(currentTrack.cover) || defaultSongCover"
            alt="Cover"
            class="spotify-now-playing-mini-cover"
          />
          <button class="spotify-now-playing-like-mini" @click="handleLike">
            <Icon
              icon="mdi:cards-heart-outline"
              v-if="currentSongLikeStatus === 0"
              class="spotify-like-icon-mini"
            />
            <Icon icon="mdi:cards-heart" v-else class="spotify-like-icon-mini spotify-like-active" />
          </button>
        </div>
        <div class="spotify-toggle-wrapper">
          <button
            class="spotify-now-playing-toggle"
            @click="toggleCollapse"
            title="展开"
          >
            <Icon icon="mdi:chevron-left" class="spotify-toggle-icon" />
          </button>
        </div>
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.spotify-now-playing {
  display: flex;
  flex-direction: column;
  background-color: var(--bg-surface, #121212);
  border-radius: 8px;
  overflow: hidden;
  transition: width 200ms ease, background-color 200ms ease;
}

.spotify-now-playing-desktop {
  display: none;
  width: 320px;
  min-width: 280px;
}

.spotify-now-playing-mobile {
  width: 100% !important;
  height: 100% !important;
  border-radius: 0 !important;
}

.spotify-now-playing-collapsed {
  width: 72px;
  min-width: 72px;
}

.spotify-now-playing-nav {
  display: flex;
  flex-direction: column;
  padding: 16px;
  height: 100%;
  overflow: hidden;
}

.spotify-now-playing-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.spotify-header-collapsed {
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.spotify-header-collapsed .spotify-now-playing-toggle {
  margin: 0;
}

.spotify-now-playing-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--text-subdued, #b3b3b3);
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-now-playing-toggle:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-toggle-icon {
  font-size: 1.25rem;
}

.spotify-now-playing-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.spotify-now-playing-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.spotify-now-playing-content::-webkit-scrollbar {
  width: 4px;
}

.spotify-now-playing-content::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-now-playing-content::-webkit-scrollbar-thumb {
  background-color: var(--scrollbar-thumb, rgba(255, 255, 255, 0.3));
  border-radius: 20px;
}

.spotify-now-playing-cover {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  flex-shrink: 0;
}

.spotify-now-playing-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-now-playing-info {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.spotify-now-playing-song {
  flex: 1;
  min-width: 0;
}

.spotify-now-playing-song-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-now-playing-song-artist {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-now-playing-like {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
}

.spotify-now-playing-like:hover .spotify-like-icon {
  color: var(--text-base, #fff);
}

.spotify-like-icon {
  font-size: 1.25rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-like-active {
  color: var(--text-accent, #1db954);
}

.spotify-now-playing-like:hover .spotify-like-active {
  color: var(--text-accent-hover, #1ed760);
}

.spotify-now-playing-section {
  background-color: var(--bg-elevated, rgba(255, 255, 255, 0.05));
  border-radius: 8px;
  padding: 16px;
  flex-shrink: 0;
}

.spotify-now-playing-section-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 12px;
}

.spotify-now-playing-artist-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  margin: -8px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-now-playing-artist-info:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-now-playing-artist-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.spotify-now-playing-artist-name {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-base, #fff);
}

.spotify-now-playing-artist-bio {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spotify-now-playing-empty {
  font-size: 0.8125rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-now-playing-queue-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.spotify-now-playing-queue-link {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: color 200ms ease;
}

.spotify-now-playing-queue-link:hover {
  color: var(--text-base, #fff);
}

.spotify-now-playing-next {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  margin: -8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-now-playing-next:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-now-playing-next-cover {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.spotify-now-playing-next-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.spotify-now-playing-next-play {
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

.spotify-now-playing-next:hover .spotify-now-playing-next-play {
  opacity: 1;
}

.spotify-now-playing-next-info {
  flex: 1;
  min-width: 0;
}

.spotify-now-playing-next-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-now-playing-next-artist {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-now-playing-collapsed-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.spotify-collapsed-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 16px;
}

.spotify-toggle-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spotify-now-playing-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--text-subdued, #b3b3b3);
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-now-playing-toggle:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-now-playing-mini-cover {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.spotify-toggle-icon {
  font-size: 1.25rem;
}

.spotify-now-playing-mini-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.spotify-now-playing-like-mini {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
}

.spotify-now-playing-like-mini:hover .spotify-like-icon-mini {
  color: var(--text-base, #fff);
}

.spotify-like-icon-mini {
  font-size: 1rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-like-active {
  color: var(--text-accent, #1db954);
}

.spotify-now-playing-like-mini:hover .spotify-like-active {
  color: var(--text-accent-hover, #1ed760);
}

/* Light Theme */
:root:not(.dark) .spotify-now-playing {
  --bg-surface: #f0f0f0;
  --bg-elevated: rgba(0, 0, 0, 0.05);
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --text-accent: #1db954;
  --text-accent-hover: #1ed760;
  --scrollbar-thumb: rgba(0, 0, 0, 0.3);
}

:root:not(.dark) .spotify-now-playing-cover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-now-playing-section {
  background-color: rgba(0, 0, 0, 0.03);
}

:root:not(.dark) .spotify-now-playing-toggle:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-now-playing-artist-info:hover,
:root:not(.dark) .spotify-now-playing-next:hover {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-now-playing-mini-cover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

@media (min-width: 768px) {
  .spotify-now-playing-desktop {
    display: flex;
  }
  
  .spotify-now-playing-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .spotify-now-playing {
    border-radius: 0;
  }
  
  .spotify-now-playing-nav {
    padding: 16px;
  }
  
  .spotify-now-playing-header {
    margin-bottom: 16px;
  }
  
  .spotify-now-playing-toggle {
    width: 32px;
    height: 32px;
  }
  
  .spotify-toggle-icon {
    font-size: 1.25rem;
  }
  
  .spotify-now-playing-content {
    gap: 24px;
  }
  
  .spotify-now-playing-cover {
    border-radius: 8px;
  }
  
  .spotify-now-playing-song-title {
    font-size: 1.125rem;
  }
  
  .spotify-now-playing-song-artist {
    font-size: 0.875rem;
  }
  
  .spotify-now-playing-like {
    width: 32px;
    height: 32px;
  }
  
  .spotify-like-icon {
    font-size: 1.25rem;
  }
  
  .spotify-now-playing-section {
    padding: 16px;
    border-radius: 8px;
  }
  
  .spotify-now-playing-section-title {
    font-size: 0.9375rem;
    margin-bottom: 12px;
  }
  
  .spotify-now-playing-artist-avatar {
    width: 48px;
    height: 48px;
  }
  
  .spotify-now-playing-artist-name {
    font-size: 0.9375rem;
  }
  
  .spotify-now-playing-artist-bio {
    font-size: 0.8125rem;
    -webkit-line-clamp: 3;
  }
  
  .spotify-now-playing-next-cover {
    width: 48px;
    height: 48px;
  }
  
  .spotify-now-playing-next-title {
    font-size: 0.875rem;
  }
  
  .spotify-now-playing-next-artist {
    font-size: 0.75rem;
  }
  
  .spotify-now-playing-queue-link {
    font-size: 0.75rem;
    padding: 0;
    background-color: transparent;
    border-radius: 0;
  }
  
  .spotify-now-playing-queue-link:hover {
    background-color: transparent;
  }
}
</style>
