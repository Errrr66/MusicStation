<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import {
  sendAgentMessage,
  sendAgentMessageStream,
  saveAgentPlaylist,
  type ChatMessage,
  type AgentChatResponse,
  type AgentSongCard,
  type AgentPlaylistCard,
} from '@/api/chat'
import { getAllSongs } from '@/api/system'
import type { Song } from '@/api/interface'
import SongRecognizer from '@/components/SongRecognizer.vue'
import { useFavoriteStore } from '@/stores/modules/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'
import Matrix from '@/components/Matrix/index.vue'
import { imageToColorFrame, createAnimatedFrames, type ColorFrame, type AnimationType } from '@/utils/matrix'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { AudioStore } from '@/stores/modules/audio'
import defaultAlbum from '@/assets/default_album.jpg'

type TimelineMessage = ChatMessage & { agentData?: AgentChatResponse }

const messages = ref<TimelineMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const voiceEnabled = ref(true)
const scrollbarRef = ref<HTMLElement | null>(null)
const matrixFrames = ref<ColorFrame[]>([])
const matrixLoading = ref(true)
const colorFrameRef = ref<ColorFrame | null>(null)
const savingPlaylistKeys = ref<Record<string, boolean>>({})
const audioStore = AudioStore()
const favoriteStore = useFavoriteStore()
const router = useRouter()
const { play, pause, nextTrack, prevTrack, loadTrack, currentTrack } = useAudioPlayer()

const recognizedTrack = ref<any>(null)
const localSongMatch = ref<Song | null>(null)
const speechRecognition = ref<any>(null)
const isLiveVoiceListening = ref(false)
const voiceHint = ref('')
const currentTtsAudio = ref<HTMLAudioElement | null>(null)
const streamState = ref<'idle' | 'connecting' | 'streaming' | 'done' | 'error'>('idle')
const streamFirstDeltaMs = ref<number | null>(null)
const streamTotalMs = ref<number | null>(null)
const streamStartedAt = ref<number | null>(null)
const streamAbortController = ref<AbortController | null>(null)
const canResumeAfterAbort = ref(false)
const lastAbortedAssistantIndex = ref<number | null>(null)
const lastRequestPayload = ref<{
  message?: string
  messages?: ChatMessage[]
  nowPlaying?: { songId?: string; title?: string; artist?: string; album?: string }
  limit?: number
  enableVoice?: boolean
  playlistSeeds?: Array<{ songId: number; songName: string; artistName: string; style?: string }>
} | null>(null)

const animationModes: AnimationType[] = ['float', 'sparkle', 'sparkle', 'float']
const currentModeIndex = ref(0)
const modeTimerId = ref<number | undefined>(undefined)

const scrollToBottom = async () => {
  await nextTick()
  if (scrollbarRef.value) {
    scrollbarRef.value.scrollTop = scrollbarRef.value.scrollHeight
  }
}

function updateAnimationMode() {
  if (!colorFrameRef.value) return
  currentModeIndex.value = (currentModeIndex.value + 1) % animationModes.length
  matrixFrames.value = createAnimatedFrames(colorFrameRef.value, animationModes[currentModeIndex.value], 12)
}

onMounted(async () => {
  initLiveSpeech()
  try {
    const colorFrame = await imageToColorFrame('/thinking.png', 45, 54)
    colorFrameRef.value = colorFrame
    matrixFrames.value = createAnimatedFrames(colorFrame, animationModes[0], 12)
    
    modeTimerId.value = window.setInterval(() => {
      updateAnimationMode()
    }, 5000)
  } catch (error) {
    console.error('Failed to load matrix frame:', error)
  } finally {
    matrixLoading.value = false
  }
})

onUnmounted(() => {
  if (streamAbortController.value) {
    streamAbortController.value.abort()
    streamAbortController.value = null
  }
  stopCurrentTtsAudio()
  isLiveVoiceListening.value = false
  if (speechRecognition.value) {
    speechRecognition.value.stop()
  }
  if (modeTimerId.value) {
    clearInterval(modeTimerId.value)
  }
})

