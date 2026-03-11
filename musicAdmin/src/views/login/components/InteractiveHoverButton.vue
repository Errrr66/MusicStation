<script setup lang="ts">
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import ArrowRight from "@iconify-icons/ep/right";

const props = withDefaults(
  defineProps<{
    text?: string;
    icon?: any;
    loading?: boolean;
    className?: string; // Additional classes
  }>(),
  {
    text: "Button",
    loading: false,
    className: ""
  }
);

const emit = defineEmits(["click"]);
</script>

<template>
  <button
    class="group relative w-32 cursor-pointer overflow-hidden rounded-full border bg-bg_color px-6 py-2 text-center font-semibold text-text_color_primary transition-all duration-300 hover:bg-bg_color border-gray-200 dark:border-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
    :class="className"
    :disabled="loading"
    @click="!loading && $emit('click')"
  >
    <span
      :class="[
        'inline-block transition-all duration-300 group-hover:translate-x-12 group-hover:opacity-0',
        loading ? 'opacity-0' : ''
      ]"
    >
      {{ text }}
    </span>
    <div
      class="absolute inset-0 z-10 flex items-center justify-center gap-2 bg-primary text-white opacity-0 transition-all duration-300 group-hover:opacity-100 rounded-full"
      :class="loading ? 'opacity-100' : ''"
    >
      <span>{{ loading ? "Loading..." : text }}</span>
      <component :is="icon" v-if="icon && !loading" />
      <component :is="useRenderIcon(ArrowRight)" v-else-if="!loading" />
    </div>
  </button>
</template>
