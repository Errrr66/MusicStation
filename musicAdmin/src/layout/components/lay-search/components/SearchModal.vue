<script setup lang="ts">
import { match } from "pinyin-pro";
import { getConfig } from "@/config";
import { useRouter } from "vue-router";
import SearchResult from "./SearchResult.vue";
import SearchFooter from "./SearchFooter.vue";
import { useNav } from "@/layout/hooks/useNav";
import SearchHistory from "./SearchHistory.vue";
import type { optionsItem, dragItem } from "../types";
import { ref, computed, shallowRef, watch } from "vue";
import { useDebounceFn, onKeyStroke } from "@vueuse/core";
import { usePermissionStoreHook } from "@/store/modules/permission";
import { cloneDeep, isAllEmpty, storageLocal } from "@pureadmin/utils";
import SearchIcon from "@iconify-icons/ri/search-line";

interface Props {
  value: boolean;
}

interface Emits {
  (e: "update:value", val: boolean): void;
}

const { device } = useNav();
const emit = defineEmits<Emits>();
const props = withDefaults(defineProps<Props>(), {});

const router = useRouter();

const HISTORY_TYPE = "history";
const COLLECT_TYPE = "collect";
const LOCALEHISTORYKEY = "menu-search-history";
const LOCALECOLLECTKEY = "menu-search-collect";

const keyword = ref("");
const resultRef = ref();
const historyRef = ref();
const scrollbarRef = ref();
const activePath = ref("");
const historyPath = ref("");
const resultOptions = shallowRef([]);
const historyOptions = shallowRef([]);
const handleSearch = useDebounceFn(search, 300);
const historyNum = getConfig().MenuSearchHistory;
const inputRef = ref<HTMLInputElement | null>(null);

const menusData = computed(() => {
  return cloneDeep(usePermissionStoreHook().wholeMenus);
});

const show = computed({
  get() {
    return props.value;
  },
  set(val: boolean) {
    emit("update:value", val);
  }
});

watch(
  () => props.value,
  newValue => {
    if (newValue) getHistory();
  }
);

const showSearchResult = computed(() => {
  return keyword.value && resultOptions.value.length > 0;
});

const showSearchHistory = computed(() => {
  return !keyword.value && historyOptions.value.length > 0;
});

const showEmpty = computed(() => {
  return (
    (!keyword.value && historyOptions.value.length === 0) ||
    (keyword.value && resultOptions.value.length === 0)
  );
});

function getStorageItem(key) {
  return storageLocal().getItem<optionsItem[]>(key) || [];
}

function setStorageItem(key, value) {
  storageLocal().setItem(key, value);
}

function flatTree(arr) {
  const res = [];
  function deep(arr) {
    arr.forEach(item => {
      if (item.children && item.children.length > 0) {
        deep(item.children);
      } else if (item.meta?.title && item.path) {
        res.push(item);
      }
    });
  }
  deep(arr);
  return res;
}

function search() {
  const flatMenusData = flatTree(menusData.value);
  resultOptions.value = flatMenusData.filter(menu =>
    keyword.value
      ? menu.meta?.title
          .toLocaleLowerCase()
          .includes(keyword.value.toLocaleLowerCase().trim()) ||
        !isAllEmpty(
          match(
            menu.meta?.title.toLocaleLowerCase(),
            keyword.value.toLocaleLowerCase().trim()
          )
        )
      : false
  );
  activePath.value =
    resultOptions.value?.length > 0 ? resultOptions.value[0].path : "";
}

function handleClose() {
  show.value = false;
  setTimeout(() => {
    resultOptions.value = [];
    historyPath.value = "";
    keyword.value = "";
  }, 200);
}

function scrollTo(index) {
  const ref = resultOptions.value.length ? resultRef.value : historyRef.value;
  const scrollTop = ref.handleScroll(index);
  scrollbarRef.value.setScrollTop(scrollTop);
}

function getCurrentOptionsAndPath() {
  const isResultOptions = resultOptions.value.length > 0;
  const options = isResultOptions ? resultOptions.value : historyOptions.value;
  const currentPath = isResultOptions ? activePath.value : historyPath.value;
  return { options, currentPath, isResultOptions };
}

function updatePathAndScroll(newIndex, isResultOptions) {
  if (isResultOptions) {
    activePath.value = resultOptions.value[newIndex].path;
  } else {
    historyPath.value = historyOptions.value[newIndex].path;
  }
  scrollTo(newIndex);
}