const stopCurrentTtsAudio = () => {
  if (currentTtsAudio.value) {
    currentTtsAudio.value.pause()
    currentTtsAudio.value.currentTime = 0
    currentTtsAudio.value = null
  }
}

const runAgentRequest = async (
  payload: {
    message?: string
    messages?: ChatMessage[]
    nowPlaying?: { songId?: string; title?: string; artist?: string; album?: string }
    limit?: number
    enableVoice?: boolean
    playlistSeeds?: Array<{ songId: number; songName: string; artistName: string; style?: string }>
  },
  assistantIndex: number
) => {
  loading.value = true
  streamState.value = 'connecting'
  streamFirstDeltaMs.value = null
  streamTotalMs.value = null
  streamStartedAt.value = performance.now()
  streamAbortController.value = new AbortController()

  lastRequestPayload.value = payload
  canResumeAfterAbort.value = false
  lastAbortedAssistantIndex.value = null

  try {
    let streamDone = false
    await sendAgentMessageStream(payload, {
      onDelta: (chunk) => {
        if (streamState.value !== 'streaming') {
          streamState.value = 'streaming'
          if (streamStartedAt.value != null) {
            streamFirstDeltaMs.value = Math.round(performance.now() - streamStartedAt.value)
          }
        }
        messages.value[assistantIndex].content += chunk
        void scrollToBottom()
      },
      onDone: async (data) => {
        streamDone = true
        canResumeAfterAbort.value = false
        lastAbortedAssistantIndex.value = null
        streamState.value = 'done'
        if (streamStartedAt.value != null) {
          streamTotalMs.value = Math.round(performance.now() - streamStartedAt.value)
        }
        if (!messages.value[assistantIndex].content?.trim()) {
          messages.value[assistantIndex].content = data.answer || '已处理'
        }
        messages.value[assistantIndex].agentData = data

        if (data.intent === 'PLAYER_CONTROL' || data.playerCommand === 'play_target') {
          await executePlayerCommand(data.playerCommand)
        }

        if (data.playerCommand === 'play_target' && data.songs?.length) {
          await playAgentSong(data.songs[0])
        }

        if (data.audio) {
          const audio = new Audio(data.audio)
          currentTtsAudio.value = audio
          audio.play().catch(e => console.error('Audio play failed', e))
        }
      },
      onError: (message) => {
        streamState.value = 'error'
        ElMessage.error(message || '流式响应失败')
      },
    }, {
      signal: streamAbortController.value?.signal,
    })

    if (!streamDone) {
      const res = await sendAgentMessage(payload)
      if (res.code === 0 && res.data) {
        streamState.value = 'done'
        if (streamStartedAt.value != null) {
          streamTotalMs.value = Math.round(performance.now() - streamStartedAt.value)
        }
        messages.value[assistantIndex].content = res.data.answer || '已处理'
        messages.value[assistantIndex].agentData = res.data

        if (res.data.audio) {
          const audio = new Audio(res.data.audio)
          currentTtsAudio.value = audio
          audio.play().catch(e => console.error('Audio play failed', e))
        }
      } else {
        streamState.value = 'error'
        ElMessage.error(res.message || '发送失败')
      }
    }
  } catch (error) {
    if ((error as Error)?.name === 'AbortError') {
      streamState.value = 'idle'
      canResumeAfterAbort.value = true
      lastAbortedAssistantIndex.value = assistantIndex
      ElMessage.info('已中断本次回复')
    } else {
      streamState.value = 'error'
      console.error(error)
      ElMessage.error('网络错误')
    }
  } finally {
    if (streamState.value === 'connecting') {
      streamState.value = 'idle'
    }
    loading.value = false
    streamAbortController.value = null
    scrollToBottom()
  }
}

const sendByText = async (text: string) => {
  if (!text.trim()) return

  // barge-in: 用户发起新一轮时打断上一轮语音播放
  stopCurrentTtsAudio()

  const userMsg = text.trim()
  messages.value.push({ role: 'user', content: userMsg })
  const historyMessages = messages.value.map((item) => ({ role: item.role, content: item.content }))
  inputMessage.value = ''
  scrollToBottom()

  const assistantIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })

  await runAgentRequest({
    message: userMsg,
    messages: historyMessages,
    nowPlaying: {
      songId: currentTrack.value?.id,
      title: currentTrack.value?.title,
      artist: currentTrack.value?.artist,
      album: currentTrack.value?.album,
    },
    limit: 8,
    enableVoice: voiceEnabled.value,
  }, assistantIndex)
}

