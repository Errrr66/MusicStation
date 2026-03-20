<script setup lang="ts">
import { categories } from '@/utils/enum'
import { getAllArtists } from '@/api/system'
import { ElNotification } from 'element-plus'
import { fixUrl } from '@/utils'

const router = useRouter()
const artistList = ref([])

const selectedGender = ref('-1')
const selectedArea = ref('-1')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const isSidebarOpen = ref(true)

const state = reactive({
  size: 'default',
  disabled: false,
  background: false,
  layout: 'total, sizes, prev, pager, next, jumper',
  total: 0,
  pageSizes: [12, 24, 36, 48],
})

const searchKeyword = ref('')

// 切换菜单显示
const toggleMenu = (index: number) => {
  categories.value[index].isOpen = !categories.value[index].isOpen
}

// 处理分页大小变化
const handleSizeChange = () => {
  currentPage.value = 1
  handleGetArtistList()
}

// 处理页码变化
const handleCurrentChange = () => {
  handleGetArtistList()
}

const handleSubCategoryClick = (id: string, index: number) => {
  if (index === 0) {
    selectedGender.value = id
  } else {
    selectedArea.value = id
  }
  currentPage.value = 1
  handleGetArtistList()
}

const handleGetArtistList = () => {
  const params = {
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    name: null,
    gender:
      selectedGender.value === '-1'
        ? null
        : categories.value[0].subCategories.find(
            (item) => item.id === selectedGender.value
          )?.value,
    area:
      selectedArea.value === '-1'
        ? null
        : categories.value[1].subCategories.find(
            (item) => item.id === selectedArea.value
          )?.value,
  }

  getAllArtists(params).then((res) => {
    if (res.code === 0 && res.data) {
      artistList.value = res.data.items.map((item) => ({
        artistId: item.artistId,
        name: item.artistName,
        picUrl: fixUrl(item.avatar),
        alias: [],
      }))
      total.value = res.data.total
      state.total = res.data.total
    } else {
      ElNotification({
        type: 'error',
        message: '获取艺人列表失败',
        duration: 2000,
      })
    }
  })
}

const handleSearch = () => {
  const params = {
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    artistName: searchKeyword.value || null,
    gender:
      selectedGender.value === '-1'
        ? null
        : categories.value[0].subCategories.find(
            (item) => item.id === selectedGender.value
          )?.value,
    area:
      selectedArea.value === '-1'
        ? null
        : categories.value[1].subCategories.find(
            (item) => item.id === selectedArea.value
          )?.value,
  }

  getAllArtists(params).then((res) => {
    if (res.code === 0 && res.data) {
      artistList.value = res.data.items.map((item) => ({
        artistId: item.artistId,
        name: item.artistName,
        picUrl: fixUrl(item.avatar),
        alias: [],
      }))
      total.value = res.data.total
      state.total = res.data.total
    } else {
      ElNotification({
        type: 'error',
        message: '获取艺人列表失败',
        duration: 2000,
      })
    }
  })
}

const handleReset = () => {
  searchKeyword.value = ''
  selectedGender.value = '-1'
  selectedArea.value = '-1'
  currentPage.value = 1
  handleGetArtistList()
}

const toggleSidebar = () => {
    isSidebarOpen.value = !isSidebarOpen.value
}

onMounted(() => {
  handleGetArtistList()
})
</script>
<template>
  <div class="spotify-artist-page">
    <!-- Mobile Overlay -->
    <div
      v-if="isSidebarOpen"
      class="spotify-sidebar-overlay"
      @click="toggleSidebar"
    ></div>

    <!-- Sidebar -->
    <aside
      class="spotify-artist-sidebar"
      :class="{ 'spotify-sidebar-open': isSidebarOpen }"
    >
      <div class="spotify-sidebar-header">
        <h2 class="spotify-sidebar-title">艺人分类</h2>
        <button @click="handleReset" class="spotify-reset-btn">
          <Icon icon="mdi:refresh" class="text-lg" />
          <span>重置</span>
        </button>
      </div>

      <div class="spotify-sidebar-content">
        <!-- Search -->
        <div class="spotify-search-box">
          <Icon icon="mdi:magnify" class="spotify-search-icon" />
          <input
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
            class="spotify-search-input"
            placeholder="搜索艺人"
          />
        </div>

        <!-- Gender Filter -->
        <div class="spotify-filter-group">
          <button class="spotify-filter-toggle" @click="toggleMenu(0)">
            <span>{{ categories[0].name }}</span>
            <Icon
              icon="mdi:chevron-right"
              class="spotify-toggle-icon"
              :class="{ 'spotify-toggle-rotated': categories[0].isOpen }"
            />
          </button>
          <div v-show="categories[0].isOpen" class="spotify-filter-options">
            <button
              v-for="(subCategory, subIndex) in categories[0].subCategories"
              :key="subIndex"
              @click="handleSubCategoryClick(subCategory.id, 0)"
              class="spotify-filter-option"
              :class="{ 'spotify-filter-active': selectedGender === subCategory.id }"
            >
              {{ subCategory.label }}
            </button>
          </div>
        </div>

        <!-- Area Filter -->
        <div class="spotify-filter-group">
          <button class="spotify-filter-toggle" @click="toggleMenu(1)">
            <span>{{ categories[1].name }}</span>
            <Icon
              icon="mdi:chevron-right"
              class="spotify-toggle-icon"
              :class="{ 'spotify-toggle-rotated': categories[1].isOpen }"
            />
          </button>
          <div v-show="categories[1].isOpen" class="spotify-filter-options">
            <button
              v-for="(subCategory, subIndex) in categories[1].subCategories"
              :key="subIndex"
              @click="handleSubCategoryClick(subCategory.id, 1)"
              class="spotify-filter-option"
              :class="{ 'spotify-filter-active': selectedArea === subCategory.id }"
            >
              {{ subCategory.label }}
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="spotify-artist-main">
      <!-- Header -->
      <div class="spotify-artist-header">
        <button
          v-if="!isSidebarOpen"
          @click="toggleSidebar"
          class="spotify-toggle-sidebar"
        >
          <Icon icon="mdi:menu" class="text-xl" />
        </button>
        <h1 class="spotify-artist-title">艺人</h1>
        <button
          v-if="isSidebarOpen"
          @click="toggleSidebar"
          class="spotify-toggle-sidebar spotify-toggle-hidden-desktop"
        >
          <Icon icon="mdi:menu-open" class="text-xl" />
        </button>
      </div>

      <!-- Artist Grid -->
      <div class="spotify-artist-grid">
        <div
          v-for="(artist, index) in artistList"
          :key="index"
          class="spotify-artist-card"
          @click="router.push(`/artist/${artist.artistId}`)"
        >
          <div class="spotify-artist-avatar">
            <el-image
              lazy
              :alt="artist.name"
              class="spotify-artist-img"
              :src="artist.picUrl + '?param=230y230'"
            />
            <div class="spotify-artist-play">
              <Icon icon="mdi:play" class="text-2xl" />
            </div>
          </div>
          <h3 class="spotify-artist-name">{{ artist.name }}</h3>
          <span class="spotify-artist-label">艺人</span>
        </div>
      </div>

      <!-- Pagination -->
      <div class="spotify-pagination">
        <el-pagination
          v-model:page-size="pageSize"
          v-model:currentPage="currentPage"
          v-bind="state"
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
    </main>
  </div>
