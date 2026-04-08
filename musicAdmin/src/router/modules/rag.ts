export default {
  path: "/rag",
  redirect: "/rag/index",
  meta: {
    icon: "ri:robot-2-fill",
    title: "RAG调试后台",
    rank: 8
  },
  children: [
    {
      path: "/rag/index",
      name: "RagDebugPanel",
      component: () => import("@/views/rag/index.vue"),
      meta: {
        title: "RAG调试后台"
      }
    }
  ]
} satisfies RouteConfigsTable;

