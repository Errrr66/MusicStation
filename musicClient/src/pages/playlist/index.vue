<script setup lang="ts">
import { getAllPlaylists, getFavoritePlaylists } from '@/api/system'
import coverImg from '@/assets/cover.png'
import { ElNotification } from 'element-plus'
import { fixUrl } from '@/utils'

// 路由
const router = useRouter()
// 播放列表
const playlists = ref([])

const selected = ref('all')
// 搜索关键词
const searchKeyword = ref('')

// 滚动条显示状态
const showScrollbar = ref(false)
const contentRef = ref<HTMLElement | null>(null)
let scrollTimeout: ReturnType<typeof setTimeout> | null = null

const handleScroll = () => {
  showScrollbar.value = true
  if (scrollTimeout) {
    clearTimeout(scrollTimeout)
  }
  scrollTimeout = setTimeout(() => {
    showScrollbar.value = false
  }, 1500)
}

// 歌单类型列表
const playlistsList = [
  { name: '精选歌单', value: 'all' },
  { name: '我的收藏', value: 'favorite' },
]
// 歌单tag
const playTags = ref<{ name: string }[]>([])
const selectedTag = ref('全部')

// 分页组件状态
const currentPage = ref(1) // 当前页
const pageSize = ref(12) // 每页显示的数量
const state = reactive({
  size: 'default',
  disabled: false,
  background: false,
  layout: 'total, sizes, prev, pager, next, jumper',
  total: 0,
  pageSizes: [12, 24, 36, 48],
})

// 监听分页变化
const handleSizeChange = () => {
  getPlaylists()
}
// 监听当前页变化
const handleCurrentChange = () => {
  getPlaylists()
}

// 获取歌单
const getPlaylists = async () => {
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      title: searchKeyword.value || null,
      style: selectedTag.value === '全部' ? null : selectedTag.value,
    }

    let res
    if (selected.value === 'favorite') {
      res = await getFavoritePlaylists(params)
    } else {
      res = await getAllPlaylists(params)
    }

    if (res.code === 0) {
      playlists.value = res.data.items.map((item) => ({
        id: item.playlistId,
        name: item.title,
        coverImgUrl: fixUrl(item.coverUrl) || coverImg,
        creator: {
          nickname: selected.value === 'favorite' ? '' : '',
          avatarUrl: coverImg,
        },
        playCount: 0,
        subscribedCount: 0,
      }))
      state.total = res.data.total
    } else {
      ElNotification({
        type: 'error',
        message: '获取歌单列表失败',
        duration: 2000,
      })
    }
  } catch (error) {
    ElNotification({
      type: 'error',
      message: '获取歌单列表失败',
      duration: 2000,
    })
  }
}

// 选择歌单
const selectPlaylist = (playlist: string) => {
  selected.value = playlist
  getPlaylists()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1 // 重置页码
  getPlaylists()
}

// 处理搜索框按下回车
const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSearch()
  }
}


