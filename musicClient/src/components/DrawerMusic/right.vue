<script setup lang="ts">
import type { SongDetail } from '@/api/interface'
import { ref, inject, type Ref, computed, watch } from 'vue'
import { formatNumber, fixUrl } from '@/utils'
import coverImg from '@/assets/cover.png'
import { likeComment, addSongComment, getSongDetail, deleteComment } from '@/api/system'
import { ElMessage } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'

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
  <div class="h-full w-full p-2 md:p-6 overflow-hidden md:mr-16 flex flex-col">
    <!-- 顶部 Tab 切换 -->
    <div class="flex justify-center gap-6 mb-6 flex-shrink-0">
      <button
          class="text-lg font-bold transition-colors"
          :class="activeTab === 'lyric' ? 'text-primary-foreground' : 'text-muted-foreground hover:text-primary-foreground'"
          @click="activeTab = 'lyric'"
      >
        歌词
      </button>
      <button
          class="text-lg font-bold transition-colors"
          :class="activeTab === 'comment' ? 'text-primary-foreground' : 'text-muted-foreground hover:text-primary-foreground'"
          @click="activeTab = 'comment'"
      >
        评论
      </button>
    </div>

    <!-- 歌词视图 -->
    <div v-show="activeTab === 'lyric'" class="flex-1 overflow-visible md:overflow-y-auto no-scrollbar mask-image-gradient min-h-[50vh] md:min-h-0" ref="lyricContainerRef">
      <div v-if="parsedLyrics.length > 0" class="flex flex-col items-center py-40 space-y-6">
        <p
            v-for="(line, index) in parsedLyrics"
            :key="index"
            class="text-center transition-all duration-300 cursor-pointer hover:text-white"
            :class="[
            index === currentLyricIndex
              ? 'text-primary-foreground text-2xl font-bold scale-110'
              : 'text-muted-foreground/60 text-lg'
          ]"
            @click="seek(line.time)"
        >
          {{ line.text }}
        </p>
      </div>
      <div v-else class="flex flex-col items-center justify-center h-full text-muted-foreground">
        <p>暂无歌词</p>
      </div>
    </div>

    <!-- 评论视图 -->
    <div v-show="activeTab === 'comment'" class="flex-1 overflow-visible md:overflow-y-auto pr-2 min-h-[50vh] md:min-h-0">
      <!-- 关键修复：将 v-if 和 v-else 放在同一层级，确保相邻 -->
      <div v-if="songDetail" class="space-y-6">
        <!-- 歌曲信息 -->
        <div class="space-y-2">
          <h3 class="text-xl font-semibold text-primary-foreground">歌曲信息</h3>
          <div class="grid grid-cols-2 gap-4 text-sm text-muted-foreground">
            <div>
              <span class="text-primary-foreground">专辑：</span>
              {{ songDetail.album }}
            </div>
            <div>
              <span class="text-primary-foreground">发行时间：</span>
              {{ formatDate(songDetail.releaseTime) }}
            </div>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="space-y-4">
          <h3 class="text-xl font-semibold text-primary-foreground mt-12">
            评论（{{ formatNumber(songDetail.comments?.length || 0) }}）
          </h3>

          <!-- 评论输入框 -->
          <div class="mb-4">
            <div class="flex items-start gap-3">
              <div class="flex-1">
                <el-input
                    v-model="commentContent"
                    type="textarea"
                    :rows="4"
                    :maxlength="maxLength"
                    placeholder="说点什么吧"
                    resize="none"
                    show-word-limit
                />
                <div class="flex justify-end items-center mt-4">
                  <button
                      @click="handleComment"
                      :disabled="!commentContent.trim()"
                      class="px-6 py-1.5 bg-primary text-white rounded-full text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-primary/90 transition-colors"
                  >
                    发布
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="space-y-4">
            <template v-for="comment in comments" :key="comment.commentId">
              <div class="flex gap-3 group">
                <div
                    class="w-10 h-10 rounded-full overflow-hidden flex-shrink-0 mt-0.5"
                >
                  <img
                      :src="fixUrl(comment.userAvatar) || coverImg"
                      alt="avatar"
                      class="w-full h-full object-cover"
                  />
                </div>
                <div class="flex-1">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-blue-500">{{
                        comment.username
                      }}</span>
                  </div>
                  <p class="text-sm mt-1 mb-2">{{ comment.content }}</p>
                  <div
                      class="flex items-center justify-between text-sm text-gray-400"
                  >
                    <span class="text-xs">{{ comment.createTime }}</span>
                    <div class="flex items-center gap-4">
                      <!-- 如果是用户自己的评论，显示删除按钮 -->
                      <button
                          v-if="comment.username === currentUsername"
                          class="flex items-center gap-1 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                          @click="handleDelete(comment)"
                      >
                        <icon-material-symbols:delete-outline />
                        <span>删除</span>
                      </button>
                      <button
                          class="flex items-center gap-1 hover:text-gray-600"
                          @click="handleLike(comment)"
                      >
                        <span>{{ formatNumber(comment.likeCount) }}</span>
                        <icon-material-symbols:thumb-up />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="border-b border-gray-300/70"></div>
            </template>
          </div>
          <div v-else class="text-center py-8 text-gray-500">
            <p>暂无评论，快来抢沙发吧~</p>
          </div>
        </div>
      </div>
      <div v-else class="flex items-center justify-center h-full">
        <el-empty description="暂无歌曲信息" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}
/* 蒙版效果，上下淡出 */
.mask-image-gradient {
  mask-image: linear-gradient(to bottom,
  transparent 0%,
  black 15%,
  black 85%,
  transparent 100%
  );
  -webkit-mask-image: linear-gradient(to bottom,
  transparent 0%,
  black 15%,
  black 85%,
  transparent 100%
  );
}

.el-button {
  --el-button-hover-text-color: var(--el-color-primary);
  --el-button-hover-bg-color: transparent;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-textarea__inner) {
  border-radius: 12px !important;
}
</style>