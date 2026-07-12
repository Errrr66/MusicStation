<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  useMatrixMorandiColors,
  drawMatrixCell,
  MATRIX_PATTERNS,
  type MatrixPattern
} from "./useMatrixColors";
import { useMatrixAnimation } from "./useMatrixAnimation";

const props = defineProps({
  data: {
    type: Array as () => number[],
    default: () => []
  },
  color: {
    type: String,
    default: ""
  }
});

const rows = 9;
const size = 4;
const gap = 2;
const cols = computed(() => props.data.length || 7);
const duration = 1500;

const containerRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const tooltipVisible = ref(false);
const tooltipContent = ref("");
const tooltipX = ref(0);
const tooltipY = ref(0);
const hoverIndex = ref(-1);

const morandiColors = useMatrixMorandiColors(12);
const resolvedColor = computed(() => props.color || "var(--matrix-color)");

const displayWidth = computed(() => cols.value * (size + gap) - gap);
const displayHeight = rows * (size + gap) - gap;

let ctx: CanvasRenderingContext2D | null = null;

interface Point {
  x: number;
  y: number;
  value: number;
  color: string;
  pattern: MatrixPattern;
}

let points: Point[] = [];

function setupCanvas() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = Math.floor(displayWidth.value * dpr);
  canvas.height = Math.floor(displayHeight * dpr);
  canvas.style.width = `${displayWidth.value}px`;
  canvas.style.height = `${displayHeight}px`;
  ctx = canvas.getContext("2d");
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function buildPoints() {
  const maxVal = Math.max(...props.data, 1);
  const palette = morandiColors.value;
  points = props.data.map((val, i) => {
    const normalized = val / maxVal;
    const x = i * (size + gap) + size / 2;
    const y = displayHeight - size / 2 - normalized * (displayHeight - size);
    return {
      x,
      y,
      value: val,
      color: palette[i % palette.length],
      pattern: MATRIX_PATTERNS[i % MATRIX_PATTERNS.length]
    };
  });
}

function drawArea(revealProgress: number) {
  if (!ctx || points.length < 2) return;
  const revealX = displayWidth.value * revealProgress;

  const gradient = ctx.createLinearGradient(0, 0, 0, displayHeight);
  gradient.addColorStop(0, resolvedColor.value);
  gradient.addColorStop(1, "transparent");

  ctx.save();
  ctx.beginPath();
  ctx.moveTo(points[0].x, displayHeight);
  for (const p of points) {
    if (p.x > revealX) break;
    ctx.lineTo(p.x, p.y);
  }
  ctx.lineTo(Math.min(revealX, points[points.length - 1].x), displayHeight);
  ctx.closePath();
  ctx.globalAlpha = 0.18;
  ctx.fillStyle = gradient;
  ctx.fill();
  ctx.restore();
}

function drawLine(revealProgress: number) {
  if (!ctx || points.length < 2) return;
  const revealX = displayWidth.value * revealProgress;

  ctx.save();
  ctx.strokeStyle = resolvedColor.value;
  ctx.lineWidth = 2;
  ctx.lineJoin = "round";
  ctx.lineCap = "round";
  ctx.globalAlpha = 0.9;

  ctx.beginPath();
  let started = false;
  for (const p of points) {
    if (p.x > revealX) break;
    if (!started) {
      ctx.moveTo(p.x, p.y);
      started = true;
    } else {
      ctx.lineTo(p.x, p.y);
    }
  }
  ctx.stroke();
  ctx.restore();
}

function drawMarkers(revealX: number, _pulse: number, elapsed = 0) {
  if (!ctx) return;
  const sweep = (elapsed / 1200) * Math.PI * 2;

  for (let i = 0; i < points.length; i++) {
    const p = points[i];
    if (p.x > revealX) break;

    const isHover = hoverIndex.value === i;
    // 矩阵变换波动：数据点沿 X 轴形成波浪起伏
    const phase = (i / points.length) * Math.PI * 2;
    const wave = Math.sin(sweep + phase) * 0.5 + 0.5;
    const radius = isHover ? size * 1.2 : size * (0.6 + wave * 0.25);
    const alpha = isHover ? 1 : 0.7 + wave * 0.22;

    ctx.globalAlpha = alpha;
    drawMatrixCell(ctx, p.x, p.y, radius, p.pattern, p.color);

    if (isHover) {
      ctx.save();
      ctx.globalCompositeOperation = "screen";
      ctx.globalAlpha = 0.25;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, radius * 2.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }
  }

  ctx.globalAlpha = 1;
  ctx.lineWidth = 1;
}