onMounted(() => {
  // 初始化歌单标签
  playTags.value = [
    { name: '全部' },
    { name: '节奏布鲁斯' },
    { name: '欧美流行' },
    { name: '华语流行' },
    { name: '粤语流行' },
    { name: '国风流行' },
    { name: '韩语流行' },
    { name: '日本流行' },
    { name: '嘻哈说唱' },
    { name: '非洲节拍' },
    { name: '原声带' },
    { name: '轻音乐' },
    { name: '摇滚' },
    { name: '朋克' },
    { name: '电子' },
    { name: '国风' },
    { name: '乡村' },
    { name: '古典' },
  ]
  getPlaylists()
})
</script>
<template>
  <div class="spotify-playlist-page">
    <!-- Header -->
    <div class="spotify-playlist-header">
      <div class="spotify-search-wrapper">
        <Icon icon="mdi:magnify" class="spotify-search-icon" />
        <input
          v-model="searchKeyword"
          @keydown="handleKeyPress"
          class="spotify-search-input"
          placeholder="搜索歌单..."
          type="search"
        />
      </div>
      <el-select class="spotify-tag-select" v-model="selectedTag" @change="getPlaylists">
        <el-option
          v-for="item in playTags"
          :key="item.name"
          :label="item.name"
          :value="item.name"
        />
      </el-select>
    </div>

    <!-- Tabs -->
    <div class="spotify-tabs">
      <button
        v-for="playlist in playlistsList"
        :key="playlist.value"
        @click="selectPlaylist(playlist.value)"
        class="spotify-tab"
        :class="{ 'spotify-tab-active': selected === playlist.value }"
      >
        {{ playlist.name }}
      </button>
    </div>

    <!-- Playlist Grid -->
    <div 
      ref="contentRef"
      class="spotify-playlist-content"
      :class="{ 'show-scrollbar': showScrollbar }"
      @scroll="handleScroll"
    >
      <div class="spotify-playlist-grid">
        <div
          v-for="playlist in playlists"
          :key="playlist.id"
          class="spotify-playlist-card"
          @click="router.push('/playlist/' + playlist.id)"
        >
          <div class="spotify-playlist-cover">
            <el-image
              lazy
              :alt="playlist.name"
              class="spotify-playlist-img"
              :src="
                (playlist.coverImgUrl && (playlist.coverImgUrl.startsWith('http') || playlist.coverImgUrl.startsWith('/minio')))
                  ? playlist.coverImgUrl + '?param=330y330'
                  : playlist.coverImgUrl
              "
            />
            <button class="spotify-playlist-play-btn">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
                <path fill="currentColor" d="M8 5v14l11-7z"/>
              </svg>
            </button>
          </div>
          <h3 class="spotify-playlist-title">{{ playlist.name }}</h3>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="spotify-pagination">
      <el-pagination
        v-model:page-size="pageSize"
        v-model:currentPage="currentPage"
        v-bind="state"
        :layout="state.layout.replace('total, sizes, ', '').replace('jumper', '')"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="spotify-pagination-desktop"
      />
      <el-pagination
        v-model:page-size="pageSize"
        v-model:currentPage="currentPage"
        layout="prev, pager, next"
        :total="state.total"
        :pager-count="5"
        @current-change="handleCurrentChange"
        class="spotify-pagination-mobile"
        small
      />
    </div>
  </div>
</template>

<style scoped>
.spotify-playlist-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 16px;
}

.spotify-playlist-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.spotify-search-wrapper {
  position: relative;
  flex: 1;
}

.spotify-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.spotify-search-input {
  width: 100%;
  padding: 10px 12px 10px 44px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
  transition: box-shadow 200ms ease;
}

.spotify-search-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-search-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px #fff;
}

.spotify-tag-select {
  width: 100%;
}

.spotify-tag-select :deep(.el-select__wrapper) {
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 500px;
  box-shadow: none;
  padding: 4px 16px;
  min-height: 40px;
}

.spotify-tag-select :deep(.el-select__wrapper:hover) {
  box-shadow: none;
}

.spotify-tag-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px #fff;
}

.spotify-tag-select :deep(.el-select__placeholder) {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-tag-select :deep(.el-select__selected-item) {
  color: var(--text-base, #fff);
}

.spotify-tag-select :deep(.el-select__suffix) {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-tag-select :deep(.el-select__caret) {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-shrink: 0;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.spotify-tabs::-webkit-scrollbar {
  display: none;
}

.spotify-tab {
  padding: 8px 16px;
  background: transparent;
  border: none;
  border-radius: 500px;
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 200ms ease;
}

.spotify-tab:hover {
  color: var(--text-base, #fff);
}

.spotify-tab-active {
  background-color: #1db954;
  color: #000;
}

.spotify-tab-active:hover {
  background-color: #1ed760;
  color: #000;
}

.spotify-playlist-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  padding-right: 8px;
}

.spotify-playlist-content::-webkit-scrollbar {
  width: 8px;
}

.spotify-playlist-content::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-playlist-content::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
  transition: background 300ms ease;
}

.spotify-playlist-content.show-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
}

.spotify-playlist-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.spotify-playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 24px;
  padding-bottom: 16px;
}

.spotify-playlist-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background-color: var(--card-bg, #181818);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-playlist-card:hover {
  background-color: var(--card-hover, #282828);
}

.spotify-playlist-card:hover .spotify-playlist-play-btn {
  opacity: 1;
  transform: translateY(0);
}

.spotify-playlist-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
}

.spotify-playlist-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.spotify-playlist-play-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 48px;
  height: 48px;
  background-color: #1db954;
  border: none;
  border-radius: 50%;
  color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: translateY(8px);
  transition: all 200ms ease;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
}

