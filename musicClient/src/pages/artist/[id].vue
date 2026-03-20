<script setup lang="ts">
import { computed, watch } from 'vue'
import { getArtistDetail } from '@/api/system'
import Table from '@/components/Table.vue'
import { useArtistStore } from '@/stores/modules/artist'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { fixUrl } from '@/utils'

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

// 格式化生日
const formatBirth = (birth: string) => {
  if (!birth) return ''
  return new Date(birth).toLocaleDateString()
}
</script>

<template>
  <div class="spotify-artist-page">
    <!-- Artist Header -->
    <div class="spotify-artist-header">
      <div class="spotify-artist-avatar">
        <img
          :src="artistInfo?.avatar"
          :alt="artistInfo?.artistName"
          class="spotify-artist-avatar-img"
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
  --bg-surface: #ffffff;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --gradient-color: #e8f4f8;
}

:root:not(.dark) .spotify-artist-avatar {
  box-shadow: 0 4px 60px rgba(0, 0, 0, 0.15);
}

/* Responsive */
@media (max-width: 768px) {
  .spotify-artist-header {
    padding: 24px 16px;
  }
  
  .spotify-artist-avatar {
    width: 180px;
    height: 180px;
  }
  
  .spotify-artist-name {
    font-size: 2rem;
  }
  
  .spotify-artist-songs {
    padding: 0 16px 16px;
  }
}
</style>
