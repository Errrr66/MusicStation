import { computed } from "vue";
import { useMatrixTheme } from "@/layout/hooks/useMatrixTheme";

/** 用户提供的莫兰蒂参考色板：低饱和、柔和、与 Matrix 黑白灰主题协调 */
const MORANDI_PALETTE = [
  "#A49193", // 灰玫瑰
  "#EDE7D9", // 奶油
  "#D3DCE3", // 浅灰蓝
  "#B5C9BE", // 鼠尾草绿
  "#F3E3E3", // 浅粉
  "#DECCA6", // 驼色
  "#E0DEDF", // 浅灰
  "#78828B", // 灰蓝
  "#D6E3D2", // 浅绿
  "#C89D94", // 陶土
  "#EFE1D6", // 米色
  "#654E5E"  // 深紫灰
];

export type MatrixPattern =
  | "solid"
  | "ring"
  | "diamond"
  | "hollow-diamond"
  | "dot"
  | "cross";

export const MATRIX_PATTERNS: MatrixPattern[] = [
  "solid",
  "ring",
  "diamond",
  "hollow-diamond",
  "dot",
  "cross"
];

/**
 * 按指定样式绘制一个矩阵单元
 */
export function drawMatrixCell(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  radius: number,
  pattern: MatrixPattern,
  color: string
) {
  ctx.fillStyle = color;
  ctx.strokeStyle = color;

  switch (pattern) {
    case "solid": {
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fill();
      break;
    }
    case "ring": {
      ctx.beginPath();
      ctx.arc(x, y, radius * 0.65, 0, Math.PI * 2);
      ctx.lineWidth = Math.max(1, radius * 0.35);
      ctx.stroke();
      break;
    }
    case "diamond": {
      ctx.beginPath();
      ctx.moveTo(x, y - radius);
      ctx.lineTo(x + radius, y);
      ctx.lineTo(x, y + radius);
      ctx.lineTo(x - radius, y);
      ctx.closePath();
      ctx.fill();
      break;
    }
    case "hollow-diamond": {
      ctx.beginPath();
      ctx.moveTo(x, y - radius * 0.75);
      ctx.lineTo(x + radius * 0.75, y);
      ctx.lineTo(x, y + radius * 0.75);
      ctx.lineTo(x - radius * 0.75, y);
      ctx.closePath();
      ctx.lineWidth = Math.max(1, radius * 0.3);
      ctx.stroke();
      break;
    }
    case "dot": {
      ctx.beginPath();
      ctx.arc(x, y, radius * 0.45, 0, Math.PI * 2);
      ctx.fill();
      break;
    }
    case "cross": {
      const arm = radius * 0.7;
      const w = Math.max(1, radius * 0.28);
      ctx.lineWidth = w;
      ctx.beginPath();
      ctx.moveTo(x, y - arm);
      ctx.lineTo(x, y + arm);
      ctx.moveTo(x - arm, y);
      ctx.lineTo(x + arm, y);
      ctx.stroke();
      break;
    }
  }

  ctx.lineWidth = 1;
}

function hexToHsl(hex: string): [number, number, number] {
  const normalized = hex.replace("#", "");
  const r = parseInt(normalized.substring(0, 2), 16) / 255;
  const g = parseInt(normalized.substring(2, 4), 16) / 255;
  const b = parseInt(normalized.substring(4, 6), 16) / 255;

  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  let h = 0;
  let s = 0;
  const l = (max + min) / 2;

  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case r:
        h = (g - b) / d + (g < b ? 6 : 0);
        break;
      case g:
        h = (b - r) / d + 2;
        break;
      case b:
        h = (r - g) / d + 4;
        break;
    }
    h /= 6;
  }

  return [h * 360, s * 100, l * 100];
}

