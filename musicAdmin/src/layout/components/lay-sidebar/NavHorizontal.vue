<script setup lang="ts">
import { useNav } from "@/layout/hooks/useNav";
import { useAdminTheme } from "@/layout/hooks/useAdminTheme";
import LaySearch from "../lay-search/index.vue";
import { ref, nextTick, computed } from "vue";
import { isAllEmpty } from "@pureadmin/utils";
import { usePermissionStoreHook } from "@/store/modules/permission";
import LaySidebarItem from "../lay-sidebar/components/SidebarItem.vue";
import LaySidebarFullScreen from "../lay-sidebar/components/SidebarFullScreen.vue";

import LogoutCircleRLine from "@iconify-icons/ri/logout-circle-r-line";
import SunLine from "@iconify-icons/ri/sun-line";
import MoonLine from "@iconify-icons/ri/moon-line";

const menuRef = ref();

const {
  route,
  logout,
  username,
  userAvatar,
  avatarsStyle
} = useNav();

const { isDark, toggleTheme } = useAdminTheme();

const defaultActive = computed(() =>
  !isAllEmpty(route.meta?.activePath) ? route.meta.activePath : route.path
);

nextTick(() => {
  menuRef.value?.handleResize();
});
</script>

<template>
  <div
    v-loading="usePermissionStoreHook().wholeMenus.length === 0"
    class="horizontal-header"
  >
    <el-menu
      ref="menuRef"
      mode="horizontal"
      popper-class="pure-scrollbar"
      class="horizontal-header-menu"
      :default-active="defaultActive"
    >
      <LaySidebarItem
        v-for="route in usePermissionStoreHook().wholeMenus"
        :key="route.path"
        :item="route"
        :base-path="route.path"
      />
    </el-menu>
    <div class="horizontal-header-right">
      <LaySearch id="header-search" />
      <LaySidebarFullScreen id="full-screen" />
      <span
        class="set-icon navbar-bg-hover theme-toggle"
        :title="isDark ? '切换浅色主题' : '切换深色主题'"
        @click="toggleTheme"
      >
        <IconifyIconOffline :icon="isDark ? SunLine : MoonLine" />
      </span>
      <el-dropdown trigger="click">
        <span class="el-dropdown-link navbar-bg-hover">
          <img :src="userAvatar" :style="avatarsStyle" />
          <p v-if="username" class="dark:text-white">{{ username }}</p>
        </span>
        <template #dropdown>
          <el-dropdown-menu class="logout">
            <el-dropdown-item @click="logout">
              <IconifyIconOffline
                :icon="LogoutCircleRLine"
                style="margin: 5px"
              />
              退出系统
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style lang="scss" scoped>
:deep(.el-loading-mask) {
  opacity: 0.45;
}

.logout {
  width: 120px;

  ::v-deep(.el-dropdown-menu__item) {
    display: inline-flex;
    flex-wrap: wrap;
    min-width: 100%;
  }
}
</style>
