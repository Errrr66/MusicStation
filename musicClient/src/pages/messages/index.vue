<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import { fixUrl } from '@/utils'
import defaultAvatar from '@/assets/user.jpg'
import {
  getConversations,
  getFollowerUsers,
  getFollowingUsers,
  getPrivateMessages,
  sendPrivateMessage,
} from '@/api/system'
import type { Conversation, PrivateMessage, UserSimple } from '@/api/interface'
import { UserStore } from '@/stores/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = UserStore()

type UiConversation = Conversation & { canChat: boolean }

const loading = ref(false)
const conversations = ref<UiConversation[]>([])
const selectedFriendId = ref<number | null>(null)
const messages = ref<PrivateMessage[]>([])
const inputText = ref('')
const pendingSongId = ref<number | null>(null)
const pendingPlaylistId = ref<number | null>(null)
const mobileConversationOpen = ref(true)
const pollingTimer = ref<number | null>(null)
const loadingConversations = ref(false)
const pollingBusy = ref(false)
const pollingEnabled = ref(false)
const ACTIVE_POLLING_INTERVAL = 5000
const IDLE_POLLING_INTERVAL = 15000

const selectedConversation = computed(() => conversations.value.find((item) => item.friendUserId === selectedFriendId.value) || null)
const hasPendingShare = computed(() => Boolean(pendingSongId.value || pendingPlaylistId.value))
const canChatWithSelected = computed(() => Boolean(selectedConversation.value?.canChat))

const openProfile = (userId?: number | null) => {
  if (!userId) return
  router.push(`/profile/${userId}`)
}

const syncRouteShareQuery = () => {
  const queryFriendId = Number(route.query.friendId || 0)
  const querySongId = Number(route.query.songId || 0)
  const queryPlaylistId = Number(route.query.playlistId || 0)

  pendingSongId.value = querySongId || null
  pendingPlaylistId.value = queryPlaylistId || null

  if (queryFriendId) {
    selectedFriendId.value = queryFriendId
    mobileConversationOpen.value = false
  }
}

const mergeConversations = (
  existing: Conversation[],
  following: UserSimple[],
  followerIdSet: Set<number>
): UiConversation[] => {
  const merged = new Map<number, UiConversation>()

  existing.forEach((item) => {
    merged.set(item.friendUserId, { ...item, canChat: true })
  })

  following.forEach((u) => {
    if (merged.has(u.userId)) return
    const isMutual = followerIdSet.has(u.userId)
    merged.set(u.userId, {
      friendUserId: u.userId,
      friendUsername: u.username,
      friendAvatar: u.userAvatar || '',
      lastMessage: isMutual ? '开始聊天吧' : '已关注，等待对方回关',
      lastMessageType: 'TEXT',
      unreadCount: 0,
      updatedAt: '',
      canChat: isMutual,
    })
  })

  return Array.from(merged.values()).sort((a, b) => {
    if (a.canChat !== b.canChat) {
      return a.canChat ? -1 : 1
    }
    const aTime = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
    const bTime = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
    return bTime - aTime
  })
}

const loadConversations = async (options: { silent?: boolean } = {}) => {
  if (loadingConversations.value) return
  if (!options.silent) {
    loadingConversations.value = true
  }
  const requestOptions = options.silent
    ? { meta: { silentProgress: true, silentError: true } }
    : undefined
  try {
    const [conversationRes, followingRes, followerRes] = await Promise.all([
      getConversations(requestOptions),
      userStore.userInfo.userId
        ? getFollowingUsers(Number(userStore.userInfo.userId), requestOptions)
        : Promise.resolve({ code: 0, data: [] as UserSimple[] }),
      userStore.userInfo.userId
        ? getFollowerUsers(Number(userStore.userInfo.userId), requestOptions)
        : Promise.resolve({ code: 0, data: [] as UserSimple[] }),
    ])

    if (conversationRes.code === 0 && conversationRes.data) {
      const following = followingRes.code === 0 && followingRes.data ? followingRes.data : []
      const followerIdSet = new Set<number>(
        followerRes.code === 0 && followerRes.data
          ? followerRes.data.map((item) => item.userId)
          : []
      )
      const nextConversations = mergeConversations(conversationRes.data, following, followerIdSet)
      if (conversationSignature(nextConversations) !== conversationSignature(conversations.value)) {
        conversations.value = nextConversations
      }
      if (!selectedFriendId.value && conversations.value.length) {
        selectedFriendId.value = conversations.value[0].friendUserId
      }
      return
    }
    if (!options.silent) {
      ElMessage.error(conversationRes.message || '会话加载失败')
    }
  } catch (error) {
    console.error(error)
    if (!options.silent) {
      ElMessage.error('会话加载失败')
    }
  } finally {
    if (!options.silent) {
      loadingConversations.value = false
    }
  }
}

