<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getArtistDetail } from '@/api/system'
import Table from '@/components/Table.vue'
import { useArtistStore } from '@/stores/modules/artist'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { fixUrl } from '@/utils'
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

const extractDominantColor = (imageUrl: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('Canvas context not available'))
        return
      }

      const sampleWidth = 50
      const sampleHeight = 50
      canvas.width = sampleWidth
      canvas.height = sampleHeight
      ctx.drawImage(img, 0, 0, sampleWidth, sampleHeight)
      const pixels = ctx.getImageData(0, 0, sampleWidth, sampleHeight).data

      let r = 0
      let g = 0
      let b = 0
      let count = 0
      for (let i = 0; i < pixels.length; i += 4) {
        const alpha = pixels[i + 3]
        if (alpha > 16) {
          r += pixels[i]
          g += pixels[i + 1]
          b += pixels[i + 2]
          count++
        }
      }

      if (!count) {
        reject(new Error('No valid pixel'))
        return
      }

      const avgR = Math.round(r / count)
      const avgG = Math.round(g / count)
      const avgB = Math.round(b / count)
      // 略微压暗，避免顶部过亮影响文字可读性
      const darken = 0.72
      const finalR = Math.max(0, Math.round(avgR * darken))
      const finalG = Math.max(0, Math.round(avgG * darken))
      const finalB = Math.max(0, Math.round(avgB * darken))
      resolve(`rgb(${finalR}, ${finalG}, ${finalB})`)
    }
    img.onerror = () => reject(new Error('Image load failed'))
    img.src = imageUrl
  })
}

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
      console.log('艺人详情数据:', artistData)
      console.log('歌曲列表:', artistData.songs)
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

    try {
      const color = await extractDominantColor(coverUrl)
      if (taskId === latestColorTaskId) {
        gradientColor.value = color
      }
    } catch {
      if (taskId === latestColorTaskId) {
        gradientColor.value = '#1e3a5f'
      }
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
  <div class="spotify-artist-page" :style="{ '--gradient-color': gradientColor }">
    <!-- Artist Header -->
    <div class="spotify-artist-header">
      <div class="spotify-artist-avatar">
        <img
          :src="artistAvatar"
          :alt="artistInfo?.artistName"
          class="spotify-artist-avatar-img"
          @error="handleArtistAvatarError"
        />
      </div>
      <div class="spotify-artist-info">
        <span class="spotify-artist-type">艺人</span>
        <h1 class="spotify-artist-name">{{ artistInfo?.artistName }}</h1>
        <div class="spotify-artist-meta">
          <span v-if="artistInfo?.birth" class="spotify-meta-item">
            生日：{{ formatBirth(artistInfo.birth) }}
          </span>
          <span v-if="artistInfo?.area" class="spotify-meta-item">
            地区：{{ artistInfo.area }}
          </span>
        </div>
        <p v-if="artistInfo?.introduction" class="spotify-artist-bio">
          {{ artistInfo.introduction }}
        </p>
      </div>
    </div>

    <!-- Songs Section -->
    <div class="spotify-artist-songs">
      <h2 class="spotify-songs-title">所有歌曲</h2>
      <div class="spotify-songs-table">
        <Table :data="artistInfo?.songs" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.spotify-artist-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(180deg, var(--gradient-color, #1e3a5f) 0%, var(--bg-surface, #121212) 300px);
}

.spotify-artist-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 48px 24px 24px;
  text-align: center;
}

.spotify-artist-avatar {
  width: 232px;
  height: 232px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.5);
}

.spotify-artist-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-artist-info {
  max-width: 600px;
}

.spotify-artist-type {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-base, #fff);
  margin-bottom: 8px;
  display: block;
}

.spotify-artist-name {
  font-size: 4rem;
  font-weight: 900;
  color: var(--text-base, #fff);
  line-height: 1.1;
  margin-bottom: 24px;
}

.spotify-artist-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.spotify-meta-item {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-artist-bio {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spotify-artist-songs {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
  display: flex;
  flex-direction: column;
}

.spotify-songs-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  margin-bottom: 16px;
  flex-shrink: 0;
}

.spotify-songs-table {
  flex: 1;
  min-height: 300px;
  overflow: hidden;
}

/* Light Theme */
:root:not(.dark) .spotify-artist-page {
  --bg-surface: #f0f0f0;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .spotify-artist-avatar {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

/* Responsive */
@media (max-width: 768px) {
  .spotify-artist-page {
    padding-bottom: 80px;
  }
  
  .spotify-artist-header {
    padding: 16px 16px;
    gap: 16px;
  }
  
  .spotify-artist-avatar {
    width: 140px;
    height: 140px;
  }
  
  .spotify-artist-name {
    font-size: 1.75rem;
    margin-bottom: 12px;
  }
  
  .spotify-artist-meta {
    gap: 8px;
  }
  
  .spotify-meta-item {
    font-size: 0.8125rem;
  }
  
  .spotify-artist-bio {
    font-size: 0.8125rem;
    -webkit-line-clamp: 2;
  }
  
  .spotify-artist-songs {
    padding: 0 12px 16px;
  }
  
  .spotify-songs-title {
    font-size: 1.25rem;
    margin-bottom: 12px;
  }
}
</style>
