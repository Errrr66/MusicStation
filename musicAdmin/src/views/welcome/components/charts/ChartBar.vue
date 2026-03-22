<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from "vue";

const props = defineProps({
  barChartData: {
    type: Array as () => number[],
    default: () => []
  }
});

const labels = ["美国", "中国", "韩国", "日本", "德国", "英国"];
const color = "var(--matrix-color)";

const size = 10;
const gap = 3;
const rows = 10;
const cols = computed(() => props.barChartData.length || 6);

const tooltipVisible = ref(false);
const tooltipContent = ref({ label: "", value: 0 });
const tooltipX = ref(0);
const tooltipY = ref(0);

const animatedValues = ref<number[]>([]);
let animationId: number | undefined;
let startTime = 0;
let wavePhase = 0;

function animate(currentTime: number) {
  if (!startTime) startTime = currentTime;
  const elapsed = currentTime - startTime;
  const duration = 1500;
  const progress = Math.min(elapsed / duration, 1);

  animatedValues.value = props.barChartData.map((val, i) => {
    const delay = i * 80;
    const adjustedProgress = Math.max(0, Math.min(1, (elapsed - delay) / (duration - delay)));
    const adjustedEased = 1 - Math.pow(1 - adjustedProgress, 3);
    return val * adjustedEased;
  });

  wavePhase = (elapsed / 1500) * Math.PI * 2;

  animationId = requestAnimationFrame(animate);
}

onMounted(() => {
  animatedValues.value = props.barChartData.map(() => 0);
  animationId = requestAnimationFrame(animate);
});

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId);
  }
});

const barFrame = computed(() => {
  const frame: { brightness: number; colIndex: number }[][] = [];
  const maxVal = Math.max(...animatedValues.value, 1);

  for (let r = 0; r < rows; r++) {
    frame[r] = [];
    for (let c = 0; c < cols.value; c++) {
      const normalizedValue = (animatedValues.value[c] || 0) / maxVal;
      const height = Math.floor(normalizedValue * rows);
      const rowFromBottom = rows - 1 - r;

      const wave = Math.sin(wavePhase + c * 0.5) * 0.08;
      const adjustedHeight = height + wave * 2;

      if (rowFromBottom < adjustedHeight) {
        const brightness = 1 - (rowFromBottom / rows) * 0.3;
        frame[r][c] = { brightness: Math.max(0.5, brightness), colIndex: c };
      } else if (rowFromBottom === Math.floor(adjustedHeight)) {
        frame[r][c] = { brightness: 0.8, colIndex: c };
      } else {
        frame[r][c] = { brightness: 0, colIndex: c };
      }
    }
  }
  return frame;
});

const cellPositions = computed(() => {
  const positions: { x: number; y: number }[][] = [];
  for (let row = 0; row < rows; row++) {
    positions[row] = [];
    for (let col = 0; col < cols.value; col++) {
      positions[row][col] = {
        x: col * (size + gap),
        y: row * (size + gap)
      };
    }
  }
  return positions;
});

const svgDimensions = computed(() => ({
  width: cols.value * (size + gap) - gap,
  height: rows * (size + gap) - gap
}));

function handleCellHover(colIndex: number, event: MouseEvent) {
  if (colIndex >= 0 && colIndex < props.barChartData.length) {
    tooltipContent.value = {
      label: labels[colIndex] || "",
      value: props.barChartData[colIndex]
    };
    tooltipVisible.value = true;
    updateTooltipPosition(event);
  }
}

function handleMouseMove(event: MouseEvent) {
  updateTooltipPosition(event);
}

function handleMouseLeave() {
  tooltipVisible.value = false;
}

function updateTooltipPosition(event: MouseEvent) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  tooltipX.value = event.clientX - rect.left + 15;
  tooltipY.value = event.clientY - rect.top - 40;
}
</script>

<template>
  <div class="matrix-bar-container">
    <div
      class="matrix-bar-chart"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
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
          <filter id="bar-pixel-glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="0.5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>
        <template v-for="(row, rowIndex) in barFrame" :key="rowIndex">
          <rect
            v-for="(cell, colIndex) in row"
            :key="`${rowIndex}-${colIndex}`"
            :class="['matrix-pixel', cell.brightness > 0.5 && 'matrix-pixel-active']"
            :x="cellPositions[rowIndex]?.[colIndex]?.x"
            :y="cellPositions[rowIndex]?.[colIndex]?.y"
            :width="size"
            :height="size"
            :fill="color"
            :opacity="cell.brightness > 0.05 ? cell.brightness : 0.1"
            @mouseenter="handleCellHover(cell.colIndex, $event)"
          />
        </template>
      </svg>
      <Transition name="tooltip">
        <div
          v-if="tooltipVisible"
          class="matrix-tooltip"
          :style="{ left: `${tooltipX}px`, top: `${tooltipY}px` }"
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
        :style="{ width: `${size}px`, marginLeft: index === 0 ? 0 : `${gap}px` }"
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
}

.matrix-bar-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.matrix-pixel {
  transition: opacity 100ms ease-out;
}

.matrix-pixel-active {
  filter: url(#bar-pixel-glow);
}

.matrix-tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.95);
  border: 1px solid var(--matrix-color);
  padding: 6px 10px;
  pointer-events: none;
  z-index: 100;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.tooltip-header {
  color: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 2px;
}

.tooltip-value {
  color: var(--matrix-color);
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
  color: var(--matrix-text-dim);
  font-family: 'SF Mono', 'Consolas', monospace;
}
</style>