function handleUp() {
  const { options, currentPath, isResultOptions } = getCurrentOptionsAndPath();
  if (options.length === 0) return;
  const index = options.findIndex(item => item.path === currentPath);
  const prevIndex = (index - 1 + options.length) % options.length;
  updatePathAndScroll(prevIndex, isResultOptions);
}

function handleDown() {
  const { options, currentPath, isResultOptions } = getCurrentOptionsAndPath();
  if (options.length === 0) return;
  const index = options.findIndex(item => item.path === currentPath);
  const nextIndex = (index + 1) % options.length;
  updatePathAndScroll(nextIndex, isResultOptions);
}

function handleEnter() {
  const { options, currentPath, isResultOptions } = getCurrentOptionsAndPath();
  if (options.length === 0 || currentPath === "") return;
  const index = options.findIndex(item => item.path === currentPath);
  if (index === -1) return;
  if (isResultOptions) {
    saveHistory();
  } else {
    updateHistory();
  }
  router.push(options[index].path);
  handleClose();
}

function handleDelete(item) {
  const key = item.type === HISTORY_TYPE ? LOCALEHISTORYKEY : LOCALECOLLECTKEY;
  let list = getStorageItem(key);
  list = list.filter(listItem => listItem.path !== item.path);
  setStorageItem(key, list);
  getHistory();
}

function handleCollect(item) {
  let searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  let searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  searchHistoryList = searchHistoryList.filter(
    historyItem => historyItem.path !== item.path
  );
  setStorageItem(LOCALEHISTORYKEY, searchHistoryList);
  if (!searchCollectList.some(collectItem => collectItem.path === item.path)) {
    searchCollectList.unshift({ ...item, type: COLLECT_TYPE });
    setStorageItem(LOCALECOLLECTKEY, searchCollectList);
  }
  getHistory();
}

function saveHistory() {
  const { path, meta } = resultOptions.value.find(
    item => item.path === activePath.value
  );
  const searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  const searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  const isCollected = searchCollectList.some(item => item.path === path);
  const existingIndex = searchHistoryList.findIndex(item => item.path === path);
  if (!isCollected) {
    if (existingIndex !== -1) searchHistoryList.splice(existingIndex, 1);
    if (searchHistoryList.length >= historyNum) searchHistoryList.pop();
    searchHistoryList.unshift({ path, meta, type: HISTORY_TYPE });
    storageLocal().setItem(LOCALEHISTORYKEY, searchHistoryList);
  }
}

function updateHistory() {
  let searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  const historyIndex = searchHistoryList.findIndex(
    item => item.path === historyPath.value
  );
  if (historyIndex !== -1) {
    const [historyItem] = searchHistoryList.splice(historyIndex, 1);
    searchHistoryList.unshift(historyItem);
    setStorageItem(LOCALEHISTORYKEY, searchHistoryList);
  }
}

function getHistory() {
  const searchHistoryList = getStorageItem(LOCALEHISTORYKEY);
  const searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  historyOptions.value = [...searchHistoryList, ...searchCollectList];
  historyPath.value = historyOptions.value[0]?.path;
}

function handleDrag(item: dragItem) {
  const searchCollectList = getStorageItem(LOCALECOLLECTKEY);
  const [reorderedItem] = searchCollectList.splice(item.oldIndex, 1);
  searchCollectList.splice(item.newIndex, 0, reorderedItem);
  storageLocal().setItem(LOCALECOLLECTKEY, searchCollectList);
  historyOptions.value = [
    ...getStorageItem(LOCALEHISTORYKEY),
    ...getStorageItem(LOCALECOLLECTKEY)
  ];
  historyPath.value = reorderedItem.path;
}

// 限定键盘监听在搜索输入框内，避免全局常驻影响其他页面
onKeyStroke("Enter", handleEnter, { target: inputRef });
onKeyStroke("ArrowUp", handleUp, { target: inputRef });
onKeyStroke("ArrowDown", handleDown, { target: inputRef });
</script>

