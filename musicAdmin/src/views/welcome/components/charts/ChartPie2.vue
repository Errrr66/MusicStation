<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from "vue";

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

const colors = ["#ff6b35", "#41b6ff"];

const size = 8;
const gap = 1;
const rows = 18;
const cols = 18;

const tooltipVisible = ref(false);
const tooltipContent = ref({ name: "", value: 0, color: "" });
const tooltipX = ref(0);
const tooltipY = ref(0);

const animatedValues = ref<number[]>([]);
let animationId: number | undefined;
let startTime = 0;
let pulsePhase = 0;

function animate(currentTime: number) {
  if (!startTime) startTime = currentTime;
  const elapsed = currentTime - startTime;
  const duration = 1500;
  const progress = Math.min(elapsed / duration, 1);
  const eased = 1 - Math.pow(1 - progress, 4);

  animatedValues.value = props.chartData.map(d => d.value * eased);
  pulsePhase = (elapsed / 2000) * Math.PI * 2;

  animationId = requestAnimationFrame(animate);
}

onMounted(() => {
  animatedValues.value = props.chartData.map(() => 0);
  animationId = requestAnimationFrame(animate);
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId);
  }
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});

function handleVisibilityChange() {
  if (document.hidden) {
    if (animationId) {
      cancelAnimationFrame(animationId);
      animationId = undefined;
    }
  } else if (!animationId) {
    startTime = 0;
    animationId = requestAnimationFrame(animate);
  }
}

const pieFrame = computed(() => {
  const frame: { color: string; opacity: number; dataIndex: number }[][] = [];
  for (let r = 0; r < rows; r++) {
    frame[r] = [];
    for (let c = 0; c < cols; c++) {
      frame[r][c] = { color: "transparent", opacity: 0, dataIndex: -1 };
    }
  }

  const centerX = cols / 2;
  const centerY = rows / 2;
  const radius = Math.min(centerX, centerY) - 1;

  const currentTotal = animatedValues.value.reduce((a, b) => a + b, 0) || 1;
  let currentAngle = -Math.PI / 2;

  const pulse = Math.sin(pulsePhase) * 0.05 + 1;

  for (let i = 0; i < animatedValues.value.length; i++) {
    const value = animatedValues.value[i];
    const angle = (value / currentTotal) * Math.PI * 2;
    const endAngle = currentAngle + angle;
    const color = colors[i % colors.length];

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const dx = c - centerX;
        const dy = r - centerY;
        const dist = Math.sqrt(dx * dx + dy * dy);

        const adjustedRadius = radius * pulse;

        if (dist <= adjustedRadius) {
          let pointAngle = Math.atan2(dy, dx);
          if (pointAngle < currentAngle) {
            pointAngle += Math.PI * 2;
          }
          if (pointAngle >= currentAngle && pointAngle < endAngle) {
            frame[r][c] = { color, opacity: 1, dataIndex: i };
          }
        }
      }
    }
    currentAngle = endAngle;
  }

  return frame;
});

const cellPositions = computed(() => {
  const positions: { x: number; y: number }[][] = [];
  for (let row = 0; row < rows; row++) {
    positions[row] = [];
    for (let col = 0; col < cols; col++) {
      positions[row][col] = {
        x: col * (size + gap),
        y: row * (size + gap)
      };
    }
  }
  return positions;
});

const svgDimensions = computed(() => ({
  width: cols * (size + gap) - gap,
  height: rows * (size + gap) - gap
}));

const legendItems = computed(() => {
  return props.chartData.map((item, index) => ({
    name: item.name,
    value: item.value,
    color: colors[index % colors.length]
  }));
});

function handleCellHover(dataIndex: number, event: MouseEvent) {
  if (dataIndex >= 0 && dataIndex < props.chartData.length) {
    const item = props.chartData[dataIndex];
    tooltipContent.value = {
      name: item.name,
      value: item.value,
      color: colors[dataIndex % colors.length]
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
  <div class="matrix-pie-container">
    <div
      class="matrix-pie-chart"
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
          <filter id="pie2-pixel-glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="0.5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>
        <template v-for="(row, rowIndex) in pieFrame" :key="rowIndex">
          <rect
            v-for="(cell, colIndex) in row"
            :key="`${rowIndex}-${colIndex}`"
            :class="['matrix-pixel', cell.opacity > 0.5 && 'matrix-pixel-active']"
            :x="cellPositions[rowIndex]?.[colIndex]?.x"
            :y="cellPositions[rowIndex]?.[colIndex]?.y"
            :width="size"
            :height="size"
            :fill="cell.color || 'rgba(255, 107, 53, 0.1)'"
            :opacity="cell.opacity || 0.1"
            @mouseenter="handleCellHover(cell.dataIndex, $event)"
          />
        </template>
      </svg>
      <Transition name="tooltip">
        <div
          v-if="tooltipVisible"
          class="matrix-tooltip"
          :style="{
            left: `${tooltipX}px`,
            top: `${tooltipY}px`,
            '--matrix-color': tooltipContent.color
          }"
        >
          <div class="tooltip-header">{{ tooltipContent.name }}</div>
          <div class="tooltip-value">{{ tooltipContent.value }}</div>
        </div>
      </Transition>
    </div>
    <div class="matrix-legend">
      <div
        v-for="(item, index) in legendItems"
        :key="index"
        class="legend-item"
        :style="{ '--legend-color': item.color }"
      >
        <div class="legend-dot" :style="{ backgroundColor: item.color }" />
        <span class="legend-name">{{ item.name }}</span>
        <span class="legend-value">{{ item.value }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.matrix-pie-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.matrix-pie-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.matrix-pixel {
  transition: opacity 100ms ease-out;
}

.matrix-pixel-active {
  filter: url(#pie2-pixel-glow);
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

.matrix-legend {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.7rem;
  font-family: 'SF Mono', 'Consolas', monospace;
}

.legend-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
}

.legend-name {
  color: rgba(255, 107, 53, 0.7);
}

.legend-value {
  color: var(--legend-color);
  font-weight: 600;
}
</style>
