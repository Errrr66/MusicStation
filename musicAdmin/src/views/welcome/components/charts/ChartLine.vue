<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from "vue";

const props = defineProps({
  data: {
    type: Array as () => number[],
    default: () => []
  },
  color: {
    type: String,
    default: "#ff6b35"
  }
});

const rows = 7;
const size = 5;
const gap = 2;

const cols = computed(() => props.data.length || 7);

const tooltipVisible = ref(false);
const tooltipContent = ref("");
const tooltipX = ref(0);
const tooltipY = ref(0);

let animationId: number | undefined;
let startTime = 0;
const phaseOffset = ref(0);

const animatedLevels = ref<number[]>(props.data.map(() => 0));
const waveLevels = ref<number[]>(props.data.map(() => 0));

function animate(currentTime: number) {
  if (!startTime) startTime = currentTime;
  const elapsed = currentTime - startTime;

  const duration = 1500;
  const progress = Math.min(elapsed / duration, 1);
  const eased = 1 - Math.pow(1 - progress, 3);

  animatedLevels.value = props.data.map((val, i) => {
    const delay = i * 50;
    const adjustedProgress = Math.max(0, Math.min(1, (elapsed - delay) / (duration - delay)));
    const adjustedEased = 1 - Math.pow(1 - adjustedProgress, 3);
    return val * adjustedEased;
  });

  phaseOffset.value = (elapsed / 1000) * Math.PI * 2;

  waveLevels.value = props.data.map((val, i) => {
    const wave = Math.sin(phaseOffset.value + i * 0.5) * 0.12;
    return Math.max(0, Math.min(1, (val / Math.max(...props.data, 1)) + wave));
  });

  animationId = requestAnimationFrame(animate);
}

onMounted(() => {
  animationId = requestAnimationFrame(animate);
});

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId);
  }
});

const animatedFrame = computed(() => {
  const result: number[][] = [];
  const maxVal = Math.max(...animatedLevels.value, 1);

  for (let r = 0; r < rows; r++) {
    result[r] = [];
    for (let c = 0; c < cols.value; c++) {
      const normalizedValue = waveLevels.value[c] || 0;
      const height = normalizedValue * rows;
      const rowFromBottom = rows - 1 - r;
      const distance = Math.abs(rowFromBottom - height);

      if (distance < 0.3) {
        result[r][c] = 1;
      } else if (distance < 0.8) {
        result[r][c] = 0.6;
      } else if (distance < 1.5) {
        result[r][c] = 0.2;
      } else {
        result[r][c] = 0;
      }
    }
  }
  return result;
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

function handleMouseEnter(colIndex: number, event: MouseEvent) {
  tooltipVisible.value = true;
  tooltipContent.value = `${props.data[colIndex]}`;
  updateTooltipPosition(event);
}

function handleMouseMove(event: MouseEvent) {
  updateTooltipPosition(event);
}

function handleMouseLeave() {
  tooltipVisible.value = false;
}

function updateTooltipPosition(event: MouseEvent) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  tooltipX.value = event.clientX - rect.left + 10;
  tooltipY.value = event.clientY - rect.top - 30;
}
</script>

<template>
  <div class="matrix-line-chart" :style="{ '--matrix-color': color }">
    <div
      class="chart-container"
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
          <filter id="pixel-glow" x="-100%" y="-100%" width="300%" height="300%">
            <feGaussianBlur stdDeviation="1" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>
        <template v-for="(row, rowIndex) in animatedFrame" :key="rowIndex">
          <rect
            v-for="(value, colIndex) in row"
            :key="`${rowIndex}-${colIndex}`"
            :class="['matrix-pixel', value > 0.4 && 'matrix-pixel-active']"
            :x="cellPositions[rowIndex]?.[colIndex]?.x"
            :y="cellPositions[rowIndex]?.[colIndex]?.y"
            :width="size"
            :height="size"
            :fill="color"
            :opacity="value > 0.1 ? value : 0.1"
            @mouseenter="handleMouseEnter(colIndex, $event)"
          />
        </template>
      </svg>
      <Transition name="tooltip">
        <div
          v-if="tooltipVisible"
          class="matrix-tooltip"
          :style="{ left: `${tooltipX}px`, top: `${tooltipY}px` }"
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
}

.chart-container {
  position: relative;
}

.matrix-pixel {
  transition: opacity 100ms ease-out;
}

.matrix-pixel-active {
  filter: url(#pixel-glow);
}

.matrix-tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.9);
  border: 1px solid var(--matrix-color);
  padding: 4px 8px;
  pointer-events: none;
  z-index: 100;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 11px;
}

.tooltip-value {
  color: var(--matrix-color);
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
