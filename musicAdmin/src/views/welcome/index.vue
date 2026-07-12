<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { ReNormalCountTo } from "@/components/ReCountTo";
import { ChartBar, ChartLine, ChartPie1, ChartPie2 } from "./components/charts";
import useChartData from "./hooks/useChartData";
import { useMatrixChartColors } from "./components/charts/useMatrixColors";

defineOptions({
  name: "Welcome"
});

const statColors = useMatrixChartColors(4, {
  grayscale: true,
  lightnessMin: 32,
  lightnessMax: 82
});

const {
  userCount,
  artistCount,
  songCount,
  playlistCount,
  westernPopCount,
  chinesePopCount,
  cantonesePopCount,
  koreanPopCount,
  classicCount,
  hiphopCount,
  rockCount,
  electronicCount,
  jazzCount,
  lightCount,
  countAmerica,
  countChina,
  countKorea,
  countJapan,
  countGermany,
  countBritain,
  maleCount,
  femaleCount
} = useChartData();

const pieChartData1 = computed(() => [
  { value: westernPopCount.value, name: "欧美流行" },
  { value: chinesePopCount.value, name: "华语流行" },
  { value: cantonesePopCount.value, name: "粤语流行" },
  { value: koreanPopCount.value, name: "韩国流行" },
  { value: classicCount.value, name: "古典" },
  { value: hiphopCount.value, name: "嘻哈说唱" },
  { value: rockCount.value, name: "摇滚" },
  { value: electronicCount.value, name: "电子" },
  { value: jazzCount.value, name: "节奏布鲁斯" },
  { value: lightCount.value, name: "轻音乐" }
]);

const pieChartData2 = computed(() => [
  { value: maleCount.value, name: "男" },
  { value: femaleCount.value, name: "女" }
]);

const statsCards = computed(() => {
  const palette = statColors.value;
  return [
    {
      label: "USERS",
      value: userCount.value,
      color: palette[0],
      data: [210, 439, 530, 496, 575, 680, 845]
    },
    {
      label: "ARTISTS",
      value: artistCount.value,
      color: palette[1],
      data: [210, 399, 500, 492, 652, 688, 850]
    },
    {
      label: "SONGS",
      value: songCount.value,
      color: palette[2],
      data: [2101, 3399, 5300, 4962, 6952, 6808, 8450]
    },
    {
      label: "PLAYLISTS",
      value: playlistCount.value,
      color: palette[3],
      data: [2101, 5288, 4239, 4962, 6752, 5208, 7450]
    }
  ];
});

const timeString = ref("");
let timeInterval: number | undefined;

function updateTime() {
  const now = new Date();
  timeString.value = now.toLocaleTimeString("zh-CN", {
    hour12: false,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  });
}

onMounted(() => {
  updateTime();
  timeInterval = window.setInterval(updateTime, 1000);
});

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval);
  }
});
</script>

<template>
  <div class="matrix-dashboard">
    <div class="matrix-header">
      <div class="header-left">
        <div class="matrix-title">
          <span class="title-bracket">[</span>
          <span class="title-text">MUSIC SYSTEM</span>
          <span class="title-bracket">]</span>
        </div>
        <div class="matrix-subtitle">DATA MONITORING CENTER</div>
      </div>
      <div class="header-right">
        <div class="matrix-time">
          <span class="time-label">SYS.TIME</span>
          <span class="time-value">{{ timeString }}</span>
        </div>
      </div>
    </div>

    <div class="matrix-grid">
      <div class="stats-row">
        <div
          v-for="(stat, index) in statsCards"
          :key="index"
          class="matrix-stat-card"
          :style="{ '--accent-color': stat.color }"
        >
          <div class="card-header">
            <div class="card-label">{{ stat.label }}</div>
            <div class="card-indicator">
              <span class="indicator-dot"></span>
              <span class="indicator-dot"></span>
              <span class="indicator-dot"></span>
            </div>
          </div>
          <div class="card-body">
            <div class="stat-value">
              <ReNormalCountTo
                :duration="2200"
                :fontSize="'2em'"
                :startVal="100"
                :endVal="stat.value"
              />
            </div>
            <div class="stat-chart">
              <ChartLine :color="stat.color" :data="stat.data" />
            </div>
          </div>
          <div class="card-footer">
            <span class="footer-line"></span>
          </div>
        </div>
      </div>

      <div class="charts-row">
        <div class="matrix-chart-card wide">
          <div class="card-header">
            <div class="card-label">歌曲类型比例</div>
            <div class="card-corner top-left"></div>
            <div class="card-corner top-right"></div>
          </div>
          <div class="card-body">
            <ChartPie1 :chartData="pieChartData1" />
          </div>
          <div class="card-footer">
            <div class="card-corner bottom-left"></div>
            <div class="card-corner bottom-right"></div>
          </div>
        </div>

        <div class="matrix-chart-card">
          <div class="card-header">
            <div class="card-label">歌手国籍分布</div>
            <div class="card-corner top-left"></div>
            <div class="card-corner top-right"></div>
          </div>
          <div class="card-body">
            <ChartBar
              :barChartData="[
                countAmerica,
                countChina,
                countKorea,
                countJapan,
                countGermany,
                countBritain
              ]"
            />
          </div>
          <div class="card-footer">
            <div class="card-corner bottom-left"></div>
            <div class="card-corner bottom-right"></div>
          </div>
        </div>

        <div class="matrix-chart-card">
          <div class="card-header">
            <div class="card-label">歌手性别比例</div>
            <div class="card-corner top-left"></div>
            <div class="card-corner top-right"></div>
          </div>
          <div class="card-body">
            <ChartPie2 :chartData="pieChartData2" />
          </div>
          <div class="card-footer">
            <div class="card-corner bottom-left"></div>
            <div class="card-corner bottom-right"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="matrix-footer">
      <div class="footer-text">SYSTEM STATUS: ONLINE</div>
      <div class="footer-dots">
        <span class="dot active"></span>
        <span class="dot active"></span>
        <span class="dot active"></span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.matrix-dashboard {
  --pixel-size: 4px;
  --gap: 8px;

  min-height: 100%;
  padding: 16px;
  background: var(--matrix-bg);
  font-family: var(--mr-font-family);
  color: var(--matrix-text);
}

