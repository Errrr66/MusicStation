<script setup lang="ts">
import type { SongDetail } from '@/api/interface'
import { ref, inject, type Ref, computed, watch } from 'vue'
import { formatNumber, fixUrl } from '@/utils'
import coverImg from '@/assets/cover.png'
import { likeComment, addSongComment, getSongDetail, deleteComment } from '@/api/system'
import { ElMessage } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { Icon } from '@iconify/vue'

const songDetail = inject<Ref<SongDetail | null>>('songDetail')
const userStore = UserStore()
const { currentTime, seek } = useAudioPlayer()

const activeTab = ref<'lyric' | 'comment'>('lyric')
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
    const feedbackItem = lyricContainerRef.value.querySelector(`p:nth-child(${newIndex + 1})`) as HTMLElement
    if (feedbackItem) {
      feedbackItem.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
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

  try {
    const songId = songDetail.value?.songId
    if (!songId) return

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
      ElMessage.error('评论发布失败')
    }
  } catch (error) {
    ElMessage.error('评论发布失败')
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
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
  <div class="spotify-lyrics-comments">
    <!-- Tab Switcher -->
    <div class="spotify-tabs">
      <button
        class="spotify-tab"
        :class="{ 'spotify-tab-active': activeTab === 'lyric' }"
        @click="activeTab = 'lyric'"
      >
        歌词
      </button>
      <button
        class="spotify-tab"
        :class="{ 'spotify-tab-active': activeTab === 'comment' }"
        @click="activeTab = 'comment'"
      >
        评论
      </button>
    </div>

    <!-- Lyrics View -->
    <div v-show="activeTab === 'lyric'" class="spotify-lyrics-view" ref="lyricContainerRef">
      <div v-if="parsedLyrics.length > 0" class="spotify-lyrics-content">
        <p
          v-for="(line, index) in parsedLyrics"
          :key="index"
          class="spotify-lyric-line"
          :class="{ 'spotify-lyric-active': index === currentLyricIndex }"
          @click="seek(line.time)"
        >
          {{ line.text }}
        </p>
      </div>
      <div v-else class="spotify-empty">
        <Icon icon="mdi:music-note-outline" class="text-4xl mb-4 opacity-50" />
        <p>暂无歌词</p>
      </div>
    </div>

    <!-- Comments View -->
    <div v-show="activeTab === 'comment'" class="spotify-comments-view">
      <div v-if="songDetail" class="spotify-comments-content">
        <!-- Song Info -->
        <div class="spotify-song-info">
          <div class="spotify-info-item">
            <span class="spotify-info-label">专辑</span>
            <span class="spotify-info-value">{{ songDetail.album }}</span>
          </div>
          <div class="spotify-info-item">
            <span class="spotify-info-label">发行时间</span>
            <span class="spotify-info-value">{{ formatDate(songDetail.releaseTime) }}</span>
          </div>
        </div>

        <!-- Comments List -->
        <div class="spotify-comments-list">
          <h3 class="spotify-comments-title">
            评论（{{ formatNumber(songDetail.comments?.length || 0) }}）
          </h3>

          <div v-if="comments.length > 0" class="spotify-comments-items">
            <div v-for="comment in comments" :key="comment.commentId" class="spotify-comment-item">
              <img
                :src="fixUrl(comment.userAvatar) || coverImg"
                alt="avatar"
                class="spotify-comment-avatar"
              />
              <div class="spotify-comment-body">
                <div class="spotify-comment-header">
                  <span class="spotify-comment-username">{{ comment.username }}</span>
                  <span class="spotify-comment-time">{{ comment.createTime }}</span>
                </div>
                <p class="spotify-comment-text">{{ comment.content }}</p>
                <div class="spotify-comment-actions">
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
                    <span>{{ formatNumber(comment.likeCount) || '0' }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="spotify-empty spotify-empty-small">
            <p>暂无评论，快来抢沙发吧~</p>
          </div>
        </div>
      </div>
      <div v-else class="spotify-empty">
        <p>暂无歌曲信息</p>
      </div>

      <!-- Comment Input -->
      <div class="spotify-comment-input">
        <div class="spotify-input-wrapper">
          <Icon icon="mdi:message-outline" class="spotify-input-icon" />
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 3 }"
            :maxlength="maxLength"
            placeholder="说点什么..."
            resize="none"
            class="spotify-input"
          />
          <span class="spotify-char-count">{{ commentContent.length }}/{{ maxLength }}</span>
        </div>
        <button
          @click="handleComment"
          :disabled="!commentContent.trim()"
          class="spotify-submit-btn"
        >
          <Icon icon="mdi:send" class="spotify-submit-icon" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spotify-lyrics-comments {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}

.spotify-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.spotify-tab {
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

.spotify-tab:hover {
  color: rgba(255, 255, 255, 0.8);
}

.spotify-tab-active {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.spotify-lyrics-view {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  mask-image: linear-gradient(to bottom, transparent 0%, black 10%, black 90%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 10%, black 90%, transparent 100%);
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.spotify-lyrics-view::-webkit-scrollbar {
  display: none;
}

.spotify-lyrics-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  gap: 16px;
}

.spotify-lyric-line {
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  font-size: 1rem;
  line-height: 1.6;
  cursor: pointer;
  transition: all 300ms ease;
  padding: 4px 16px;
  border-radius: 4px;
}

.spotify-lyric-line:hover {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.05);
}

.spotify-lyric-active {
  color: #fff;
  font-size: 1.25rem;
  font-weight: 700;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.3);
}

.spotify-comments-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.spotify-comments-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.spotify-comments-content::-webkit-scrollbar {
  width: 4px;
}

.spotify-comments-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}

