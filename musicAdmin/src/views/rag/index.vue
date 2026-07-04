<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { message } from "@/utils/message";
import echarts from "@/plugins/echarts";
import {
  getAdminRagHealth,
  refreshAdminRagArtistAlias,
  debugAdminRagRetrieve,
  evaluateAdminRag,
  type AdminRagHealth,
  type AdminRagDebugRetrieveResult,
  type AdminRagEvalResult
} from "@/api/system";

defineOptions({
  name: "RagDebugPanel"
});

const healthLoading = ref(false);
const debugLoading = ref(false);
const evalLoading = ref(false);

const health = ref<AdminRagHealth | null>(null);
const debugResult = ref<AdminRagDebugRetrieveResult | null>(null);
const evalResult = ref<AdminRagEvalResult | null>(null);
const trendRef = ref<HTMLElement | null>(null);
let trendChart: any = null;

type TrendItem = {
  timestamp: number;
  recallAtK: number;
  mrr: number;
  hitRate: number;
  caseCount: number;
  topK: number;
};

const TREND_STORAGE_KEY = "rag-debug-trend-history-v1";
const trendHistory = ref<TrendItem[]>([]);
const trendWindow = ref(20);

const debugForm = ref({
  query: "播放 Yellow",
  intent: "SEARCH_MUSIC",
  topK: 8,
  enableRag: true
});

const queryPresets = [
  { label: "歌手检索", query: "来一份 Coldplay 歌单", intent: "CREATE_PLAYLIST" },
  { label: "点歌", query: "播放 Yellow", intent: "SEARCH_MUSIC" },
  { label: "风格推荐", query: "推荐安静学习时听的歌", intent: "RECOMMEND" },
  { label: "歌单搜索", query: "搜索轻音乐歌单", intent: "SEARCH_PLAYLIST" },
  { label: "常识门控", query: "地球有多大", intent: "CHAT" }
];

const evalInput = ref(`{
  "topK": 4,
  "cases": [
    {
      "query": "播放 Yellow",
      "relevant": [
        { "sourceType": "song", "sourceId": "123" }
      ]
    }
  ]
}`);

const retrievalUpdatedAt = computed(() => {
  const ts = health.value?.ragLastRetrieval?.updatedAtEpochMs;
  if (!ts) {
    return "-";
  }
  return new Date(ts).toLocaleString();
});

const visibleTrendHistory = computed(() => {
  const limit = Math.max(1, Math.min(trendWindow.value, 100));
  return trendHistory.value.slice(-limit);
});

const refreshHealth = async () => {
  healthLoading.value = true;
  try {
    const res = await getAdminRagHealth();
    if (res.code === 0) {
      health.value = res.data;
      return;
    }
    message(res.message || "获取健康状态失败", { type: "error" });
  } catch (error) {
    console.error(error);
    message("获取健康状态失败", { type: "error" });
  } finally {
    healthLoading.value = false;
  }
};

const refreshArtistAlias = async () => {
  try {
    const res = await refreshAdminRagArtistAlias();
    if (res.code === 0) {
      message(`别名索引已刷新，aliasCount=${res.data.aliasCount ?? 0}`, {
        type: "success"
      });
      return;
    }
    message(res.message || "刷新别名索引失败", { type: "error" });
  } catch (error) {
    console.error(error);
    message("刷新别名索引失败", { type: "error" });
  }
};

const runDebugRetrieve = async () => {
  if (!debugForm.value.query.trim()) {
    message("请输入查询内容", { type: "warning" });
    return;
  }
  debugLoading.value = true;
  try {
    const res = await debugAdminRagRetrieve({
      query: debugForm.value.query.trim(),
      intent: debugForm.value.intent,
      topK: debugForm.value.topK,
      enableRag: debugForm.value.enableRag
    });
    if (res.code === 0) {
      debugResult.value = res.data;
      return;
    }
    message(res.message || "调试检索失败", { type: "error" });
  } catch (error) {
    console.error(error);
    message("调试检索失败", { type: "error" });
  } finally {
    debugLoading.value = false;
  }
};