const loadMessages = async (options: { silent?: boolean } = {}) => {
  if (!selectedFriendId.value) return
  if (!canChatWithSelected.value) {
    messages.value = []
    return
  }
  if (!options.silent) {
    loading.value = true
  }
  try {
    const res = await getPrivateMessages(
      selectedFriendId.value,
      1,
      60,
      options.silent ? { meta: { silentProgress: true, silentError: true } } : undefined
    )
    if (res.code === 0 && res.data) {
      if (messageSignature(res.data) !== messageSignature(messages.value)) {
        messages.value = res.data
      }
      return
    }
    if (!options.silent) {
      ElMessage.error(res.message || '消息加载失败')
    }
  } catch (error) {
    console.error(error)
    if (!options.silent) {
      ElMessage.error('消息加载失败')
    }
  } finally {
    if (!options.silent) {
      loading.value = false
    }
  }
}

const sendText = async () => {
  if (!canChatWithSelected.value) {
    ElMessage.warning('需互相关注后才可私信')
    return
  }
  if (!selectedFriendId.value || !inputText.value.trim()) return
  const res = await sendPrivateMessage({
    toUserId: selectedFriendId.value,
    messageType: 'TEXT',
    content: inputText.value.trim(),
  })
  if (res.code !== 0) {
    ElMessage.error(res.message || '发送失败')
    return
  }
  inputText.value = ''
  await loadMessages()
  await loadConversations()
}

const clearShare = () => {
  pendingSongId.value = null
  pendingPlaylistId.value = null
}

const sendShare = async () => {
  if (!canChatWithSelected.value) {
    ElMessage.warning('需互相关注后才可私信')
    return
  }
  if (!selectedFriendId.value) {
    ElMessage.warning('请先选择一位好友')
    if (window.innerWidth <= 768) {
      mobileConversationOpen.value = true
    }
    return
  }
  if (!hasPendingShare.value) return
  const payload = pendingSongId.value
    ? { toUserId: selectedFriendId.value, messageType: 'SONG', songId: pendingSongId.value }
    : { toUserId: selectedFriendId.value, messageType: 'PLAYLIST', playlistId: pendingPlaylistId.value! }
  const res = await sendPrivateMessage(payload)
  if (res.code !== 0) {
    ElMessage.error(res.message || '分享失败')
    return
  }
  ElMessage.success('已分享给好友')
  clearShare()
  if (route.query.songId || route.query.playlistId) {
    const nextQuery = { ...route.query } as Record<string, any>
    delete nextQuery.songId
    delete nextQuery.playlistId
    delete nextQuery.shareAt
    await router.replace({ path: route.path, query: nextQuery })
  }
  await loadMessages()
  await loadConversations()
}

