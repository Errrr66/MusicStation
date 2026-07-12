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
  barChartData: {
    type: Array as () => number[],
    default: () => []
  }
});

const labels = ["美国", "中国", "韩国", "日本", "德国", "英国"];

const size = 10;
const gap = 3;
const rows = 16;
const cols = computed(() => props.barChartData.length || 6);
const duration = 1500;

const containerRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const tooltipVisible = ref(false);
const tooltipContent = ref({ label: "", value: 0 });
const tooltipX = ref(0);
const tooltipY = ref(0);
const activeCol = ref(-1);

const colors = useMatrixMorandiColors(6);

const displayWidth = computed(() => cols.value * (size + gap) - gap);
const displayHeight = rows * (size + gap) - gap;

let ctx: CanvasRenderingContext2D | null = null;

interface Cell {
  x: number;
  y: number;
  col: number;
  row: number;
  color: string;
  baseBrightness: number;
  pattern: MatrixPattern;
  isTop: boolean;
}

const cells: Cell[] = [];

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

function buildCells() {
  cells.length = 0;
  const palette = colors.value;
  const count = cols.value;

  for (let c = 0; c < count; c++) {
    const color = palette[c % palette.length];
    const colPattern = MATRIX_PATTERNS[c % MATRIX_PATTERNS.length];
    for (let r = 0; r < rows; r++) {
      const rowFromBottom = rows - 1 - r;
      const baseBrightness = 0.45 + (rowFromBottom / rows) * 0.55;
      // 每个柱子内部做变化：主体用一种 pattern，顶部用 ring 强调
      const isTop = rowFromBottom === 0;
      cells.push({
        x: c * (size + gap) + size / 2,
        y: r * (size + gap) + size / 2,
        col: c,
        row: r,
        color,
        baseBrightness,
        pattern: isTop ? "ring" : colPattern,
        isTop
      });
    }
  }
}

function drawCells(progress: number, _pulse: number, elapsed = 0) {
  if (!ctx) return;
  ctx.clearRect(0, 0, displayWidth.value, displayHeight);

  const maxVal = Math.max(...props.barChartData, 1);
  const dimOthers = activeCol.value >= 0;
  const count = cols.value;
  const sweep = (elapsed / 1200) * Math.PI * 2;

  for (const cell of cells) {
    const colIndex = cell.col;
    const val = props.barChartData[colIndex] || 0;
    const normalizedHeight = (val / maxVal) * rows;
    const rowFromBottom = rows - 1 - cell.row;

    const delay = colIndex * 80;
    const colProgress = Math.max(
      0,
      Math.min(1, (progress * duration - delay) / (duration - delay))
    );
    const eased = 1 - Math.pow(1 - colProgress, 3);
    const animatedHeight = normalizedHeight * eased;

    if (rowFromBottom >= animatedHeight) continue;

    // 矩阵变换波动：沿列形成横向波纹，VU meter 感
    const phase = (colIndex / count) * Math.PI * 2;
    const wave = Math.sin(sweep + phase) * 0.5 + 0.5;
    let alpha = cell.baseBrightness * (0.86 + wave * 0.18);
    const isTopCell = rowFromBottom >= animatedHeight - 1 && rowFromBottom < animatedHeight;

    if (dimOthers && colIndex !== activeCol.value) {
      alpha *= 0.22;
    } else if (isTopCell) {
      alpha = Math.min(1, alpha * 1.35);
    }

    if (alpha <= 0.05) continue;

    ctx.globalAlpha = Math.min(1, alpha);

    const radius = isTopCell ? (size / 2) * 0.95 : (size / 2) * 0.78;
    drawMatrixCell(ctx, cell.x, cell.y, radius, cell.pattern, cell.color);
  }

  if (activeCol.value >= 0) {
    drawActiveGlow();
  }

  ctx.globalAlpha = 1;
  ctx.lineWidth = 1;
}

