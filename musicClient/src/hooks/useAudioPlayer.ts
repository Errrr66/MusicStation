import { trackModel } from '@/stores/interface'
import { defaultSong } from '@/mock'
import { ElNotification } from 'element-plus'
import { PlayMode } from './interface'
import { urlV1 } from '@/api'
import { AudioStore } from '@/stores/modules/audio'
import { fixUrl } from '@/utils'

interface AudioAnalyser {
  audioContext: AudioContext
  analyser: AnalyserNode
  source: MediaElementAudioSourceNode
}

interface AudioPlayer {
  isPlaying: Ref<boolean>
  currentTrack: ComputedRef<trackModel>
  currentTime: Ref<number>
  duration: Ref<number>
  volume: Ref<number>
  playMode: Ref<PlayMode>
  audioElement: Ref<HTMLAudioElement | null>
  play: () => Promise<void>
  pause: () => void
  nextTrack: () => Promise<void>
  prevTrack: () => Promise<void>
  seek: (time: number) => void
  togglePlayPause: () => void
  setVolume: (volume: number) => void
  setPlayMode: (mode: PlayMode) => void
  togglePlayMode: () => void
  loadTrack: (index?: number) => Promise<void>
  initAnalyser: () => AudioAnalyser | null
  getAnalyser: () => AudioAnalyser | null
}