</template>

<style scoped>
.spotify-artist-page {
  display: flex;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.spotify-sidebar-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 20;
}

.spotify-artist-sidebar {
  position: absolute;
  inset-y: 0;
  left: 0;
  width: 280px;
  background-color: var(--bg-surface, #121212);
  border-right: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  padding: 16px;
  overflow-y: auto;
  z-index: 30;
  transform: translateX(-100%);
  transition: transform 300ms ease;
}

.spotify-sidebar-open {
  transform: translateX(0);
}

.spotify-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.spotify-sidebar-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.spotify-reset-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
  cursor: pointer;
  transition: color 200ms ease;
}

.spotify-reset-btn:hover {
  color: var(--text-base, #fff);
}

.spotify-sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spotify-search-box {
  position: relative;
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

.spotify-filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spotify-filter-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-filter-toggle:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-toggle-icon {
  font-size: 1.25rem;
  transition: transform 200ms ease;
}

.spotify-toggle-rotated {
  transform: rotate(90deg);
}

.spotify-filter-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 8px;
}

.spotify-filter-option {
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-filter-option:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-filter-active {
  background-color: #1db954 !important;
  color: #000 !important;
  font-weight: 700;
}

.spotify-filter-active:hover {
  background-color: #1ed760 !important;
  color: #000 !important;
}

.spotify-artist-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}

.spotify-artist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.spotify-artist-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  flex: 1;
  text-align: center;
}

.spotify-toggle-sidebar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: color 200ms ease, background-color 200ms ease;
}

.spotify-toggle-sidebar:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.spotify-artist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 24px;
  flex: 1;
  overflow-y: auto;
  padding-bottom: 16px;
}

.spotify-artist-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background-color: var(--card-bg, #181818);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.spotify-artist-card:hover {
  background-color: var(--card-hover, #282828);
}

.spotify-artist-card:hover .spotify-artist-play {
  opacity: 1;
  transform: translateY(0);
}

.spotify-artist-avatar {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  margin-bottom: 16px;
}

.spotify-artist-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.spotify-artist-play {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 48px;
  height: 48px;
  background-color: #1db954;
  border: none;
  border-radius: 50%;
  color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: translateY(8px);
  transition: all 200ms ease;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
}

.spotify-artist-card:hover .spotify-artist-play:hover {
  transform: translateY(0) scale(1.04);
  background-color: #1ed760;
}

.spotify-artist-name {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  text-align: center;
  margin-bottom: 4px;
}

.spotify-artist-label {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
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
:root:not(.dark) .spotify-artist-sidebar {
  --bg-surface: #ffffff;
  --bg-elevated: #f0f0f0;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-artist-main {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --card-bg: #f5f5f5;
  --card-hover: #e8e8e8;
  --bg-hover: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .spotify-artist-img {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .spotify-search-input:focus {
  box-shadow: 0 0 0 2px #000;
}

@media (min-width: 768px) {
  .spotify-sidebar-overlay {
    display: none;
  }
  
  .spotify-artist-sidebar {
    position: relative;
    transform: translateX(0);
    flex-shrink: 0;
  }
  
  .spotify-toggle-hidden-desktop {
    display: flex;
  }
  
  .spotify-artist-header {
    justify-content: flex-start;
  }
  
  .spotify-artist-title {
    text-align: left;
  }
  
  .spotify-pagination-desktop {
    display: flex;
  }
  
  .spotify-pagination-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .spotify-artist-page {
    padding-bottom: 80px;
  }
  
  .spotify-artist-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .spotify-artist-card {
    padding: 8px;
  }
}
</style>
