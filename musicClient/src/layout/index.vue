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
  <div class="spotify-app-container">
    <div class="spotify-main-wrapper">
      <!-- Navbar -->
      <Header />
      
      <!-- Main Content Area -->
      <div class="spotify-content-wrapper">
        <!-- Left Sidebar - Library -->
        <Aside />
        
        <!-- Mobile Drawer - Library -->
        <el-drawer
            v-model="isMobileMenuOpen"
            direction="ltr"
            size="85%"
            :with-header="false"
            :show-close="false"
            class="spotify-mobile-drawer spotify-mobile-drawer-left"
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
            class="spotify-mobile-drawer spotify-mobile-drawer-right"
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

.spotify-app-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background-color: var(--bg-base, #000000);
  transition: background-color 200ms ease;
}

.spotify-main-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px;
  gap: 8px;
}

.spotify-content-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 8px;
  min-height: 0;
}

:deep(.spotify-mobile-drawer.el-drawer) {
  background-color: var(--bg-surface, #121212);
}

:deep(.spotify-mobile-drawer-left.el-drawer) {
  border-top-right-radius: 12px;
  border-bottom-right-radius: 12px;
  overflow: hidden;
}

:deep(.spotify-mobile-drawer-right.el-drawer) {
  border-top-left-radius: 12px;
  border-bottom-left-radius: 12px;
  overflow: hidden;
}

/* Light Theme */
:root:not(.dark) .spotify-app-container {
  --bg-base: #f5f5f5;
  --bg-surface: #ffffff;
}

@media (max-width: 768px) {
  .spotify-main-wrapper {
    padding: 0;
    gap: 0;
  }
  
  .spotify-content-wrapper {
    gap: 0;
  }
}
</style>
