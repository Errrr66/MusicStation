import { defineStore } from 'pinia'
import piniaPersistConfig from '@/stores/helper/persist'
import { MenuState } from '@/stores/interface'
/**
 * 主题设置
 */
export const MenuStore = defineStore({
  id: 'MenuStore',
  state: (): MenuState => ({
    menuIndex: '1-0',
    isPlaylistOpen: false,
    isMobileMenuOpen: false,
    isRightAsideOpen: false,
    isSongDrawerOpen: false,
  }),
  actions: {
    setMenuIndex(menuIndex: string) {
      this.menuIndex = menuIndex
    },
    setPlaylistOpen(isOpen: boolean) {
      this.isPlaylistOpen = isOpen
    },
    setMobileMenuOpen(isOpen: boolean) {
      this.isMobileMenuOpen = isOpen
    },
    setRightAsideOpen(isOpen: boolean) {
      this.isRightAsideOpen = isOpen
    },
    setSongDrawerOpen(isOpen: boolean) {
      this.isSongDrawerOpen = isOpen
    },
  },
  persist: piniaPersistConfig('MenuStore'),
})