const stopStreaming = () => {
  if (!loading.value || !streamAbortController.value) {
    return
  }
  streamAbortController.value.abort()
}

const resumeStreaming = async () => {
  if (loading.value || !lastRequestPayload.value) {
    return
  }
  const targetIndex = lastAbortedAssistantIndex.value;
  const assistantIndex = targetIndex != null && messages.value[targetIndex]
    ? targetIndex
    : messages.value.push({ role: 'assistant', content: '' }) - 1

  messages.value[assistantIndex].content = ''
  messages.value[assistantIndex].agentData = undefined

  await runAgentRequest(lastRequestPayload.value, assistantIndex)
}

const handleSend = async () => {
  if (!inputMessage.value.trim()) return
  await sendByText(inputMessage.value)
}

const executePlayerCommand = async (command: string) => {
  switch (command) {
    case 'play':
      await play()
      break
    case 'pause':
      pause()
      break
    case 'next':
      await nextTrack()
      break
    case 'prev':
      await prevTrack()
      break
    default:
      break
  }
}

const handleRecognitionSuccess = async (result: any) => {
  recognizedTrack.value = result?.track || null
  if (!recognizedTrack.value) {
    ElMessage.warning('未能识别歌曲')
    return
  }

  localSongMatch.value = null
  try {
    const title = recognizedTrack.value.title || ''
    const artist = recognizedTrack.value.subtitle || ''
    const res = await getAllSongs({ songName: title, pageNum: 1, pageSize: 20 })
    if (res.code === 0 && res.data?.items?.length) {
      const exact = (res.data.items as Song[]).find((item) => {
        const itemArtist = (item.artistName || '').toLowerCase()
        return artist ? itemArtist.includes(artist.toLowerCase()) : true
      })
      localSongMatch.value = exact || (res.data.items[0] as Song)
    }
  } catch (error) {
    console.error('识曲后本地匹配失败', error)
  }

  if (localSongMatch.value) {
    await playAgentSong({
      songId: localSongMatch.value.songId,
      songName: localSongMatch.value.songName,
      artistName: localSongMatch.value.artistName,
      album: localSongMatch.value.album,
      coverUrl: localSongMatch.value.coverUrl,
      audioUrl: localSongMatch.value.audioUrl,
    })
    ElMessage.success('已识别并播放本地曲库歌曲')
    return
  }

  inputMessage.value = `播放 ${recognizedTrack.value.title || ''} ${recognizedTrack.value.subtitle || ''}`.trim()
  ElMessage.info('已填入识曲结果，回车即可让 AI 播放')
}

const initLiveSpeech = () => {
  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SpeechRecognition) {
    return
  }
  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.interimResults = true
  recognition.continuous = true

  recognition.onresult = async (event: any) => {
    let finalText = ''
    let interimText = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript || ''
      if (event.results[i].isFinal) {
        finalText += transcript
      } else {
        interimText += transcript
      }
    }
    voiceHint.value = interimText

    // barge-in: 用户讲话时立刻打断正在播放的 AI 语音
    if ((interimText || finalText) && currentTtsAudio.value) {
      stopCurrentTtsAudio()
    }

    if (finalText.trim() && !loading.value) {
      await sendByText(finalText)
    }
  }

  recognition.onend = () => {
    if (isLiveVoiceListening.value) {
      recognition.start()
    }
  }

  recognition.onerror = () => {
    voiceHint.value = ''
  }

  speechRecognition.value = recognition
}

const toggleLiveVoice = () => {
  if (!speechRecognition.value) {
    ElMessage.warning('当前浏览器不支持实时语音识别')
    return
  }

  if (isLiveVoiceListening.value) {
    isLiveVoiceListening.value = false
    voiceHint.value = ''
    speechRecognition.value.stop()
    return
  }

  isLiveVoiceListening.value = true
  speechRecognition.value.start()
}

