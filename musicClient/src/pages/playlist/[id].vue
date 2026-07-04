<script setup lang="ts">
import {
  getPlaylistDetail,
  addPlaylistComment,
  likeComment,
  deleteComment,
} from '@/api/system'
import { formatNumber, fixUrl, durationToMs } from '@/utils'
import { extractDominantColor } from '@/utils/imageUtils'
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
let latestColorTaskId = 0

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

watch(
  () => songs.value,
  async (newSongs) => {
    if (newSongs && newSongs.length > 0 && newSongs[0].coverUrl) {
      const taskId = ++latestColorTaskId
      const color = await extractDominantColor(newSongs[0].coverUrl + '?param=50y50')
      if (taskId === latestColorTaskId) {
        dominantColor.value = color
      }
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
    duration: durationToMs(song.duration),
    likeStatus: song.likeStatus,
  }))

  audui.setAudioStore('trackList', result)
  audui.setAudioStore('currentSongIndex', 0)
  await loadTrack()
  play()
}
</script>
<template>
  <div class="mr-playlist-page" :style="{ '--gradient-color': dominantColor }">
    <!-- Playlist Header -->
    <div class="mr-playlist-header">
      <div class="mr-playlist-cover">
        <img
          :alt="playlist?.name"
          class="mr-playlist-cover-img"
          :src="
            (playlist?.coverImgUrl && playlist.coverImgUrl.startsWith('http')
              ? playlist.coverImgUrl + '?param=500y500'
              : playlist?.coverImgUrl || coverImg)
          "
        />
      </div>
      <div class="mr-playlist-info">
        <span class="mr-playlist-type">歌单</span>
        <h1 class="mr-playlist-title">{{ playlist?.name }}</h1>
        <p class="mr-playlist-description" :title="playlist?.description">
          {{ playlist?.description }}
        </p>
        <div class="mr-playlist-creator">
          <img
            class="mr-creator-avatar"
            :alt="playlist?.creator.nickname"
            :src="playlist?.creator.avatarUrl"
          />
          <span class="mr-creator-name">{{ playlist?.creator.nickname }}</span>
          <span class="mr-meta-separator">•</span>
          <span class="mr-meta-text">{{ playlist?.trackCount }} 首歌曲</span>
        </div>
        <div class="mr-playlist-actions">
          <button @click="handlePlayAll" class="mr-play-btn">
            <Icon icon="mdi:play" class="text-xl" />
            <span>播放</span>
          </button>
          <button @click="sharePlaylist" class="mr-action-btn" title="分享给好友">
            <Icon icon="mdi:share-variant-outline" class="text-xl" />
          </button>
          <button @click="toggleCollect" class="mr-action-btn" :class="{ 'mr-action-active': isCollected }">
            <Icon :icon="isCollected ? 'mdi:check' : 'mdi:plus'" class="text-xl" />
          </button>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="mr-tabs">
      <button
        v-for="tab in [
          { name: '歌曲', value: 'songs' },
          { name: '评论', value: 'comments' },
        ]"
        :key="tab.value"
        @click="activeTab = tab.value"
        class="mr-tab"
        :class="{ 'mr-tab-active': activeTab === tab.value }"
      >
        {{ tab.name }}
      </button>
    </div>

    <!-- Content -->
    <div class="mr-playlist-content">
      <!-- Songs Tab -->
      <div v-show="activeTab === 'songs'" class="mr-songs-content">
        <Table :data="songs" />
      </div>

      <!-- Comments Tab -->
      <div v-show="activeTab === 'comments'" class="mr-comments-content">
        <!-- Comment Input -->
        <div class="mr-comment-input">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            :maxlength="maxLength"
            placeholder="说点什么吧"
            resize="none"
            show-word-limit
          />
          <div class="mr-comment-actions">
            <button
              @click="handleComment"
              :disabled="!commentContent.trim()"
              class="mr-comment-submit"
            >
              发布
            </button>
          </div>
        </div>

        <!-- Comments List -->
        <div class="mr-comments-list">
          <h3 class="mr-comments-title">
            最新评论（{{ formatNumber(playlist?.commentCount ?? 0) }}）
          </h3>
          
          <div v-if="comments.length" class="mr-comments-items">
            <div v-for="comment in comments" :key="comment.commentId" class="mr-comment-item">
              <img
                :src="comment.userAvatar || coverImg"
                alt="avatar"
                class="mr-comment-avatar"
                @click="goUserProfile(comment.userId)"
              />
              <div class="mr-comment-body">
                <div class="mr-comment-header">
                  <span class="mr-comment-username" @click="goUserProfile(comment.userId)">{{ comment.username }}</span>
                </div>
                <p class="mr-comment-content">{{ comment.content }}</p>
                <div class="mr-comment-footer">
                  <span class="mr-comment-time">{{ comment.createTime }}</span>
                  <div class="mr-comment-actions-row">
                    <button
                      v-if="comment.username === currentUsername"
                      class="mr-comment-action"
                      @click="handleDelete(comment)"
                    >
                      <Icon icon="mdi:delete-outline" />
                      <span>删除</span>
                    </button>
                    <button class="mr-comment-action" @click="handleLike(comment)">
                      <Icon icon="mdi:thumb-up-outline" />
                      <span>{{ formatNumber(comment.likeCount) }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="mr-comments-empty">
            <p>暂无评论，快来抢沙发吧~</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mr-playlist-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 300px);
}