export const AudioPlayer = () => {
  const audioStore = AudioStore()
  const audioElement = ref<HTMLAudioElement | null>(null)
  const isPlaying = ref(false)
  const volume = ref()
  const playMode = ref<PlayMode>('order')
  const audioAnalyser = ref<AudioAnalyser | null>(null)

  // 当前播放的歌曲
  const currentTrack = computed<trackModel>(() => {
    const track = audioStore.trackList[audioStore.currentSongIndex] || defaultSong
    return {
      ...track,
      cover: fixUrl(track.cover),
      url: fixUrl(track.url),
    }
  })
  const currentTime = ref(0)
  const duration = ref(0)
  // 播放音乐
  const play = async () => {
    if (audioElement.value) {
      // 初始化分析器（如果尚未初始化）
      if (!audioAnalyser.value) {
        initAnalyser()
      }
      // 恢复 AudioContext 状态（浏览器要求用户交互后才能播放）
      if (audioAnalyser.value && audioAnalyser.value.audioContext.state === 'suspended') {
        await audioAnalyser.value.audioContext.resume()
      }
      audioElement.value.play()
      isPlaying.value = true
    }
  }
  // 跳转到指定时间
  const seek = (time: number) => {
    if (audioElement.value) {
      audioElement.value.currentTime = time
      currentTime.value = time
      // 自动播放
      if (!isPlaying.value) {
        play()
      }
    }
  }
  // 暂停音乐
  const pause = () => {
    if (audioElement.value) {
      audioElement.value.pause()
      isPlaying.value = false
    }
  }

  // 播放下一首
  const nextTrack = async () => {
    switch (playMode.value) {
      case 'loop':
        if (audioStore.currentSongIndex < audioStore.trackList.length - 1) {
          audioStore.currentSongIndex++
        } else {
          audioStore.currentSongIndex = 0 // 从头开始
        }
        break
      case 'shuffle':
        audioStore.currentSongIndex = Math.floor(
          Math.random() * audioStore.trackList.length
        )
        break
      case 'single':
        audioElement.value!.currentTime = 0
        break
      case 'order':
      default:
        if (audioStore.currentSongIndex < audioStore.trackList.length - 1) {
          audioStore.currentSongIndex++
        } else {
          audioStore.currentSongIndex = 0 // 从头开始
        }
        break
    }
    await loadTrack()
    play()
  }

  // 播放上一首
  const prevTrack = async () => {
    switch (playMode.value) {
      case 'loop':
        if (audioStore.currentSongIndex > 0) {
          audioStore.currentSongIndex--
        } else {
          audioStore.currentSongIndex = audioStore.trackList.length - 1 // 从尾开始
        }
        break
      case 'shuffle':
        audioStore.currentSongIndex = Math.floor(
          Math.random() * audioStore.trackList.length
        )
        break
      case 'single':
        audioElement.value!.currentTime = 0
        break
      case 'order':
      default:
        if (audioStore.currentSongIndex > 0) {
          audioStore.currentSongIndex--
        } else {
          audioStore.currentSongIndex = audioStore.trackList.length - 1 // 从尾开始
        }
        break
    }
    await loadTrack()
    play()
  }

  // 加载当前歌曲
  const loadTrack = async (index?: number) => {
    // 如果传入了索引，先更新当前歌曲索引
    if (index !== undefined) {
      audioStore.currentSongIndex = index
    }
    // 检查歌曲 URL
    await checkUrl()
    // 歌词是否存在
    // checkLyrics()

    if (audioElement.value) {
      audioElement.value.src = fixUrl(currentTrack.value.url)
      audioElement.value.load()
    }
  }

  // 检查歌曲 URL
  const checkUrl = async () => {
    // 查看歌曲 URL 是否存在
    if (!currentTrack.value.url) {
      // 如果 currentTrack 的 url 不存在，则获取 URL
      const response = await urlV1(currentTrack.value.id)
      const url = response.data[0]?.url // 获取第一个 URL

      if (!url) return
      // 更新 trackList 中的对应歌曲的 url
      const trackIndex = audioStore.trackList.findIndex(
        (track: { id: any }) => track.id === currentTrack.value.id
      )
      if (trackIndex !== -1) {
        audioStore.trackList[trackIndex].url = fixUrl(url) // 更新 URL
      }
    }
    return Promise.resolve()
  }

  // 更新当前播放时间
  const updateTime = () => {
    if (audioElement.value) {
      currentTime.value = audioElement.value.currentTime
    }
  }

  // 更新总时长
  const onLoadedMetadata = () => {
    if (audioElement.value) {
      duration.value = audioElement.value.duration
    }
  }

  // 切换播放/暂停状态
  const togglePlayPause = () => {
    if (isPlaying.value) {
      pause()
    } else {
      play()
    }
  }

  // 设置音量
  const setVolume = (newVolume: number) => {
    if (audioElement.value) {
      volume.value = newVolume
      audioStore.setAudioStore('volume', newVolume)
      audioElement.value.volume = newVolume / 100
    }
  }

  // 设置播放模式
  const setPlayMode = (mode: PlayMode) => {
    playMode.value = mode
    const modeText = {
      order: '顺序播放',
      shuffle: '随机播放',
      loop: '列表循环',
      single: '单曲循环',
    }
    ElNotification({
      title: '播放模式',
      message: `已切换为${modeText[mode]}`,
      type: 'success',
    })
  }
  
  const togglePlayMode = () => {
    const modes: PlayMode[] = ['order', 'shuffle', 'loop', 'single']
    const currentIndex = modes.indexOf(playMode.value)
    const nextIndex = (currentIndex + 1) % modes.length
    setPlayMode(modes[nextIndex])
  }

  const initAnalyser = (): AudioAnalyser | null => {
    if (!audioElement.value) return null
    
    if (audioAnalyser.value) {
      return audioAnalyser.value
    }

    try {
      const ctx = new (window.AudioContext || (window as any).webkitAudioContext)()
      const analyser = ctx.createAnalyser()
      analyser.fftSize = 256
      analyser.smoothingTimeConstant = 0.8
      analyser.minDecibels = -90
      analyser.maxDecibels = -10

      const source = ctx.createMediaElementSource(audioElement.value)
      source.connect(analyser)
      analyser.connect(ctx.destination)

      // 立即尝试恢复 AudioContext
      if (ctx.state === 'suspended') {
        ctx.resume()
      }

      audioAnalyser.value = {
        audioContext: ctx,
        analyser,
        source
      }

      return audioAnalyser.value
    } catch (error) {
      console.error('Failed to initialize audio analyser:', error)
      return null
    }
  }

  const getAnalyser = (): AudioAnalyser | null => {
    return audioAnalyser.value
  }

  // 组件挂载时初始化音频元素
  onMounted(() => {
    audioElement.value = new Audio()
    audioElement.value.crossOrigin = 'anonymous'
    audioElement.value.src = currentTrack.value.url
    volume.value = audioStore.volume || 50
    audioElement.value.volume = volume.value / 100
    // 歌词是否存在
    // checkLyrics()
    // 添加事件监听器
    audioElement.value.addEventListener('timeupdate', updateTime)
    audioElement.value.addEventListener('ended', nextTrack)
    audioElement.value.addEventListener('loadedmetadata', onLoadedMetadata)
  })

  // 组件卸载时移除事件监听器
  onUnmounted(() => {
    if (audioElement.value) {
      audioElement.value.removeEventListener('timeupdate', updateTime)
      audioElement.value.removeEventListener('ended', nextTrack)
      audioElement.value.removeEventListener('loadedmetadata', onLoadedMetadata)
    }
    if (audioAnalyser.value) {
      audioAnalyser.value.source.disconnect()
      audioAnalyser.value.analyser.disconnect()
      audioAnalyser.value.audioContext.close()
      audioAnalyser.value = null
    }
  })

  const audioPlayer: AudioPlayer = {
    isPlaying,
    currentTrack,
    currentTime,
    duration,
    volume,
    playMode,
    audioElement,
    play,
    pause,
    nextTrack,
    prevTrack,
    seek,
    togglePlayPause,
    setVolume,
    setPlayMode,
    togglePlayMode,
    loadTrack,
    initAnalyser,
    getAnalyser,
  }

  return audioPlayer
}

export const useAudioPlayer = (): AudioPlayer => {
  const audioPlayer = inject<AudioPlayer>('audioPlayer')
  if (!audioPlayer) {
    throw new Error('useAudioPlayer must be used within a provider')
  }
  return audioPlayer
}
