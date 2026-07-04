<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from "vue";

export type Frame = number[][];

interface MatrixProps {
  rows: number;
  cols: number;
  pattern?: Frame;
  frames?: Frame[];
  fps?: number;
  autoplay?: boolean;
  loop?: boolean;
  size?: number;
  gap?: number;
  palette?: {
    on: string;
    off: string;
  };
  brightness?: number;
  ariaLabel?: string;
  onFrame?: (index: number) => void;
  mode?: "default" | "vu";
  levels?: number[];
}

const props = withDefaults(defineProps<MatrixProps>(), {
  fps: 12,
  autoplay: true,
  loop: true,
  size: 10,
  gap: 2,
  palette: () => ({
    on: "currentColor",
    off: "rgba(128, 128, 128, 0.3)"
  }),
  brightness: 1,
  mode: "default"
});

function clamp(value: number): number {
  return Math.max(0, Math.min(1, value));
}

function ensureFrameSize(frame: Frame, rows: number, cols: number): Frame {
  const result: Frame = [];
  for (let r = 0; r < rows; r++) {
    const row = frame[r] || [];
    result.push([]);
    for (let c = 0; c < cols; c++) {
      result[r][c] = row[c] ?? 0;
    }
  }
  return result;
}

function emptyFrame(rows: number, cols: number): Frame {
  return Array.from({ length: rows }, () => Array(cols).fill(0));
}

function vu(columns: number, levels: number[]): Frame {
  const rows = 7;
  const frame = emptyFrame(rows, columns);
  for (let col = 0; col < Math.min(columns, levels.length); col++) {
    const level = Math.max(0, Math.min(1, levels[col]));
    const height = Math.floor(level * rows);
    for (let row = 0; row < rows; row++) {
      const rowFromBottom = rows - 1 - row;
      if (rowFromBottom < height) {
        let brightness = 1;
        if (row < rows * 0.3) {
          brightness = 1;
        } else if (row < rows * 0.6) {
          brightness = 0.8;
        } else {
          brightness = 0.6;
        }
        frame[row][col] = brightness;
      }
    }
  }
  return frame;
}

const frameIndex = ref(0);
const isPlaying = ref(props.autoplay);
let frameId: number | undefined;
let lastTime = 0;
let accumulator = 0;

const currentFrame = computed(() => {
  if (props.mode === "vu" && props.levels && props.levels.length > 0) {
    return ensureFrameSize(vu(props.cols, props.levels), props.rows, props.cols);
  }
  if (props.pattern) {
    return ensureFrameSize(props.pattern, props.rows, props.cols);
  }
  if (props.frames && props.frames.length > 0) {
    return ensureFrameSize(props.frames[frameIndex.value] || props.frames[0], props.rows, props.cols);
  }
  return ensureFrameSize([], props.rows, props.cols);
});

const cellPositions = computed(() => {
  const positions: { x: number; y: number }[][] = [];
  for (let row = 0; row < props.rows; row++) {
    positions[row] = [];
    for (let col = 0; col < props.cols; col++) {
      positions[row][col] = {
        x: col * (props.size + props.gap),
        y: row * (props.size + props.gap)
      };
    }
  }
  return positions;
});

const svgDimensions = computed(() => ({
  width: props.cols * (props.size + props.gap) - props.gap,
  height: props.rows * (props.size + props.gap) - props.gap
}));

function animate(currentTime: number) {
  if (!props.frames || props.frames.length === 0 || !isPlaying.value) {
    return;
  }

  const frameInterval = 1000 / props.fps;

  if (lastTime === 0) {
    lastTime = currentTime;
  }

  const deltaTime = currentTime - lastTime;
  lastTime = currentTime;
  accumulator += deltaTime;

  if (accumulator >= frameInterval) {
    accumulator -= frameInterval;
    const next = frameIndex.value + 1;

    if (next >= props.frames.length) {
      if (props.loop) {
        props.onFrame?.(0);
        frameIndex.value = 0;
      } else {
        isPlaying.value = false;
      }
    } else {
      props.onFrame?.(next);
      frameIndex.value = next;
    }
  }

  frameId = requestAnimationFrame(animate);
}