<template>
  <el-dialog
    v-model="show"
    top="5vh"
    class="pure-search-dialog matrix-search-dialog"
    :show-close="false"
    :width="device === 'mobile' ? '80vw' : '40vw'"
    :before-close="handleClose"
    append-to-body
    @opened="inputRef.focus()"
    @closed="inputRef.blur()"
  >
    <div class="search-header">
      <div class="header-line"></div>
    </div>
    <div class="search-input-wrapper">
      <div class="input-indicator">
        <span class="indicator-bracket">></span>
      </div>
      <el-input
        ref="inputRef"
        v-model="keyword"
        size="large"
        clearable
        placeholder="搜索菜单（支持拼音搜索）"
        @input="handleSearch"
      >
        <template #prefix>
          <IconifyIconOffline
            :icon="SearchIcon"
            class="search-icon"
          />
        </template>
      </el-input>
    </div>
    <div class="search-content">
      <el-scrollbar ref="scrollbarRef" max-height="calc(90vh - 200px)">
        <div v-if="showEmpty" class="empty-container">
          <div class="empty-icon">[ ]</div>
          <div class="empty-text">暂无搜索结果</div>
        </div>
        <SearchHistory
          v-if="showSearchHistory"
          ref="historyRef"
          v-model:value="historyPath"
          :options="historyOptions"
          @click="handleEnter"
          @delete="handleDelete"
          @collect="handleCollect"
          @drag="handleDrag"
        />
        <SearchResult
          v-if="showSearchResult"
          ref="resultRef"
          v-model:value="activePath"
          :options="resultOptions"
          @click="handleEnter"
        />
      </el-scrollbar>
    </div>
    <template #footer>
      <SearchFooter :total="resultOptions.length" />
    </template>
    <div class="dialog-corner top-left"></div>
    <div class="dialog-corner top-right"></div>
    <div class="dialog-corner bottom-left"></div>
    <div class="dialog-corner bottom-right"></div>
  </el-dialog>
</template>

<style lang="scss" scoped>
.matrix-search-dialog {
  :deep(.el-dialog) {
    background: var(--matrix-bg);
    border: 1px solid var(--matrix-border);
    border-radius: 0;
    box-shadow: 0 0 30px var(--matrix-shadow);
  }

  :deep(.el-dialog__header) {
    display: none;
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 12px 16px;
    border-top: 1px solid var(--matrix-border);
  }

  .search-header {
    padding: 12px 16px 0;

    .header-line {
      height: 2px;
      background: repeating-linear-gradient(
        90deg,
        var(--matrix-color) 0,
        var(--matrix-color) 4px,
        transparent 4px,
        transparent 8px
      );
      opacity: 0.4;
    }
  }

  .search-input-wrapper {
    display: flex;
    align-items: center;
    padding: 16px;
    gap: 12px;

    .input-indicator {
      .indicator-bracket {
        color: var(--matrix-color);
        font-family: var(--mr-font-family);
        font-size: 18px;
        font-weight: 600;
        animation: blink 1s infinite;
      }
    }

    :deep(.el-input) {
      flex: 1;

      .el-input__wrapper {
        background: var(--matrix-bg-light);
        border: 1px solid var(--matrix-border);
        border-radius: 0;
        box-shadow: none;

        &:hover,
        &:focus {
          border-color: var(--matrix-color);
          box-shadow: 0 0 10px var(--matrix-shadow);
        }
      }

      .el-input__inner {
        color: var(--matrix-text);
        font-family: var(--mr-font-family);
        font-size: 14px;
        letter-spacing: 0.5px;

        &::placeholder {
          color: var(--matrix-color-dim);
        }
      }

      .el-input__prefix {
        color: var(--matrix-color);
      }
    }

    .search-icon {
      color: var(--matrix-color);
      width: 20px;
      height: 20px;
    }
  }

  .search-content {
    padding: 0 16px 16px;
  }

  .empty-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
    color: var(--matrix-color-dim);

    .empty-icon {
      font-family: var(--mr-font-family);
      font-size: 32px;
      margin-bottom: 12px;
      animation: blink 1.5s infinite;
    }

    .empty-text {
      font-family: var(--mr-font-family);
      font-size: 13px;
      letter-spacing: 1px;
    }
  }

  .dialog-corner {
    position: absolute;
    width: 12px;
    height: 12px;
    border: 2px solid var(--matrix-color);

    &.top-left {
      top: -1px;
      left: -1px;
      border-right: none;
      border-bottom: none;
    }

    &.top-right {
      top: -1px;
      right: -1px;
      border-left: none;
      border-bottom: none;
    }

    &.bottom-left {
      bottom: -1px;
      left: -1px;
      border-right: none;
      border-top: none;
    }

    &.bottom-right {
      bottom: -1px;
      right: -1px;
      border-left: none;
      border-top: none;
    }
  }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}
</style>
