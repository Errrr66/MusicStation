<script setup lang="ts">
import Artplayer from 'artplayer'

const props = defineProps({
  src: {
    type: String,
    required: true,
  },
  poster: {
    type: String,
    default: '',
  },
  theme: {
    type: String,
    default: '',
  },
})

const artRef = ref<HTMLElement>()

const art = ref<Artplayer>()

const resolveTheme = (): string => {
  if (props.theme) return props.theme
  const cssVar = getComputedStyle(document.documentElement)
    .getPropertyValue('--mr-accent')
    .trim()
  return cssVar || 'var(--mr-accent)'
}

onMounted(() => {
  if (!artRef.value) return
  art.value = new Artplayer({
    container: artRef.value as HTMLDivElement,
    url: '',
    autoSize: true,
    poster: props.poster,
    theme: resolveTheme(),
    flip: true,
    setting: true,
    playbackRate: true,
    aspectRatio: true,
    screenshot: true,
    hotkey: true,
  })
})

onUnmounted(() => {
  art.value?.destroy(false)
})

watch(
  () => props.src,
  (val) => {
    if (val) {
      if (!art.value) return
      art.value.url = val
      art.value.poster = props.poster
    }
  }
)
</script>
<template>
  <div ref="artRef" class="artplayer-app aspect-video" />
</template>