const normalizeText = (value?: string) =>
  (value || '')
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/[\(（\[].*?[\)）\]]/g, '')
    .trim()

const findLocalExactSong = async (song: AgentSongCard): Promise<Song | null> => {
  const keyword = (song.songName || '').trim()
  if (!keyword) {
    return null
  }

  try {
    const res = await getAllSongs({
      songName: keyword,
      pageNum: 1,
      pageSize: 50,
    })

    const items = (res.code === 0 && res.data?.items ? (res.data.items as Song[]) : [])
    if (!items.length) {
      return null
    }

    const targetTitle = normalizeText(song.songName)
    const targetArtist = normalizeText(song.artistName)

    const strictMatch = items.find((item) => {
      const itemTitle = normalizeText(item.songName)
      const itemArtist = normalizeText(item.artistName)
      const titleMatch = itemTitle === targetTitle || itemTitle.includes(targetTitle) || targetTitle.includes(itemTitle)
      const artistMatch = !targetArtist || itemArtist === targetArtist || itemArtist.includes(targetArtist)
      return titleMatch && artistMatch
    })

    return strictMatch || null
  } catch {
    return null
  }
}

const playAgentSong = async (song: AgentSongCard) => {
  // 1) 优先匹配本地同名同歌手，播放完整本地音频
  const local = await findLocalExactSong(song)
  if (local?.audioUrl) {
    const localTrack = {
      id: String(local.songId),
      title: local.songName,
      artist: local.artistName,
      album: local.album || '',
      cover: local.coverUrl || defaultAlbum,
      url: local.audioUrl,
      duration: Number(local.duration) || 0,
      likeStatus: local.likeStatus || 0,
    }
    audioStore.addTracks(localTrack)
    await loadTrack()
    await play()
    ElMessage.success('已优先播放本地完整音频')
    return
  }

  // 2) 本地未命中则回退外部地址（通常仅试听片段）
  if (!song.audioUrl) {
    ElMessage.warning('未匹配到本地完整音频，且外部歌曲暂无可播放地址')
    return
  }

  const externalTrack = {
    id: String(song.songId ?? `${song.songName}-${song.artistName}`),
    title: song.songName,
    artist: song.artistName,
    album: song.album || '',
    cover: song.coverUrl || defaultAlbum,
    url: song.audioUrl,
    duration: 0,
    likeStatus: 0,
  }

  audioStore.addTracks(externalTrack)
  await loadTrack()
  await play()
  ElMessage.info('未匹配到本地完整音频，当前仅可试听片段')
}

const saveGeneratedPlaylist = async (playlist: AgentPlaylistCard, key: string) => {
  if (!playlist.tracks?.length) {
    ElMessage.warning('当前歌单草案没有可保存的歌曲')
    return
  }
  if (savingPlaylistKeys.value[key]) {
    return
  }

  let editedTitle = playlist.title || 'AI歌单'
  try {
    const promptResult = await ElMessageBox.prompt('你可以修改歌单标题后再保存', '保存 AI 歌单', {
      inputValue: editedTitle,
      inputPlaceholder: '请输入歌单标题',
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      inputValidator: (value) => {
        if (!value || !value.trim()) {
          return '歌单标题不能为空'
        }
        if (value.trim().length > 60) {
          return '歌单标题不能超过 60 个字符'
        }
        return true
      },
    })
    editedTitle = promptResult.value.trim()
  } catch {
    return
  }

  savingPlaylistKeys.value[key] = true
  try {
    const res = await saveAgentPlaylist({
      title: editedTitle,
      introduction: playlist.reason || '由 AI 助手自动生成',
      style: 'AI推荐',
      coverUrl: playlist.coverUrl,
      tracks: playlist.tracks.map((item) => ({
        songId: item.songId ?? undefined,
        songName: item.songName,
        artistName: item.artistName,
      })),
    })

    if (res.code === 0) {
      ElMessage.success(`已保存歌单：${res.data.title}（${res.data.songCount} 首）`)
      await favoriteStore.getFavoritePlaylists()
      if (res.data.playlistId) {
        await router.push({
          path: `/playlist/${res.data.playlistId}`,
          query: { from: 'chat', saved: '1' },
        })
      }
      return
    }
    ElMessage.error(res.message || '保存失败')
  } catch (error) {
    console.error(error)
    ElMessage.error('保存歌单失败')
  } finally {
    savingPlaylistKeys.value[key] = false
  }
}
</script>

