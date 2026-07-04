<script setup lang="ts">
import type { SongDetail } from '@/api/interface'
import { ref, inject, type Ref, computed, watch } from 'vue'

const injectedActiveTab = inject<Ref<'lyric' | 'comment'>>('activeRightTab')
const closeDrawer = inject<() => void>('closeDrawer')
const goBackToLeft = inject<() => void>('goBackToLeft')
import { formatNumber, fixUrl } from '@/utils'
import coverImg from '@/assets/cover.png'
import { likeComment, addSongComment, getSongDetail, deleteComment } from '@/api/system'
import { ElMessage } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { Icon } from '@iconify/vue'

const router = useRouter()

const songDetail = inject<Ref<SongDetail | null>>('songDetail')
const userStore = UserStore()
const { currentTime, seek } = useAudioPlayer()

const activeTab = injectedActiveTab || ref<'lyric' | 'comment'>('lyric')
const lyricContainerRef = ref<HTMLElement | null>(null)

// 歌词解析
interface LyricLine {
  time: number
  text: string
}

const parseLyric = (lrc: string): LyricLine[] => {
  const lines = lrc.split('\n')
  const result: LyricLine[] = []
  const timeExp = /\[(\d{2}):(\d{2})(\.\d{2,3})?\]/

  for (const line of lines) {
    const matches = timeExp.exec(line)
    if (matches) {
      const minutes = parseInt(matches[1])
      const seconds = parseInt(matches[2])
      const milliseconds = matches[3] ? parseFloat(matches[3]) : 0
      const time = minutes * 60 + seconds + milliseconds
      const text = line.replace(timeExp, '').trim()
      if (text) {
        result.push({ time, text })
      }
    }
  }
  return result
}

const parsedLyrics = computed(() => {
  if (!songDetail.value?.lyric) return []
  return parseLyric(songDetail.value.lyric)
})

const currentLyricIndex = computed(() => {
  if (!parsedLyrics.value.length) return -1
  const time = currentTime.value
  return parsedLyrics.value.findIndex((line, index) => {
    const nextLine = parsedLyrics.value[index + 1]
    return time >= line.time && (!nextLine || time < nextLine.time)
  })
})

// 歌词滚动
watch(currentLyricIndex, (newIndex) => {
  if (newIndex > -1 && lyricContainerRef.value && activeTab.value === 'lyric') {
    const container = lyricContainerRef.value
    const lyricItems = container.querySelectorAll('.mr-lyric-line')
    const targetItem = lyricItems[newIndex] as HTMLElement
    
    if (targetItem && container) {
      const containerHeight = container.clientHeight
      const itemTop = targetItem.offsetTop
      const itemHeight = targetItem.clientHeight
      const scrollTo = itemTop - containerHeight / 2 + itemHeight / 2
      
      container.scrollTo({
        top: Math.max(0, scrollTo),
        behavior: 'smooth'
      })
    }
  }
})

// 获取当前用户名
const currentUsername = computed(() => userStore.userInfo?.username || '')

// 评论相关
const commentContent = ref('')
const maxLength = 180

