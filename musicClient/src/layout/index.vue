<script setup lang="ts">
import Header from './components/header/index.vue'
import Aside from './components/aside/index.vue'
import RightAside from './components/aside/RightAside.vue'
import Main from './components/main/index.vue'
import Footer from './components/footer/index.vue'
import MobilePlayer from './components/footer/MobilePlayer.vue'
// import BG from './components/bg/index.vue'
import { AudioPlayer } from '@/hooks/useAudioPlayer'
import { MenuStore } from '@/stores/modules/menu'
import { storeToRefs } from 'pinia'

const menuStore = MenuStore()
const { isMobileMenuOpen, isRightAsideOpen } = storeToRefs(menuStore)

provide('audioPlayer', AudioPlayer())

const closeMobileMenu = () => {
    menuStore.setMobileMenuOpen(false)
}

const closeRightAside = () => {
    menuStore.setRightAsideOpen(false)
}
</script>
<template>
  <!-- <BG /> -->
  <div class="mr-app-container">
    <div class="mr-main-wrapper">
      <!-- Navbar -->
      <Header />
      
      <!-- Main Content Area -->
      <div class="mr-content-wrapper">
        <!-- Left Sidebar - Library -->
        <Aside />
        
        <!-- Mobile Drawer - Library -->
        <el-drawer
            v-model="isMobileMenuOpen"
            direction="ltr"
            size="85%"
            :with-header="false"
            :show-close="false"
            class="mr-mobile-drawer mr-mobile-drawer-left"
        >
          <Aside :is-mobile="true" @close="closeMobileMenu" />
        </el-drawer>
        
        <!-- Mobile Drawer - Now Playing -->
        <el-drawer
            v-model="isRightAsideOpen"
            direction="rtl"
            size="85%"
            :with-header="false"
            :show-close="false"
            class="mr-mobile-drawer mr-mobile-drawer-right"
        >
          <RightAside :is-mobile="true" @close="closeRightAside" />
        </el-drawer>
        
        <!-- Main Content -->
        <Main />
        
        <!-- Right Sidebar - Now Playing -->
        <RightAside />
      </div>
      
      <!-- Playing Bar -->
      <Footer />
      
      <!-- Mobile Player -->
      <MobilePlayer />
    </div>
  </div>
</template>

<style scoped>
@use '../style/spotify-variables.scss' as *;

.mr-app-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background-color: var(--bg-base, #000000);
  transition: background-color 200ms ease;
}

.mr-main-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px;
  gap: 8px;
}

.mr-content-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 8px;
  min-height: 0;
}

:deep(.mr-mobile-drawer.el-drawer) {
  background-color: var(--bg-surface, #121212);
}

:deep(.mr-mobile-drawer-left.el-drawer) {
  border-top-right-radius: 12px;
  border-bottom-right-radius: 12px;
  overflow: hidden;
}

:deep(.mr-mobile-drawer-right.el-drawer) {
  border-top-left-radius: 12px;
  border-bottom-left-radius: 12px;
  overflow: hidden;
}

/* Light Theme */
:root:not(.dark) .mr-app-container {
  --bg-base: #f5f5f5;
  --bg-surface: #f0f0f0;
}

@media (max-width: 768px) {
  .mr-main-wrapper {
    padding: 0;
    gap: 0;
  }
  
  .mr-content-wrapper {
    gap: 0;
  }
}
</style>
