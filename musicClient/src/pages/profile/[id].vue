<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import { fixUrl } from '@/utils'
import defaultAvatar from '@/assets/user.jpg'
import {
  followUser,
  getUserProfile,
  unfollowUser,
} from '@/api/system'
import type { UserProfile } from '@/api/interface'
import { UserStore } from '@/stores/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = UserStore()

const loading = ref(false)
const followLoading = ref(false)
const profile = ref<UserProfile | null>(null)
const activeTab = ref<'follower' | 'following' | 'favoriteSong' | 'favoritePlaylist'>('favoriteSong')

const profileUserId = computed(() => Number(route.params.id))
const isSelfProfile = computed(() => Number(userStore.userInfo.userId || 0) === profileUserId.value)
const followBtnText = computed(() => {
  if (followLoading.value) return '处理中...'
  if (!profile.value?.following) return '关注'
  return profile.value.mutualFollow ? '互相关注' : '已关注'
})

const followBtnIcon = computed(() => {
  if (followLoading.value) return 'svg-spinners:180-ring'
  if (!profile.value?.following) return 'mdi:account-plus-outline'
  return profile.value.mutualFollow ? 'mdi:account-check-outline' : 'mdi:check-circle-outline'
})

const loadProfile = async () => {
  if (!profileUserId.value) return
  loading.value = true
  try {
    const res = await getUserProfile(profileUserId.value)
    if (res.code === 0 && res.data) {
      profile.value = res.data
      return
    }
    ElMessage.error(res.message || '加载主页失败')
  } catch (error) {
    console.error(error)
    ElMessage.error('加载主页失败')
  } finally {
    loading.value = false
  }
}

const applyLocalFollowState = (nextFollowing: boolean) => {
  if (!profile.value) return
  const prevFollowing = !!profile.value.following
  if (prevFollowing === nextFollowing) return

  profile.value.following = nextFollowing
  const currentFollowerCount = profile.value.followerCount || 0
  profile.value.followerCount = Math.max(0, currentFollowerCount + (nextFollowing ? 1 : -1))
}

