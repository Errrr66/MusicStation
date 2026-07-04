<template>
  <main class="mr-main-content">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </main>
</template>

<style scoped>
.mr-main-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background-color: var(--bg-surface, #121212);
  border-radius: 8px;
  position: relative;
  min-width: 0;
  transition: background-color 200ms ease;
}

.mr-main-content::-webkit-scrollbar {
  width: 12px;
}

.mr-main-content::-webkit-scrollbar-track {
  background: transparent;
}

.mr-main-content::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb, rgba(255, 255, 255, 0.3));
  border-radius: 10px;
  border: 3px solid transparent;
  background-clip: content-box;
}

.mr-main-content::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-hover, rgba(255, 255, 255, 0.5));
  border: 3px solid transparent;
  background-clip: content-box;
}

/* Light Theme */
:root:not(.dark) .mr-main-content {
  --bg-surface: #f0f0f0;
  --scrollbar-thumb: rgba(0, 0, 0, 0.3);
  --scrollbar-thumb-hover: rgba(0, 0, 0, 0.5);
}

@media (max-width: 768px) {
  .mr-main-content {
    border-radius: 0;
  }
}
</style>
