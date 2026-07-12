<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  useMatrixMorandiColors,
  drawMatrixCell,
  MATRIX_PATTERNS,
  type MatrixPattern
} from "./useMatrixColors";
import { useMatrixAnimation } from "./useMatrixAnimation";

interface ChartDataItem {
  value: number;
  name: string;
}

const props = defineProps({
  chartData: {
    type: Array as () => ChartDataItem[],
    default: () => []
  }
});

const size = 6;
const gap = 2;
const rows = 24;
const cols = 24;
const cellRadius = (size / 2) * 0.85;
const duration = 1400;

const containerRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const tooltipVisible = ref(false);
const tooltipContent = ref({ name: "", value: 0, percent: 0, color: "" });
const tooltipX = ref(0);
const tooltipY = ref(0);
const activeSegment = ref(-1);

const colors = useMatrixMorandiColors(10);

const displayWidth = cols * (size + gap) - gap;
const displayHeight = rows * (size + gap) - gap;

const totalValue = computed(
  () => props.chartData.reduce((sum, d) => sum + d.value, 0) || 1
);

let ctx: CanvasRenderingContext2D | null = null;

interface Cell {
  x: number;
  y: number;
  color: string;
  brightness: number;
  segmentIndex: number;
  pattern: MatrixPattern;
  angle: number;
}

const cells: Cell[] = [];

function setupCanvas() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = Math.floor(displayWidth * dpr);
  canvas.height = Math.floor(displayHeight * dpr);
  canvas.style.width = `${displayWidth}px`;
  canvas.style.height = `${displayHeight}px`;
  ctx = canvas.getContext("2d");
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function buildCells() {
  cells.length = 0;
  const palette = colors.value;
  const centerX = cols / 2;
  const centerY = rows / 2;
  const radius = Math.min(centerX, centerY) - 1;

  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const dx = c - centerX + 0.5;
      const dy = r - centerY + 0.5;
      const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist > radius) continue;

      let angle = Math.atan2(dy, dx) + Math.PI / 2;
      angle = (angle + Math.PI * 2) % (Math.PI * 2);

      const segmentIndex = resolveSegmentByAngle(angle);
      const color = palette[segmentIndex % palette.length];
      const brightness = 0.55 + (dist / radius) * 0.45;
      const pattern = MATRIX_PATTERNS[segmentIndex % MATRIX_PATTERNS.length];

      cells.push({
        x: c * (size + gap) + size / 2,
        y: r * (size + gap) + size / 2,
        color,
        brightness,
        segmentIndex,
        pattern,
        angle
      });
    }
  }
}

function resolveSegmentByAngle(angle: number): number {
  let current = 0;
  const total = totalValue.value;
  for (let i = 0; i < props.chartData.length; i++) {
    const segmentAngle = (props.chartData[i].value / total) * Math.PI * 2;
    if (angle >= current && angle < current + segmentAngle) {
      return i;
    }
    current += segmentAngle;
  }
  return props.chartData.length - 1;
}

function drawCells(revealProgress: number, _pulse: number, elapsed = 0) {
  if (!ctx) return;
  ctx.clearRect(0, 0, displayWidth, displayHeight);

  const dimOthers = activeSegment.value >= 0;
  const revealAngle = revealProgress * Math.PI * 2;
  const sweep = (elapsed / 1400) * Math.PI * 2;

  for (const cell of cells) {
    if (cell.angle > revealAngle) continue;

    // 矩阵变换波动：沿角度形成旋转波纹
    const wave = Math.sin(cell.angle * 4 - sweep) * 0.5 + 0.5;
    let alpha = cell.brightness * (0.86 + wave * 0.22);
    if (dimOthers && cell.segmentIndex !== activeSegment.value) {
      alpha *= 0.2;
    }
    if (alpha <= 0.05) continue;

    ctx.globalAlpha = Math.min(1, alpha);
    drawMatrixCell(ctx, cell.x, cell.y, cellRadius, cell.pattern, cell.color);
  }

  drawLabels();

  if (activeSegment.value >= 0) {
    drawActiveGlow();
  }

  ctx.globalAlpha = 1;
  ctx.lineWidth = 1;
}

