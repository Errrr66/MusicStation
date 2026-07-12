import { ref, onMounted, onUnmounted, type Ref } from "vue";

export interface MatrixAnimationOptions {
  /** 目标帧率，默认 30fps */
  fps?: number;
  /** 动画持续时长(ms)，默认无限循环 */
  duration?: number;
  /** 每一帧的回调 */
  onFrame: (elapsed: number) => void;
  /** 动画结束回调 */
  onComplete?: () => void;
  /** 是否自动开始 */
  autoStart?: boolean;
}

/**
 * Matrix 图表动画控制
 * - 帧率节流（默认 30fps，降低 CPU/GPU 压力）
 * - 页面不可见时自动暂停
 * - 元素离开视口时自动暂停
 * - 到达 duration 后自动停止，避免无限 rAF
 */
export function useMatrixAnimation(
  containerRef: Ref<HTMLElement | null>,
  options: MatrixAnimationOptions
) {
  const isRunning = ref(false);
  const isInView = ref(false);

  let animationId: number | undefined;
  let startTime = 0;
  let lastFrameTime = 0;
  let completed = false;

  const fps = options.fps ?? 30;
  const frameInterval = 1000 / fps;
  const duration = options.duration ?? Infinity;
  const autoStart = options.autoStart ?? true;

  function loop(currentTime: number) {
    if (!isRunning.value || completed) {
      animationId = undefined;
      return;
    }

    if (!startTime) startTime = currentTime;
    const elapsed = currentTime - startTime;

    if (currentTime - lastFrameTime >= frameInterval) {
      lastFrameTime = currentTime;
      options.onFrame(elapsed);

      if (duration !== Infinity && elapsed >= duration) {
        completed = true;
        options.onComplete?.();
        animationId = undefined;
        return;
      }
    }

    animationId = requestAnimationFrame(loop);
  }

  function start() {
    if (animationId || completed) return;
    isRunning.value = true;
    startTime = 0;
    animationId = requestAnimationFrame(loop);
  }

  function stop() {
    isRunning.value = false;
    if (animationId) {
      cancelAnimationFrame(animationId);
      animationId = undefined;
    }
  }

  function restart() {
    completed = false;
    startTime = 0;
    lastFrameTime = 0;
    start();
  }

  function handleVisibilityChange() {
    if (document.hidden) {
      stop();
    } else if (isInView.value && !completed) {
      start();
    }
  }

  let observer: IntersectionObserver | undefined;

  onMounted(() => {
    observer = new IntersectionObserver(
      ([entry]) => {
        isInView.value = entry.isIntersecting;
        if (isInView.value && !document.hidden && !completed) {
          start();
        } else if (!isInView.value) {
          stop();
        }
      },
      { threshold: 0 }
    );

    if (containerRef.value) {
      observer.observe(containerRef.value);
    }

    document.addEventListener("visibilitychange", handleVisibilityChange);
  });

  onUnmounted(() => {
    stop();
    observer?.disconnect();
    document.removeEventListener("visibilitychange", handleVisibilityChange);
  });

  return {
    start,
    stop,
    restart,
    isRunning
  };
}
