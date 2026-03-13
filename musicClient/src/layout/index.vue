<script setup lang="ts">
import Header from './components/header/index.vue'
import Aside from './components/aside/index.vue'
import RightAside from './components/aside/RightAside.vue'
import Main from './components/main/index.vue'
import Footer from './components/footer/index.vue'
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
  <div class="absolute w-full flex flex-col h-full">
    <div
      class="w-full flex flex-col h-full overflow-hidden bg-themeBgColor backdrop-blur-lg"
    >
      <Header />
      <div class="flex flex-1 overflow-hidden">
        <Aside />
        <!-- 移动端侧边栏 Drawer -->
        <el-drawer
            v-model="isMobileMenuOpen"
            direction="ltr"
            size="80%"
            :with-header="false"
            :show-close="false"
            class="mobile-menu-drawer"
        >
          <Aside :is-mobile="true" @close="closeMobileMenu" />
        </el-drawer>
        <!-- 移动端NowPlaying Drawer -->
         <el-drawer
            v-model="isRightAsideOpen"
            direction="rtl"
            size="85%"
            :with-header="false"
            :show-close="false"
            class="mobile-right-aside-drawer"
        >
          <RightAside :is-mobile="true" @close="closeRightAside" />
        </el-drawer>
        <Main />
        <RightAside />
      </div>
      <Footer />
    </div>
  </div>
</template>