function drawLabels() {
  if (!ctx || props.chartData.length < 2) return;
  const centerX = displayWidth / 2;
  const centerY = displayHeight / 2;
  const radius = (Math.min(cols, rows) * (size + gap)) / 2 - (size + gap) * 4;

  ctx.font = "600 11px 'SF Mono', 'Consolas', monospace";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";

  let currentAngle = -Math.PI / 2;
  for (let i = 0; i < props.chartData.length; i++) {
    const segmentAngle =
      (props.chartData[i].value / totalValue.value) * Math.PI * 2;
    const midAngle = currentAngle + segmentAngle / 2;
    const x = centerX + Math.cos(midAngle) * radius * 0.55;
    const y = centerY + Math.sin(midAngle) * radius * 0.55;
    const palette = colors.value;
    ctx.fillStyle = palette[i % palette.length];
    ctx.globalAlpha = 0.95;
    ctx.fillText(props.chartData[i].name, x, y);
    ctx.fillText(`${props.chartData[i].value}`, x, y + 14);
    currentAngle += segmentAngle;
  }
}

function drawActiveGlow() {
  if (!ctx) return;
  const active = activeSegment.value;
  const color = colors.value[active % colors.value.length];
  ctx.save();
  ctx.globalCompositeOperation = "screen";
  ctx.fillStyle = color;
  ctx.globalAlpha = 0.14;
  for (const cell of cells) {
    if (cell.segmentIndex !== active) continue;
    ctx.beginPath();
    ctx.arc(cell.x, cell.y, cellRadius * 2.4, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();
}

function renderFrame(elapsed: number) {
  if (!ctx) setupCanvas();
  if (!ctx) return;

  const progress = Math.min(elapsed / duration, 1);
  const eased = 1 - Math.pow(1 - progress, 3);
  const pulse = Math.sin((elapsed / 1600) * Math.PI * 2) * 0.5 + 0.5;

  drawCells(eased, pulse, elapsed);
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
  () => props.chartData,
  () => {
    activeSegment.value = -1;
    buildCells();
    setupCanvas();
    restart();
  },
  { deep: true }
);

watch(activeSegment, () => {
  redrawStatic();
});

const legendItems = computed(() => {
  const palette = colors.value;
  return props.chartData.map((item, index) => ({
    name: item.name,
    value: item.value,
    percent: ((item.value / totalValue.value) * 100).toFixed(1),
    color: palette[index % palette.length],
    pattern: MATRIX_PATTERNS[index % MATRIX_PATTERNS.length]
  }));
});

function resolveDataIndex(event: MouseEvent): number {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;

  const centerX = displayWidth / 2;
  const centerY = displayHeight / 2;
  const dx = x - centerX;
  const dy = y - centerY;
  const dist = Math.sqrt(dx * dx + dy * dy);
  const radius = (Math.min(cols, rows) * (size + gap)) / 2 - (size + gap);

  if (dist > radius) return -1;

  let angle = Math.atan2(dy, dx) + Math.PI / 2;
  angle = (angle + Math.PI * 2) % (Math.PI * 2);
  return resolveSegmentByAngle(angle);
}

function handleMouseMove(event: MouseEvent) {
  const dataIndex = resolveDataIndex(event);
  if (dataIndex >= 0 && dataIndex < props.chartData.length) {
    activeSegment.value = dataIndex;
    const palette = colors.value;
    const item = props.chartData[dataIndex];
    tooltipContent.value = {
      name: item.name,
      value: item.value,
      percent: parseFloat(((item.value / totalValue.value) * 100).toFixed(1)),
      color: palette[dataIndex % palette.length]
    };
    tooltipVisible.value = true;
  } else {
    tooltipVisible.value = false;
    activeSegment.value = -1;
  }
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  tooltipX.value = event.clientX - rect.left + 12;
  tooltipY.value = event.clientY - rect.top - 44;
}

function handleMouseLeave() {
  tooltipVisible.value = false;
  activeSegment.value = -1;
}
</script>

<template>
  <div ref="containerRef" class="matrix-pie-container">
    <div
      class="matrix-pie-chart"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <canvas
        ref="canvasRef"
        :width="displayWidth"
        :height="displayHeight"
        :style="{ width: `${displayWidth}px`, height: `${displayHeight}px` }"
        class="matrix-canvas"
        aria-label="Matrix 风格饼图"
      />
      <Transition name="tooltip">
        <div
          v-if="tooltipVisible"
          class="matrix-tooltip"
          :style="{
            left: `${tooltipX}px`,
            top: `${tooltipY}px`,
            '--chart-color': tooltipContent.color
          }"
        >
          <div class="tooltip-header">{{ tooltipContent.name }}</div>
          <div class="tooltip-value">
            {{ tooltipContent.value }}
            <span class="tooltip-percent">({{ tooltipContent.percent }}%)</span>
          </div>
        </div>
      </Transition>
    </div>
    <div class="matrix-legend">
      <div
        v-for="(item, index) in legendItems"
        :key="index"
        class="legend-item"
        :class="{ active: activeSegment === index }"
        :style="{ '--legend-color': item.color }"
        @mouseenter="activeSegment = index"
        @mouseleave="activeSegment = -1"
      >
        <div
          class="legend-dot"
          :class="[`pattern-${item.pattern}`]"
          :style="{ borderColor: item.color, background: item.color }"
        />
        <span class="legend-name">{{ item.name }}</span>
        <span class="legend-value">{{ item.value }}</span>
        <span class="legend-percent">{{ item.percent }}%</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.matrix-pie-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  width: 100%;
  contain: paint layout;
}