const runEval = async () => {
  evalLoading.value = true;
  try {
    const parsed = JSON.parse(evalInput.value);
    const res = await evaluateAdminRag(parsed);
    if (res.code === 0) {
      evalResult.value = res.data;
      appendTrendPoint(res.data);
      return;
    }
    message(res.message || "RAG评估失败", { type: "error" });
  } catch (error) {
    console.error(error);
    message("评估输入 JSON 不合法或请求失败", { type: "error" });
  } finally {
    evalLoading.value = false;
  }
};

const loadTrendHistory = () => {
  try {
    const raw = localStorage.getItem(TREND_STORAGE_KEY);
    if (!raw) {
      trendHistory.value = [];
      return;
    }
    const parsed = JSON.parse(raw) as TrendItem[];
    trendHistory.value = Array.isArray(parsed) ? parsed : [];
  } catch {
    trendHistory.value = [];
  }
};

const saveTrendHistory = () => {
  try {
    localStorage.setItem(TREND_STORAGE_KEY, JSON.stringify(trendHistory.value));
  } catch (e) {
    // 存储空间不足或被禁用时降级：仅保留最近 10 条并警告
    console.warn("趋势历史保存失败", e);
    trendHistory.value = trendHistory.value.slice(-10);
  }
};

const appendTrendPoint = (result: AdminRagEvalResult) => {
  const item: TrendItem = {
    timestamp: Date.now(),
    recallAtK: Number(result.recallAtK ?? 0),
    mrr: Number(result.mrr ?? 0),
    hitRate: Number(result.hitRate ?? 0),
    caseCount: Number(result.caseCount ?? 0),
    topK: Number(result.topK ?? 0)
  };
  const merged = [...trendHistory.value, item];
  trendHistory.value = merged.slice(-200);
  saveTrendHistory();
  renderTrendChart();
};

const clearTrendHistory = () => {
  trendHistory.value = [];
  saveTrendHistory();
  renderTrendChart();
  message("趋势历史已清空", { type: "success" });
};