const selectConversation = (friendId: number) => {
  selectedFriendId.value = friendId
  mobileConversationOpen.value = false
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return ''
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

const messagePreview = (item: UiConversation) => {
  if (!item.canChat) return '已关注，等待对方回关'
  if (item.lastMessageType === 'SONG') return '分享了一首歌曲'
  if (item.lastMessageType === 'PLAYLIST') return '分享了一个歌单'
  return item.lastMessage || '开始聊天吧'
}

const getPollingInterval = () => {
  if (selectedFriendId.value && !document.hidden) {
    return ACTIVE_POLLING_INTERVAL
  }
  return IDLE_POLLING_INTERVAL
}

const conversationSignature = (list: UiConversation[]) =>
  list
    .map((item) => `${item.friendUserId}|${item.unreadCount}|${item.updatedAt}|${item.canChat}|${item.lastMessageType}|${item.lastMessage}`)
    .join(';')

const messageSignature = (list: PrivateMessage[]) =>
  list
    .map((item) => `${item.id}|${item.messageType}|${item.createTime}|${item.content || ''}|${item.readStatus}`)
    .join(';')

const openSharedTarget = (msg: PrivateMessage) => {
  if (msg.messageType === 'SONG') {
    router.push({
      path: '/library',
      query: { query: msg.songName || String(msg.songId || '') },
    })
    return
  }
  if (msg.messageType === 'PLAYLIST' && msg.playlistId) {
    router.push(`/playlist/${msg.playlistId}`)
  }
}

const isMe = (msg: PrivateMessage) => msg.fromUserId !== selectedFriendId.value

const bubbleText = (msg: PrivateMessage) => {
  if (msg.messageType === 'SONG') return `分享了歌曲 ${msg.songName || `#${msg.songId}`}`
  if (msg.messageType === 'PLAYLIST') return `分享了歌单 ${msg.playlistTitle || `#${msg.playlistId}`}`
  return msg.content || ''
}

const startPolling = () => {
  if (pollingTimer.value) return
  pollingEnabled.value = true
  const poll = async () => {
    if (!pollingEnabled.value) {
      pollingTimer.value = null
      return
    }
    if (document.hidden || pollingBusy.value) {
      if (pollingEnabled.value) {
        pollingTimer.value = window.setTimeout(poll, getPollingInterval())
      }
      return
    }
    pollingBusy.value = true
    try {
      await loadConversations({ silent: true })
      await loadMessages({ silent: true })
    } finally {
      pollingBusy.value = false
      if (pollingEnabled.value) {
        pollingTimer.value = window.setTimeout(poll, getPollingInterval())
      }
    }
  }
  pollingTimer.value = window.setTimeout(poll, getPollingInterval())
}

const stopPolling = () => {
  pollingEnabled.value = false
  if (!pollingTimer.value) return
  window.clearTimeout(pollingTimer.value)
  pollingTimer.value = null
}

const restartPolling = () => {
  stopPolling()
  startPolling()
}

const onVisibilityChange = () => {
  if (!document.hidden) {
    void loadConversations({ silent: true })
    void loadMessages({ silent: true })
    startPolling()
  } else {
    stopPolling()
  }
}

watch(selectedFriendId, () => {
  loadMessages()
  restartPolling()
})

watch(
  () => [route.query.friendId, route.query.songId, route.query.playlistId, route.query.shareAt],
  () => {
    syncRouteShareQuery()
  }
)

onMounted(async () => {
  await loadConversations()
  syncRouteShareQuery()
  startPolling()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="mr-message-page" :class="{ 'mr-conversation-open': mobileConversationOpen }">
    <aside class="mr-conversation-panel" :class="{ 'mr-conversation-hidden': !mobileConversationOpen }">
      <div class="mr-panel-title-row">
        <h3>私信</h3>
      </div>

      <div v-if="!conversations.length" class="mr-empty-panel">
        <p>暂无会话，先去关注好友吧</p>
      </div>

      <button
        v-for="item in conversations"
        :key="item.friendUserId"
        class="mr-conversation-item"
        :class="{ 'mr-conversation-active': item.friendUserId === selectedFriendId }"
        @click="selectConversation(item.friendUserId)"
      >
        <img :src="fixUrl(item.friendAvatar) || defaultAvatar" alt="avatar" @click.stop="openProfile(item.friendUserId)" />
        <div class="mr-meta">
          <div class="mr-meta-top">
            <p class="mr-name" @click.stop="openProfile(item.friendUserId)">{{ item.friendUsername }}</p>
            <span class="mr-time">{{ formatTime(item.updatedAt) }}</span>
          </div>
          <div class="mr-meta-bottom">
            <p class="mr-last">{{ messagePreview(item) }}</p>
            <div class="mr-meta-actions">
              <span v-if="!item.canChat" class="mr-lock">待回关</span>
              <span v-else-if="item.unreadCount" class="mr-unread">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</span>
              <button class="mr-profile-btn" title="访问主页" @click.stop="openProfile(item.friendUserId)">
                <Icon icon="mdi:account-circle-outline" />
              </button>
            </div>
          </div>
        </div>
      </button>
    </aside>

    <section class="mr-chat-panel">
      <header class="mr-chat-header">
        <button class="mr-back-btn" @click="mobileConversationOpen = true">
          返回会话
        </button>
        <h3 class="mr-chat-title" @click="openProfile(selectedConversation?.friendUserId)">
          {{ selectedConversation?.friendUsername || '选择好友开始聊天' }}
        </h3>

        <div v-if="hasPendingShare" class="mr-share-row">
          <span class="mr-share-chip">
            {{ pendingSongId ? `待分享歌曲 #${pendingSongId}` : `待分享歌单 #${pendingPlaylistId}` }}
          </span>
          <button class="mr-share-btn" @click="sendShare">发送分享</button>
          <button class="mr-share-clear-btn" @click="clearShare">取消</button>
        </div>
      </header>

      <div class="mr-chat-body" v-loading="loading">
        <div v-if="!selectedFriendId" class="mr-empty-chat">
          <p>从左侧选择一个好友开始聊天</p>
        </div>

        <div v-else-if="!canChatWithSelected" class="mr-empty-chat">
          <p>你已关注对方，等待对方回关后即可私信</p>
        </div>

        <div v-else-if="!messages.length" class="mr-empty-chat">
          <p>还没有历史消息，打个招呼吧</p>
        </div>

        <div v-else v-for="msg in messages" :key="msg.id" class="mr-msg-row" :class="{ 'mr-msg-me': isMe(msg) }">
          <div v-if="msg.messageType === 'TEXT'" class="mr-bubble">
            <span>{{ bubbleText(msg) }}</span>
            <span class="mr-bubble-time">{{ formatTime(msg.createTime) }}</span>
          </div>

          <div v-else class="mr-share-card" @click="openSharedTarget(msg)">
            <img
              v-if="msg.messageType === 'SONG'"
              :src="fixUrl(msg.songCoverUrl || '') || '/song.jpg'"
              alt="song cover"
              class="mr-share-cover"
            />
            <img
              v-else
              :src="fixUrl(msg.playlistCoverUrl || '') || '/cover.png'"
              alt="playlist cover"
              class="mr-share-cover"
            />
            <div class="mr-share-meta">
              <span class="mr-type-tag">{{ msg.messageType }}</span>
              <p class="mr-share-title">
                {{ msg.messageType === 'SONG' ? (msg.songName || `歌曲 #${msg.songId}`) : (msg.playlistTitle || `歌单 #${msg.playlistId}`) }}
              </p>
              <p class="mr-share-subtitle" v-if="msg.messageType === 'SONG'">{{ msg.songArtistName || '未知歌手' }}</p>
              <p class="mr-share-subtitle" v-else>点击查看歌单</p>
            </div>
            <span class="mr-bubble-time">{{ formatTime(msg.createTime) }}</span>
          </div>
        </div>
      </div>

      <footer class="mr-chat-input">
        <input
          v-model="inputText"
          placeholder="输入消息..."
          :disabled="!selectedFriendId || !canChatWithSelected"
          @keyup.enter="sendText"
        />
        <button :disabled="!selectedFriendId || !inputText.trim() || !canChatWithSelected" @click="sendText">发送</button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.mr-message-page {
  --msg-bubble-bg: var(--bg-hover, rgba(255, 255, 255, 0.08));
  --msg-bubble-color: var(--text-base, #fff);
  --msg-lock-color: #f59e0b;
  --msg-lock-border: rgba(245, 158, 11, 0.5);
  --msg-share-me-bg: color-mix(in srgb, var(--mr-accent) 14%, transparent);
  --msg-share-me-border: color-mix(in srgb, var(--mr-accent) 50%, transparent);

  display: grid;
  grid-template-columns: 300px 1fr;
  height: 100%;
  gap: 12px;
}

.mr-conversation-panel,
.mr-chat-panel {
  border-radius: 12px;
  background: var(--bg-elevated, #1d1d1d);
}

.mr-conversation-panel {
  padding: 12px;
  overflow-y: auto;
}

.mr-panel-title-row h3 {
  color: var(--text-base, #fff);
  margin: 2px 4px 10px;
}

.mr-conversation-item {
  width: 100%;
  border: 0;
  background: transparent;
  color: inherit;
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
}

.mr-conversation-item img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
}

.mr-conversation-item:hover,
.mr-conversation-active {
  background: var(--bg-hover, rgba(255, 255, 255, 0.08));
}

.mr-meta {
  min-width: 0;
  flex: 1;
}

.mr-meta-top,
.mr-meta-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.mr-meta-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mr-name {
  color: var(--text-base, #fff);
  font-size: 0.9rem;
  cursor: pointer;
}

.mr-time,
.mr-last {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.75rem;
}

.mr-last {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mr-unread {
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: var(--mr-accent);
  color: #000;
  font-size: 0.6875rem;
  line-height: 18px;
  text-align: center;
  padding: 0 5px;
}

.mr-lock {
  font-size: 0.6875rem;
  color: var(--msg-lock-color);
  border: 1px solid var(--msg-lock-border);
  border-radius: 999px;
  padding: 1px 6px;
}

.mr-profile-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.2));
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
}

.mr-profile-btn:hover {
  color: var(--text-base, #fff);
  border-color: var(--text-base, #fff);
}

.mr-chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.mr-chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.08));
}

.mr-chat-header h3 {
  color: var(--text-base, #fff);
}

.mr-chat-title {
  cursor: pointer;
}

.mr-back-btn {
  display: none;
  margin-bottom: 8px;
  border: 0;
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
}

.mr-share-row {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.mr-share-chip {
  border-radius: 999px;
  border: 1px solid var(--mr-accent);
  color: var(--mr-accent);
  padding: 4px 10px;
  font-size: 0.75rem;
}

.mr-share-btn,
.mr-share-clear-btn {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.75rem;
  cursor: pointer;
}

.mr-share-btn {
  border: 0;
  background: var(--mr-accent);
  color: #000;
}

.mr-share-clear-btn {
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.2));
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
}

.mr-chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mr-msg-row {
  display: flex;
}

.mr-msg-me {
  justify-content: flex-end;
}

.mr-bubble {
  max-width: min(72%, 560px);
  background: var(--msg-bubble-bg);
  color: var(--msg-bubble-color);
  padding: 8px 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mr-msg-me .mr-bubble {
  background: var(--mr-accent);
  color: #000;
}

.mr-share-card {
  max-width: min(72%, 560px);
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
}

.mr-msg-me .mr-share-card {
  border-color: var(--msg-share-me-border);
  background: var(--msg-share-me-bg);
}

.mr-share-cover {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.mr-share-meta {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mr-share-title {
  color: var(--text-base, #fff);
  font-size: 0.875rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mr-share-subtitle {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.75rem;
}

.mr-type-tag {
  font-size: 0.625rem;
  border-radius: 999px;
  padding: 1px 6px;
  border: 1px solid currentColor;
  opacity: 0.8;
}

.mr-bubble-time {
  margin-left: 4px;
  font-size: 0.625rem;
  opacity: 0.8;
}

.mr-chat-input {
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.08));
  padding: 12px;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.mr-chat-input input {
  flex: 1;
  border: 0;
  border-radius: 999px;
  padding: 10px 14px;
  background: var(--bg-surface, #121212);
  color: var(--text-base, #fff);
}

.mr-chat-input button {
  border: 0;
  border-radius: 999px;
  padding: 0 16px;
  background: var(--mr-accent);
  color: #000;
  font-weight: 600;
  cursor: pointer;
}

.mr-chat-input button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.mr-empty-chat,
.mr-empty-panel {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
  text-align: center;
  padding: 24px 8px;
}

:root:not(.dark) .mr-message-page {
  --bg-elevated: #f0f0f0;
  --bg-surface: #ffffff;
  --bg-hover: rgba(0, 0, 0, 0.06);
  --text-base: #000;
  --text-subdued: #666;
  --border-color: rgba(0, 0, 0, 0.12);
  --msg-bubble-bg: #efefef;
  --msg-bubble-color: #111;
  --msg-share-me-bg: color-mix(in srgb, var(--mr-accent) 20%, transparent);
  --msg-share-me-border: color-mix(in srgb, var(--mr-accent) 50%, transparent);
}

:root:not(.dark) .mr-share-card {
  background: var(--bg-surface);
  border-color: var(--border-color);
}

:root:not(.dark) .mr-msg-me .mr-bubble {
  background: var(--mr-accent);
  color: #000;
}

@media (max-width: 768px) {
  .mr-message-page {
    grid-template-columns: 1fr;
    position: relative;
    gap: 0;
    padding: 0 0 140px 0;
  }

  .mr-conversation-panel,
  .mr-chat-panel {
    border-radius: 0;
    height: 100%;
  }

  .mr-conversation-panel {
    max-height: none;
    padding: 12px;
  }

  .mr-conversation-open .mr-chat-panel {
    display: none;
  }

  .mr-message-page:not(.mr-conversation-open) .mr-conversation-panel {
    display: none;
  }

  .mr-conversation-hidden {
    display: none;
  }

  .mr-back-btn {
    display: inline-block;
  }

  .mr-chat-header {
    padding: 12px;
  }

  .mr-chat-title {
    font-size: 0.9375rem;
  }

  .mr-share-row {
    width: 100%;
    margin-top: 10px;
  }

  .mr-chat-body {
    padding: 12px;
  }

  .mr-bubble {
    max-width: 84%;
    padding: 8px 10px;
  }

  .mr-share-card {
    max-width: 84%;
    padding: 8px;
  }

  .mr-share-cover {
    width: 40px;
    height: 40px;
  }

  .mr-chat-input {
    padding: 10px 12px;
  }

  .mr-chat-input input {
    padding: 8px 12px;
  }

  .mr-chat-input button {
    padding: 0 14px;
  }
}
</style>