// 对评论进行排序，最新的显示在前面
const comments = computed(() => {
  if (!songDetail.value?.comments) return []
  return [...songDetail.value.comments].sort(
      (a, b) => b.commentId - a.commentId
  )
})

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

  if (commentContent.value.trim().length > maxLength) {
    ElMessage.warning(`评论内容不能超过 ${maxLength} 字`)
    return
  }

  const songId = songDetail.value?.songId
  if (!songId) {
    ElMessage.warning('歌曲信息未加载完成，请稍后再试')
    return
  }

  try {
    const content = commentContent.value.trim()
    const res = await addSongComment({
      songId,
      content,
    })

    if (res.code === 0) {
      ElMessage.success('评论发布成功')
      commentContent.value = ''
      // 重新获取歌曲详情以更新评论列表
      const detailRes = await getSongDetail(songId)
      if (detailRes.code === 0 && detailRes.data) {
        songDetail.value = detailRes.data as unknown as SongDetail
      }
    } else {
      ElMessage.error(res.message || '评论发布失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '评论发布失败，请检查网络连接')
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

const goUserProfile = (userId?: number) => {
  if (!userId) return
  closeDrawer?.()
  router.push(`/profile/${userId}`)
}

const shareCurrentSong = () => {
  if (!songDetail.value?.songId) return
  closeDrawer?.()
  router.push({
    path: '/messages',
    query: {
      songId: songDetail.value.songId,
      shareAt: Date.now(),
    },
  })
}

// 处理点赞
const handleLike = async (comment: any) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    // 调用点赞接口
    const res = await likeComment(comment.commentId)
    if (res.code === 0) {
      // 更新评论的点赞数量
      if (songDetail.value && songDetail.value.comments) {
        const updatedComments = songDetail.value.comments.map((item) => {
          if (item.commentId === comment.commentId) {
            return {
              ...item,
              likeCount: item.likeCount + 1,
            }
          }
          return item
        })

        songDetail.value = {
          ...songDetail.value,
          comments: updatedComments,
        }
      }

      ElMessage.success('点赞成功')
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

// 删除评论
const handleDelete = async (comment: any) => {
  try {
    const res = await deleteComment(comment.commentId)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      // 重新获取歌曲详情以更新评论列表
      const songId = songDetail.value?.songId
      if (songId) {
        const detailRes = await getSongDetail(songId)
        if (detailRes.code === 0 && detailRes.data) {
          songDetail.value = detailRes.data as unknown as SongDetail
        }
      }
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}
</script>

<template>
  <div class="mr-lyrics-comments">
    <!-- Tab Switcher -->
    <div class="mr-tabs">
      <button
        class="mr-tab-back"
        @click="goBackToLeft?.()"
        aria-label="返回播放器"
      >
        <Icon icon="mdi:chevron-left" />
      </button>
      <div class="mr-tab-group">
        <button
          class="mr-tab"
          :class="{ 'mr-tab-active': activeTab === 'lyric' }"
          @click="activeTab = 'lyric'"
        >
          歌词
        </button>
        <button
          class="mr-tab"
          :class="{ 'mr-tab-active': activeTab === 'comment' }"
          @click="activeTab = 'comment'"
        >
          评论
        </button>
      </div>
      <div class="mr-tab-spacer"></div>
    </div>

    <!-- Lyrics View -->
    <div v-show="activeTab === 'lyric'" class="mr-lyrics-view" ref="lyricContainerRef">
      <div v-if="parsedLyrics.length > 0" class="mr-lyrics-content">
        <p
          v-for="(line, index) in parsedLyrics"
          :key="index"
          class="mr-lyric-line"
          :class="{ 'mr-lyric-active': index === currentLyricIndex }"
          @click="seek(line.time)"
        >
          {{ line.text }}
        </p>
      </div>
      <div v-else class="mr-empty">
        <Icon icon="mdi:music-note-outline" class="text-4xl mb-4 opacity-50" />
        <p>暂无歌词</p>
      </div>
    </div>

    <!-- Comments View -->
    <div v-show="activeTab === 'comment'" class="mr-comments-view">
      <div v-if="songDetail" class="mr-comments-content">
        <!-- Song Info -->
        <div class="mr-song-info">
          <div class="mr-info-item">
            <span class="mr-info-label">专辑</span>
            <span class="mr-info-value">{{ songDetail.album }}</span>
          </div>
          <div class="mr-info-item">
            <span class="mr-info-label">发行时间</span>
            <span class="mr-info-value">{{ formatDate(songDetail.releaseTime) }}</span>
          </div>
          <div class="mr-info-item">
            <button class="mr-share-btn" @click="shareCurrentSong">
              <Icon icon="mdi:share-variant-outline" />
              <span>分享歌曲给好友</span>
            </button>
          </div>
        </div>

        <!-- Comments List -->
        <div class="mr-comments-list">
          <h3 class="mr-comments-title">
            评论（{{ formatNumber(songDetail.comments?.length || 0) }}）
          </h3>

          <div v-if="comments.length > 0" class="mr-comments-items">
            <div v-for="comment in comments" :key="comment.commentId" class="mr-comment-item">
              <img
                :src="fixUrl(comment.userAvatar) || coverImg"
                alt="avatar"
                class="mr-comment-avatar"
                @click="goUserProfile(comment.userId)"
              />
              <div class="mr-comment-body">
                <div class="mr-comment-header">
                  <span class="mr-comment-username" @click="goUserProfile(comment.userId)">{{ comment.username }}</span>
                  <span class="mr-comment-time">{{ comment.createTime }}</span>
                </div>
                <p class="mr-comment-text">{{ comment.content }}</p>
                <div class="mr-comment-actions">
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
                    <span>{{ formatNumber(comment.likeCount) || '0' }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="mr-empty mr-empty-small">
            <p>暂无评论，快来抢沙发吧~</p>
          </div>
        </div>

      </div>
      <div v-else class="mr-empty">
        <p>暂无歌曲信息</p>
      </div>

      <!-- Comment Input -->
      <div class="mr-comment-input">
        <div class="mr-input-wrapper">
          <Icon icon="mdi:message-outline" class="mr-input-icon" />
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 3 }"
            :maxlength="maxLength"
            placeholder="说点什么..."
            resize="none"
            class="mr-input"
          />
          <span class="mr-char-count">{{ commentContent.length }}/{{ maxLength }}</span>
        </div>
        <button
          @click="handleComment"
          :disabled="!commentContent.trim()"
          class="mr-submit-btn"
        >
          <Icon icon="mdi:send" class="mr-submit-icon" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mr-lyrics-comments {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  position: relative;
}

.mr-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.mr-tab-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.7);
  font-size: 1.25rem;
  cursor: pointer;
  transition: color 200ms ease, background-color 200ms ease;
  flex-shrink: 0;
}

.mr-tab-back:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.mr-tab-group {
  display: flex;
  gap: 8px;
}

.mr-tab-spacer {
  width: 36px;
  flex-shrink: 0;
}

.mr-tab {
  padding: 8px 24px;
  background: transparent;
  border: none;
  border-radius: 500px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 200ms ease;
}

.mr-tab:hover {
  color: rgba(255, 255, 255, 0.8);
}

.mr-tab-active {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.mr-lyrics-view {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  mask-image: linear-gradient(to bottom, transparent 0%, black 10%, black 90%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 10%, black 90%, transparent 100%);
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.mr-lyrics-view::-webkit-scrollbar {
  display: none;
}

.mr-lyrics-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  gap: 16px;
}

.mr-lyric-line {
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  font-size: 1rem;
  line-height: 1.6;
  cursor: pointer;
  transition: all 300ms ease;
  padding: 4px 16px;
  border-radius: 4px;
}

.mr-lyric-line:hover {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.05);
}

.mr-lyric-active {
  color: #fff;
  font-size: 1.25rem;
  font-weight: 700;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.3);
}

.mr-comments-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.mr-comments-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.mr-comments-content::-webkit-scrollbar {
  width: 4px;
}

.mr-comments-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.mr-song-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-bottom: 24px;
}

.mr-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mr-info-label {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.mr-info-value {
  font-size: 0.875rem;
  color: #fff;
  font-weight: 500;
}

.mr-share-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid color-mix(in srgb, var(--text-base, #fff) 70%, transparent);
  background: transparent;
  color: var(--mr-accent);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.75rem;
  cursor: pointer;
}

.mr-comments-list {
  flex: 1;
}

.mr-comments-title {
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
}

.mr-comments-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mr-comment-item {
  display: flex;
  gap: 12px;
}

.mr-comment-avatar {
  width: 36px;
  height: 36px;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.mr-comment-username {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}

.mr-comment-time {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.4);
}

.mr-comment-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.5;
  margin-bottom: 8px;
}

.mr-comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 200ms ease;
}

.mr-comment-item:hover .mr-comment-actions {
  opacity: 1;
}

.mr-comment-action {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.75rem;
  cursor: pointer;
  transition: color 200ms ease;
}

.mr-comment-action:hover {
  color: #fff;
}

.mr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
}