.mr-playlist-header {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 24px;
  padding-top: 48px;
}

.mr-playlist-cover {
  width: 232px;
  height: 232px;
  flex-shrink: 0;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.mr-playlist-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.mr-playlist-info {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  flex: 1;
  min-width: 0;
}

.mr-playlist-type {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-base, #fff);
  margin-bottom: 8px;
}

.mr-playlist-title {
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

.mr-playlist-description {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.mr-playlist-creator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.mr-creator-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.mr-creator-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.mr-meta-separator {
  color: var(--text-subdued, #b3b3b3);
}

.mr-meta-text {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-playlist-actions {
  display: flex;
  align-items: center;
  gap: 24px;
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

.mr-action-btn {
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

.mr-action-btn:hover {
  border-color: var(--text-base, #fff);
  color: var(--text-base, #fff);
  transform: scale(1.05);
}

.mr-action-active {
  background-color: var(--mr-accent);
  border-color: var(--mr-accent);
  color: #000;
}

.mr-action-active:hover {
  background-color: var(--mr-accent-hover);
  border-color: var(--mr-accent-hover);
}

.mr-tabs {
  display: flex;
  gap: 4px;
  padding: 0 24px;
  margin-bottom: 16px;
}

.mr-tab {
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

.mr-tab:hover {
  color: var(--text-base, #fff);
}

.mr-tab-active {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-playlist-content {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

.mr-playlist-content::-webkit-scrollbar {
  width: 8px;
}

.mr-playlist-content::-webkit-scrollbar-track {
  background: transparent;
}

.mr-playlist-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.mr-playlist-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.mr-songs-content {
  height: 100%;
}

.mr-comments-content {
  padding-top: 16px;
}

.mr-comment-input {
  margin-bottom: 24px;
}

.mr-comment-input :deep(.el-textarea__inner) {
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 8px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
}

.mr-comment-input :deep(.el-textarea__inner::placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.mr-comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.mr-comment-submit {
  padding: 8px 24px;
  background-color: var(--mr-accent);
  border: none;
  border-radius: 500px;
  color: #000;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
}

.mr-comment-submit:hover:not(:disabled) {
  transform: scale(1.04);
  background-color: var(--mr-accent-hover);
}

.mr-comment-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mr-comments-list {
  margin-top: 24px;
}

.mr-comments-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 16px;
}

.mr-comments-items {
  display: flex;
  flex-direction: column;
}

.mr-comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.mr-comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.mr-comment-body {
  flex: 1;
  min-width: 0;
}

.mr-comment-header {
  margin-bottom: 4px;
}

.mr-comment-username {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  cursor: pointer;
}

.mr-comment-content {
  font-size: 0.875rem;
  color: var(--text-base, #fff);
  line-height: 1.5;
  margin-bottom: 8px;
}

.mr-comment-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mr-comment-time {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-comment-actions-row {
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 200ms ease;
}

.mr-comment-item:hover .mr-comment-actions-row {
  opacity: 1;
}

.mr-comment-action {
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

.mr-comment-action:hover {
  color: var(--text-base, #fff);
}

.mr-comments-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-subdued, #b3b3b3);
}

/* Light Theme */
:root:not(.dark) .mr-playlist-page {
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.15);
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .mr-playlist-cover {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .mr-action-btn {
  border-color: rgba(0, 0, 0, 0.25);
  color: #5a5a5a;
}

:root:not(.dark) .mr-action-btn:hover {
  border-color: rgba(0, 0, 0, 0.5);
  color: #000000;
}

:root:not(.dark) .mr-action-active {
  background-color: var(--mr-accent);
  border-color: var(--mr-accent);
  color: #000000;
}

:root:not(.dark) .mr-action-active:hover {
  background-color: var(--mr-accent-hover);
  border-color: var(--mr-accent-hover);
  color: #000000;
}

:root:not(.dark) .mr-comment-input :deep(.el-textarea__inner) {
  background-color: #f5f5f5;
}

/* Responsive */
@media (max-width: 768px) {
  .mr-playlist-page {
    padding-bottom: 140px;
  }
  
  .mr-playlist-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px 16px;
    padding-top: 16px;
  }
  
  .mr-playlist-cover {
    width: 160px;
    height: 160px;
  }
  
  .mr-playlist-info {
    align-items: center;
  }
  
  .mr-playlist-title {
    font-size: 1.5rem;
    margin-bottom: 8px;
  }
  
  .mr-playlist-description {
    font-size: 0.8125rem;
  }
  
  .mr-playlist-creator {
    justify-content: center;
    margin-bottom: 16px;
  }
  
  .mr-playlist-actions {
    justify-content: center;
    gap: 16px;
  }
  
  .mr-play-btn {
    height: 40px;
    padding: 0 24px;
    font-size: 0.875rem;
  }
  
  .mr-action-btn {
    width: 36px;
    height: 36px;
  }
  
  .mr-tabs {
    padding: 0 12px;
  }
  
  .mr-tab {
    padding: 10px 12px;
    font-size: 0.8125rem;
  }
  
  .mr-playlist-content {
    padding: 0 12px 16px;
  }
  
  .mr-comment-item {
    padding: 12px 0;
  }
  
  .mr-comment-avatar {
    width: 32px;
    height: 32px;
  }
  
  .mr-comment-actions-row {
    opacity: 1;
  }
}
</style>
