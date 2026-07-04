<script setup lang="ts">
import {
  ref,
  unref,
  reactive
} from "vue";
import LayPanel from "../lay-panel/index.vue";
import { useAppStoreHook } from "@/store/modules/app";
import { useDataThemeChange } from "@/layout/hooks/useDataThemeChange";
import { useGlobal } from "@pureadmin/utils";
import { useMatrixTheme } from "@/layout/hooks/useMatrixTheme";
import Check from "@iconify-icons/ep/check";

const { $storage } = useGlobal<GlobalPropertiesApi>();

const {
  toggleClass,
  dataTheme,
  overallStyle,
  dataThemeChange
} = useDataThemeChange();

const { themes: matrixThemes, currentThemeIndex, setMatrixTheme } = useMatrixTheme();
const configure = $storage.configure as Record<string, any>;

const settings = reactive({
  greyVal: configure.grey,
  colorInvert: configure.colorInvert ?? false
});

/** 灰色模式设置 */
const greyChange = (value): void => {
  const htmlEl = document.querySelector("html");
  toggleClass(settings.greyVal, "html-grey", htmlEl);
  const storageConfigure = $storage.configure;
  storageConfigure.grey = value;
  $storage.configure = storageConfigure;
};

/** 设置导航模式为 horizontal */
function setLayoutModel() {
  const layout = "horizontal";
  window.document.body.setAttribute("layout", layout);
  $storage.layout = {
    layout,
    theme: $storage.layout?.theme,
    darkMode: $storage.layout?.darkMode,
    sidebarStatus: $storage.layout?.sidebarStatus,
    epThemeColor: $storage.layout?.epThemeColor,
    themeColor: $storage.layout?.themeColor,
    overallStyle: $storage.layout?.overallStyle
  };
  useAppStoreHook().setLayout(layout);
}

/** 整体颜色反转（图片除外） */
const toggleColorInvert = (value: boolean): void => {
  const htmlEl = document.documentElement;
  if (value) {
    htmlEl.classList.add("color-invert");
  } else {
    htmlEl.classList.remove("color-invert");
  }
  const storageConfigure = $storage.configure as Record<string, any>;
  storageConfigure.colorInvert = value;
  $storage.configure = storageConfigure;
};

// 初始化灰色模式
if (settings.greyVal) {
  document.querySelector("html")?.classList.add("html-grey");
}

// 初始化颜色反转
if (settings.colorInvert) {
  document.querySelector("html")?.classList.add("color-invert");
}

// 初始化布局为 horizontal
setLayoutModel();
</script>

<template>
  <LayPanel>
    <div class="p-5">
      <p class="mb-[12px] font-medium text-sm dark:text-white">Matrix 主题色</p>
      <ul class="matrix-theme-color">
        <li
          v-for="(theme, index) in matrixThemes"
          :key="index"
          :class="{ 'is-active': currentThemeIndex === index }"
          :style="{ background: theme.color }"
          @click="setMatrixTheme(index)"
        >
          <el-icon
            v-if="currentThemeIndex === index"
            :size="14"
            color="#fff"
          >
            <IconifyIconOffline :icon="Check" />
          </el-icon>
        </li>
      </ul>

      <p class="mt-5 font-medium text-sm dark:text-white">界面显示</p>
      <ul class="setting">
        <li>
          <span class="dark:text-white">颜色反转</span>
          <el-switch
            v-model="settings.colorInvert"
            inline-prompt
            active-text="开"
            inactive-text="关"
            @change="toggleColorInvert"
          />
        </li>
        <li>
          <span class="dark:text-white">灰色模式</span>
          <el-switch
            v-model="settings.greyVal"
            inline-prompt
            active-text="开"
            inactive-text="关"
            @change="greyChange"
          />
        </li>
      </ul>
    </div>
  </LayPanel>
</template>

<style lang="scss" scoped>
:deep(.el-switch__core) {
  --el-switch-off-color: var(--pure-switch-off-color);

  min-width: 36px;
  height: 18px;
}

:deep(.el-switch__core .el-switch__action) {
  height: 14px;
}

.matrix-theme-color {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  li {
    position: relative;
    width: 24px;
    height: 24px;
    cursor: pointer;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    border: 2px solid transparent;

    &:hover {
      transform: scale(1.1);
    }

    &.is-active {
      border-color: #fff;
      box-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
    }
  }
}

.setting {
  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
    font-size: 14px;
  }
}
</style>
