<template>
  <main
    class="relative flex-1 overflow-x-hidden m-2 ml-0 md:mr-0 rounded-2xl bg-gray-100 dark:bg-[#121212] shadow-xl"
  >
    <!-- Purple Gradient Background -->
    <div
      class="absolute top-0 left-0 w-full h-2/3 bg-gradient-to-b from-violet-500/20 to-transparent pointer-events-none"
    ></div>

    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </main>
</template>
