<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import {
  sendAgentMessage,
  sendAgentMessageStream,
  saveAgentPlaylist,
  getChatHealth,
  type ChatMessage,
  type AgentChatResponse,
  type AgentSongCard,
  type AgentPlaylistCard,
  type AgentCitation,
  type ChatHealthResponse,
} from '@/api/chat'
import { getAllSongs } from '@/api/system'
import type { Song } from '@/api/interface'
import SongRecognizer from '@/components/SongRecognizer.vue'
import { useFavoriteStore } from '@/stores/modules/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'
import Orb from '@/components/voice/Orb.vue'
import Matrix from '@/components/Matrix/index.vue'
import { imageToColorFrame, createAnimatedFrames, type ColorFrame, type AnimationType } from '@/utils/matrix'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import { AudioStore } from '@/stores/modules/audio'
import defaultAlbum from '@/assets/default_album.jpg'
import songCoverFallback from '@/assets/song.jpg'
import coverImg from '@/assets/cover.png'

// 集中管理 public 目录下的静态资源路径，避免在模板中硬编码
const AI_AVATAR_CONGYU = '/congyu.png'
const AI_AVATAR_THINKING = '/thinking.png'

type TimelineMessage = ChatMessage & { id: number; agentData?: AgentChatResponse; pending?: boolean }

// 消息唯一 id 计数器，用作 v-for key，避免用 index 导致的关键状态错乱
let messageIdCounter = 0

const messages = ref<TimelineMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const voiceEnabled = ref(true)
const viewMode = ref<'tts' | 'voice'>('tts')
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
const isRecognitionRunning = ref(false)
const isRecognitionStarting = ref(false)
const pausedByTts = ref(false)
const lastQueuedVoiceText = ref('')
const lastQueuedVoiceAt = ref(0)
const voiceHint = ref('')
const voiceQuestionQueue = ref<string[]>([])
const currentTtsAudio = ref<HTMLAudioElement | null>(null)
const isTtsPlaying = ref(false)
const orbColorsRef = ref<[string, string]>(['#CADCFC', '#A0B9D1'])

const inputAudioContext = ref<AudioContext | null>(null)
const inputStream = ref<MediaStream | null>(null)
const inputAnalyser = ref<AnalyserNode | null>(null)
const inputDataArray = ref<Uint8Array | null>(null)

const outputAudioContext = ref<AudioContext | null>(null)
const outputAnalyser = ref<AnalyserNode | null>(null)
const outputDataArray = ref<Uint8Array | null>(null)
const outputSourceNode = ref<AudioNode | null>(null)
const outputPulsePhase = ref(0)
const streamState = ref<'idle' | 'connecting' | 'streaming' | 'done' | 'error'>('idle')
const streamFirstDeltaMs = ref<number | null>(null)
const streamTotalMs = ref<number | null>(null)
const streamStartedAt = ref<number | null>(null)
const streamAbortController = ref<AbortController | null>(null)
const canResumeAfterAbort = ref(false)
const lastAbortedAssistantIndex = ref<number | null>(null)
const chatHealth = ref<ChatHealthResponse | null>(null)
const healthLoading = ref(false)
const healthError = ref('')
const healthExpanded = ref(false)
const isVoiceMode = computed(() => viewMode.value === 'voice')
const lastRequestPayload = ref<{
  message?: string
  messages?: ChatMessage[]
  nowPlaying?: { songId?: string; title?: string; artist?: string; album?: string }
  limit?: number
  enableVoice?: boolean
  enableRag?: boolean
  playlistSeeds?: Array<{ songId: number; songName: string; artistName: string; style?: string }>
} | null>(null)

const orbAgentState = computed<null | 'thinking' | 'listening' | 'talking'>(() => {
  if (isTtsPlaying.value) return 'talking'
  if (isLiveVoiceListening.value) return 'listening'
  if (loading.value || streamState.value === 'connecting' || streamState.value === 'streaming') return 'thinking'
  return null
})

const animationModes: AnimationType[] = ['float', 'sparkle', 'sparkle', 'float']
const currentModeIndex = ref(0)
const modeTimerId = ref<number | undefined>(undefined)
let inputAgcGain = 1
let outputAgcGain = 1

const streamStateTextMap: Record<string, string> = {
  connecting: '连接中',
  streaming: '生成中',
  done: '已完成',
  error: '失败',
}

