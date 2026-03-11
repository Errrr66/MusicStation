<script setup lang="ts">
import { MenuData } from './data'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'
import { ref, watch } from 'vue'
import { UserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import AuthTabs from '@/components/Auth/AuthTabs.vue'
import { useFavoriteStore } from '@/stores/modules/favorite'

const route = useRoute()
const router = useRouter()
const user = UserStore()
const favoriteStore = useFavoriteStore()
const authVisible = ref(false)
const isCollapsed = ref(false)

// 处理需要登录的路由
const handleProtectedRoute = (path: string) => {
  if (!user.isLoggedIn && (path === '/like' || path === '/user')) {
    ElMessage.warning('请先登录')
    authVisible.value = true
    return false
  }
  return true
}

// 切换侧边栏收起/展开
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 监听用户登录状态
watch(
  () => user.isLoggedIn,
  (newVal) => {
    if (newVal) {
      favoriteStore.getFavoritePlaylists()
    } else {
      favoriteStore.clearFavoritePlaylists()
    }
  },
  { immediate: true }
)
</script>

<template>
  <aside
    class="sidebar-card hidden h-[calc(100%-1rem)] overflow-hidden md:block shadow-xl transition-all duration-300 m-2 rounded-2xl dark:bg-[#121212] bg-gray-100"
    :class="[isCollapsed ? 'w-[120px]' : 'w-96']"
  >
    <nav
      class="flex flex-col p-4 space-y-4 flex-1 h-full box-border overflow-hidden"
    >
      <div
        class="flex items-center gap-2 cursor-pointer p-2 rounded-lg hover:bg-hoverMenuBg mb-2 text-primary-foreground transition-colors duration-200"
        :class="isCollapsed ? 'justify-center' : 'justify-end'"
        @click="toggleCollapse"
        :title="isCollapsed ? '展开' : '收起'"
      >
        <Icon
          :icon="isCollapsed ? 'ri:menu-unfold-line' : 'ri:menu-fold-line'"
          class="text-xl"
        />
      </div>

      <div
        v-for="(item, index) in MenuData"
        :key="index"
        class="w-full flex flex-col gap-1"
      >
        <h3
          v-if="!isCollapsed && item.title"
          class="ml-4 text-sm font-semibold text-inactive transition-opacity duration-300"
        >
          {{ item.title }}
        </h3>
        <div
          v-for="(item2, index2) in item.children"
          :key="index2"
          class="mx-2 rounded-lg transition-all duration-300 py-2 px-2 flex items-center gap-2 text-primary-foreground cursor-pointer"
          :class="{
            'bg-activeMenuBg': route.path === item2.router,
            'hover:bg-hoverMenuBg': route.path !== item2.router,
            'justify-center': isCollapsed,
          }"
          @click="
            handleProtectedRoute(item2.router) && router.push(item2.router)
          "
          :title="isCollapsed ? item2.title : ''"
        >
          <Icon :icon="item2.icon" class="flex-shrink-0 text-xl" />
          <span
            v-if="!isCollapsed"
            class="whitespace-nowrap transition-opacity duration-300"
            >{{ item2.title }}</span
          >
          <span
            v-if="!isCollapsed"
            class="!ml-auto text-xs text-primary-foreground bg-emphasis border-border p-1 rounded-lg"
          ></span>
        </div>
      </div>

      <!-- 收藏的歌单 -->
      <div
        class="w-full flex flex-col gap-1 flex-1 overflow-hidden"
        v-if="user.isLoggedIn"
      >
        <h3
          v-if="!isCollapsed"
          class="ml-4 text-sm font-semibold text-inactive transition-opacity duration-300"
        >
          收藏（{{ favoriteStore.favoritePlaylists.length }}）
        </h3>
        <div v-else class="h-6"></div>

        <el-scrollbar>
          <el-skeleton
            :loading="favoriteStore.loading"
            animated
            :count="3"
            v-if="favoriteStore.loading"
          >
            <template #template>
              <div class="flex items-center space-x-2 p-2 mx-2">
                <el-skeleton-item
                  variant="image"
                  style="width: 28px; height: 28px"
                />
                <el-skeleton-item
                  v-if="!isCollapsed"
                  variant="text"
                  style="width: 130px"
                />
              </div>
            </template>
          </el-skeleton>

          <template v-else>
            <div
              v-for="item in favoriteStore.favoritePlaylists"
              :key="item.id"
              class="mx-2 my-1 rounded-lg transition-all duration-300 py-2 px-2 flex items-center gap-2 text-primary-foreground cursor-pointer"
              :class="{
                'bg-activeMenuBg': route.path === `/playlist/${item.id}`,
                'hover:bg-hoverMenuBg': route.path !== `/playlist/${item.id}`,
                'justify-center': isCollapsed,
              }"
              @click="router.push(`/playlist/${item.id}`)"
              :title="isCollapsed ? item.name : ''"
            >
              <el-image
                lazy
                :src="item.coverImgUrl + '?param=50y50'"
                class="w-10 h-10 rounded-md flex-shrink-0"
                :alt="item.name"
              />
              <div
                v-if="!isCollapsed"
                class="flex-1 min-w-0 transition-opacity duration-300"
              >
                <span class="line-clamp-2 text-sm leading-normal">{{
                  item.name
                }}</span>
              </div>
            </div>
          </template>
        </el-scrollbar>
      </div>
    </nav>

    <!-- 登录对话框 -->
    <AuthTabs v-model="authVisible" />
  </aside>
</template>