.mr-empty-small {
  height: auto;
  padding: 32px 0;
}

.mr-comment-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  margin-top: 16px;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.mr-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.mr-input-icon {
  font-size: 1.25rem;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
  flex-shrink: 0;
}

.mr-input {
  flex: 1;
}

.mr-input.el-textarea {
  background: transparent !important;
}

.mr-input.el-textarea :deep(.el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  border-radius: 12px;
  box-shadow: none !important;
  padding: 8px 12px;
  color: #fff !important;
  font-size: 0.875rem;
  line-height: 1.5;
}

.mr-input.el-textarea :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

.mr-input.el-textarea :deep(.el-textarea__inner:focus) {
  background: transparent !important;
}

.mr-char-count {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8px;
  flex-shrink: 0;
  min-width: 36px;
  text-align: right;
}

.mr-submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: var(--mr-accent);
  border: none;
  border-radius: 50%;
  color: #000;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
  flex-shrink: 0;
}

.mr-submit-btn:hover:not(:disabled) {
  transform: scale(1.06);
  background: var(--mr-accent-hover);
}

.mr-submit-btn:disabled {
  background: #535353;
  color: #b3b3b3;
  cursor: not-allowed;
}

.mr-submit-icon {
  font-size: 1.25rem;
}

@media (min-width: 768px) {
  .mr-lyrics-comments {
    padding: 24px;
  }

  .mr-lyric-line {
    font-size: 1.125rem;
  }

  .mr-lyric-active {
    font-size: 1.5rem;
  }
}


</style>

<style>
</style>