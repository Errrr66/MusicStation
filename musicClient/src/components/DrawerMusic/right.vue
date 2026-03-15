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
  <div class="h-full w-full p-4 md:p-6 overflow-hidden md:mr-16 flex flex-col">
    <!-- 顶部 Tab 切换 -->
    <div class="flex justify-center gap-6 mb-6 flex-shrink-0">
      <button
          class="text-lg font-bold transition-colors"
          :class="activeTab === 'lyric' ? 'text-white' : 'text-white/50 hover:text-white'"
          @click="activeTab = 'lyric'"
      >
        歌词
      </button>
      <button
          class="text-lg font-bold transition-colors"
          :class="activeTab === 'comment' ? 'text-white' : 'text-white/50 hover:text-white'"
          @click="activeTab = 'comment'"
      >
        评论
      </button>
    </div>

    <!-- 歌词视图 -->
    <div v-show="activeTab === 'lyric'" class="flex-1 overflow-y-auto overflow-x-hidden no-scrollbar mask-image-gradient min-h-0 w-full" ref="lyricContainerRef">
      <div v-if="parsedLyrics.length > 0" class="flex flex-col items-center py-40 space-y-3 md:space-y-6 w-full px-4">
        <p
            v-for="(line, index) in parsedLyrics"
            :key="index"
            class="text-center transition-all duration-300 cursor-pointer hover:text-white break-words w-full"
            :class="[
            index === currentLyricIndex
              ? 'text-white text-base md:text-2xl font-bold'
              : 'text-white/60 text-xs md:text-lg'
          ]"
            @click="seek(line.time)"
        >
          {{ line.text }}
        </p>
      </div>
      <div v-else class="flex flex-col items-center justify-center h-full text-white/50">
        <p>暂无歌词</p>
      </div>
    </div>

    <!-- 评论视图 -->
    <div v-show="activeTab === 'comment'" class="flex-1 flex flex-col overflow-hidden min-h-0 w-full">
      <!-- 关键修复：将 v-if 和 v-else 放在同一层级，确保相邻 -->
      <div v-if="songDetail" class="flex flex-col h-full overflow-hidden relative">
        <!-- 可滚动区域：歌曲信息 + 评论列表 -->
        <div class="flex-1 overflow-y-auto overflow-x-hidden pr-2 no-scrollbar pb-4">
          <!-- 歌曲信息 -->
          <div class="space-y-2">
            <h3 class="text-xl font-semibold text-white">歌曲信息</h3>
            <div class="grid grid-cols-2 gap-4 text-sm text-white/60">
              <div>
                <span class="text-white">专辑：</span>
                {{ songDetail.album }}
              </div>
              <div>
                <span class="text-white">发行时间：</span>
                {{ formatDate(songDetail.releaseTime) }}
              </div>
            </div>
          </div>

          <!-- 评论列表区域 -->
          <div class="space-y-4 mt-8">
            <h3 class="text-xl font-semibold text-white">
              评论（{{ formatNumber(songDetail.comments?.length || 0) }}）
            </h3>

            <div v-if="comments.length > 0" class="space-y-6 pb-4">
              <template v-for="comment in comments" :key="comment.commentId">
                <div class="flex gap-3">
                  <!-- Avatar -->
                  <div class="w-9 h-9 rounded-full overflow-hidden flex-shrink-0 border border-white/10">
                    <img
                        :src="fixUrl(comment.userAvatar) || coverImg"
                        alt="avatar"
                        class="w-full h-full object-cover"
                    />
                  </div>
                  <!-- Content Right -->
                  <div class="flex-1 min-w-0 flex flex-col">
                    <!-- Header: User & Like -->
                    <div class="flex justify-between items-start">
                      <div class="flex flex-col gap-0.5">
                         <span class="text-sm text-white/90 font-medium leading-none">{{ comment.username }}</span>
                         <span class="text-[11px] text-white/50">{{ comment.createTime }}</span>
                      </div>

                      <div class="flex items-center gap-4">
                        <button
                            v-if="comment.username === currentUsername"
                            class="text-white/60 hover:text-red-500 transition-colors"
                            @click="handleDelete(comment)"
                        >
                          <icon-material-symbols:delete-outline class="text-lg" />
                        </button>
                        <button
                            class="flex items-center gap-1 text-white/60 hover:text-red-500 transition-colors"
                            @click="handleLike(comment)"
                        >
                          <span class="text-xs font-medium">{{ formatNumber(comment.likeCount) || '0' }}</span>
                          <icon-material-symbols:thumb-up class="text-lg" />
                        </button>
                      </div>
                    </div>

                    <!-- Comment Body -->
                    <p class="text-[15px] text-white/95 mt-2 leading-relaxed break-words font-normal">
                      {{ comment.content }}
                    </p>
                  </div>
                </div>
              </template>
            </div>
            <div v-else class="text-center py-8 text-white/50">
              <p>暂无评论，快来抢沙发吧~</p>
            </div>
          </div>
        </div>

        <!-- 固定底部的评论输入框 -->
        <div class="flex-shrink-0 p-3 z-10 w-full bg-white/5 backdrop-blur-xl border border-white/5 rounded-2xl">
            <div class="flex gap-3 items-end px-3">
              <el-input
                  v-model="commentContent"
                  type="textarea"
                  :rows="1"
                  :autosize="{ minRows: 1, maxRows: 3 }"
                  :maxlength="maxLength"
                  placeholder="说点什么..."
                  resize="none"
                  class="flex-1 !bg-transparent custom-input"
              />
              <button
                  @click="handleComment"
                  :disabled="!commentContent.trim()"
                  class="px-5 h-9 bg-primary text-primary-foreground rounded-full text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-primary/90 transition-all flex items-center justify-center shrink-0 mb-0.5 shadow-sm active:scale-95"
              >
                发布
              </button>
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
  border-radius: 24px;
}

:deep(.el-textarea__inner) {
  border-radius: 20px !important;
  background-color: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
  padding: 8px 16px;
  color: white !important; /* Force white text */
}

/* Placeholder styling */
:deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}
</style>