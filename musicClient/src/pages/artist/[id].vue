<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getArtistDetail } from '@/api/system'
import Table from '@/components/Table.vue'
import { useArtistStore } from '@/stores/modules/artist'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { fixUrl } from '@/utils'
import { extractDominantColor } from '@/utils/imageUtils'
import defaultArtistAvatar from '@/assets/user.jpg'

interface ArtistDetailResponse {
  artistId: number
  artistName: string
  avatar: string
  birth: string
  area: string
  introduction: string
  songs: any[]
}

const route = useRoute()
const artistStore = useArtistStore()
// 艺人数据
const artistInfo = computed(() => artistStore.artistInfo)
const artistAvatar = computed(
  () => fixUrl(artistInfo.value?.avatar) || defaultArtistAvatar
)
const gradientColor = ref('#1e3a5f')
let latestColorTaskId = 0

const firstSongCover = computed(() => {
  const firstSong = artistInfo.value?.songs?.[0] as { coverUrl?: string } | undefined
  return fixUrl(firstSong?.coverUrl)
})

const handleArtistAvatarError = (event: Event) => {
  const target = event.target as HTMLImageElement | null
  if (target && target.src !== defaultArtistAvatar) {
    target.src = defaultArtistAvatar
  }
}

const fetchArtistDetail = async () => {
  const id = route.params.id
  const numericId = parseInt(id.toString())

  try {
    artistStore.setArtistInfo(null) // 清空之前的数据
    const res = await getArtistDetail(numericId)

    if (res.code === 0 && res.data) {
      const artistData = res.data as ArtistDetailResponse
      artistStore.setArtistInfo({
        artistId: artistData.artistId,
        artistName: artistData.artistName || '未知艺人',
        avatar: fixUrl(artistData.avatar) || '',
        birth: artistData.birth || '',
        area: artistData.area || '未知',
        introduction: artistData.introduction || '暂无简介',
        songs: artistData.songs || [],
      })
    } else {
      ElMessage.error(res.message || '获取艺人信息失败')
    }
  } catch (error) {
    console.error('获取艺人详情失败:', error)
    ElMessage.error('获取艺人信息失败，请稍后重试')
  }
}

watch(
  () => route.params.id,
  () => {
    fetchArtistDetail()
  },
  { immediate: true }
)

watch(
  firstSongCover,
  async (coverUrl) => {
    const taskId = ++latestColorTaskId
    if (!coverUrl) {
      gradientColor.value = '#1e3a5f'
      return
    }

    // extractDominantColor 永远 resolve，失败时返回默认色，无需 try/catch
    const color = await extractDominantColor(coverUrl)
    if (taskId === latestColorTaskId) {
      gradientColor.value = color
    }
  },
  { immediate: true }
)

// 格式化生日
const formatBirth = (birth: string) => {
  if (!birth) return ''
  return new Date(birth).toLocaleDateString()
}
</script>

<template>
  <div class="mr-artist-page" :style="{ '--gradient-color': gradientColor }">
    <!-- Artist Header -->
    <div class="mr-artist-header">
      <div class="mr-artist-avatar">
        <img
          :src="artistAvatar"
          :alt="artistInfo?.artistName"
          class="mr-artist-avatar-img"
          @error="handleArtistAvatarError"
        />
      </div>
      <div class="mr-artist-info">
        <span class="mr-artist-type">艺人</span>
        <h1 class="mr-artist-name">{{ artistInfo?.artistName }}</h1>
        <div class="mr-artist-meta">
          <span v-if="artistInfo?.birth" class="mr-meta-item">
            生日：{{ formatBirth(artistInfo.birth) }}
          </span>
          <span v-if="artistInfo?.area" class="mr-meta-item">
            地区：{{ artistInfo.area }}
          </span>
        </div>
        <p v-if="artistInfo?.introduction" class="mr-artist-bio">
          {{ artistInfo.introduction }}
        </p>
      </div>
    </div>

    <!-- Songs Section -->
    <div class="mr-artist-songs">
      <h2 class="mr-songs-title">所有歌曲</h2>
      <div class="mr-songs-table">
        <Table :data="artistInfo?.songs" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.mr-artist-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 300px);
}

.mr-artist-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 48px 24px 24px;
  text-align: center;
}

.mr-artist-avatar {
  width: 232px;
  height: 232px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.mr-artist-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mr-artist-info {
  max-width: 600px;
}

.mr-artist-type {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-base, #fff);
  margin-bottom: 8px;
  display: block;
}

.mr-artist-name {
  font-size: 4rem;
  font-weight: 900;
  color: var(--text-base, #fff);
  line-height: 1.1;
  margin-bottom: 24px;
}

.mr-artist-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.mr-meta-item {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-artist-bio {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mr-artist-songs {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
  display: flex;
  flex-direction: column;
}

.mr-songs-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 16px;
  flex-shrink: 0;
}

.mr-songs-table {
  flex: 1;
  min-height: 300px;
  overflow: hidden;
}

/* Light Theme */
:root:not(.dark) .mr-artist-page {
  --bg-surface: #f0f0f0;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .mr-artist-avatar {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

/* Responsive */
@media (max-width: 768px) {
  .mr-artist-page {
    padding-bottom: 140px;
  }
  
  .mr-artist-header {
    padding: 16px 16px;
    gap: 16px;
  }
  
  .mr-artist-avatar {
    width: 140px;
    height: 140px;
  }
  
  .mr-artist-name {
    font-size: 1.75rem;
    margin-bottom: 12px;
  }
  
  .mr-artist-meta {
    gap: 8px;
  }
  
  .mr-meta-item {
    font-size: 0.8125rem;
  }
  
  .mr-artist-bio {
    font-size: 0.8125rem;
    -webkit-line-clamp: 2;
  }
  
  .mr-artist-songs {
    padding: 0 12px 16px;
  }
  
  .mr-songs-title {
    font-size: 1.25rem;
    margin-bottom: 12px;
  }
}
</style>