onMounted(() => {
  if (props.autoplay && props.frames && props.frames.length > 0) {
    frameId = requestAnimationFrame(animate);
  }
  // 页面隐藏时暂停 rAF，可见时恢复，避免后台持续渲染
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onUnmounted(() => {
  if (frameId) {
    cancelAnimationFrame(frameId);
  }
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});

function handleVisibilityChange() {
  if (document.hidden) {
    if (frameId) {
      cancelAnimationFrame(frameId);
      frameId = undefined;
    }
  } else if (isPlaying.value && props.frames && props.frames.length > 0) {
    lastTime = 0;
    accumulator = 0;
    if (!frameId) {
      frameId = requestAnimationFrame(animate);
    }
  }
}

watch(
  () => props.frames,
  () => {
    frameIndex.value = 0;
    isPlaying.value = props.autoplay;
    lastTime = 0;
    accumulator = 0;
  }
);
</script>

<template>
  <div
    class="matrix-display"
    role="img"
    :aria-label="ariaLabel ?? 'matrix display'"
    :style="{
      '--matrix-on': palette.on,
      '--matrix-off': palette.off,
      '--matrix-gap': `${gap}px`,
      '--matrix-size': `${size}px`
    }"
  >
    <svg
      :width="svgDimensions.width"
      :height="svgDimensions.height"
      :viewBox="`0 0 ${svgDimensions.width} ${svgDimensions.height}`"
      xmlns="http://www.w3.org/2000/svg"
      class="block"
      style="overflow: visible"
    >
      <defs>
        <radialGradient id="matrix-pixel-on" cx="50%" cy="50%" r="50%">
          <stop offset="0%" :stop-color="palette.on" stop-opacity="1" />
          <stop offset="70%" :stop-color="palette.on" stop-opacity="0.85" />
          <stop offset="100%" :stop-color="palette.on" stop-opacity="0.6" />
        </radialGradient>
        <radialGradient id="matrix-pixel-off" cx="50%" cy="50%" r="50%">
          <stop offset="0%" :stop-color="palette.off" stop-opacity="1" />
          <stop offset="100%" :stop-color="palette.off" stop-opacity="0.7" />
        </radialGradient>
        <filter id="matrix-glow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="2" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>
      <template v-for="(row, rowIndex) in currentFrame" :key="rowIndex">
        <circle
          v-for="(value, colIndex) in row"
          :key="`${rowIndex}-${colIndex}`"
          :class="[
            'matrix-pixel',
            clamp(brightness * value) > 0.5 && 'matrix-pixel-active',
            clamp(brightness * value) <= 0.05 && 'opacity-20'
          ]"
          :cx="cellPositions[rowIndex]?.[colIndex]?.x + size / 2"
          :cy="cellPositions[rowIndex]?.[colIndex]?.y + size / 2"
          :r="(size / 2) * 0.9"
          :fill="clamp(brightness * value) > 0.05 ? 'url(#matrix-pixel-on)' : 'url(#matrix-pixel-off)'"
          :opacity="clamp(brightness * value) > 0.05 ? clamp(brightness * value) : 0.1"
          :style="{ transform: `scale(${clamp(brightness * value) > 0.5 ? 1.1 : 1})` }"
        />
      </template>
    </svg>
  </div>
</template>

<style scoped>
.matrix-display {
  position: relative;
  display: inline-block;
}

.matrix-pixel {
  transition: opacity 300ms ease-out, transform 150ms ease-out;
  transform-origin: center;
  transform-box: fill-box;
}

.matrix-pixel-active {
  filter: url(#matrix-glow);
}
</style>