<template>
  <div class="spotify-chat-page">
    <div class="spotify-chat-header">
      <span class="spotify-chat-subtitle">Ciallo～(∠・ω< )⌒★</span>
    </div>

    <div ref="scrollbarRef" class="spotify-chat-messages">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="spotify-message"
        :class="msg.role === 'user' ? 'spotify-message-user' : 'spotify-message-assistant'"
      >
        <div
          v-if="msg.role === 'assistant' && ((msg.content && msg.content.trim().length > 0) || msg.agentData)"
          class="spotify-avatar spotify-avatar-assistant"
        >
          <img src="/congyu.png" alt="AI" />
        </div>
        <div class="spotify-message-content">
          <div
            v-if="msg.content && msg.content.trim().length > 0"
            class="spotify-message-bubble"
            :class="msg.role === 'user' ? 'spotify-bubble-user' : 'spotify-bubble-assistant'"
          >
            {{ msg.content }}
          </div>

          <div v-if="msg.role === 'assistant' && msg.agentData" class="spotify-agent-panel">
            <div v-if="msg.agentData.toolTrace?.length" class="spotify-trace-row">
              <span v-for="(trace, tIndex) in msg.agentData.toolTrace" :key="`${index}-${tIndex}`" class="spotify-trace-chip">
                {{ trace.summary }}
              </span>
            </div>

            <div v-if="msg.agentData.songs?.length" class="spotify-songs-grid">
              <button
                v-for="(song, songIndex) in msg.agentData.songs"
                :key="`${index}-song-${songIndex}`"
                class="spotify-song-card"
                @click="playAgentSong(song)"
              >
                <img :src="song.coverUrl || '/song.jpg'" alt="cover" class="spotify-song-cover" />
                <div class="spotify-song-meta">
                  <span class="spotify-song-name">{{ song.songName }}</span>
                  <span class="spotify-song-artist">{{ song.artistName }}</span>
                </div>
                <Icon icon="mdi:play-circle" class="spotify-song-play" />
              </button>
            </div>

            <div v-if="msg.agentData.playlists?.length" class="spotify-playlist-grid">
              <div
                v-for="(playlist, pIndex) in msg.agentData.playlists"
                :key="`${index}-playlist-${pIndex}`"
                class="spotify-playlist-card"
              >
                <img :src="playlist.coverUrl || '/cover.png'" alt="playlist" class="spotify-playlist-cover" />
                <div class="spotify-playlist-meta">
                  <span class="spotify-playlist-name">{{ playlist.title }}</span>
                  <span class="spotify-playlist-reason">{{ playlist.reason || 'AI 歌单建议' }}</span>
                </div>
                <button
                  v-if="playlist.tracks?.length"
                  class="spotify-save-playlist-btn"
                  :disabled="savingPlaylistKeys[`${index}-${pIndex}`]"
                  @click="saveGeneratedPlaylist(playlist, `${index}-${pIndex}`)"
                >
                  {{ savingPlaylistKeys[`${index}-${pIndex}`] ? '保存中...' : '一键保存' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="spotify-message spotify-message-assistant">
        <div class="spotify-avatar spotify-avatar-assistant">
          <img src="/thinking.png" alt="AI" />
        </div>
        <div class="spotify-bubble-assistant spotify-message-bubble spotify-typing">
          <div class="spotify-typing-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span class="spotify-typing-text">思考中...</span>
        </div>
      </div>

      <div v-if="messages.length === 0" class="spotify-empty-state">
        <div v-if="matrixLoading" class="spotify-matrix-loading">
          <img src="/congyu.png" alt="AI Assistant" class="spotify-matrix-fallback" />
        </div>
        <Matrix
          v-else-if="matrixFrames.length > 0"
          :rows="45"
          :cols="54"
          :color-frames="matrixFrames"
          :size="4"
          :gap="1"
          :brightness="1.0"
          :fps="12"
          loop
        />
      </div>
    </div>

    <div class="spotify-chat-input">
      <div class="spotify-input-tools">
        <SongRecognizer @success="handleRecognitionSuccess" />
        <button
          v-if="canResumeAfterAbort && !loading"
          class="spotify-resume-btn"
          @click="resumeStreaming"
          title="继续生成"
        >
          <Icon icon="mdi:play-circle-outline" />
          <span>继续生成</span>
        </button>
      </div>
      <div class="spotify-input-wrapper">
        <Icon icon="ri:chat-1-line" class="spotify-input-icon" />
        <input
          v-model="inputMessage"
          :placeholder="voiceHint ? `识别中: ${voiceHint}` : '输入消息...'"
          @keyup.enter="handleSend"
          :disabled="loading"
          class="spotify-input"
        />
        <button
          class="spotify-live-voice-btn"
          :class="{ 'spotify-live-voice-btn-active': isLiveVoiceListening }"
          @click="toggleLiveVoice"
          :title="isLiveVoiceListening ? '停止语音输入' : '语音输入'"
        >
          <Icon :icon="isLiveVoiceListening ? 'mdi:microphone' : 'mdi:microphone-outline'" />
        </button>
      </div>
      <button
        v-if="!loading"
        @click="handleSend"
        :disabled="!inputMessage.trim()"
        class="spotify-send-btn"
        :class="{ 'spotify-send-btn-disabled': !inputMessage.trim() }"
      >
        <Icon icon="mdi:send" class="text-lg" />
      </button>
      <button
        v-else
        @click="stopStreaming"
        class="spotify-stop-btn"
        title="中断生成"
      >
        <Icon icon="mdi:stop" class="text-lg" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.spotify-chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-surface, #121212);
  padding: 16px;
  gap: 16px;
}

.spotify-chat-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.spotify-chat-icon {
  font-size: 1.75rem;
  color: #1db954;
}

.spotify-chat-subtitle {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-stream-status {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  margin-top: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: var(--text-subdued, #b3b3b3);
}

.spotify-stream-status-connecting,
.spotify-stream-status-streaming {
  border-color: rgba(29, 185, 84, 0.6);
  color: #1db954;
}

.spotify-stream-status-error {
  border-color: rgba(239, 68, 68, 0.65);
  color: #ef4444;
}

.spotify-stream-metric {
  opacity: 0.9;
}

.spotify-chat-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
  min-height: 0;
}

.spotify-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: spotify-fade-in 200ms ease-out;
}

.spotify-message-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  max-width: 70%;
}