const handleFollow = async () => {
  if (!profile.value || followLoading.value) return
  const prevFollowing = !!profile.value.following
  followLoading.value = true
  try {
    const action = prevFollowing ? unfollowUser : followUser
    const res = await action(profile.value.userId)
    if (res.code !== 0) {
      ElMessage.error(res.message || '操作失败')
      return
    }
    applyLocalFollowState(!prevFollowing)
    ElMessage.success(prevFollowing ? '已取消关注' : '关注成功')
    await loadProfile()
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

const openUser = (userId: number) => {
  if (!userId) return
  router.push(`/profile/${userId}`)
}

const openPlaylist = (playlistId: number) => {
  router.push(`/playlist/${playlistId}`)
}

const openMessage = () => {
  if (!profile.value) return
  router.push({ path: '/messages', query: { friendId: profile.value.userId } })
}

watch(
  () => route.params.id,
  () => {
    loadProfile()
  }
)

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div v-if="profile" class="profile-header">
      <img :src="fixUrl(profile.userAvatar) || defaultAvatar" class="profile-avatar" alt="avatar" />
      <div class="profile-meta">
        <h1 class="profile-name">{{ profile.username }}</h1>
        <p class="profile-intro">{{ profile.introduction || '这个人很神秘，什么都没写。' }}</p>
        <div class="profile-counts">
          <span>粉丝 {{ profile.followerCount || 0 }}</span>
          <span>关注 {{ profile.followingCount || 0 }}</span>
        </div>
        <div class="profile-actions">
          <button
            v-if="!isSelfProfile"
            class="follow-btn"
            :class="{ active: profile.following, loading: followLoading }"
            :disabled="followLoading"
            @click="handleFollow"
          >
            <span class="follow-btn-inner">
              <Icon
                class="follow-btn-icon"
                :icon="followBtnIcon"
              />
              {{ followBtnText }}
            </span>
          </button>
          <button v-if="!isSelfProfile" class="message-btn" @click="openMessage">私信</button>
        </div>
      </div>
    </div>

    <div v-if="profile" class="tab-row">
      <button class="tab-btn" :class="{ active: activeTab === 'favoriteSong' }" @click="activeTab = 'favoriteSong'">收藏歌曲</button>
      <button class="tab-btn" :class="{ active: activeTab === 'favoritePlaylist' }" @click="activeTab = 'favoritePlaylist'">收藏歌单</button>
      <button class="tab-btn" :class="{ active: activeTab === 'following' }" @click="activeTab = 'following'">关注</button>
      <button class="tab-btn" :class="{ active: activeTab === 'follower' }" @click="activeTab = 'follower'">粉丝</button>
    </div>

    <div v-if="profile" class="content-card">
      <template v-if="activeTab === 'favoriteSong'">
        <div v-if="profile.favoriteSongs?.length" class="list-grid">
          <button v-for="song in profile.favoriteSongs" :key="song.songId" class="list-item" @click="router.push('/library?query=' + song.songName)">
            <img :src="fixUrl(song.coverUrl) || '/song.jpg'" alt="song" />
            <div>
              <p class="title">{{ song.songName }}</p>
              <p class="sub">{{ song.artistName }}</p>
            </div>
          </button>
        </div>
        <p v-else class="empty">暂无收藏歌曲</p>
      </template>

      <template v-if="activeTab === 'favoritePlaylist'">
        <div v-if="profile.favoritePlaylists?.length" class="list-grid">
          <button v-for="playlist in profile.favoritePlaylists" :key="playlist.playlistId" class="list-item" @click="openPlaylist(playlist.playlistId)">
            <img :src="fixUrl(playlist.coverUrl) || '/cover.png'" alt="playlist" />
            <div>
              <p class="title">{{ playlist.title }}</p>
              <p class="sub">歌单</p>
            </div>
          </button>
        </div>
        <p v-else class="empty">暂无收藏歌单</p>
      </template>

      <template v-if="activeTab === 'following'">
        <div v-if="profile.followingUsers?.length" class="user-grid">
          <button v-for="u in profile.followingUsers" :key="u.userId" class="user-item" @click="openUser(u.userId)">
            <img :src="fixUrl(u.userAvatar) || defaultAvatar" alt="avatar" />
            <div>
              <p class="title">{{ u.username }}</p>
              <p class="sub">{{ u.introduction || '这个人很神秘' }}</p>
            </div>
          </button>
        </div>
        <p v-else class="empty">暂无关注</p>
      </template>

      <template v-if="activeTab === 'follower'">
        <div v-if="profile.followers?.length" class="user-grid">
          <button v-for="u in profile.followers" :key="u.userId" class="user-item" @click="openUser(u.userId)">
            <img :src="fixUrl(u.userAvatar) || defaultAvatar" alt="avatar" />
            <div>
              <p class="title">{{ u.username }}</p>
              <p class="sub">{{ u.introduction || '这个人很神秘' }}</p>
            </div>
          </button>
        </div>
        <p v-else class="empty">暂无粉丝</p>
      </template>
    </div>
  </div>
</template>

<style scoped>
.profile-page { padding: 20px; height: 100%; overflow-y: auto; }
.profile-header { display: flex; gap: 16px; padding: 20px; border-radius: 12px; background: var(--bg-elevated, #1d1d1d); }
.profile-avatar { width: 92px; height: 92px; border-radius: 50%; object-fit: cover; }
.profile-meta { flex: 1; }
.profile-name { font-size: 1.4rem; color: var(--text-base, #fff); margin-bottom: 6px; }
.profile-intro { color: var(--text-subdued, #b3b3b3); margin-bottom: 8px; }
.profile-counts { display: flex; gap: 14px; color: var(--text-subdued, #b3b3b3); margin-bottom: 10px; }
.follow-btn { border: 1px solid var(--mr-accent); background: transparent; color: var(--mr-accent); padding: 6px 16px; border-radius: 999px; cursor: pointer; }
.follow-btn.active { background: var(--mr-accent); color: #000; }
.follow-btn.active:hover { background: var(--mr-accent); border-color: var(--mr-accent); color: #000; }
.follow-btn.loading { opacity: 0.8; cursor: wait; }
.follow-btn:disabled { cursor: not-allowed; }
.follow-btn-inner { display: inline-flex; align-items: center; gap: 6px; }
.follow-btn-icon { font-size: 0.95rem; }
.profile-actions { display: flex; gap: 8px; }
.message-btn { border: 1px solid var(--border-color, rgba(255,255,255,.22)); background: transparent; color: var(--text-base, #fff); padding: 6px 16px; border-radius: 999px; cursor: pointer; }
.message-btn:hover { border-color: var(--text-base, #fff); }
.tab-row { margin: 14px 0; display: flex; flex-wrap: wrap; gap: 8px; }
.tab-btn { border: 0; border-radius: 999px; padding: 6px 14px; background: var(--bg-elevated, #242424); color: var(--text-subdued, #b3b3b3); cursor: pointer; }
.tab-btn.active { background: var(--mr-accent); color: #000; }
.content-card { border-radius: 12px; background: var(--bg-elevated, #1d1d1d); padding: 16px; }
.list-grid, .user-grid { display: grid; gap: 10px; }
.list-item, .user-item { display: flex; gap: 10px; align-items: center; border: 0; border-radius: 10px; background: var(--bg-surface, #121212); padding: 10px; color: inherit; cursor: pointer; text-align: left; }
.list-item img, .user-item img { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; }
.user-item img { border-radius: 50%; }
.title { color: var(--text-base, #fff); font-size: 0.9rem; }
.sub { color: var(--text-subdued, #b3b3b3); font-size: 0.75rem; }
.empty { color: var(--text-subdued, #b3b3b3); text-align: center; padding: 24px 0; }

:root:not(.dark) .profile-page {
  --bg-elevated: #f0f0f0;
  --bg-surface: #ffffff;
  --text-base: #111111;
  --text-subdued: #666666;
  --border-color: rgba(0, 0, 0, 0.15);
}

:root:not(.dark) .profile-header,
:root:not(.dark) .content-card {
  background: #f5f5f5;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

:root:not(.dark) .tab-btn {
  background: #e9e9e9;
  color: #666;
}

:root:not(.dark) .tab-btn.active {
  background: var(--mr-accent);
  color: #000;
}

:root:not(.dark) .list-item,
:root:not(.dark) .user-item {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

@media (max-width: 768px) {
  .profile-page { padding: 12px; padding-top: 56px; }
  .profile-header { flex-direction: column; align-items: center; text-align: center; padding: 16px; gap: 12px; }
  .profile-avatar { width: 72px; height: 72px; }
  .profile-name { font-size: 1.2rem; }
  .profile-intro { font-size: 0.85rem; }
  .profile-counts { justify-content: center; font-size: 0.85rem; }
  .profile-actions { justify-content: center; }
  .tab-row { gap: 6px; margin: 12px 0; }
  .tab-btn { padding: 5px 12px; font-size: 0.8125rem; }
  .content-card { padding: 12px; }
  .list-item, .user-item { padding: 8px; gap: 8px; }
  .list-item img, .user-item img { width: 38px; height: 38px; border-radius: 6px; }
  .title { font-size: 0.85rem; }
  .sub { font-size: 0.7rem; }
  .list-item div, .user-item div { min-width: 0; text-align: left; }
  .title, .sub { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
</style>

