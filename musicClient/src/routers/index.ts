import {
  createRouter,
  createWebHistory,
  createWebHashHistory,
} from 'vue-router'
import { UserStore } from '@/stores/modules/user'

const mode = import.meta.env.VITE_ROUTER_MODE

const routerMode = {
  hash: () => createWebHashHistory(),
  history: () => createWebHistory(),
}

const router = createRouter({
  history: routerMode[mode](),
  strict: false,
  scrollBehavior: () => ({ left: 0, top: 0 }),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/pages/index.vue'),
    },
    {
      path: '/library',
      name: 'Library',
      component: () => import('@/pages/library/index.vue'),
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('@/pages/search/index.vue'),
    },
    {
      path: '/artist',
      name: 'ArtistList',
      component: () => import('@/pages/artist/index.vue'),
    },
    {
      path: '/artist/:id',
      name: 'ArtistDetail',
      component: () => import('@/pages/artist/[id].vue'),
    },
    {
      path: '/playlist',
      name: 'PlaylistList',
      component: () => import('@/pages/playlist/index.vue'),
    },
    {
      path: '/playlist/:id',
      name: 'PlaylistDetail',
      component: () => import('@/pages/playlist/[id].vue'),
    },
    {
      path: '/like',
      name: 'Like',
      component: () => import('@/pages/like/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/user',
      name: 'User',
      component: () => import('@/pages/user/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/:id',
      name: 'Profile',
      component: () => import('@/pages/profile/[id].vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/messages',
      name: 'Messages',
      component: () => import('@/pages/messages/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/pages/chat/index.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/pages/index.vue'),
    },
  ],
})

// 全局路由守卫：对 meta.requiresAuth 路由统一鉴权
router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    const userStore = UserStore()
    if (!userStore.isLoggedIn) {
      next({ path: '/', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
