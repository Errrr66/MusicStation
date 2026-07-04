<script setup lang="ts">
import { getAllSongs } from '@/api/system'
import { useLibraryStore } from '@/stores/modules/library'

const route = useRoute()
const libraryStore = useLibraryStore()

const props = defineProps({
  selected: {
    type: String,
    default: '1',
  },
})
const tableData = computed(() => libraryStore.tableData)

const currentPage = ref(1) // 当前页
const pageSize = ref(20) // 每页显示的数量

const state = reactive({
  size: 'default' as const,
  disabled: false,
  background: false,
  layout: 'total, sizes, prev, pager, next, jumper',
  total: 0,
  pageSizes: [20, 30, 50],
})

// 监听分页变化
const handleSizeChange = () => {
  getSongs()
}
// 监听当前页变化
const handleCurrentChange = () => {
  getSongs()
}

const getSongs = () => {
  libraryStore.setTableData(null)
  getAllSongs({
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    songName: (route.query.query as string) || '',
    artistName: '',
    album: '',
  }).then((res) => {
    if (res.code === 0 && res.data) {
      libraryStore.setTableData(res.data)
      state.total = res.data.total || 0
    }
  })
}

watch(
  () => [route.query.query, props.selected],
  (val) => {
    if (!val[1] || val[1] != '1') return
    getSongs()
  },
  {
    immediate: true,
  }
)
</script>

<template>
  <div class="mr-library-page">
    <Table :data="tableData?.items" class="mr-library-table" />
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
        background
      />
    </div>
  </div>
</template>

<style scoped>
.mr-library-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.mr-library-table {
  flex: 1;
  overflow-x: hidden;
}

.mr-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
  gap: 8px;
}

.mr-pagination-desktop {
  display: none;
}

.mr-pagination-mobile {
  display: flex;
}

/* Spotify Style Pagination */
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

.mr-pagination :deep(.el-pagination .el-input-number .el-input__wrapper) {
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 4px;
  box-shadow: none;
}

.mr-pagination :deep(.el-pagination .el-input-number .el-input__inner) {
  color: var(--text-base, #fff);
}

/* Light Theme */
:root:not(.dark) .mr-pagination :deep(.el-pagination .el-pager li) {
  --text-subdued: #6a6a6a;
  --text-base: #000000;
}

:root:not(.dark) .mr-pagination :deep(.el-pagination .el-pager li:hover) {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-pagination :deep(.el-pagination .btn-prev:hover),
:root:not(.dark) .mr-pagination :deep(.el-pagination .btn-next:hover) {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-pagination :deep(.el-pagination .el-select .el-select__wrapper) {
  background-color: #f0f0f0;
}

:root:not(.dark) .mr-pagination :deep(.el-pagination .el-input-number .el-input__wrapper) {
  background-color: #f0f0f0;
}

@media (min-width: 768px) {
  .mr-pagination-desktop {
    display: flex;
  }
  
  .mr-pagination-mobile {
    display: none;
  }
}

@media (max-width: 768px) {
  .mr-library-page {
    padding-bottom: 140px;
  }
  
  .mr-pagination {
    padding: 16px 0;
  }
}
</style>