@keyframes spotify-fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.spotify-message-user {
  flex-direction: row-reverse;
}

.spotify-message-user .spotify-message-content {
  align-items: flex-end;
}

.spotify-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.spotify-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-avatar-assistant {
  background: linear-gradient(135deg, #1db954 0%, #1ed760 100%);
}

.spotify-message-bubble {
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 0.9375rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.spotify-agent-panel {
  width: 100%;
  margin-top: 8px;
}

.spotify-trace-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.spotify-trace-chip {
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  padding: 3px 8px;
  color: var(--text-subdued, #b3b3b3);
}

.spotify-songs-grid,
.spotify-playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.spotify-song-card,
.spotify-playlist-card {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.spotify-song-card {
  cursor: pointer;
  color: inherit;
}

.spotify-song-cover,
.spotify-playlist-cover {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
}

.spotify-song-meta,
.spotify-playlist-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.spotify-song-name,
.spotify-playlist-name {
  font-size: 13px;
  color: var(--text-base, #fff);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spotify-song-artist,
.spotify-playlist-reason {
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spotify-song-play {
  margin-left: auto;
  font-size: 1.2rem;
}

.spotify-save-playlist-btn {
  margin-left: auto;
  border: 1px solid rgba(29, 185, 84, 0.7);
  background: transparent;
  color: #1db954;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.spotify-save-playlist-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spotify-bubble-user {
  background-color: #1db954;
  color: #000;
  border-bottom-right-radius: 4px;
  font-weight: 500;
}

.spotify-bubble-assistant {
  background-color: var(--bg-elevated, #242424);
  color: var(--text-base, #fff);
  border-bottom-left-radius: 4px;
}

.spotify-typing {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spotify-typing-dots {
  display: flex;
  gap: 4px;
}

.spotify-typing-dots span {
  width: 6px;
  height: 6px;
  background-color: #1db954;
  border-radius: 50%;
  animation: spotify-bounce 1.4s infinite ease-in-out both;
}

.spotify-typing-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.spotify-typing-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes spotify-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.spotify-typing-text {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
}

.spotify-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.spotify-matrix-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spotify-matrix-fallback {
  max-width: 200px;
  border-radius: 16px;
}

.spotify-chat-input {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-input-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spotify-live-voice-btn {
  position: absolute;
  right: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-live-voice-btn:hover {
  color: var(--text-base, #fff);
  border-color: rgba(255, 255, 255, 0.35);
}

.spotify-live-voice-btn-active {
  border-color: rgba(29, 185, 84, 0.8);
  color: #1db954;
}

.spotify-resume-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(29, 185, 84, 0.6);
  background: rgba(29, 185, 84, 0.08);
  color: #1db954;
  cursor: pointer;
  transition: all 200ms ease;
}

.spotify-resume-btn:hover {
  background: rgba(29, 185, 84, 0.16);
}

.spotify-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.spotify-input-icon {
  position: absolute;
  left: 16px;
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.spotify-input {
  width: 100%;
  padding: 14px 48px 14px 48px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
  transition: box-shadow 200ms ease;
}

.spotify-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px #fff;
}

.spotify-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spotify-send-btn {
  width: 48px;
  height: 48px;
  background-color: #1db954;
  border: none;
  border-radius: 50%;
  color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 200ms ease;
  flex-shrink: 0;
}

.spotify-send-btn:hover:not(.spotify-send-btn-disabled) {
  background-color: #1ed760;
  transform: scale(1.04);
}

.spotify-send-btn:active:not(.spotify-send-btn-disabled) {
  transform: scale(1);
}

.spotify-send-btn-disabled {
  background-color: #535353;
  color: #b3b3b3;
  cursor: not-allowed;
}

.spotify-stop-btn {
  width: 48px;
  height: 48px;
  background-color: #ef4444;
  border: none;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 200ms ease;
  flex-shrink: 0;
}

.spotify-stop-btn:hover {
  background-color: #dc2626;
  transform: scale(1.04);
}

/* Light Theme */
:root:not(.dark) .spotify-chat-page {
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-input:focus {
  box-shadow: 0 0 0 2px #000;
}

:root:not(.dark) .spotify-empty-image {
  background: linear-gradient(135deg, rgba(29, 185, 84, 0.1) 0%, rgba(30, 215, 96, 0.05) 100%);
}

/* Scrollbar */
.spotify-chat-messages::-webkit-scrollbar {
  width: 8px;
}

.spotify-chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-chat-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.spotify-chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

@media (max-width: 640px) {
  .spotify-chat-page {
    padding-bottom: 80px;
  }
  
  .spotify-chat-header {
    padding: 12px 16px;
  }
  
  .spotify-chat-subtitle {
    font-size: 0.8125rem;
  }
  
  .spotify-message-bubble {
    max-width: 85%;
  }
  
  .spotify-chat-input {
    padding: 12px;
    gap: 8px;
    flex-wrap: wrap;
  }

  .spotify-input-tools {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  
  .spotify-input-wrapper {
    padding: 10px 12px 10px 40px;
  }
  
  .spotify-input {
    font-size: 0.875rem;
  }
  
  .spotify-send-btn {
    width: 40px;
    height: 40px;
  }

  .spotify-stop-btn {
    width: 40px;
    height: 40px;
  }

  .spotify-stream-status {
    width: 100%;
    justify-content: space-between;
    font-size: 11px;
    padding: 4px 8px;
    gap: 6px;
  }

  .spotify-stream-metric {
    font-size: 10px;
  }
}
</style>
