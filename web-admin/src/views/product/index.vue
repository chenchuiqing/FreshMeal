<script setup lang="ts">
import { ref } from "vue";
import { useProduct } from "./utils/hook";
import { PureTableBar } from "@/components/RePureTableBar";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { productTypeOptions, statusOptions } from "./utils/enums";

import Delete from "~icons/ep/delete";
import EditPen from "~icons/ep/edit-pen";
import Refresh from "~icons/ep/refresh";
import AddFill from "~icons/ri/add-circle-line";
import StarFill from "~icons/ep/star-filled";
import Star from "~icons/ep/star";

defineOptions({
  name: "CommerceProduct"
});

const formRef = ref();
const tableRef = ref();
const {
  form,
  loading,
  columns,
  dataList,
  categoryTree,
  pagination,
  statusLoadingId,
  recommendLoadingId,
  handleSearch,
  resetForm,
  handleSizeChange,
  handleCurrentChange,
  onSearch,
  onStatusChange,
  onRecommendChange,
  handleDelete,
  openDialog
} = useProduct();

function onFullscreen() {
  // 重置表格高度
  tableRef.value.setAdaptive();
}
</script>

<template>
  <div class="main">
    <el-form
      ref="formRef"
      :inline="true"
      :model="form"
      class="search-form bg-bg_color w-full pl-8 pt-3 overflow-auto"
    >
      <el-form-item label="商品名称：" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入商品名称"
          clearable
          class="w-45!"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="商品分类：" prop="categoryId">
        <el-cascader
          v-model="form.categoryId"
          class="w-45!"
          :options="categoryTree"
          :props="{
            value: 'id',
            label: 'name',
            children: 'children',
            emitPath: false,
            checkStrictly: true
          }"
          clearable
          filterable
          placeholder="全部"
        />
      </el-form-item>
      <el-form-item label="状态：" prop="status">
        <el-select
          v-model="form.status"
          placeholder="全部"
          clearable
          class="w-37.5!"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="类型：" prop="productType">
        <el-select
          v-model="form.productType"
          placeholder="全部"
          clearable
          class="w-37.5!"
        >
          <el-option
            v-for="item in productTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :icon="useRenderIcon('ri/search-line')"
          :loading="loading"
          @click="handleSearch"
        >
          搜索
        </el-button>
        <el-button :icon="useRenderIcon(Refresh)" @click="resetForm(formRef)">
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <PureTableBar
      title="商品管理"
      :columns="columns"
      @refresh="onSearch"
      @fullscreen="onFullscreen"
    >
      <template #buttons>
        <el-button
          type="primary"
          :icon="useRenderIcon(AddFill)"
          @click="openDialog()"
        >
          新增商品
        </el-button>
      </template>
      <template v-slot="{ size, dynamicColumns }">
        <pure-table
          ref="tableRef"
          adaptive
          :adaptiveConfig="{ offsetBottom: 108 }"
          align-whole="center"
          row-key="id"
          table-layout="auto"
          :loading="loading"
          :size="size"
          :data="dataList"
          :columns="dynamicColumns"
          :pagination="{ ...pagination, size }"
          :header-cell-style="{
            background: 'var(--el-fill-color-light)',
            color: 'var(--el-text-color-primary)'
          }"
          @page-size-change="handleSizeChange"
          @page-current-change="handleCurrentChange"
        >
          <template #cover="{ row }">
            <el-image
              class="cover-thumb"
              :src="row.cover"
              fit="cover"
              :preview-src-list="[row.cover]"
              preview-teleported
            />
          </template>
          <template #healthTags="{ row }">
            <div v-if="row.healthTags?.length" class="tag-list">
              <el-tag
                v-for="tag in row.healthTags"
                :key="tag"
                size="small"
                type="success"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
          <template #status="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              :loading="statusLoadingId === row.id"
              @change="val => onStatusChange(row, val)"
            />
          </template>
          <template #recommend="{ row }">
            <el-switch
              :model-value="row.isRecommend"
              :active-value="1"
              :inactive-value="0"
              :loading="recommendLoadingId === row.id"
              :active-icon="StarFill"
              :inactive-icon="Star"
              @change="val => onRecommendChange(row, val)"
            />
          </template>
          <template #operation="{ row }">
            <el-button
              class="reset-margin"
              link
              type="primary"
              :size="size"
              :icon="useRenderIcon(EditPen)"
              @click="openDialog('编辑', row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              :title="`是否确认删除商品「${row.name}」？`"
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
</template>

<style lang="scss" scoped>
.search-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}

.cover-thumb {
  width: 48px;
  height: 48px;
  border-radius: 4px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
