<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { searchAll } from '@/api/system'
import { fixUrl } from '@/utils'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const activeTab = ref<'song' | 'artist' | 'playlist' | 'user'>('song')
const songs = ref<any[]>([])
const artists = ref<any[]>([])
const playlists = ref<any[]>([])
const users = ref<any[]>([])

const runSearch = async () => {
  const query = String(route.query.query || '').trim()
  keyword.value = query
  if (!query) {
    songs.value = []
    artists.value = []
    playlists.value = []
    users.value = []
    return
  }
  loading.value = true
  try {
    const res = await searchAll(query, 30)
    if (res.code === 0 && res.data) {
      songs.value = res.data.songs || []
      artists.value = res.data.artists || []
      playlists.value = res.data.playlists || []
      users.value = res.data.users || []
    }
  } finally {
    loading.value = false
  }
}

watch(() => route.query.query, runSearch)
onMounted(runSearch)
</script>

<template>
  <div class="search-page" v-loading="loading">
    <h2 class="title">搜索：{{ keyword || '请输入关键词' }}</h2>

    <div class="tabs">
      <button class="tab" :class="{ active: activeTab === 'song' }" @click="activeTab = 'song'">歌曲</button>
      <button class="tab" :class="{ active: activeTab === 'artist' }" @click="activeTab = 'artist'">歌手</button>
      <button class="tab" :class="{ active: activeTab === 'playlist' }" @click="activeTab = 'playlist'">歌单</button>
      <button class="tab" :class="{ active: activeTab === 'user' }" @click="activeTab = 'user'">用户</button>
    </div>

    <div class="card" v-if="activeTab === 'song'">
      <button v-for="song in songs" :key="song.songId" class="item" @click="router.push('/library?query=' + song.songName)">
        <img :src="fixUrl(song.coverUrl) || '/song.jpg'" alt="song" />
        <div><p>{{ song.songName }}</p><span>{{ song.artistName }}</span></div>
      </button>
      <p v-if="!songs.length" class="empty">没有找到歌曲</p>
    </div>

    <div class="card" v-if="activeTab === 'artist'">
      <button v-for="artist in artists" :key="artist.artistId" class="item" @click="router.push('/artist/' + artist.artistId)">
        <img :src="fixUrl(artist.avatar) || '/user.jpg'" alt="artist" class="circle" />
        <div><p>{{ artist.artistName }}</p><span>{{ artist.area || '未知地区' }}</span></div>
      </button>
      <p v-if="!artists.length" class="empty">没有找到歌手</p>
    </div>

    <div class="card" v-if="activeTab === 'playlist'">
      <button v-for="playlist in playlists" :key="playlist.playlistId" class="item" @click="router.push('/playlist/' + playlist.playlistId)">
        <img :src="fixUrl(playlist.coverUrl) || '/cover.png'" alt="playlist" />
        <div><p>{{ playlist.title }}</p><span>歌单</span></div>
      </button>
      <p v-if="!playlists.length" class="empty">没有找到歌单</p>
    </div>

    <div class="card" v-if="activeTab === 'user'">
      <button v-for="user in users" :key="user.userId" class="item" @click="router.push('/profile/' + user.userId)">
        <img :src="fixUrl(user.userAvatar) || '/user.jpg'" alt="user" class="circle" />
        <div><p>{{ user.username }}</p><span>{{ user.introduction || '暂无简介' }}</span></div>
      </button>
      <p v-if="!users.length" class="empty">没有找到用户</p>
    </div>
  </div>
</template>

<style scoped>
.search-page { height: 100%; overflow-y: auto; padding: 20px; }
.title { color: var(--text-base, #fff); margin-bottom: 12px; }
.tabs { display: flex; gap: 8px; margin-bottom: 12px; }
.tab { border: 0; border-radius: 999px; padding: 6px 14px; background: var(--bg-elevated, #242424); color: var(--text-subdued, #b3b3b3); cursor: pointer; }
.tab.active { background: #1db954; color: #000; }
.card { background: var(--bg-elevated, #1d1d1d); border-radius: 12px; padding: 12px; display: grid; gap: 8px; }
.item { border: 0; background: var(--bg-surface, #121212); border-radius: 10px; padding: 10px; display: flex; align-items: center; gap: 10px; color: inherit; cursor: pointer; text-align: left; }
.item img { width: 42px; height: 42px; border-radius: 8px; object-fit: cover; }
.item img.circle { border-radius: 50%; }
.item p { color: var(--text-base, #fff); font-size: .9rem; }
.item span { color: var(--text-subdued, #b3b3b3); font-size: .75rem; }
.empty { color: var(--text-subdued, #b3b3b3); text-align: center; padding: 24px 0; }
</style>