watch(orbAgentState, (state) => {
  if (state === 'listening') {
    orbColorsRef.value = ['#7EE8FA', '#80FFDB']
    return
  }
  if (state === 'thinking') {
    orbColorsRef.value = ['#CADCFC', '#A0B9D1']
    return
  }
  if (state === 'talking') {
    orbColorsRef.value = ['#FF9A9E', '#FAD0C4']
    return
  }
  orbColorsRef.value = ['#CADCFC', '#A0B9D1']
})

const refreshChatHealth = async () => {
  healthLoading.value = true
  healthError.value = ''
  try {
    const res = await getChatHealth()
    if (res.code === 0 && res.data) {
      chatHealth.value = res.data
      return
    }
    healthError.value = res.message || '状态检查失败'
  } catch (error) {
    console.error(error)
    healthError.value = '状态检查失败'
  } finally {
    healthLoading.value = false
  }
}

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

const sampleAnalyserVolume = (analyser: AnalyserNode | null, dataArray: Uint8Array | null, channel: 'input' | 'output') => {
  if (!analyser || !dataArray) return 0
  analyser.getByteTimeDomainData(dataArray as Uint8Array<ArrayBuffer>)
  let sum = 0
  for (let i = 0; i < dataArray.length; i++) {
    const normalized = (dataArray[i] - 128) / 128
    sum += normalized * normalized
  }
  const rms = Math.sqrt(sum / dataArray.length)
  const isInput = channel === 'input'
  const targetGain = (isInput ? 0.12 : 0.1) / Math.max(rms, 0.01)
  if (isInput) {
    inputAgcGain += (targetGain - inputAgcGain) * 0.08
  } else {
    outputAgcGain += (targetGain - outputAgcGain) * 0.08
  }
  const gain = isInput ? inputAgcGain : outputAgcGain
  const gated = rms < 0.008 ? 0 : rms
  return Math.max(0, Math.min(1, gated * gain * 4.2))
}

const getInputVolume = () => sampleAnalyserVolume(inputAnalyser.value, inputDataArray.value, 'input')
const getOutputVolume = () => {
  const analyserVolume = sampleAnalyserVolume(outputAnalyser.value, outputDataArray.value, 'output')
  if (analyserVolume > 0.001) {
    return analyserVolume
  }
  // If analyser is unavailable but TTS is playing, keep subtle pulse to reflect speaking state.
  if (isTtsPlaying.value) {
    outputPulsePhase.value += 0.2
    return 0.18 + (Math.sin(outputPulsePhase.value) + 1) * 0.12
  }
  return 0
}