.spotify-song-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  margin-bottom: 24px;
}

.spotify-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.spotify-info-label {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.spotify-info-value {
  font-size: 0.875rem;
  color: #fff;
  font-weight: 500;
}

.spotify-comments-list {
  flex: 1;
}

.spotify-comments-title {
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
}

.spotify-comments-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spotify-comment-item {
  display: flex;
  gap: 12px;
}

.spotify-comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.spotify-comment-body {
  flex: 1;
  min-width: 0;
}

.spotify-comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.spotify-comment-username {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #fff;
}

.spotify-comment-time {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.4);
}

.spotify-comment-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.5;
  margin-bottom: 8px;
}

.spotify-comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 200ms ease;
}

.spotify-comment-item:hover .spotify-comment-actions {
  opacity: 1;
}

.spotify-comment-action {
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

.spotify-comment-action:hover {
  color: #fff;
}

.spotify-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
}

.spotify-empty-small {
  height: auto;
  padding: 32px 0;
}

.spotify-comment-input {
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

.spotify-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.spotify-input-icon {
  font-size: 1.25rem;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
  flex-shrink: 0;
}

.spotify-input {
  flex: 1;
}

.spotify-input :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  border-radius: 12px;
  box-shadow: none;
  padding: 8px 12px;
  color: #fff;
  font-size: 0.875rem;
  line-height: 1.5;
}

.spotify-input :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.spotify-input :deep(.el-textarea__inner:focus) {
  background: rgba(255, 255, 255, 0.05);
}

.spotify-char-count {
  font-size: 0.6875rem;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8px;
  flex-shrink: 0;
  min-width: 36px;
  text-align: right;
}

.spotify-submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: #1db954;
  border: none;
  border-radius: 50%;
  color: #000;
  cursor: pointer;
  transition: transform 33ms ease, background-color 200ms ease;
  flex-shrink: 0;
}

.spotify-submit-btn:hover:not(:disabled) {
  transform: scale(1.06);
  background: #1ed760;
}

.spotify-submit-btn:disabled {
  background: #535353;
  color: #b3b3b3;
  cursor: not-allowed;
}

.spotify-submit-icon {
  font-size: 1.25rem;
}

@media (min-width: 768px) {
  .spotify-lyrics-comments {
    padding: 24px;
  }
  
  .spotify-lyric-line {
    font-size: 1.125rem;
  }
  
  .spotify-lyric-active {
    font-size: 1.5rem;
  }
}
</style>