.matrix-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 16px;
  border: 1px solid var(--matrix-border);
  background: var(--matrix-bg);
  position: relative;

  &::before,
  &::after {
    content: '';
    position: absolute;
    width: var(--pixel-size);
    height: var(--pixel-size);
    background: var(--matrix-color);
  }

  &::before {
    top: -1px;
    left: -1px;
  }

  &::after {
    bottom: -1px;
    right: -1px;
  }
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.matrix-title {
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: 4px;
  display: flex;
  align-items: center;
  gap: 8px;

  .title-bracket {
    color: var(--matrix-color-dim);
    animation: blink 1s infinite;
  }

  .title-text {
    text-shadow: 0 0 10px var(--matrix-color);
  }
}

.matrix-subtitle {
  font-size: 0.625rem;
  letter-spacing: 3px;
  color: var(--matrix-text-dim);
  text-transform: uppercase;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.matrix-time {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;

  .time-label {
    font-size: 0.5rem;
    letter-spacing: 2px;
    color: var(--matrix-text-dim);
  }

  .time-value {
    font-size: 1.125rem;
    font-weight: 600;
    letter-spacing: 2px;
    text-shadow: 0 0 8px var(--matrix-color);
  }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.matrix-grid {
  display: flex;
  flex-direction: column;
  gap: var(--gap);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--gap);

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.matrix-stat-card {
  border: 1px solid var(--matrix-border);
  background: var(--matrix-bg);
  padding: 12px;
  position: relative;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--accent-color, var(--matrix-color));
    box-shadow: 0 0 20px var(--matrix-color);
  }

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 8px;
    height: 8px;
    border-left: 2px solid var(--accent-color, var(--matrix-color));
    border-top: 2px solid var(--accent-color, var(--matrix-color));
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    right: 0;
    width: 8px;
    height: 8px;
    border-right: 2px solid var(--accent-color, var(--matrix-color));
    border-bottom: 2px solid var(--accent-color, var(--matrix-color));
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-label {
  font-size: 0.625rem;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--matrix-text-dim);
}

.card-indicator {
  display: flex;
  gap: 3px;

  .indicator-dot {
    width: 4px;
    height: 4px;
    background: var(--accent-color, var(--matrix-color));
    opacity: 0.5;

    &:first-child {
      opacity: 1;
      animation: pulse 1.5s infinite;
    }

    &:nth-child(2) {
      animation: pulse 1.5s infinite 0.3s;
    }

    &:last-child {
      animation: pulse 1.5s infinite 0.6s;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.stat-value {
  flex-shrink: 0;
}

.stat-chart {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.card-footer {
  margin-top: 8px;

  .footer-line {
    display: block;
    height: 2px;
    background: repeating-linear-gradient(
      90deg,
      var(--matrix-color) 0,
      var(--matrix-color) 4px,
      transparent 4px,
      transparent 8px
    );
    opacity: 0.3;
  }
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--gap);

  @media (max-width: 1200px) {
    grid-template-columns: 1fr 1fr;
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.matrix-chart-card {
  border: 1px solid var(--matrix-border);
  background: var(--matrix-bg);
  padding: 12px;
  position: relative;

  &.wide {
    grid-column: span 1;

    @media (min-width: 1400px) {
      grid-column: span 1;
    }
  }

  .card-header {
    position: relative;
    padding-bottom: 8px;
  }

  .card-body {
    min-height: 280px;
    justify-content: center;
  }

  .card-corner {
    position: absolute;
    width: 6px;
    height: 6px;
    border: 1px solid var(--matrix-color);

    &.top-left {
      top: 0;
      left: 0;
      border-right: none;
      border-bottom: none;
    }

    &.top-right {
      top: 0;
      right: 0;
      border-left: none;
      border-bottom: none;
    }

    &.bottom-left {
      bottom: 0;
      left: 0;
      border-right: none;
      border-top: none;
    }

    &.bottom-right {
      bottom: 0;
      right: 0;
      border-left: none;
      border-top: none;
    }
  }

  .card-footer {
    position: relative;
    padding-top: 8px;
    margin-top: 8px;
  }
}

.matrix-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  margin-top: 16px;
  border: 1px solid var(--matrix-border);
  background: var(--matrix-bg);
}

.footer-text {
  font-size: 0.625rem;
  letter-spacing: 2px;
  color: var(--matrix-color);
}

.footer-dots {
  display: flex;
  gap: 6px;

  .dot {
    width: 6px;
    height: 6px;
    background: var(--matrix-text-dim);

    &.active {
      background: var(--matrix-color);
      box-shadow: 0 0 6px var(--matrix-color);
      animation: dotPulse 2s infinite;
    }
  }
}

@keyframes dotPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

:deep(.el-card) {
  --el-card-border-color: none;
  background: transparent;
}
</style>
