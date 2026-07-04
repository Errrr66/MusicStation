<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'

interface Props {
  barCount?: number
  minHeight?: number
  maxHeight?: number
  gap?: number
  barWidth?: number
  borderRadius?: number
  color?: string
  activeColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  barCount: 32,
  minHeight: 4,
  maxHeight: 32,
  gap: 2,
  barWidth: 3,
  borderRadius: 2,
  color: 'rgba(255, 255, 255, 0.3)',
  activeColor: 'var(--mr-accent)'
})

const { isPlaying, initAnalyser, getAnalyser } = useAudioPlayer()

const frequencyBands = ref<number[]>(new Array(props.barCount).fill(0))
let animationFrameId: number | null = null

const startVisualization = async () => {
  const analyserData = getAnalyser() || initAnalyser()
  if (!analyserData) return

  // 恢复 AudioContext 状态
  if (analyserData.audioContext.state === 'suspended') {
    await analyserData.audioContext.resume()
  }

  const { analyser } = analyserData
  const bufferLength = analyser.frequencyBinCount
  const dataArray = new Uint8Array(bufferLength)

  const updateFrequency = () => {
    if (!analyser) return

    analyser.getByteFrequencyData(dataArray)

    const step = Math.floor(bufferLength / props.barCount)
    const newBands: number[] = []

    for (let i = 0; i < props.barCount; i++) {
      let sum = 0
      for (let j = 0; j < step; j++) {
        sum += dataArray[i * step + j]
      }
      const average = sum / step
      newBands.push(average / 255)
    }

    frequencyBands.value = newBands
    animationFrameId = requestAnimationFrame(updateFrequency)
  }

  animationFrameId = requestAnimationFrame(updateFrequency)
}

const stopVisualization = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
}

const barStyle = computed(() => ({
  width: `${props.barWidth}px`,
  borderRadius: `${props.borderRadius}px`
}))

const getBarHeight = (volume: number) => {
  const height = props.minHeight + volume * (props.maxHeight - props.minHeight)
  return `${height}px`
}


const getBarClass = (volume: number) => {
  return {
    'visualizer-bar-active': volume > 0.5
  }
}

watch(isPlaying, (playing) => {
  if (playing) {
    startVisualization()
  } else {
    stopVisualization()
  }
})

onMounted(() => {
  if (isPlaying.value) {
    startVisualization()
  }
})

onUnmounted(() => {
  stopVisualization()
})
</script>

<template>
  <div class="audio-visualizer">
    <div
      class="visualizer-bars"
      :style="{ gap: `${gap}px` }"
    >
      <div
        v-for="(volume, index) in frequencyBands"
        :key="index"
        class="visualizer-bar"
        :class="getBarClass(volume)"
        :style="{
          ...barStyle,
          height: getBarHeight(volume)
        }"
      />
    </div>
  </div>
</template>

<style scoped>
.audio-visualizer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 8px;
}

.visualizer-bars {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 100%;
}

.visualizer-bar {
  background-color: var(--visualizer-bar-color, rgba(255, 255, 255, 0.3));
  transition: height 0.05s ease-out, background-color 0.15s ease;
  min-height: 4px;
}

.visualizer-bar-active {
  background-color: var(--visualizer-bar-active-color, var(--mr-accent));
}
</style>
