<script setup lang="ts">
import {
  getPlaylistDetail,
  addPlaylistComment,
  likeComment,
  deleteComment,
} from '@/api/system'
import { formatNumber, fixUrl } from '@/utils'
import type { PlaylistDetail, Song } from '@/api/interface'
import coverImg from '@/assets/cover.png'
import { usePlaylistStore } from '@/stores/modules/playlist'
import { useFavoriteStore } from '@/stores/modules/favorite'
import { ElMessage } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import { Icon } from '@iconify/vue'
import { MenuStore } from '@/stores/modules/menu'

const route = useRoute()
const router = useRouter()
const audui = AudioStore()
const playlistStore = usePlaylistStore()
const favoriteStore = useFavoriteStore()
const userStore = UserStore()
const menuStore = MenuStore()
const playlist = computed(() => playlistStore.playlist)
const songs = computed(() => playlistStore.songs)
const { loadTrack, play } = useAudioPlayer()

const dominantColor = ref('#1e3a5f')
const hasShownSavedToast = ref(false)

const maybeShowSavedToast = async () => {
  if (hasShownSavedToast.value) {
    return
  }
  if (String(route.query.from || '') === 'chat' && String(route.query.saved || '') === '1') {
    hasShownSavedToast.value = true
    ElMessage.success('已保存到你的歌单')

    const nextQuery: Record<string, any> = { ...route.query }
    delete nextQuery.from
    delete nextQuery.saved
    await router.replace({ path: route.path, query: nextQuery })
  }
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

watch(
  () => songs.value,
  async (newSongs) => {
    if (newSongs && newSongs.length > 0 && newSongs[0].coverUrl) {
      const color = await extractDominantColor(newSongs[0].coverUrl + '?param=50y50')
      dominantColor.value = color
    }
  },
  { immediate: true }
)

// 添加激活的选项卡变量
const activeTab = ref('songs')

// 计算当前歌单是否已收藏
const isCollected = computed(() => {
  const playlistId = Number(route.params.id)
  return favoriteStore.favoritePlaylists.some((item) => item.id === playlistId)
})

// 收藏/取消收藏歌单
const toggleCollect = async () => {
  try {
    const playlistId = Number(route.params.id)
    if (isCollected.value) {
      await favoriteStore.cancelCollectPlaylist(playlistId)
    } else {
      await favoriteStore.collectPlaylist(playlistId)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const sharePlaylist = () => {
  const playlistId = Number(route.params.id)
  if (!playlistId) return
  router.push({ path: '/messages', query: { playlistId } })
}

interface PlaylistComment {
  commentId: number
  userId?: number
  username: string
  userAvatar: string
  content: string
  createTime: string
  likeCount: number
}

// 评论相关
const commentContent = ref('')
const maxLength = 180
const comments = computed(() => {
  const rawComments = (playlistStore.playlist?.comments ||
    []) as PlaylistComment[]
  return rawComments
    .map((comment) => ({
      ...comment,
      userAvatar: fixUrl(comment.userAvatar),
      likeCount: comment.likeCount,
    }))
    .sort((a, b) => {
      // 使用commentId进行降序排序，id大的排在前面
      return b.commentId - a.commentId
    })
})

// 获取当前用户名
const currentUsername = computed(() => userStore.userInfo?.username || '')

const goUserProfile = (userId?: number) => {
  if (!userId) return
  menuStore.setSongDrawerOpen(false)
  router.push(`/profile/${userId}`)
}

// 发布评论
const handleComment = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    const playlistId = Number(route.params.id)
    const content = commentContent.value.trim()

    const res = await addPlaylistComment({
      playlistId,
      content,
    })

    if (res.code === 0) {
      ElMessage.success('评论发布成功')
      commentContent.value = ''
      // 重新获取歌单详情以更新评论列表
      const detailRes = await getPlaylistDetail(playlistId)
      if (detailRes.code === 0 && detailRes.data) {
        const playlistData = detailRes.data as PlaylistDetail
        playlistStore.setPlaylistInfo({
          ...playlistStore.playlist!,
          comments: playlistData.comments || [],
        })
      }
    } else {
      ElMessage.error('评论发布失败')
    }
  } catch (error) {
    ElMessage.error('评论发布失败')
  }
}

// 处理点赞
const handleLike = async (comment: PlaylistComment) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    // 调用点赞接口
    const res = await likeComment(comment.commentId)
    if (res.code === 0) {
      // 更新评论的点赞数量
      const updatedComments = comments.value.map((item) => {
        if (item.commentId === comment.commentId) {
          return {
            ...item,
            likeCount: item.likeCount + 1,
          }
        }
        return item
      })

      // 更新到store
      playlistStore.setPlaylistInfo({
        ...playlistStore.playlist!,
        comments: updatedComments,
      })

      ElMessage.success('点赞成功')
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

// 删除评论
const handleDelete = async (comment: PlaylistComment) => {
  try {
    const res = await deleteComment(comment.commentId)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      // 重新获取歌单详情以更新评论列表
      const playlistId = Number(route.params.id)
      const detailRes = await getPlaylistDetail(playlistId)
      if (detailRes.code === 0 && detailRes.data) {
        const playlistData = detailRes.data as PlaylistDetail
        playlistStore.setPlaylistInfo({
          ...playlistStore.playlist!,
          comments: playlistData.comments || [],
        })
      }
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

watch(
  () => route.params.id,
  async (id) => {
    if (id) {
      await maybeShowSavedToast()
      playlistStore.setPlaylistInfo(null)
      playlistStore.setSongs([])
      const res = await getPlaylistDetail(Number(id))
      if (
        res.code === 0 &&
        res.data &&
        typeof res.data === 'object' &&
        'songs' in res.data
      ) {
        const playlistData = res.data as PlaylistDetail
        // 转换歌曲数据为 Song 类型
        const convertedSongs: Song[] = playlistData.songs.map((song) => ({
          songId: song.songId,
          songName: song.songName,
          artistName: song.artistName,
          album: song.album,
          duration: song.duration,
          coverUrl: fixUrl(song.coverUrl) || coverImg,
          audioUrl: song.audioUrl,
          likeStatus: song.likeStatus,
          releaseTime: song.releaseTime,
        }))

        playlistStore.setSongs(convertedSongs)
        playlistStore.setPlaylistInfo({
          name: playlistData.title,
          description: playlistData.introduction,
          coverImgUrl: fixUrl(playlistData.coverUrl) || coverImg,
          creator: {
            nickname: 'creator',
            avatarUrl: coverImg,
          },
          trackCount: playlistData.songs.length,
          tracks: convertedSongs,
          commentCount: playlistData.comments?.length || 0,
          tags: [],
          comments: playlistData.comments || [],
        })
      }
    }
  },
  { immediate: true }
)

const handlePlayAll = async () => {
  audui.setAudioStore('trackList', [])

  if (!songs.value.length) return

  const result = songs.value.map((song) => ({
    id: song.songId.toString(),
    title: song.songName,
    artist: song.artistName,
    album: song.album,
    cover: song.coverUrl || coverImg, // coverUrl already fixed above
    url: song.audioUrl,
    duration: parseFloat(song.duration) * 1000,
    likeStatus: song.likeStatus,
  }))

  audui.setAudioStore('trackList', result)
  audui.setAudioStore('currentSongIndex', 0)
  await loadTrack()
  play()
}
</script>
<template>
  <div class="spotify-playlist-page" :style="{ '--gradient-color': dominantColor }">
    <!-- Playlist Header -->
    <div class="spotify-playlist-header">
      <div class="spotify-playlist-cover">
        <img
          :alt="playlist?.name"
          class="spotify-playlist-cover-img"
          :src="
            (playlist?.coverImgUrl && playlist.coverImgUrl.startsWith('http')
              ? playlist.coverImgUrl + '?param=500y500'
              : playlist?.coverImgUrl || coverImg)
          "
        />
      </div>
      <div class="spotify-playlist-info">
        <span class="spotify-playlist-type">歌单</span>
        <h1 class="spotify-playlist-title">{{ playlist?.name }}</h1>
        <p class="spotify-playlist-description" :title="playlist?.description">
          {{ playlist?.description }}
        </p>
        <div class="spotify-playlist-creator">
          <img
            class="spotify-creator-avatar"
            :alt="playlist?.creator.nickname"
            :src="playlist?.creator.avatarUrl"
          />
          <span class="spotify-creator-name">{{ playlist?.creator.nickname }}</span>
          <span class="spotify-meta-separator">•</span>
          <span class="spotify-meta-text">{{ playlist?.trackCount }} 首歌曲</span>
        </div>
        <div class="spotify-playlist-actions">
          <button @click="handlePlayAll" class="spotify-play-btn">
            <Icon icon="mdi:play" class="text-xl" />
            <span>播放</span>
          </button>
          <button @click="sharePlaylist" class="spotify-action-btn" title="分享给好友">
            <Icon icon="mdi:share-variant-outline" class="text-xl" />
          </button>
          <button @click="toggleCollect" class="spotify-action-btn" :class="{ 'spotify-action-active': isCollected }">
            <Icon :icon="isCollected ? 'mdi:check' : 'mdi:plus'" class="text-xl" />
          </button>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="spotify-tabs">
      <button
        v-for="tab in [
          { name: '歌曲', value: 'songs' },
          { name: '评论', value: 'comments' },
        ]"
        :key="tab.value"
        @click="activeTab = tab.value"
        class="spotify-tab"
        :class="{ 'spotify-tab-active': activeTab === tab.value }"
      >
        {{ tab.name }}
      </button>
    </div>

    <!-- Content -->
    <div class="spotify-playlist-content">
      <!-- Songs Tab -->
      <div v-show="activeTab === 'songs'" class="spotify-songs-content">
        <Table :data="songs" />
      </div>

      <!-- Comments Tab -->
      <div v-show="activeTab === 'comments'" class="spotify-comments-content">
        <!-- Comment Input -->
        <div class="spotify-comment-input">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            :maxlength="maxLength"
            placeholder="说点什么吧"
            resize="none"
            show-word-limit
          />
          <div class="spotify-comment-actions">
            <button
              @click="handleComment"
              :disabled="!commentContent.trim()"
              class="spotify-comment-submit"
            >
              发布
            </button>
          </div>
        </div>

        <!-- Comments List -->
        <div class="spotify-comments-list">
          <h3 class="spotify-comments-title">
            最新评论（{{ formatNumber(playlist?.commentCount ?? 0) }}）
          </h3>
          
          <div v-if="comments.length" class="spotify-comments-items">
            <div v-for="comment in comments" :key="comment.commentId" class="spotify-comment-item">
              <img
                :src="comment.userAvatar || coverImg"
                alt="avatar"
                class="spotify-comment-avatar"
                @click="goUserProfile(comment.userId)"
              />
              <div class="spotify-comment-body">
                <div class="spotify-comment-header">
                  <span class="spotify-comment-username" @click="goUserProfile(comment.userId)">{{ comment.username }}</span>
                </div>
                <p class="spotify-comment-content">{{ comment.content }}</p>
                <div class="spotify-comment-footer">
                  <span class="spotify-comment-time">{{ comment.createTime }}</span>
                  <div class="spotify-comment-actions-row">
                    <button
                      v-if="comment.username === currentUsername"
                      class="spotify-comment-action"
                      @click="handleDelete(comment)"
                    >
                      <Icon icon="mdi:delete-outline" />
                      <span>删除</span>
                    </button>
                    <button class="spotify-comment-action" @click="handleLike(comment)">
                      <Icon icon="mdi:thumb-up-outline" />
                      <span>{{ formatNumber(comment.likeCount) }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="spotify-comments-empty">
            <p>暂无评论，快来抢沙发吧~</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spotify-playlist-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 300px);
}

.spotify-playlist-header {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 24px;
  padding-top: 48px;
}

.spotify-playlist-cover {
  width: 232px;
  height: 232px;
  flex-shrink: 0;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.spotify-playlist-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.spotify-playlist-info {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  flex: 1;
  min-width: 0;
}

.spotify-playlist-type {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-base, #fff);
  margin-bottom: 8px;
}

.spotify-playlist-title {
  font-size: 3rem;
  font-weight: 900;
  color: var(--text-base, #fff);
  line-height: 1.1;
  margin-bottom: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.spotify-playlist-description {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.spotify-playlist-creator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.spotify-creator-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.spotify-creator-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.spotify-meta-separator {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-meta-text {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-playlist-actions {
  display: flex;
  align-items: center;
  gap: 24px;
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

.spotify-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: transparent;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.3));
  border-radius: 50%;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: border-color 200ms ease, color 200ms ease, background-color 200ms ease, transform 33ms ease;
}

.spotify-action-btn:hover {
  border-color: var(--text-base, #fff);
  color: var(--text-base, #fff);
  transform: scale(1.05);
}

.spotify-action-active {
  background-color: #1db954;
  border-color: #1db954;
  color: #000;
}

.spotify-action-active:hover {
  background-color: #1ed760;
  border-color: #1ed760;
}

.spotify-tabs {
  display: flex;
  gap: 4px;
  padding: 0 24px;
  margin-bottom: 16px;
}

.spotify-tab {
  padding: 12px 16px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-tab:hover {
  color: var(--text-base, #fff);
}

.spotify-tab-active {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-playlist-content {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

.spotify-playlist-content::-webkit-scrollbar {
  width: 8px;
}

.spotify-playlist-content::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-playlist-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.spotify-playlist-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.spotify-songs-content {
  height: 100%;
}

.spotify-comments-content {
  padding-top: 16px;
}

.spotify-comment-input {
  margin-bottom: 24px;
}

.spotify-comment-input :deep(.el-textarea__inner) {
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 8px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
}

.spotify-comment-input :deep(.el-textarea__inner::placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.spotify-comment-submit {
  padding: 8px 24px;
  background-color: #1db954;
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
}

.spotify-comment-submit:hover:not(:disabled) {
  transform: scale(1.04);
  background-color: #1ed760;
}

.spotify-comment-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spotify-comments-list {
  margin-top: 24px;
}

.spotify-comments-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 16px;
}

.spotify-comments-items {
  display: flex;
  flex-direction: column;
}

.spotify-comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.spotify-comment-body {
  flex: 1;
  min-width: 0;
}

.spotify-comment-header {
  margin-bottom: 4px;
}

.spotify-comment-username {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  cursor: pointer;
}

.spotify-comment-content {
  font-size: 0.875rem;
  color: var(--text-base, #fff);
  line-height: 1.5;
  margin-bottom: 8px;
}

.spotify-comment-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spotify-comment-time {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-comment-actions-row {
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-comment-item:hover .spotify-comment-actions-row {
  opacity: 1;
}

.spotify-comment-action {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.75rem;
  cursor: pointer;
  transition: color 200ms ease;
}

.spotify-comment-action:hover {
  color: var(--text-base, #fff);
}

.spotify-comments-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-subdued, #b3b3b3);
}

/* Light Theme */
:root:not(.dark) .spotify-playlist-page {
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.15);
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .spotify-playlist-cover {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-action-btn {
  border-color: rgba(0, 0, 0, 0.25);
  color: #5a5a5a;
}

:root:not(.dark) .spotify-action-btn:hover {
  border-color: rgba(0, 0, 0, 0.5);
  color: #000000;
}

:root:not(.dark) .spotify-action-active {
  background-color: #1db954;
  border-color: #1db954;
  color: #000000;
}

:root:not(.dark) .spotify-action-active:hover {
  background-color: #1ed760;
  border-color: #1ed760;
  color: #000000;
}

:root:not(.dark) .spotify-comment-input :deep(.el-textarea__inner) {
  background-color: #f5f5f5;
}

/* Responsive */
@media (max-width: 768px) {
  .spotify-playlist-page {
    padding-bottom: 80px;
  }
  
  .spotify-playlist-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px 16px;
    padding-top: 16px;
  }
  
  .spotify-playlist-cover {
    width: 160px;
    height: 160px;
  }
  
  .spotify-playlist-info {
    align-items: center;
  }
  
  .spotify-playlist-title {
    font-size: 1.5rem;
    margin-bottom: 8px;
  }
  
  .spotify-playlist-description {
    font-size: 0.8125rem;
  }
  
  .spotify-playlist-creator {
    justify-content: center;
    margin-bottom: 16px;
  }
  
  .spotify-playlist-actions {
    justify-content: center;
    gap: 16px;
  }
  
  .spotify-play-btn {
    height: 40px;
    padding: 0 24px;
    font-size: 0.875rem;
  }
  
  .spotify-action-btn {
    width: 36px;
    height: 36px;
  }
  
  .spotify-tabs {
    padding: 0 12px;
  }
  
  .spotify-tab {
    padding: 10px 12px;
    font-size: 0.8125rem;
  }
  
  .spotify-playlist-content {
    padding: 0 12px 16px;
  }
  
  .spotify-comment-item {
    padding: 12px 0;
  }
  
  .spotify-comment-avatar {
    width: 32px;
    height: 32px;
  }
  
  .spotify-comment-actions-row {
    opacity: 1;
  }
}
</style>
