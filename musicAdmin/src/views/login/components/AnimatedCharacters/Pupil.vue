<script setup lang="ts">
import { ref, computed, toRefs } from "vue";
import { useMouse } from "@vueuse/core";

const props = withDefaults(
  defineProps<{
    size?: number;
    maxDistance?: number;
    pupilColor?: string;
    forceLookX?: number;
    forceLookY?: number;
  }>(),
  {
    size: 12,
    maxDistance: 5,
    pupilColor: "black"
  }
);

const { x: mouseX, y: mouseY } = useMouse();
const pupilRef = ref<HTMLDivElement | null>(null);

const pupilPosition = computed(() => {
  if (!pupilRef.value) return { x: 0, y: 0 };

  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY };
  }

  const rect = pupilRef.value.getBoundingClientRect();
  const pupilCenterX = rect.left + rect.width / 2;
  const pupilCenterY = rect.top + rect.height / 2;

  const deltaX = mouseX.value - pupilCenterX;
  const deltaY = mouseY.value - pupilCenterY;
  const distance = Math.min(
    Math.sqrt(deltaX ** 2 + deltaY ** 2),
    props.maxDistance
  );

  const angle = Math.atan2(deltaY, deltaX);
  const x = Math.cos(angle) * distance;
  const y = Math.sin(angle) * distance;

  return { x, y };
});
</script>

<template>
  <div
    ref="pupilRef"
    class="rounded-full"
    :style="{
      width: `${size}px`,
      height: `${size}px`,
      backgroundColor: pupilColor,
      transform: `translate(${pupilPosition.x}px, ${pupilPosition.y}px)`,
      transition: 'transform 0.1s ease-out'
    }"
  />
</template>
