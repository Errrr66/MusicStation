import { ref, watch } from "vue";
import { storageLocal } from "@pureadmin/utils";

export interface MatrixThemeColor {
  name: string;
  color: string;
  colorLight: string;
  colorDark: string;
  bg: string;
  border: string;
  text: string;
  textDim: string;
  bgLight: string;
  bgHover: string;
  shadow: string;
}

const MATRIX_THEME_KEY = "matrix-theme-color";

/**
 * 黑白灰双主题：深色矩阵（默认）/ 浅色矩阵。
 * 整体 UI 色调保持黑白灰色系，仅在激活态使用高对比灰阶。
 */
const defaultThemes: MatrixThemeColor[] = [
  {
    name: "深色矩阵",
    color: "#e8e8e8",
    colorLight: "#ffffff",
    colorDark: "#a3a3a3",
    bg: "#050505",
    border: "rgba(255, 255, 255, 0.12)",
    text: "#f2f2f2",
    textDim: "#8a8a8a",
    bgLight: "#0a0a0a",
    bgHover: "#141414",
    shadow: "rgba(255, 255, 255, 0.15)"
  },
  {
    name: "浅色矩阵",
    color: "#0a0a0a",
    colorLight: "#000000",
    colorDark: "#525252",
    bg: "#f5f5f5",
    border: "rgba(0, 0, 0, 0.12)",
    text: "#0a0a0a",
    textDim: "#737373",
    bgLight: "#ffffff",
    bgHover: "#e8e8e8",
    shadow: "rgba(0, 0, 0, 0.15)"
  }
];

const savedThemeIndex = Math.min(
  storageLocal().getItem<number>(MATRIX_THEME_KEY) ?? 0,
  defaultThemes.length - 1
);

const currentThemeIndex = ref(savedThemeIndex);

const currentTheme = ref<MatrixThemeColor>(defaultThemes[savedThemeIndex]);

function setMatrixTheme(index: number) {
  if (index >= 0 && index < defaultThemes.length) {
    currentThemeIndex.value = index;
    currentTheme.value = defaultThemes[index];
    storageLocal().setItem(MATRIX_THEME_KEY, index);
    applyMatrixTheme(defaultThemes[index]);
  }
}

function applyMatrixTheme(theme: MatrixThemeColor) {
  const root = document.documentElement;
  root.style.setProperty("--matrix-color", theme.color);
  root.style.setProperty("--matrix-color-light", theme.colorLight);
  root.style.setProperty("--matrix-color-dark", theme.colorDark);
  root.style.setProperty("--matrix-bg", theme.bg);
  root.style.setProperty("--matrix-border", theme.border);
  root.style.setProperty("--matrix-text", theme.text);
  root.style.setProperty("--matrix-text-dim", theme.textDim);
  root.style.setProperty("--matrix-color-dim", theme.textDim);
  root.style.setProperty("--matrix-bg-light", theme.bgLight);
  root.style.setProperty("--matrix-bg-hover", theme.bgHover);
  root.style.setProperty("--matrix-shadow", theme.shadow);
  root.style.setProperty("--matrix-input-bg", theme.bgHover);
  root.style.setProperty("--matrix-card-bg", theme.bgLight);

  // 同步 MR 变量系统，确保 Element Plus 组件与图表保持一致
  root.style.setProperty("--mr-accent", theme.color);
  root.style.setProperty("--mr-accent-hover", theme.colorLight);
  root.style.setProperty("--mr-accent-active", theme.colorDark);
  root.style.setProperty("--mr-accent-glow", theme.shadow);
  root.style.setProperty("--mr-bg-base", theme.bg);
  root.style.setProperty("--mr-bg-surface", theme.bgLight);
  root.style.setProperty("--mr-bg-elevated", theme.bgHover);
  root.style.setProperty("--mr-bg-tinted", theme.bgLight);
  root.style.setProperty("--mr-text-base", theme.text);
  root.style.setProperty("--mr-text-subdued", theme.textDim);
  root.style.setProperty("--mr-border-subdued", theme.border);
  root.style.setProperty("--mr-border-bright", theme.color);
  root.style.setProperty("--mr-text-bright", theme.colorLight);
  root.style.setProperty("--mr-essential-positive", theme.color);
  root.style.setProperty("--mr-essential-negative", theme.bg);
  root.style.setProperty("--mr-essential-subdued", theme.textDim);
}

function initMatrixTheme() {
  applyMatrixTheme(currentTheme.value);
}

watch(currentThemeIndex, () => {
  initMatrixTheme();
});

export function useMatrixTheme() {
  return {
    themes: defaultThemes,
    currentTheme,
    currentThemeIndex,
    setMatrixTheme,
    initMatrixTheme
  };
}