const normalizeVoiceFinalText = (value: string) =>
  value
    .replace(/[，。！？、,.!?]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const isMeaningfulVoiceText = (value: string) => {
  const normalized = value.toLowerCase().trim()
  if (!normalized || normalized.length < 2) {
    return false
  }
  const fillerWords = ['嗯', '啊', '额', '呃', '哦', '唉', '诶', '嗯嗯', '啊啊', '喂', '哈喽']
  return !fillerWords.includes(normalized)
}

const startRecognitionSafely = () => {
  if (!speechRecognition.value) {
    return
  }
  if (!isLiveVoiceListening.value || isRecognitionRunning.value || isRecognitionStarting.value) {
    return
  }
  isRecognitionStarting.value = true
  try {
    speechRecognition.value.start()
  } catch (error) {
    isRecognitionStarting.value = false
    console.error('recognition start failed', error)
  }
}

const stopRecognitionSafely = () => {
  if (!speechRecognition.value) {
    return
  }
  if (!isRecognitionRunning.value && !isRecognitionStarting.value) {
    return
  }
  try {
    speechRecognition.value.stop()
  } catch (error) {
    console.error('recognition stop failed', error)
  } finally {
    isRecognitionRunning.value = false
    isRecognitionStarting.value = false
  }
}

const ensureInputAnalyser = async () => {
  if (inputAnalyser.value) return
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  inputStream.value = stream
  const ctx = new AudioContext()
  const source = ctx.createMediaStreamSource(stream)
  const analyser = ctx.createAnalyser()
  analyser.fftSize = 1024
  analyser.smoothingTimeConstant = 0.85
  source.connect(analyser)

  inputAudioContext.value = ctx
  inputAnalyser.value = analyser
  inputDataArray.value = new Uint8Array(analyser.fftSize)
}

const attachOutputAnalyser = (audio: HTMLAudioElement) => {
  try {
    if (!outputAudioContext.value) {
      outputAudioContext.value = new AudioContext()
    }

    outputSourceNode.value?.disconnect()
    outputAnalyser.value?.disconnect()

    const mediaWithCapture = audio as HTMLMediaElement & {
      captureStream?: () => MediaStream
      mozCaptureStream?: () => MediaStream
    }
    const analyser = outputAudioContext.value.createAnalyser()
    analyser.fftSize = 1024
    analyser.smoothingTimeConstant = 0.85

    const stream = mediaWithCapture.captureStream?.() || mediaWithCapture.mozCaptureStream?.()
    let sourceNode: AudioNode | null = null
    if (stream) {
      sourceNode = outputAudioContext.value.createMediaStreamSource(stream)
      sourceNode.connect(analyser)
    } else {
      // Fallback for browsers without captureStream support.
      const source = outputAudioContext.value.createMediaElementSource(audio)
      source.connect(analyser)
      analyser.connect(outputAudioContext.value.destination)
      sourceNode = source
    }

    outputSourceNode.value = sourceNode
    outputAnalyser.value = analyser
    outputDataArray.value = new Uint8Array(analyser.fftSize)

    if (outputAudioContext.value.state === 'suspended') {
      outputAudioContext.value.resume().catch(() => undefined)
    }
  } catch (error) {
    console.error('attachOutputAnalyser failed', error)
    outputSourceNode.value = null
    outputAnalyser.value = null
    outputDataArray.value = null
  }
}

const closeVoiceAnalyser = () => {
  inputStream.value?.getTracks().forEach((track) => track.stop())
  inputStream.value = null
  inputAnalyser.value = null
  inputDataArray.value = null
  inputAudioContext.value?.close().catch(() => undefined)
  inputAudioContext.value = null
}

const closeOutputAnalyser = () => {
  outputSourceNode.value?.disconnect()
  outputAnalyser.value?.disconnect()
  outputSourceNode.value = null
  outputAnalyser.value = null
  outputDataArray.value = null
  outputAudioContext.value?.close().catch(() => undefined)
  outputAudioContext.value = null
}

const switchToVoiceMode = async () => {
  viewMode.value = 'voice'
  orbColorsRef.value = ['#7EE8FA', '#80FFDB']
  try {
    await ensureInputAnalyser()
    if (!isLiveVoiceListening.value) {
      isLiveVoiceListening.value = true
    }
    startRecognitionSafely()
  } catch (error) {
    console.error(error)
    ElMessage.warning('无法访问麦克风，语音可视化不可用')
  }
}

const switchToTtsMode = () => {
  viewMode.value = 'tts'
  isLiveVoiceListening.value = false
  pausedByTts.value = false
  voiceHint.value = ''
  stopRecognitionSafely()
  orbColorsRef.value = ['#CADCFC', '#A0B9D1']
}

onMounted(async () => {
  initLiveSpeech()
  await refreshChatHealth()
  try {
    const colorFrame = await imageToColorFrame(AI_AVATAR_THINKING, 45, 54)
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
    stopRecognitionSafely()
  }
  if (modeTimerId.value) {
    clearInterval(modeTimerId.value)
  }
  closeVoiceAnalyser()
  closeOutputAnalyser()
})

const stopCurrentTtsAudio = () => {
  if (currentTtsAudio.value) {
    currentTtsAudio.value.pause()
    currentTtsAudio.value.currentTime = 0
    currentTtsAudio.value = null
  }
  isTtsPlaying.value = false
}

const playTtsAudio = (audioUrl: string) => {
  const audio = new Audio(audioUrl)
  audio.preload = 'auto'
  try {
    attachOutputAnalyser(audio)
  } catch (error) {
    console.error('Attach output analyser failed', error)
  }
  audio.onplay = () => {
    isTtsPlaying.value = true
    outputAudioContext.value?.resume().catch(() => undefined)
    if (viewMode.value !== 'voice' && isLiveVoiceListening.value && isRecognitionRunning.value) {
      pausedByTts.value = true
      stopRecognitionSafely()
    }
  }
  audio.onpause = () => {
    isTtsPlaying.value = false
    if (pausedByTts.value && isLiveVoiceListening.value) {
      pausedByTts.value = false
      startRecognitionSafely()
    }
  }
  audio.onended = () => {
    isTtsPlaying.value = false
    if (pausedByTts.value && isLiveVoiceListening.value) {
      pausedByTts.value = false
      startRecognitionSafely()
    }
    if (currentTtsAudio.value === audio) {
      currentTtsAudio.value = null
    }
  }
  currentTtsAudio.value = audio
  audio.play().catch((e) => {
    isTtsPlaying.value = false
    console.error('Audio play failed', e)
  })
}

const flushVoiceQueue = async () => {
  if (loading.value || !voiceQuestionQueue.value.length) {
    return
  }
  const nextQuestion = voiceQuestionQueue.value.shift()
  if (!nextQuestion) {
    return
  }
  await sendByText(nextQuestion)
}

const runAgentRequest = async (
  payload: {
    message?: string
    messages?: ChatMessage[]
    nowPlaying?: { songId?: string; title?: string; artist?: string; album?: string }
    limit?: number
    enableVoice?: boolean
    enableRag?: boolean
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
    let streamHadError = false
    await sendAgentMessageStream(payload, {
      onDelta: (chunk) => {
        if (streamState.value !== 'streaming') {
          streamState.value = 'streaming'
          if (streamStartedAt.value != null) {
            streamFirstDeltaMs.value = Math.round(performance.now() - streamStartedAt.value)
          }
        }
        messages.value[assistantIndex].content += chunk
        messages.value[assistantIndex].pending = false
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
        messages.value[assistantIndex].pending = false

        if (data.intent === 'PLAYER_CONTROL' || data.playerCommand === 'play_target') {
          await executePlayerCommand(data.playerCommand)
        }

        if (data.playerCommand === 'play_target' && data.songs?.length) {
          await playAgentSong(data.songs[0])
        }

        if (data.audio) {
          playTtsAudio(data.audio)
        }
      },
      onError: (message) => {
        streamHadError = true
        streamState.value = 'error'
        ElMessage.error(message || '流式响应失败')
      },
    }, {
      signal: streamAbortController.value?.signal,
    })

    // 仅在流式出错时回退到非流式接口；流式正常结束但未收到 done 事件时不再自动回退
    if (streamHadError && !streamDone) {
      const res = await sendAgentMessage(payload)
      if (res.code === 0 && res.data) {
        streamState.value = 'done'
        if (streamStartedAt.value != null) {
          streamTotalMs.value = Math.round(performance.now() - streamStartedAt.value)
        }
        messages.value[assistantIndex].content = res.data.answer || '已处理'
        messages.value[assistantIndex].agentData = res.data
        messages.value[assistantIndex].pending = false

        if (res.data.audio) {
          playTtsAudio(res.data.audio)
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
  messages.value.push({ id: ++messageIdCounter, role: 'user', content: userMsg })
  const historyMessages = messages.value.map((item) => ({ role: item.role, content: item.content }))
  inputMessage.value = ''
  scrollToBottom()

  const assistantIndex = messages.value.length
  messages.value.push({ id: ++messageIdCounter, role: 'assistant', content: '', pending: true })

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
    enableRag: true,
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
    : messages.value.push({ id: ++messageIdCounter, role: 'assistant', content: '', pending: true }) - 1

  messages.value[assistantIndex].content = ''
  messages.value[assistantIndex].agentData = undefined
  messages.value[assistantIndex].pending = true

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

  recognition.onstart = () => {
    isRecognitionStarting.value = false
    isRecognitionRunning.value = true
  }

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

    const normalizedFinal = normalizeVoiceFinalText(finalText)
    if (normalizedFinal && isMeaningfulVoiceText(normalizedFinal)) {
      const now = Date.now()
      if (normalizedFinal === lastQueuedVoiceText.value && now - lastQueuedVoiceAt.value < 1200) {
        return
      }
      lastQueuedVoiceText.value = normalizedFinal
      lastQueuedVoiceAt.value = now
      voiceQuestionQueue.value.push(normalizedFinal)
      if (loading.value) {
        stopStreaming()
      }
      await flushVoiceQueue()
    }
  }

  recognition.onend = () => {
    isRecognitionRunning.value = false
    isRecognitionStarting.value = false
    if (isLiveVoiceListening.value) {
      startRecognitionSafely()
    }
  }

  recognition.onerror = () => {
    voiceHint.value = ''
    isRecognitionRunning.value = false
    isRecognitionStarting.value = false
    if (isLiveVoiceListening.value) {
      startRecognitionSafely()
    }
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
    pausedByTts.value = false
    voiceHint.value = ''
    stopRecognitionSafely()
    return
  }

  if (!isVoiceMode.value) {
    void switchToVoiceMode()
    return
  }

  isLiveVoiceListening.value = true
  ensureInputAnalyser()
    .catch((error) => {
      console.error(error)
      ElMessage.warning('麦克风权限未开启，无法采集实时音量')
    })
    .finally(() => {
      startRecognitionSafely()
    })
}

watch(loading, (isLoading) => {
  if (!isLoading) {
    void flushVoiceQueue()
  }
})

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
    const promptResult = await ElMessageBox.prompt('修改标题', '保存 AI 歌单', {
      inputValue: editedTitle,
      inputPlaceholder: '请输入歌单标题',
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      customClass: 'mr-save-playlist-dialog',
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

const extractCitationKeyword = (title?: string) => {
  if (!title) return ''
  return title
    .replace(/^歌曲《/, '')
    .replace(/^歌单《/, '')
    .replace(/[》]/g, '')
    .replace(/\s*-\s*.+$/, '')
    .trim()
}

const handleCitationClick = async (citation: AgentCitation, agentData?: AgentChatResponse) => {
  if (citation.sourceType === 'playlist' && citation.sourceId) {
    await router.push(`/playlist/${citation.sourceId}`)
    return
  }

  if (citation.sourceType === 'song') {
    const targetId = Number(citation.sourceId)
    const matchedSong = (agentData?.songs || []).find((song) => song.songId != null && Number(song.songId) === targetId)
    if (matchedSong) {
      await playAgentSong(matchedSong)
      return
    }

    const keyword = extractCitationKeyword(citation.title)
    await router.push({
      path: '/library',
      query: keyword ? { query: keyword } : undefined,
    })
    ElMessage.info(keyword ? `已跳转曲库并搜索: ${keyword}` : '已跳转曲库')
    return
  }

  ElMessage.info('当前引用暂不支持跳转')
}
</script>

<template>
  <div class="mr-chat-page">
    <div class="mr-chat-header">
      <span class="mr-chat-subtitle">(∠・ω< )⌒★</span>
    </div>

    <div v-if="viewMode === 'tts'" ref="scrollbarRef" class="mr-chat-messages">
      <div
        v-for="(msg, index) in messages"
        :key="msg.id"
        v-show="msg.role === 'user' || !msg.pending"
        class="mr-message"
        :class="msg.role === 'user' ? 'mr-message-user' : 'mr-message-assistant'"
      >
        <div
          v-if="msg.role === 'assistant' && ((msg.content && msg.content.trim().length > 0) || msg.agentData)"
          class="mr-avatar mr-avatar-assistant"
        >
          <img :src="AI_AVATAR_CONGYU" alt="AI" />
        </div>
        <div class="mr-message-content">
          <div
            v-if="msg.content && msg.content.trim().length > 0"
            class="mr-message-bubble"
            :class="msg.role === 'user' ? 'mr-bubble-user' : 'mr-bubble-assistant'"
          >
            {{ msg.content }}
          </div>

          <div v-if="msg.role === 'assistant' && msg.agentData" class="mr-agent-panel">
            <div v-if="msg.agentData.toolTrace?.length" class="mr-trace-row">
              <span v-for="(trace, tIndex) in msg.agentData.toolTrace" :key="`${msg.id}-${tIndex}`" class="mr-trace-chip">
                {{ trace.summary }}
              </span>
            </div>

            <div v-if="msg.agentData.songs?.length" class="mr-agent-section">
              <div class="mr-agent-section-title">歌曲结果</div>
              <div class="mr-songs-grid">
                <button
                  v-for="(song, songIndex) in msg.agentData.songs"
                  :key="`${msg.id}-song-${songIndex}`"
                  class="mr-song-card"
                  @click="playAgentSong(song)"
                >
                  <img :src="song.coverUrl || songCoverFallback" alt="cover" class="mr-song-cover" />
                  <div class="mr-song-meta">
                    <span class="mr-song-name">{{ song.songName }}</span>
                    <span class="mr-song-artist">{{ song.artistName }}</span>
                  </div>
                  <Icon icon="mdi:play-circle" class="mr-song-play" />
                </button>
              </div>
            </div>

            <div v-if="msg.agentData.playlists?.length" class="mr-agent-section">
              <div class="mr-agent-section-title">歌单建议</div>
              <div class="mr-playlist-grid">
                <div
                  v-for="(playlist, pIndex) in msg.agentData.playlists"
                  :key="`${msg.id}-playlist-${pIndex}`"
                  class="mr-playlist-card"
                >
                  <img :src="playlist.coverUrl || coverImg" alt="playlist" class="mr-playlist-cover" />
                  <div class="mr-playlist-meta">
                    <span class="mr-playlist-name">{{ playlist.title }}</span>
                    <span class="mr-playlist-reason">{{ playlist.reason || 'AI 歌单建议' }}</span>
                  </div>
                  <button
                    v-if="playlist.tracks?.length"
                    class="mr-save-playlist-btn"
                    :disabled="savingPlaylistKeys[`${msg.id}-${pIndex}`]"
                    @click="saveGeneratedPlaylist(playlist, `${msg.id}-${pIndex}`)"
                  >
                    {{ savingPlaylistKeys[`${msg.id}-${pIndex}`] ? '保存中...' : '保存到我的歌单' }}
                  </button>
                </div>
              </div>
            </div>

            <div v-if="msg.agentData.citations?.length" class="mr-agent-section mr-citation-section">
              <div class="mr-agent-section-title mr-citation-title">参考资料</div>
              <div class="mr-citation-grid">
                <div
                  v-for="(citation, cIndex) in msg.agentData.citations"
                  :key="`${msg.id}-citation-${cIndex}`"
                  class="mr-citation-card"
                  @click="handleCitationClick(citation, msg.agentData)"
                >
                  <span class="mr-citation-type">{{ citation.sourceType }}</span>
                  <span class="mr-citation-name">{{ citation.title }}</span>
                  <span class="mr-citation-snippet">{{ citation.snippet }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="mr-message mr-message-assistant">
        <div class="mr-avatar mr-avatar-assistant">
          <img :src="AI_AVATAR_THINKING" alt="AI" />
        </div>
        <div class="mr-bubble-assistant mr-message-bubble mr-typing">
          <div class="mr-typing-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span class="mr-typing-text">思考中...</span>
        </div>
      </div>

      <div v-if="messages.length === 0" class="mr-empty-state">
        <div v-if="matrixLoading" class="mr-matrix-loading">
          <img :src="AI_AVATAR_CONGYU" alt="AI Assistant" class="mr-matrix-fallback" />
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

    <div v-else class="mr-voice-stage">
      <Orb
        class="mr-orb"
        :agent-state="orbAgentState"
        :colors-ref="orbColorsRef"
        :seed="20260418"
        volume-mode="auto"
        :get-input-volume="getInputVolume"
        :get-output-volume="getOutputVolume"
      />
      <p class="mr-orb-hint">
        {{ orbAgentState === 'talking' ? 'AI 正在回答，语音振幅实时驱动 Orb' : orbAgentState === 'listening' ? '实时语音输入中，请继续说' : orbAgentState === 'thinking' ? 'AI 思考中...' : '点击下方麦克风开始语音输入' }}
      </p>
    </div>

    <div class="mr-chat-input">
      <div class="mr-input-tools">
        <div class="mr-mode-switch">
          <button
            class="mr-mode-btn"
            :class="{ 'mr-mode-btn-active': viewMode === 'tts' }"
            @click="switchToTtsMode"
          >
            TTS
          </button>
          <button
            class="mr-mode-btn"
            :class="{ 'mr-mode-btn-active': viewMode === 'voice' }"
            @click="switchToVoiceMode"
          >
            语音
          </button>
        </div>
        <SongRecognizer v-if="viewMode === 'tts'" @success="handleRecognitionSuccess" />
        <button
          v-if="viewMode === 'tts' && canResumeAfterAbort && !loading"
          class="mr-resume-btn"
          @click="resumeStreaming"
          title="继续生成"
        >
          <Icon icon="mdi:play-circle-outline" />
          <span>继续生成</span>
        </button>
      </div>
      <div class="mr-input-wrapper">
        <Icon icon="ri:chat-1-line" class="mr-input-icon" />
        <input
          v-model="inputMessage"
          :placeholder="viewMode === 'voice' ? (voiceHint ? `识别中: ${voiceHint}` : '语音模式：点击右侧麦克风开始实时输入') : (voiceHint ? `识别中: ${voiceHint}` : '输入消息...')"
          @keyup.enter="handleSend"
          :disabled="loading || viewMode === 'voice'"
          class="mr-input"
        />
        <button
          class="mr-live-voice-btn"
          :class="{ 'mr-live-voice-btn-active': isLiveVoiceListening || viewMode === 'voice' }"
          @click="toggleLiveVoice"
          :title="isLiveVoiceListening ? '停止语音输入' : '语音输入'"
        >
          <Icon :icon="isLiveVoiceListening ? 'mdi:microphone' : 'mdi:microphone-outline'" />
        </button>
      </div>
      <button
        v-if="!loading && viewMode === 'tts'"
        @click="handleSend"
        :disabled="!inputMessage.trim()"
        class="mr-send-btn"
        :class="{ 'mr-send-btn-disabled': !inputMessage.trim() }"
      >
        <Icon icon="mdi:send" class="text-lg" />
      </button>
      <button
        v-else-if="loading"
        @click="stopStreaming"
        class="mr-stop-btn"
        title="中断生成"
      >
        <Icon icon="mdi:stop" class="text-lg" />
      </button>
      <div v-else class="mr-voice-pill">语音模式</div>
    </div>
  </div>
</template>

<style scoped>
.mr-chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-surface, #121212);
  padding: 16px;
  gap: 16px;
}

.mr-chat-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.mr-chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.mr-chat-icon {
  font-size: 1.75rem;
  color: var(--mr-accent);
}

.mr-chat-subtitle {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-health-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.mr-health-refresh {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.mr-health-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.mr-health-detail {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
}

.mr-health-chip {
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
}

.mr-health-ok {
  border-color: color-mix(in srgb, var(--text-base, #fff) 50%, transparent);
  color: var(--mr-accent-hover);
}

.mr-health-bad {
  border-color: rgba(239, 68, 68, 0.6);
  color: #ef4444;
}

.mr-health-error {
  margin-top: 6px;
  font-size: 12px;
  color: #ef4444;
}

.mr-rotating {
  animation: mr-rotate 1s linear infinite;
}

@keyframes mr-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.mr-stream-status {
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

.mr-stream-status-connecting,
.mr-stream-status-streaming {
  border-color: color-mix(in srgb, var(--text-base, #fff) 60%, transparent);
  color: var(--mr-accent);
}

.mr-stream-status-error {
  border-color: rgba(239, 68, 68, 0.65);
  color: #ef4444;
}

.mr-stream-metric {
  opacity: 0.9;
}

.mr-chat-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
  min-height: 0;
}

.mr-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: mr-fade-in 200ms ease-out;
}

.mr-message-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  max-width: 70%;
}

.mr-message-assistant .mr-message-content {
  max-width: min(920px, 88%);
}

@keyframes mr-fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mr-message-user {
  flex-direction: row-reverse;
}

.mr-message-user .mr-message-content {
  align-items: flex-end;
}

.mr-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.mr-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mr-avatar-assistant {
  background: linear-gradient(135deg, var(--mr-accent) 0%, var(--mr-accent-hover) 100%);
}

.mr-message-bubble {
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 0.9375rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.mr-agent-panel {
  width: 100%;
  margin-top: 8px;
}

.mr-trace-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.mr-trace-chip {
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  padding: 3px 8px;
  color: var(--text-subdued, #b3b3b3);
}

.mr-songs-grid,
.mr-playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}

.mr-citation-section {
  margin-top: 10px;
}

.mr-agent-section {
  margin-top: 10px;
}

.mr-agent-section-title {
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 8px;
}

.mr-citation-title {
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
  margin-bottom: 8px;
}

.mr-citation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}

.mr-citation-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease;
}

.mr-citation-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--text-base, #fff) 50%, transparent);
}

.mr-citation-type {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--text-base, #fff) 45%, transparent);
  color: var(--mr-accent-hover);
  font-size: 11px;
  text-transform: uppercase;
}

.mr-citation-name {
  font-size: 13px;
  color: var(--text-base, #fff);
}

.mr-citation-snippet {
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
  line-height: 1.4;
}

.mr-song-card,
.mr-playlist-card {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.mr-playlist-card {
  align-items: center;
  min-height: 82px;
  padding: 10px 12px;
}

.mr-song-card {
  cursor: pointer;
  color: inherit;
}

.mr-song-cover,
.mr-playlist-cover {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
}

.mr-song-meta,
.mr-playlist-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.mr-song-name,
.mr-playlist-name {
  font-size: 13px;
  color: var(--text-base, #fff);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mr-song-artist,
.mr-playlist-reason {
  font-size: 12px;
  color: var(--text-subdued, #b3b3b3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mr-song-play {
  margin-left: auto;
  font-size: 1.2rem;
}

.mr-save-playlist-btn {
  margin-left: 8px;
  align-self: center;
  border: 1px solid color-mix(in srgb, var(--text-base, #fff) 70%, transparent);
  background: transparent;
  color: var(--mr-accent);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  min-width: 112px;
  text-align: center;
  cursor: pointer;
  white-space: nowrap;
}

.mr-save-playlist-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mr-bubble-user {
  background-color: var(--mr-accent);
  color: #000;
  border-bottom-right-radius: 4px;
  font-weight: 500;
}

.mr-bubble-assistant {
  background-color: var(--bg-elevated, #242424);
  color: var(--text-base, #fff);
  border-bottom-left-radius: 4px;
}

.mr-typing {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mr-typing-dots {
  display: flex;
  gap: 4px;
}

.mr-typing-dots span {
  width: 6px;
  height: 6px;
  background-color: var(--mr-accent);
  border-radius: 50%;
  animation: mr-bounce 1.4s infinite ease-in-out both;
}

.mr-typing-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.mr-typing-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes mr-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.mr-typing-text {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
}

.mr-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.mr-matrix-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.mr-matrix-fallback {
  max-width: 200px;
  border-radius: 16px;
}

.mr-voice-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.mr-orb {
  width: min(60vw, 420px);
  height: min(60vw, 420px);
  min-width: 240px;
  min-height: 240px;
}

.mr-orb-hint {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
}

.mr-chat-input {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.mr-input-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mr-mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 4px;
}

.mr-mode-btn {
  height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: transparent;
  color: var(--text-subdued, #b3b3b3);
  padding: 0 12px;
  cursor: pointer;
}

.mr-mode-btn-active {
  border-color: color-mix(in srgb, var(--text-base, #fff) 70%, transparent);
  color: var(--mr-accent);
  background: color-mix(in srgb, var(--text-base, #fff) 12%, transparent);
}

.mr-live-voice-btn {
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

.mr-live-voice-btn:hover {
  color: var(--text-base, #fff);
  border-color: rgba(255, 255, 255, 0.35);
}

.mr-live-voice-btn-active {
  border-color: color-mix(in srgb, var(--text-base, #fff) 80%, transparent);
  color: var(--mr-accent);
}

.mr-resume-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--text-base, #fff) 60%, transparent);
  background: color-mix(in srgb, var(--text-base, #fff) 8%, transparent);
  color: var(--mr-accent);
  cursor: pointer;
  transition: all 200ms ease;
}

.mr-resume-btn:hover {
  background: color-mix(in srgb, var(--text-base, #fff) 16%, transparent);
}

.mr-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.mr-input-icon {
  position: absolute;
  left: 16px;
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.mr-input {
  width: 100%;
  padding: 14px 48px 14px 48px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
  transition: box-shadow 200ms ease;
}

.mr-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.mr-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px #fff;
}

.mr-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mr-send-btn {
  width: 48px;
  height: 48px;
  background-color: var(--mr-accent);
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

.mr-send-btn:hover:not(.mr-send-btn-disabled) {
  background-color: var(--mr-accent-hover);
  transform: scale(1.04);
}

.mr-send-btn:active:not(.mr-send-btn-disabled) {
  transform: scale(1);
}

.mr-send-btn-disabled {
  background-color: #535353;
  color: #b3b3b3;
  cursor: not-allowed;
}

.mr-stop-btn {
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

.mr-stop-btn:hover {
  background-color: #dc2626;
  transform: scale(1.04);
}

.mr-voice-pill {
  height: 34px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(126, 232, 250, 0.5);
  color: #7ee8fa;
  background: rgba(126, 232, 250, 0.12);
  font-size: 12px;
}

/* Light Theme */
:root:not(.dark) .mr-chat-page {
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .mr-input:focus {
  box-shadow: 0 0 0 2px #000;
}

:root:not(.dark) .mr-empty-image {
  background: linear-gradient(135deg, color-mix(in srgb, var(--text-base, #000) 10%, transparent) 0%, color-mix(in srgb, var(--text-base, #000) 5%, transparent) 100%);
}

/* Scrollbar */
.mr-chat-messages::-webkit-scrollbar {
  width: 8px;
}

.mr-chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.mr-chat-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.mr-chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

@media (max-width: 640px) {
  .mr-chat-page {
    padding-bottom: 140px;
  }
  
  .mr-chat-header {
    padding: 12px 16px;
  }
  
  .mr-chat-subtitle {
    font-size: 0.8125rem;
  }
  
  .mr-message-bubble {
    max-width: 85%;
  }

  .mr-message-assistant .mr-message-content {
    max-width: 100%;
  }

  .mr-songs-grid,
  .mr-playlist-grid {
    grid-template-columns: 1fr;
  }

  .mr-chat-input {
    padding: 12px;
    gap: 8px;
    flex-wrap: wrap;
  }

  .mr-input-tools {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  
  .mr-input-wrapper {
    padding: 10px 12px 10px 40px;
  }
  
  .mr-input {
    font-size: 0.875rem;
  }
  
  .mr-send-btn {
    width: 40px;
    height: 40px;
  }

  .mr-stop-btn {
    width: 40px;
    height: 40px;
  }

  .mr-stream-status {
    width: 100%;
    justify-content: space-between;
    font-size: 11px;
    padding: 4px 8px;
    gap: 6px;
  }

  .mr-stream-metric {
    font-size: 10px;
  }
}
</style>