function drawActiveGlow() {
  if (!ctx) return;
  const active = activeCol.value;
  const color = colors.value[active % colors.value.length];
  ctx.save();
  ctx.globalCompositeOperation = "screen";
  ctx.fillStyle = color;
  ctx.globalAlpha = 0.12;
  for (const cell of cells) {
    if (cell.col !== active) continue;
    ctx.beginPath();
    ctx.arc(cell.x, cell.y, size, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();
}

function renderFrame(elapsed: number) {
  if (!ctx) setupCanvas();
  if (!ctx) return;

  const progress = Math.min(elapsed / duration, 1);
  const pulse = Math.sin((elapsed / 1500) * Math.PI * 2) * 0.5 + 0.5;

  drawCells(progress, pulse, elapsed);
}

function redrawStatic() {
  drawCells(1, 0.5, 0);
}

const { restart } = useMatrixAnimation(containerRef, {
  fps: 30,
  duration,
  onFrame: renderFrame
});

watch(
  () => props.barChartData,
  () => {
    activeCol.value = -1;
    buildCells();
    setupCanvas();
    restart();
  },
  { deep: true }
);

watch(activeCol, () => {
  redrawStatic();
});

function resolveColIndex(event: MouseEvent): number {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  const x = event.clientX - rect.left;
  return Math.floor(x / (size + gap));
}

function handleMouseMove(event: MouseEvent) {
  const colIndex = resolveColIndex(event);
  if (colIndex >= 0 && colIndex < props.barChartData.length) {
    activeCol.value = colIndex;
    tooltipContent.value = {
      label: labels[colIndex] || "",
      value: props.barChartData[colIndex]
    };
    tooltipVisible.value = true;
  }
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  tooltipX.value = event.clientX - rect.left + 15;
  tooltipY.value = event.clientY - rect.top - 40;
}

function handleMouseLeave() {
  tooltipVisible.value = false;
  activeCol.value = -1;
}
</script>

<template>
  <div ref="containerRef" class="matrix-bar-container">
    <div
      class="matrix-bar-chart"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <canvas
        ref="canvasRef"
        :width="displayWidth"
        :height="displayHeight"
        :style="{
          width: `${displayWidth}px`,
          height: `${displayHeight}px`
        }"
        class="matrix-canvas"
        aria-label="Matrix 风格柱状图"
      />
      <Transition name="tooltip">
        <div
          v-if="tooltipVisible"
          class="matrix-tooltip"
          :style="{
            left: `${tooltipX}px`,
            top: `${tooltipY}px`,
            '--chart-color': colors[activeCol % colors.length]
          }"
        >
          <div class="tooltip-header">{{ tooltipContent.label }}</div>
          <div class="tooltip-value">{{ tooltipContent.value }}</div>
        </div>
      </Transition>
    </div>
    <div class="matrix-bar-labels">
      <div
        v-for="(label, index) in labels"
        :key="index"
        class="bar-label"
        :class="{ active: activeCol === index }"
        :style="{
          width: `${size}px`,
          marginLeft: index === 0 ? 0 : `${gap}px`,
          '--label-color': colors[index % colors.length]
        }"
      >
        {{ label }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.matrix-bar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
  contain: paint layout;
}

.matrix-bar-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.matrix-canvas {
  display: block;
}

.matrix-tooltip {
  position: absolute;
  background: var(--mr-bg-elevated);
  border: 1px solid var(--chart-color, var(--matrix-color));
  padding: 6px 10px;
  pointer-events: none;
  z-index: 100;
  font-family: var(--mr-font-family, 'SF Mono', 'Consolas', monospace);
  box-shadow: 0 0 8px var(--matrix-shadow);
}

.tooltip-header {
  color: var(--mr-text-subdued);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 2px;
}

.tooltip-value {
  color: var(--chart-color, var(--matrix-color));
  font-size: 14px;
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

.matrix-bar-labels {
  display: flex;
  justify-content: center;
}

.bar-label {
  text-align: center;
  font-size: 0.6rem;
  color: var(--mr-text-subdued);
  font-family: var(--mr-font-family, 'SF Mono', 'Consolas', monospace);
  transition: color 120ms ease;
}

.bar-label.active {
  color: var(--label-color, var(--matrix-color));
  font-weight: 600;
}
</style>
