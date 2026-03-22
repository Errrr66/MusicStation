import { ref, watch } from "vue";
import { storageLocal } from "@pureadmin/utils";

export interface MatrixThemeColor {
  name: string;
  color: string;
  bg: string;
  border: string;
  text: string;
  textDim: string;
  bgLight: string;
  bgHover: string;
  shadow: string;
}

const MATRIX_THEME_KEY = "matrix-theme-color";

const defaultThemes: MatrixThemeColor[] = [
  {
    name: "橙红",
    color: "#ff6b35",
    bg: "rgba(15, 10, 8, 0.95)",
    border: "rgba(255, 107, 53, 0.4)",
    text: "rgba(255, 107, 53, 0.9)",
    textDim: "rgba(255, 107, 53, 0.5)",
    bgLight: "rgba(255, 107, 53, 0.05)",
    bgHover: "rgba(255, 107, 53, 0.1)",
    shadow: "rgba(255, 107, 53, 0.2)"
  },
  {
    name: "翠绿",
    color: "#26ce83",
    bg: "rgba(10, 15, 12, 0.95)",
    border: "rgba(38, 206, 131, 0.4)",
    text: "rgba(38, 206, 131, 0.9)",
    textDim: "rgba(38, 206, 131, 0.5)",
    bgLight: "rgba(38, 206, 131, 0.05)",
    bgHover: "rgba(38, 206, 131, 0.1)",
    shadow: "rgba(38, 206, 131, 0.2)"
  },
  {
    name: "天蓝",
    color: "#41b6ff",
    bg: "rgba(8, 12, 18, 0.95)",
    border: "rgba(65, 182, 255, 0.4)",
    text: "rgba(65, 182, 255, 0.9)",
    textDim: "rgba(65, 182, 255, 0.5)",
    bgLight: "rgba(65, 182, 255, 0.05)",
    bgHover: "rgba(65, 182, 255, 0.1)",
    shadow: "rgba(65, 182, 255, 0.2)"
  },
  {
    name: "紫罗兰",
    color: "#7846e5",
    bg: "rgba(12, 8, 18, 0.95)",
    border: "rgba(120, 70, 229, 0.4)",
    text: "rgba(120, 70, 229, 0.9)",
    textDim: "rgba(120, 70, 229, 0.5)",
    bgLight: "rgba(120, 70, 229, 0.05)",
    bgHover: "rgba(120, 70, 229, 0.1)",
    shadow: "rgba(120, 70, 229, 0.2)"
  },
  {
    name: "玫红",
    color: "#eb2f96",
    bg: "rgba(18, 8, 14, 0.95)",
    border: "rgba(235, 47, 150, 0.4)",
    text: "rgba(235, 47, 150, 0.9)",
    textDim: "rgba(235, 47, 150, 0.5)",
    bgLight: "rgba(235, 47, 150, 0.05)",
    bgHover: "rgba(235, 47, 150, 0.1)",
    shadow: "rgba(235, 47, 150, 0.2)"
  },
  {
    name: "金黄",
    color: "#f5a623",
    bg: "rgba(18, 14, 8, 0.95)",
    border: "rgba(245, 166, 35, 0.4)",
    text: "rgba(245, 166, 35, 0.9)",
    textDim: "rgba(245, 166, 35, 0.5)",
    bgLight: "rgba(245, 166, 35, 0.05)",
    bgHover: "rgba(245, 166, 35, 0.1)",
    shadow: "rgba(245, 166, 35, 0.2)"
  },
  {
    name: "青色",
    color: "#13c2c2",
    bg: "rgba(8, 15, 15, 0.95)",
    border: "rgba(19, 194, 194, 0.4)",
    text: "rgba(19, 194, 194, 0.9)",
    textDim: "rgba(19, 194, 194, 0.5)",
    bgLight: "rgba(19, 194, 194, 0.05)",
    bgHover: "rgba(19, 194, 194, 0.1)",
    shadow: "rgba(19, 194, 194, 0.2)"
  },
  {
    name: "珊瑚红",
    color: "#ff6b6b",
    bg: "rgba(18, 10, 10, 0.95)",
    border: "rgba(255, 107, 107, 0.4)",
    text: "rgba(255, 107, 107, 0.9)",
    textDim: "rgba(255, 107, 107, 0.5)",
    bgLight: "rgba(255, 107, 107, 0.05)",
    bgHover: "rgba(255, 107, 107, 0.1)",
    shadow: "rgba(255, 107, 107, 0.2)"
  }
];

const savedThemeIndex = storageLocal().getItem<number>(MATRIX_THEME_KEY) ?? 0;

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
  root.style.setProperty("--matrix-bg", theme.bg);
  root.style.setProperty("--matrix-border", theme.border);
  root.style.setProperty("--matrix-text", theme.text);
  root.style.setProperty("--matrix-text-dim", theme.textDim);
  root.style.setProperty("--matrix-color-dim", theme.textDim);
  root.style.setProperty("--matrix-bg-light", theme.bgLight);
  root.style.setProperty("--matrix-bg-hover", theme.bgHover);
  root.style.setProperty("--matrix-shadow", theme.shadow);
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