function drawGrid() {
  if (!ctx) return;
  ctx.save();
  ctx.strokeStyle = resolvedColor.value;
  ctx.lineWidth = 1;
  ctx.globalAlpha = 0.08;

  for (let r = 1; r < rows; r += 2) {
    const y = r * (size + gap) + size / 2;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(displayWidth.value, y);
    ctx.stroke();
  }
  ctx.restore();
}

function renderFrame(elapsed: number) {
  if (!ctx) setupCanvas();
  if (!ctx) return;

  const progress = Math.min(elapsed / duration, 1);
  const eased = 1 - Math.pow(1 - progress, 3);
  const pulse = Math.sin((elapsed / 1200) * Math.PI * 2) * 0.5 + 0.5;

  ctx.clearRect(0, 0, displayWidth.value, displayHeight);
  drawGrid();
  drawArea(eased);
  drawLine(eased);
  drawMarkers(displayWidth.value * eased, pulse, elapsed);
}

const { restart } = useMatrixAnimation(containerRef, {
  fps: 30,
  duration,
  onFrame: renderFrame
});

watch(
  () => props.data,
  () => {
    hoverIndex.value = -1;
    buildPoints();
    setupCanvas();
    restart();
  },
  { deep: true }
);

watch(hoverIndex, () => {
  renderFrame(duration);
});

function resolveIndex(event: MouseEvent): number {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  const x = event.clientX - rect.left;
  let closest = -1;
  let minDist = Infinity;
  for (let i = 0; i < points.length; i++) {
    const dist = Math.abs(points[i].x - x);
    if (dist < minDist) {
      minDist = dist;
      closest = i;
    }
  }
  return closest;
}

function handleMouseMove(event: MouseEvent) {
  const index = resolveIndex(event);
  hoverIndex.value = index;
  if (index >= 0 && index < props.data.length) {
    tooltipContent.value = `${props.data[index] ?? 0}`;
    tooltipVisible.value = true;
  }
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  tooltipX.value = event.clientX - rect.left + 10;
  tooltipY.value = event.clientY - rect.top - 30;
}

function handleMouseLeave() {
  tooltipVisible.value = false;
  hoverIndex.value = -1;
}
</script>

<template>
  <div ref="containerRef" class="matrix-line-chart" :style="{ '--chart-color': resolvedColor }">
    <div
      class="chart-container"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <canvas
        ref="canvasRef"
        :width="displayWidth"
        :height="displayHeight"
        :style="{ width: `${displayWidth}px`, height: `${displayHeight}px` }"
        class="matrix-canvas"
        aria-label="Matrix 风格折线趋势图"
      />
      <Transition name="tooltip">
        <div
          v-if="tooltipVisible"
          class="matrix-tooltip"
          :style="{
            left: `${tooltipX}px`,
            top: `${tooltipY}px`,
            '--chart-color': points[hoverIndex]?.color || resolvedColor
          }"
        >
          <span class="tooltip-value">{{ tooltipContent }}</span>
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.matrix-line-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  contain: paint layout;
}

.chart-container {
  position: relative;
}

.matrix-canvas {
  display: block;
}

.matrix-tooltip {
  position: absolute;
  background: var(--mr-bg-elevated);
  border: 1px solid var(--chart-color, var(--matrix-color));
  padding: 4px 8px;
  pointer-events: none;
  z-index: 100;
  font-family: var(--mr-font-family, 'SF Mono', 'Consolas', monospace);
  font-size: 11px;
  box-shadow: 0 0 8px var(--matrix-shadow);
}

.tooltip-value {
  color: var(--chart-color, var(--matrix-color));
  font-weight: 600;
}

.tooltip-enter-active,
.tooltip-leave-active {
  transition: opacity 100ms ease;
}

.tooltip-enter-from,
.tooltip-leave-to {
  opacity: 0;
}
</style>
