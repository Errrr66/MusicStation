<script setup lang="ts">
import { ref, reactive } from "vue";
import { useBanner } from "./utils/hook";
import { PureTableBar } from "@/components/RePureTableBar";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { deviceDetection } from "@pureadmin/utils";
import type { FormInstance } from "element-plus";
import AddFill from "@iconify-icons/ri/add-circle-line";
import EditPen from "@iconify-icons/ep/edit-pen";
import Delete from "@iconify-icons/ep/delete";
import Refresh from "@iconify-icons/ep/refresh";

defineOptions({
  name: "BannerManagement"
});

const formRef = ref<FormInstance>();
const form = reactive({
  bannerStatus: null
});

const tableRef = ref();
const {
  loading,
  columns,
  dataList,
  pagination,
  selectedNum,
  onSearch,
  handleSizeChange,
  handleCurrentChange,
  handleSelectionChange,
  handleDelete,
  onBatchDelete,
  handleUpload
} = useBanner(form, tableRef);

const resetForm = (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  formEl.resetFields();
  onSearch();
};
</script>

<template>
  <div :class="['matrix-page', 'flex', 'justify-between', deviceDetection() && 'flex-wrap']">
    <div
      :class="[deviceDetection() ? ['w-full', 'mt-2'] : 'w-[calc(100%-0px)]']"
    >
      <el-form
        ref="formRef"
        :inline="true"
        :model="form"
        class="matrix-search-form"
      >
        <el-form-item label="状态" prop="bannerStatus">
          <el-select
            v-model="form.bannerStatus"
            placeholder="请选择"
            clearable
            class="!w-[180px]"
          >
            <el-option label="启用" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :icon="useRenderIcon('ri:search-line')"
            :loading="loading"
            @click="onSearch"
          >
            搜索
          </el-button>
          <el-button :icon="useRenderIcon(Refresh)" @click="resetForm(formRef)">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <PureTableBar title="轮播图管理" :columns="columns" @refresh="onSearch">
        <template #buttons>
          <el-button
            type="primary"
            :icon="useRenderIcon(AddFill)"
            @click="handleUpload()"
          >
            新增轮播图
          </el-button>
        </template>
        <template v-slot="{ size, dynamicColumns }">
          <div
            v-if="selectedNum > 0"
            v-motion-fade
            class="matrix-selection-bar"
          >
            <div class="flex-auto selection-text">
              已选 <span class="count">{{ selectedNum }}</span> 项
            </div>
            <el-popconfirm title="是否确认删除?" @confirm="onBatchDelete">
              <template #reference>
                <el-button type="danger" text class="mr-1">
                  批量删除
                </el-button>
              </template>
            </el-popconfirm>
          </div>

          <pure-table
            ref="tableRef"
            row-key="bannerId"
            adaptive
            :adaptiveConfig="{ offsetBottom: 108 }"
            align-whole="center"
            table-layout="auto"
            :loading="loading"
            :size="size"
            :data="dataList"
            :columns="dynamicColumns"
            :pagination="{ ...pagination, size }"
            :header-cell-style="{
              background: 'var(--matrix-bg-light)',
              color: 'var(--matrix-color)'
            }"
            @page-size-change="handleSizeChange"
            @page-current-change="handleCurrentChange"
            @selection-change="handleSelectionChange"
          >
            <template #operation="{ row }">
              <el-button
                class="reset-margin"
                link
                type="primary"
                :size="size"
                :icon="useRenderIcon(EditPen)"
                @click="handleUpload(row)"
              >
                编辑
              </el-button>
              <el-popconfirm
                :title="`是否确认删除编号为 ${row.bannerId} 的轮播图?`"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button
                    class="reset-margin"
                    link
                    type="primary"
                    :size="size"
                    :icon="useRenderIcon(Delete)"
                  >
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </pure-table>
        </template>
      </PureTableBar>
    </div>
  </div>
</template>

<style scoped lang="scss">
:deep(.el-dropdown-menu__item i) {
  margin: 0;
}
</style>
