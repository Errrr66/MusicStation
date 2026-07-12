// 抽离可公用的工具函数等用于系统管理页面逻辑
import { computed } from "vue";

export function usePublicHooks() {
  const switchStyle = computed(() => {
    return {
      "--el-switch-on-color": "var(--mr-status-success)",
      "--el-switch-off-color": "var(--mr-status-danger)"
    };
  });

  const tagStyle = computed(() => {
    return (status: number) => {
      return status === 1
        ? {
            "--el-tag-text-color": "var(--mr-status-success)",
            "--el-tag-bg-color": "var(--mr-status-success-bg)",
            "--el-tag-border-color": "var(--mr-status-success-border)"
          }
        : {
            "--el-tag-text-color": "var(--mr-status-danger)",
            "--el-tag-bg-color": "var(--mr-status-danger-bg)",
            "--el-tag-border-color": "var(--mr-status-danger-border)"
          };
    };
  });

  return {
    /** 表现更鲜明的`el-switch`组件  */
    switchStyle,
    /** 表现更鲜明的`el-tag`组件  */
    tagStyle
  };
}