.spotify-playlist-play-btn svg {
  color: #000;
  fill: #000;
}

.spotify-playlist-card:hover .spotify-playlist-play-btn:hover {
  transform: translateY(0) scale(1.04);
  background-color: #1ed760;
}

.spotify-playlist-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spotify-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
  gap: 8px;
  flex-shrink: 0;
}

.spotify-pagination-desktop {
  display: none;
}

.spotify-pagination-mobile {
  display: flex;
}

/* Pagination Styles */
.spotify-pagination :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-hover-color: #1db954;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-button-color: #b3b3b3;
  --el-pagination-button-disabled-bg-color: transparent;
  --el-pagination-button-disabled-color: #535353;
}

.spotify-pagination :deep(.el-pagination .el-pager li) {
  background-color: transparent;
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
  font-weight: 500;
  min-width: 32px;
  height: 32px;
  line-height: 32px;
  border-radius: 4px;
  margin: 0 2px;
  transition: all 200ms ease;
}

.spotify-pagination :deep(.el-pagination .el-pager li:hover) {
  color: var(--text-base, #fff);
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-pagination :deep(.el-pagination .el-pager li.is-active) {
  background-color: #1db954;
  color: #000;
}

.spotify-pagination :deep(.el-pagination .btn-prev),
.spotify-pagination :deep(.el-pagination .btn-next) {
  background-color: transparent;
  color: var(--text-subdued, #b3b3b3);
  width: 32px;
  height: 32px;
  border-radius: 4px;
  transition: all 200ms ease;
}

.spotify-pagination :deep(.el-pagination .btn-prev:hover),
.spotify-pagination :deep(.el-pagination .btn-next:hover) {
  color: var(--text-base, #fff);
  background-color: rgba(255, 255, 255, 0.1);
}

.spotify-pagination :deep(.el-pagination .el-pagination__total),
.spotify-pagination :deep(.el-pagination .el-pagination__jump) {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
}

.spotify-pagination :deep(.el-pagination .el-select .el-select__wrapper) {
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 4px;
  box-shadow: none;
  min-height: 28px;
}

.spotify-pagination :deep(.el-pagination .el-select .el-select__selected-item) {
  color: var(--text-base, #fff);
}

/* Light Theme */
:root:not(.dark) .spotify-playlist-page {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --bg-elevated: #f0f0f0;
  --card-bg: #f5f5f5;
  --card-hover: #e8e8e8;
}

:root:not(.dark) .spotify-playlist-img {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-search-input:focus {
  box-shadow: 0 0 0 2px #000;
}

:root:not(.dark) .spotify-tag-select :deep(.el-select__wrapper) {
  background-color: #f0f0f0;
}

:root:not(.dark) .spotify-tag-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px #000;
}

:root:not(.dark) .spotify-playlist-content.show-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
}

:root:not(.dark) .spotify-playlist-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.4);
}

@media (min-width: 768px) {
  .spotify-playlist-header {
    flex-direction: row;
    align-items: center;
  }
  
  .spotify-search-wrapper {
    max-width: 400px;
  }
  
  .spotify-tag-select {
    width: 200px;
  }
  
  .spotify-pagination-desktop {
    display: flex;
  }
  
  .spotify-pagination-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .spotify-playlist-page {
    padding-bottom: 80px;
  }
  
  .spotify-playlist-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .spotify-playlist-card {
    padding: 8px;
  }
}
</style>