.matrix-pie-chart {
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
  border-radius: 2px;
  padding: 6px 10px;
  pointer-events: none;
  z-index: 100;
  font-family: var(--mr-font-family, 'SF Mono', 'Consolas', monospace);
  box-shadow: 0 0 10px var(--matrix-shadow);
  min-width: 80px;
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
  font-size: 13px;
  font-weight: 600;
}

.tooltip-percent {
  color: var(--mr-text-subdued);
  font-weight: 400;
  margin-left: 4px;
}

.tooltip-enter-active,
.tooltip-leave-active {
  transition: opacity 100ms ease;
}

.tooltip-enter-from,
.tooltip-leave-to {
  opacity: 0;
}

.matrix-legend {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-family: var(--mr-font-family, 'SF Mono', 'Consolas', monospace);
  cursor: default;
  transition: opacity 120ms ease;
  padding: 2px 6px;
  border-radius: 2px;
}

.legend-item.active {
  background: var(--mr-bg-elevated);
}

.legend-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--legend-color, var(--matrix-color));
}

.legend-dot.pattern-ring,
.legend-dot.pattern-hollow-diamond {
  background: transparent;
}

.legend-dot.pattern-ring {
  border: 1.5px solid var(--legend-color, var(--matrix-color));
  border-radius: 50%;
}

.legend-dot.pattern-diamond,
.legend-dot.pattern-hollow-diamond {
  border: 1.5px solid var(--legend-color, var(--matrix-color));
  border-radius: 0;
  transform: rotate(45deg) scale(0.8);
}

.legend-dot.pattern-cross {
  background: transparent;
  position: relative;
}

.legend-dot.pattern-cross::before,
.legend-dot.pattern-cross::after {
  content: "";
  position: absolute;
  background: var(--legend-color, var(--matrix-color));
}

.legend-dot.pattern-cross::before {
  width: 1.5px;
  height: 100%;
  left: calc(50% - 0.75px);
  top: 0;
}

.legend-dot.pattern-cross::after {
  width: 100%;
  height: 1.5px;
  top: calc(50% - 0.75px);
  left: 0;
}

.legend-name {
  color: var(--mr-text-subdued);
}

.legend-value {
  color: var(--legend-color, var(--matrix-color));
  font-weight: 600;
}

.legend-percent {
  color: var(--mr-text-subdued);
  opacity: 0.8;
}
</style>