const exportTrendHistory = () => {
  if (!trendHistory.value.length) {
    message("暂无趋势历史可导出", { type: "warning" });
    return;
  }
  const content = JSON.stringify(trendHistory.value, null, 2);
  const blob = new Blob([content], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `rag-trend-history-${Date.now()}.json`;
  link.click();
  URL.revokeObjectURL(url);
};

const renderTrendChart = async () => {
  await nextTick();
  if (!trendRef.value) {
    return;
  }
  if (!trendChart) {
    trendChart = echarts.init(trendRef.value);
  }

  const rows = visibleTrendHistory.value;
  const xData = rows.map(item => new Date(item.timestamp).toLocaleTimeString());
  const recallData = rows.map(item => item.recallAtK);
  const mrrData = rows.map(item => item.mrr);
  const hitData = rows.map(item => item.hitRate);

  trendChart.setOption({
    tooltip: {
      trigger: "axis"
    },
    legend: {
      data: ["Recall@K", "MRR", "HitRate"],
      top: 4
    },
    grid: {
      left: 40,
      right: 18,
      top: 40,
      bottom: 30
    },
    xAxis: {
      type: "category",
      data: xData,
      boundaryGap: false
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 1
    },
    series: [
      { name: "Recall@K", type: "line", smooth: true, data: recallData },
      { name: "MRR", type: "line", smooth: true, data: mrrData },
      { name: "HitRate", type: "line", smooth: true, data: hitData }
    ]
  });
};

const applyPreset = (preset: { query: string; intent: string }) => {
  debugForm.value.query = preset.query;
  debugForm.value.intent = preset.intent;
};

const formatEvalJson = () => {
  try {
    const parsed = JSON.parse(evalInput.value);
    evalInput.value = JSON.stringify(parsed, null, 2);
    message("JSON 已格式化", { type: "success" });
  } catch {
    message("JSON 格式错误，无法格式化", { type: "error" });
  }
};

const copyPromptContext = async () => {
  if (!debugResult.value?.promptContext) {
    message("暂无可复制的 Prompt 上下文", { type: "warning" });
    return;
  }
  try {
    await navigator.clipboard.writeText(debugResult.value.promptContext);
    message("Prompt 上下文已复制", { type: "success" });
  } catch {
    message("复制失败，请手动复制", { type: "error" });
  }
};

const exportEvalResult = () => {
  if (!evalResult.value) {
    message("暂无评估结果可导出", { type: "warning" });
    return;
  }
  const content = JSON.stringify(evalResult.value, null, 2);
  const blob = new Blob([content], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `rag-eval-${Date.now()}.json`;
  link.click();
  URL.revokeObjectURL(url);
};

onMounted(() => {
  loadTrendHistory();
  refreshHealth();
  renderTrendChart();
});

onUnmounted(() => {
  if (trendChart) {
    trendChart.dispose();
    trendChart = null;
  }
});

watch(visibleTrendHistory, () => {
  renderTrendChart();
});
</script>

<template>
  <div class="matrix-page rag-debug-page">
    <el-card shadow="never" class="rag-card">
      <template #header>
        <div class="rag-card-header">
          <span>RAG 健康状态</span>
          <div class="rag-header-actions">
            <el-button :loading="healthLoading" @click="refreshHealth">刷新状态</el-button>
            <el-button type="primary" plain @click="refreshArtistAlias">刷新歌手别名索引</el-button>
          </div>
        </div>
      </template>
      <div class="rag-health-grid">
        <el-tag :type="health?.ragEnabled ? 'success' : 'danger'">RAG {{ health?.ragEnabled ? "ON" : "OFF" }}</el-tag>
        <el-tag>模式 {{ health?.ragMode || "-" }}</el-tag>
        <el-tag :type="health?.providers?.deepseekConfigured ? 'success' : 'warning'">DeepSeek {{ health?.providers?.deepseekConfigured ? "OK" : "MISSING" }}</el-tag>
        <el-tag :type="health?.providers?.ttsConfigured ? 'success' : 'warning'">TTS {{ health?.providers?.ttsConfigured ? "OK" : "MISSING" }}</el-tag>
        <el-tag :type="health?.providers?.semanticConfigured ? 'success' : 'warning'">Semantic {{ health?.providers?.semanticConfigured ? "OK" : "MISSING" }}</el-tag>
      </div>
      <div v-if="health?.ragLastRetrieval" class="rag-health-detail">
        <span>策略: {{ health.ragLastRetrieval.strategy }}</span>
        <span>查询数: {{ health.ragLastRetrieval.queryCount }}</span>
        <span>候选数: {{ health.ragLastRetrieval.candidateCount }}</span>
        <span>引用数: {{ health.ragLastRetrieval.citationCount }}</span>
        <span>更新时间: {{ retrievalUpdatedAt }}</span>
      </div>
    </el-card>

    <el-card shadow="never" class="rag-card">
      <template #header>
        <div class="rag-card-header">
          <span>检索调试</span>
          <el-button link type="primary" @click="copyPromptContext">复制 Prompt</el-button>
        </div>
      </template>
      <div class="rag-presets">
        <el-tag
          v-for="item in queryPresets"
          :key="item.label"
          class="rag-preset-tag"
          effect="plain"
          @click="applyPreset(item)"
        >
          {{ item.label }}
        </el-tag>
      </div>
      <el-form inline class="matrix-search-form">
        <el-form-item label="Query">
          <el-input v-model="debugForm.query" clearable placeholder="输入调试查询" class="!w-[320px]" />
        </el-form-item>
        <el-form-item label="Intent">
          <el-select v-model="debugForm.intent" class="!w-[180px]">
            <el-option label="CHAT" value="CHAT" />
            <el-option label="SEARCH_MUSIC" value="SEARCH_MUSIC" />
            <el-option label="RECOMMEND" value="RECOMMEND" />
            <el-option label="CREATE_PLAYLIST" value="CREATE_PLAYLIST" />
            <el-option label="SEARCH_PLAYLIST" value="SEARCH_PLAYLIST" />
          </el-select>
        </el-form-item>
        <el-form-item label="TopK">
          <el-input-number v-model="debugForm.topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item>
          <el-switch v-model="debugForm.enableRag" active-text="启用RAG" inactive-text="关闭RAG" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="debugLoading" @click="runDebugRetrieve">执行调试</el-button>
        </el-form-item>
      </el-form>

      <div v-if="debugResult" class="rag-debug-result">
        <div class="rag-health-detail">
          <span>命中策略: {{ debugResult.retrievalHealth.strategy }}</span>
          <span>候选数: {{ debugResult.retrievalHealth.candidateCount }}</span>
          <span>引用数: {{ debugResult.retrievalHealth.citationCount }}</span>
        </div>
        <el-table :data="debugResult.citations" border stripe>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="sourceType" label="类型" width="120" />
          <el-table-column prop="sourceId" label="ID" width="120" />
          <el-table-column prop="title" label="标题" min-width="240" />
          <el-table-column prop="snippet" label="摘要" min-width="300" />
          <el-table-column prop="reason" label="原因" min-width="220" />
        </el-table>
        <el-input
          :model-value="debugResult.promptContext"
          type="textarea"
          :rows="8"
          readonly
          class="mt-3"
        />
      </div>
    </el-card>

    <el-card shadow="never" class="rag-card">
      <template #header>
        <div class="rag-card-header">
          <span>趋势面板（近 N 次评估）</span>
          <div class="rag-header-actions">
            <span class="rag-inline-label">N</span>
            <el-input-number v-model="trendWindow" :min="5" :max="100" :step="5" size="small" />
            <el-button link type="primary" @click="exportTrendHistory">导出趋势</el-button>
            <el-button link type="danger" @click="clearTrendHistory">清空趋势</el-button>
          </div>
        </div>
      </template>
      <div ref="trendRef" class="rag-trend-chart" />
    </el-card>

    <el-card shadow="never" class="rag-card">
      <template #header>
        <div class="rag-card-header">
          <span>离线评估（Recall@K / MRR / HitRate）</span>
          <div class="rag-header-actions">
            <el-button link type="primary" @click="formatEvalJson">格式化 JSON</el-button>
            <el-button link type="primary" @click="exportEvalResult">导出结果</el-button>
          </div>
        </div>
      </template>
      <el-input v-model="evalInput" type="textarea" :rows="10" />
      <div class="mt-3">
        <el-button type="primary" :loading="evalLoading" @click="runEval">执行评估</el-button>
      </div>

      <div v-if="evalResult" class="rag-eval-metrics">
        <el-tag type="success">Recall@K {{ evalResult.recallAtK }}</el-tag>
        <el-tag type="success">MRR {{ evalResult.mrr }}</el-tag>
        <el-tag type="success">HitRate {{ evalResult.hitRate }}</el-tag>
        <el-tag type="warning">SoftRecall@K {{ evalResult.softRecallAtK }}</el-tag>
        <el-tag type="warning">SoftMRR {{ evalResult.softMrr }}</el-tag>
        <el-tag type="warning">SoftHitRate {{ evalResult.softHitRate }}</el-tag>
        <el-tag>Case {{ evalResult.caseCount }}</el-tag>
      </div>

      <el-table v-if="evalResult" :data="evalResult.details" border stripe>
        <el-table-column prop="query" label="Query" min-width="260" />
        <el-table-column prop="intent" label="Intent" width="150" />
        <el-table-column prop="hit" label="Hit" width="80" />
        <el-table-column prop="softHit" label="SoftHit" width="90" />
        <el-table-column prop="recall" label="Recall" width="120" />
        <el-table-column prop="softRecall" label="SoftRecall" width="120" />
        <el-table-column prop="reciprocalRank" label="RR" width="120" />
        <el-table-column prop="softReciprocalRank" label="SoftRR" width="120" />
        <el-table-column label="检索结果数" width="120">
          <template #default="{ row }">
            {{ row.retrieved?.length ?? 0 }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.rag-debug-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rag-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rag-header-actions {
  display: flex;
  gap: 8px;
}

.rag-health-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rag-health-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.rag-inline-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-right: 2px;
}

.rag-trend-chart {
  width: 100%;
  height: 280px;
}

.rag-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.rag-preset-tag {
  cursor: pointer;
}

.rag-debug-result,
.rag-eval-metrics {
  margin-top: 12px;
}

.rag-eval-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

