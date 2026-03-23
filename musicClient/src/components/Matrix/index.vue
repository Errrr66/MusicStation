<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, shallowRef } from 'vue'
import type { Frame, ColorFrame, ColorPixel } from '@/utils/matrix'

interface Props {
  rows: number
  cols: number
  pattern?: Frame
  frames?: Frame[]
  colorPattern?: ColorFrame
  colorFrames?: ColorFrame[]
  fps?: number
  autoplay?: boolean
  loop?: boolean
  size?: number
  gap?: number
  brightness?: number
  flipInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  fps: 12,
  autoplay: true,
  loop: true,
  size: 10,
  gap: 2,
  brightness: 1,
  flipInterval: 5000,
})

const canvasRef = shallowRef<HTMLCanvasElement | null>(null)
const frameIndex = ref(0)
const isPlaying = ref(props.autoplay)
const animationId = shallowRef<number | undefined>(undefined)
const lastTimeRef = ref(0)
const accumulatorRef = ref(0)
const isFlipped = ref(false)
const flipTimerId = shallowRef<number | undefined>(undefined)

const canvasWidth = computed(() => props.cols * (props.size + props.gap) - props.gap)
const canvasHeight = computed(() => props.rows * (props.size + props.gap) - props.gap)

function getCurrentFrame(): ColorFrame | Frame | null {
  if (props.colorPattern) return props.colorPattern
  if (props.colorFrames && props.colorFrames.length > 0) {
    return props.colorFrames[frameIndex.value] || props.colorFrames[0]
  }
  if (props.pattern) return props.pattern
  if (props.frames && props.frames.length > 0) {
    return props.frames[frameIndex.value] || props.frames[0]
  }
  return null
}

function isColorFrame(frame: ColorFrame | Frame | null): frame is ColorFrame {
  if (!frame || frame.length === 0) return false
  const firstRow = frame[0]
  if (!firstRow || firstRow.length === 0) return false
  return typeof firstRow[0] !== 'number'
}

function render() {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  const frame = getCurrentFrame()
  if (!frame) return

  const colorMode = isColorFrame(frame)

  for (let y = 0; y < props.rows; y++) {
    for (let x = 0; x < props.cols; x++) {
      const value = frame[y]?.[x]
      if (value === undefined) continue

      let r: number, g: number, b: number, brightness: number

      if (colorMode) {
        const pixel = value as ColorPixel
        r = pixel.r
        g = pixel.g
        b = pixel.b
        brightness = pixel.brightness * props.brightness
      } else {
        brightness = (value as number) * props.brightness
        r = 255
        g = 255
        b = 255
      }

      if (brightness < 0.05) continue

      const renderX = isFlipped.value ? props.cols - 1 - x : x
      const px = renderX * (props.size + props.gap)
      const py = y * (props.size + props.gap)
      const radius = (props.size / 2) * 0.85

      ctx.beginPath()
      ctx.arc(px + props.size / 2, py + props.size / 2, radius, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(${Math.round(r * brightness)},${Math.round(g * brightness)},${Math.round(b * brightness)},${brightness})`
      ctx.fill()
    }
  }
}

function animate(currentTime: number) {
  const frames = props.colorFrames || props.frames
  if (!frames || frames.length === 0 || !isPlaying.value) {
    return
  }

  const frameInterval = 1000 / props.fps

  if (lastTimeRef.value === 0) {
    lastTimeRef.value = currentTime
  }

  const deltaTime = currentTime - lastTimeRef.value
  lastTimeRef.value = currentTime
  accumulatorRef.value += deltaTime

  if (accumulatorRef.value >= frameInterval) {
    accumulatorRef.value -= frameInterval
    const next = frameIndex.value + 1
    if (next >= frames.length) {
      if (props.loop) {
        frameIndex.value = 0
      } else {
        isPlaying.value = false
      }
    } else {
      frameIndex.value = next
    }
    render()
  }

  animationId.value = requestAnimationFrame(animate)
}

function startAnimation() {
  if (animationId.value) {
    cancelAnimationFrame(animationId.value)
  }
  lastTimeRef.value = 0
  accumulatorRef.value = 0
  isPlaying.value = props.autoplay && !props.pattern && !props.colorPattern
  render()
  if (isPlaying.value) {
    animationId.value = requestAnimationFrame(animate)
  }
}

function stopAnimation() {
  if (animationId.value) {
    cancelAnimationFrame(animationId.value)
    animationId.value = undefined
  }
}

function startFlipTimer() {
  if (flipTimerId.value) {
    clearInterval(flipTimerId.value)
  }
  if (props.flipInterval > 0) {
    flipTimerId.value = window.setInterval(() => {
      isFlipped.value = !isFlipped.value
      render()
    }, props.flipInterval)
  }
}

function stopFlipTimer() {
  if (flipTimerId.value) {
    clearInterval(flipTimerId.value)
    flipTimerId.value = undefined
  }
}

watch(
  () => [props.frames, props.colorFrames, props.pattern, props.colorPattern, props.autoplay, props.brightness],
  () => {
    frameIndex.value = 0
    startAnimation()
  }
)

onMounted(() => {
  render()
  startAnimation()
  startFlipTimer()
})

onUnmounted(() => {
  stopAnimation()
  stopFlipTimer()
})

defineExpose({
  frameIndex,
  isPlaying,
  isFlipped,
})
</script>

<template>
  <div class="matrix-wrapper">
    <canvas
      ref="canvasRef"
      :width="canvasWidth"
      :height="canvasHeight"
      class="matrix-canvas"
    />
  </div>
</template>

<style scoped>
.matrix-wrapper {
  background-color: rgba(0, 0, 0, 0.85);
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

:root:not(.dark) .matrix-wrapper {
  background-color: rgba(0, 0, 0, 0.95);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.matrix-canvas {
  display: block;
}
</style>
