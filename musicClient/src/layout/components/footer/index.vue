<script setup lang="ts">
import Left from './components/left.vue'
import Center from './components/center.vue'
import Right from './components/right.vue'
import Recently from './components/recently.vue'
</script>
<template>
  <footer class="spotify-playing-bar">
    <Left />
    <Center />
    <Right />
    <Recently />
  </footer>
</template>

<style scoped>
.spotify-playing-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #000000;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0 16px;
  height: 90px;
  flex-shrink: 0;
  transition: background-color 200ms ease, border-color 200ms ease;
}

/* Light Theme */
:root:not(.dark) .spotify-playing-bar {
  background-color: #ffffff;
  border-top-color: rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .spotify-playing-bar {
    display: none;
  }
}
</style>