function hslToHex(h: number, s: number, l: number): string {
  const normalizedH = h / 360;
  const normalizedS = s / 100;
  const normalizedL = l / 100;

  const hue2rgb = (p: number, q: number, t: number) => {
    if (t < 0) t += 1;
    if (t > 1) t -= 1;
    if (t < 1 / 6) return p + (q - p) * 6 * t;
    if (t < 1 / 2) return q;
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
    return p;
  };

  let r: number;
  let g: number;
  let b: number;

  if (normalizedS === 0) {
    r = g = b = normalizedL;
  } else {
    const q =
      normalizedL < 0.5
        ? normalizedL * (1 + normalizedS)
        : normalizedL + normalizedS - normalizedL * normalizedS;
    const p = 2 * normalizedL - q;
    r = hue2rgb(p, q, normalizedH + 1 / 3);
    g = hue2rgb(p, q, normalizedH);
    b = hue2rgb(p, q, normalizedH - 1 / 3);
  }

  const toHex = (c: number) =>
    Math.round(c * 255)
      .toString(16)
      .padStart(2, "0");
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

/**
 * 基于当前 Matrix 主题色生成图表配色
 * @param count 需要的颜色数量
 * @param options 可选：基础色、饱和度偏移、亮度偏移、是否灰阶
 */
export function useMatrixChartColors(
  count: number,
  options?: {
    saturationOffset?: number;
    lightnessMin?: number;
    lightnessMax?: number;
    grayscale?: boolean;
  }
) {
  const { currentTheme } = useMatrixTheme();

  return computed(() => {
    const theme = currentTheme.value;
    const {
      saturationOffset = 0,
      lightnessMin,
      lightnessMax,
      grayscale = false
    } = options || {};

    const palette: string[] = [];
    const steps = Math.max(count, 1);

    if (grayscale) {
      // 在主题高亮与暗淡色之间生成灰阶；若指定范围则优先使用，以获得更强区分度
      const start = hexToHsl(theme.colorLight);
      const end = hexToHsl(theme.textDim);
      const startL = lightnessMin ?? start[2];
      const endL = lightnessMax ?? end[2];
      for (let i = 0; i < count; i++) {
        const ratio = steps === 1 ? 0 : i / (steps - 1);
        const lightness = startL + (endL - startL) * ratio;
        palette.push(hslToHex(0, 0, Math.max(8, Math.min(98, lightness))));
      }
      return palette;
    }

    const base = theme.color;
    const [h, s] = hexToHsl(base);
    const step = 360 / steps;
    const minL = lightnessMin ?? 42;
    const maxL = lightnessMax ?? 62;

    for (let i = 0; i < count; i++) {
      const hue = (h + i * step) % 360;
      const saturation = Math.min(85, Math.max(35, s + saturationOffset));
      const lightness = minL + ((i % 3) / 2) * (maxL - minL);
      palette.push(hslToHex(hue, saturation, lightness));
    }

    return palette;
  });
}

/**
 * 获取莫兰蒂色系：低饱和、柔和，且在不同主题下自动微调亮度以保证可读性
 */
export function useMatrixMorandiColors(count: number) {
  const { currentTheme } = useMatrixTheme();

  return computed(() => {
    const isDark = currentTheme.value.name === "深色矩阵";
    const steps = Math.max(count, 1);
    const palette: string[] = [];

    for (let i = 0; i < count; i++) {
      const base = MORANDI_PALETTE[i % MORANDI_PALETTE.length];
      const [h, s, l] = hexToHsl(base);
      // 将参考色统一映射到主题可读区间，保留色相与低饱和特征
      const adjustedL = isDark
        ? Math.min(72, Math.max(45, l - 5))
        : Math.min(60, Math.max(30, l - 18));
      palette.push(hslToHex(h, s, adjustedL));
    }

    return palette;
  });
}

/**
 * 获取当前主题的主色与辅助色（用于少量数据场景）
 */
export function useMatrixAccentColors() {
  const { currentTheme } = useMatrixTheme();

  return computed(() => ({
    primary: currentTheme.value.color,
    light: currentTheme.value.colorLight,
    dark: currentTheme.value.colorDark,
    dim: currentTheme.value.textDim
  }));
}
