<script setup lang="ts">
import { categories } from '@/utils/enum'
import { getAllArtists } from '@/api/system'
import { ElNotification } from 'element-plus'
import { fixUrl } from '@/utils'
import defaultArtistAvatar from '@/assets/user.jpg'

const router = useRouter()
const artistList = ref([])

const genderLabelMap: Record<number, string> = {
  0: '男歌手',
  1: '女歌手',
  2: '组合/乐队',
}

const selectedGender = ref('-1')
const selectedArea = ref('-1')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const isSidebarOpen = ref(true)

const state = reactive({
  size: 'default' as const,
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
    artistName: null,
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
        area: item.area || '其他',
        gender: item.gender,
        introduction: item.introduction || '',
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
        area: item.area || '其他',
        gender: item.gender,
        introduction: item.introduction || '',
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
  <div class="mr-artist-page">
    <!-- Mobile Overlay -->
    <div
      v-if="isSidebarOpen"
      class="mr-sidebar-overlay"
      @click="toggleSidebar"
    ></div>

    <!-- Sidebar -->
    <aside
      class="mr-artist-sidebar"
      :class="{ 'mr-sidebar-open': isSidebarOpen }"
    >
      <div class="mr-sidebar-header">
        <h2 class="mr-sidebar-title">艺人分类</h2>
        <button @click="handleReset" class="mr-reset-btn" title="重置">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
            <path fill="currentColor" d="M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-8 3.58-8 8s3.58 8 8 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
          </svg>
        </button>
      </div>

      <div class="mr-sidebar-content">
        <!-- Search -->
        <div class="mr-search-box">
          <Icon icon="mdi:magnify" class="mr-search-icon" />
          <input
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
            class="mr-search-input"
            placeholder="搜索艺人"
          />
        </div>

        <!-- Gender Filter -->
        <div class="mr-filter-group">
          <button class="mr-filter-toggle" @click="toggleMenu(0)">
            <span>{{ categories[0].name }}</span>
            <Icon
              icon="mdi:chevron-right"
              class="mr-toggle-icon"
              :class="{ 'mr-toggle-rotated': categories[0].isOpen }"
            />
          </button>
          <div v-show="categories[0].isOpen" class="mr-filter-options">
            <button
              v-for="(subCategory, subIndex) in categories[0].subCategories"
              :key="subIndex"
              @click="handleSubCategoryClick(subCategory.id, 0)"
              class="mr-filter-option"
              :class="{ 'mr-filter-active': selectedGender === subCategory.id }"
            >
              {{ subCategory.label }}
            </button>
          </div>
        </div>

        <!-- Area Filter -->
        <div class="mr-filter-group">
          <button class="mr-filter-toggle" @click="toggleMenu(1)">
            <span>{{ categories[1].name }}</span>
            <Icon
              icon="mdi:chevron-right"
              class="mr-toggle-icon"
              :class="{ 'mr-toggle-rotated': categories[1].isOpen }"
            />
          </button>
          <div v-show="categories[1].isOpen" class="mr-filter-options">
            <button
              v-for="(subCategory, subIndex) in categories[1].subCategories"
              :key="subIndex"
              @click="handleSubCategoryClick(subCategory.id, 1)"
              class="mr-filter-option"
              :class="{ 'mr-filter-active': selectedArea === subCategory.id }"
            >
              {{ subCategory.label }}
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="mr-artist-main">
      <!-- Header -->
      <div class="mr-artist-header">
        <button
          v-if="!isSidebarOpen"
          @click="toggleSidebar"
          class="mr-toggle-sidebar"
        >
          <Icon icon="mdi:menu" class="text-xl" />
        </button>
        <h1 class="mr-artist-title">艺人</h1>
        <button
          v-if="isSidebarOpen"
          @click="toggleSidebar"
          class="mr-toggle-sidebar mr-toggle-hidden-desktop"
        >
          <Icon icon="mdi:menu-open" class="text-xl" />
        </button>
      </div>

      <!-- Artist Grid -->
      <div class="mr-artist-grid">
        <div
          v-for="(artist, index) in artistList"
          :key="index"
          class="mr-artist-card"
          @click="router.push(`/artist/${artist.artistId}`)"
        >
          <div class="mr-artist-avatar">
            <el-image
              lazy
              :alt="artist.name"
              class="mr-artist-img"
              :src="fixUrl(artist.picUrl) || defaultArtistAvatar"
            >
              <template #error>
                <img :src="defaultArtistAvatar" :alt="artist.name" class="mr-artist-img" />
              </template>
            </el-image>
          </div>
          <h3 class="mr-artist-name">{{ artist.name }}</h3>
          <span class="mr-artist-label">{{ genderLabelMap[artist.gender] || '艺人' }} · {{ artist.area || '其他' }}</span>
        </div>
      </div>

      <!-- Pagination -->
      <div class="mr-pagination">
        <el-pagination
          v-model:page-size="pageSize"
          v-model:currentPage="currentPage"
          v-bind="state"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="mr-pagination-desktop"
        />
        <el-pagination
          v-model:page-size="pageSize"
          v-model:currentPage="currentPage"
          layout="prev, pager, next"
          :total="state.total"
          :pager-count="5"
          @current-change="handleCurrentChange"
          class="mr-pagination-mobile"
          small
        />
      </div>
    </main>
  </div>
</template>

<style scoped>
.mr-artist-page {
  display: flex;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.mr-sidebar-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 20;
}

.mr-artist-sidebar {
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

.mr-sidebar-open {
  transform: translateX(0);
}

.mr-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.mr-sidebar-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.mr-reset-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: color 200ms ease, background-color 200ms ease;
}

.mr-reset-btn:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mr-search-box {
  position: relative;
}

.mr-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.mr-search-input {
  width: 100%;
  padding: 10px 12px 10px 44px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.875rem;
  transition: box-shadow 200ms ease;
}

.mr-search-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.mr-search-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px #fff;
}

.mr-filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mr-filter-toggle {
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

.mr-filter-toggle:hover {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-toggle-icon {
  font-size: 1.25rem;
  transition: transform 200ms ease;
}

.mr-toggle-rotated {
  transform: rotate(90deg);
}

.mr-filter-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 8px;
}

.mr-filter-option {
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

.mr-filter-option:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-filter-active {
  background-color: var(--mr-accent) !important;
  color: #000 !important;
  font-weight: 700;
}

.mr-filter-active:hover {
  background-color: var(--mr-accent-hover) !important;
  color: #000 !important;
}

.mr-artist-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}

.mr-artist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.mr-artist-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  flex: 1;
  text-align: center;
}

.mr-toggle-sidebar {
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

.mr-toggle-sidebar:hover {
  color: var(--text-base, #fff);
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-artist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 24px;
  flex: 1;
  overflow-y: auto;
  padding-bottom: 16px;
}

.mr-artist-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background-color: var(--card-bg, #181818);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-artist-card:hover {
  background-color: var(--card-hover, #282828);
}

.mr-artist-avatar {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  margin-bottom: 16px;
}

.mr-artist-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.mr-artist-name {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--text-base, #fff);
  text-align: center;
  margin-bottom: 4px;
}

.mr-artist-label {
  font-size: 0.75rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
  gap: 8px;
  flex-shrink: 0;
}

.mr-pagination-desktop {
  display: none;
}

.mr-pagination-mobile {
  display: flex;
}

/* Pagination Styles */
.mr-pagination :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-hover-color: var(--mr-accent);
  --el-pagination-button-bg-color: transparent;
  --el-pagination-button-color: #b3b3b3;
  --el-pagination-button-disabled-bg-color: transparent;
  --el-pagination-button-disabled-color: #535353;
}

.mr-pagination :deep(.el-pagination .el-pager li) {
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

.mr-pagination :deep(.el-pagination .el-pager li:hover) {
  color: var(--text-base, #fff);
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-pagination :deep(.el-pagination .el-pager li.is-active) {
  background-color: var(--mr-accent);
  color: #000;
}

.mr-pagination :deep(.el-pagination .btn-prev),
.mr-pagination :deep(.el-pagination .btn-next) {
  background-color: transparent;
  color: var(--text-subdued, #b3b3b3);
  width: 32px;
  height: 32px;
  border-radius: 4px;
  transition: all 200ms ease;
}

.mr-pagination :deep(.el-pagination .btn-prev:hover),
.mr-pagination :deep(.el-pagination .btn-next:hover) {
  color: var(--text-base, #fff);
  background-color: rgba(255, 255, 255, 0.1);
}

.mr-pagination :deep(.el-pagination .el-pagination__total),
.mr-pagination :deep(.el-pagination .el-pagination__jump) {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
}

.mr-pagination :deep(.el-pagination .el-select .el-select__wrapper) {
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 4px;
  box-shadow: none;
  min-height: 28px;
}

.mr-pagination :deep(.el-pagination .el-select .el-select__selected-item) {
  color: var(--text-base, #fff);
}

/* Light Theme */
:root:not(.dark) .mr-artist-sidebar {
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --bg-hover: rgba(0, 0, 0, 0.08);
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-artist-main {
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --card-bg: #e8e8e8;
  --card-hover: #d8d8d8;
  --bg-hover: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-artist-img {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .mr-search-input:focus {
  box-shadow: 0 0 0 2px #000;
}

@media (min-width: 768px) {
  .mr-sidebar-overlay {
    display: none;
  }
  
  .mr-artist-sidebar {
    position: relative;
    transform: translateX(0);
    flex-shrink: 0;
  }
  
  .mr-toggle-hidden-desktop {
    display: flex;
  }
  
  .mr-artist-header {
    justify-content: flex-start;
  }
  
  .mr-artist-title {
    text-align: left;
  }
  
  .mr-pagination-desktop {
    display: flex;
  }
  
  .mr-pagination-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .mr-artist-page {
    padding-bottom: 140px;
  }
  
  .mr-artist-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .mr-artist-card {
    padding: 8px;
  }
}
</style>
