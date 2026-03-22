<script setup lang="ts">
import { useNav } from "@/layout/hooks/useNav";
import MdiKeyboardEsc from "@/assets/svg/keyboard_esc.svg?component";
import EnterOutlined from "@/assets/svg/enter_outlined.svg?component";
import ArrowUpLine from "@iconify-icons/ri/arrow-up-line";
import ArrowDownLine from "@iconify-icons/ri/arrow-down-line";

const { device } = useNav();

defineProps<{
  total?: number;
}>();
</script>

<template>
  <div class="search-footer matrix-footer">
    <span class="search-footer-item">
      <EnterOutlined />
      <span class="item-text">确认</span>
    </span>
    <span class="search-footer-item">
      <IconifyIconOffline :icon="ArrowUpLine" class="icon-sm" />
      <IconifyIconOffline :icon="ArrowDownLine" class="icon-sm" />
      <span class="item-text">切换</span>
    </span>
    <span class="search-footer-item">
      <MdiKeyboardEsc />
      <span class="item-text">关闭</span>
    </span>
    <span v-if="total != null" class="search-footer-total">
      <span class="total-bracket">[</span>
      <span class="total-count">{{ total }}</span>
      <span class="total-text">项</span>
      <span class="total-bracket">]</span>
    </span>
  </div>
</template>

<style lang="scss" scoped>
.matrix-footer {
  display: flex;
  align-items: center;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  color: var(--matrix-color-dim);

  .search-footer-item {
    display: flex;
    align-items: center;
    margin-right: 16px;
    padding: 4px 8px;
    background: var(--matrix-bg-light);
    border: 1px solid var(--matrix-border);
    transition: all 0.2s ease;

    &:hover {
      border-color: var(--matrix-color);
      background: var(--matrix-bg-hover);
    }

    svg {
      width: 16px;
      height: 16px;
      margin-right: 4px;
      color: var(--matrix-color);
    }

    .icon-sm {
      width: 14px;
      height: 14px;
    }

    .item-text {
      letter-spacing: 0.5px;
    }
  }

  .search-footer-total {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 4px;

    .total-bracket {
      color: var(--matrix-color-dim);
    }

    .total-count {
      color: var(--matrix-color);
      font-weight: 600;
    }

    .total-text {
      color: var(--matrix-color-dim);
    }
  }
}
</style>
