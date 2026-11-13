<script setup lang="ts">
import { ref } from "vue";
import SpecsSection from "./specs.vue";
import OptionsSection from "./options.vue";
import ImagesSection from "./images.vue";
import ReCol from "@/components/ReCol";
import { message } from "@/utils/message";
import { formRules } from "../utils/rule";
import type { ShellProps } from "../utils/types";
import { uploadProductImage } from "@/api/product";
import { productTypeOptions, storageTypeOptions } from "../utils/enums";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import type { UploadRequestOptions } from "element-plus";

import UploadIcon from "~icons/ep/upload";

type TabName = "base" | "nutrition" | "spec" | "option" | "image";

const props = defineProps<ShellProps>();
const form = ref(props.formInline);
const activeTab = ref<TabName>("base");
const ruleFormRef = ref();

function getRef() {
  return ruleFormRef.value;
}

function setActiveTab(tab: TabName) {
  activeTab.value = tab;
}

defineExpose({ getRef, setActiveTab });

/** 主图上传（也可直接粘贴图片地址） */
async function handleCoverUpload(options: UploadRequestOptions) {
  try {
    const { code, message: msg, data } = await uploadProductImage(options.file);
    if (code !== 200 || !data) throw new Error(msg || "主图上传失败");
    form.value.cover = data.url;
    message("主图上传成功", { type: "success" });
  } catch (error) {
    message(
      error instanceof Error && error.message ? error.message : "主图上传失败",
      { type: "error" }
    );
  }
}
</script>

<template>
  <el-form
    ref="ruleFormRef"
    :model="form"
    :rules="formRules"
    label-width="110px"
    scroll-to-error
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础信息" name="base">
        <el-row :gutter="30">
          <re-col>
            <el-form-item label="商品分类" prop="categoryId">
              <el-cascader
                v-model="form.categoryId"
                class="w-full"
                :options="props.categoryTree"
                :props="{
                  value: 'id',
                  label: 'name',
                  children: 'children',
                  emitPath: false,
                  checkStrictly: true
                }"
                clearable
                filterable
                placeholder="请选择商品分类"
              />
            </el-form-item>
          </re-col>

          <re-col :value="12" :xs="24" :sm="24">
            <el-form-item label="商品名称" prop="name">
              <el-input
                v-model="form.name"
                maxlength="100"
                clearable
                placeholder="请输入商品名称"
              />
            </el-form-item>
          </re-col>
          <re-col :value="12" :xs="24" :sm="24">
            <el-form-item label="副标题" prop="subtitle">
              <el-input
                v-model="form.subtitle"
                maxlength="200"
                clearable
                placeholder="请输入副标题（选填）"
              />
            </el-form-item>
          </re-col>

          <re-col>
            <el-form-item label="商品主图" prop="cover">
              <el-input
                v-model="form.cover"
                maxlength="255"
                clearable
                placeholder="图片地址，或点击右侧按钮上传"
              >
                <template #append>
                  <el-upload
                    :show-file-list="false"
                    accept="image/jpeg,image/png,image/webp"
                    :http-request="handleCoverUpload"
                  >
                    <el-button :icon="useRenderIcon(UploadIcon)">
                      上传
                    </el-button>
                  </el-upload>
                </template>
              </el-input>
            </el-form-item>
          </re-col>

          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="商品类型" prop="productType">
              <el-radio-group v-model="form.productType">
                <el-radio-button
                  v-for="item in productTypeOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="售价" prop="price">
              <el-input-number
                v-model="form.price"
                class="w-full!"
                :min="0.01"
                :precision="2"
                :step="0.1"
                controls-position="right"
                placeholder="元"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="划线价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                class="w-full!"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
                placeholder="元（选填）"
              />
            </el-form-item>
          </re-col>

          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="单位" prop="unit">
              <el-input
                v-model="form.unit"
                maxlength="20"
                clearable
                placeholder="如：份、盒"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                class="w-full!"
                :min="0"
                :precision="0"
                controls-position="right"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="排序" prop="sort">
              <el-input-number
                v-model="form.sort"
                class="w-full!"
                :min="0"
                :precision="0"
                controls-position="right"
              />
            </el-form-item>
          </re-col>

          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="状态" prop="status">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="推荐" prop="isRecommend">
              <el-switch
                v-model="form.isRecommend"
                :active-value="1"
                :inactive-value="0"
                active-text="推荐"
                inactive-text="不推荐"
              />
            </el-form-item>
          </re-col>

          <re-col>
            <el-form-item label="商品详情" prop="detail">
              <el-input
                v-model="form.detail"
                type="textarea"
                :rows="4"
                placeholder="请输入商品详情描述（选填）"
              />
            </el-form-item>
          </re-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="营养与储存" name="nutrition">
        <el-row :gutter="30">
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="热量(kcal)" prop="calories">
              <el-input-number
                v-model="form.calories"
                class="w-full!"
                :min="0"
                :precision="0"
                controls-position="right"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="蛋白质(g)" prop="protein">
              <el-input-number
                v-model="form.protein"
                class="w-full!"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="脂肪(g)" prop="fat">
              <el-input-number
                v-model="form.fat"
                class="w-full!"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
              />
            </el-form-item>
          </re-col>

          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="碳水化合物(g)" prop="carbohydrate">
              <el-input-number
                v-model="form.carbohydrate"
                class="w-full!"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="钠(mg)" prop="sodium">
              <el-input-number
                v-model="form.sodium"
                class="w-full!"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="储存方式" prop="storageType">
              <el-select
                v-model="form.storageType"
                class="w-full"
                placeholder="请选择储存方式"
              >
                <el-option
                  v-for="item in storageTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </re-col>

          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="保质期(天)" prop="shelfLifeDays">
              <el-input-number
                v-model="form.shelfLifeDays"
                class="w-full!"
                :min="0"
                :max="3650"
                :precision="0"
                controls-position="right"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="生产商" prop="producer">
              <el-input
                v-model="form.producer"
                maxlength="100"
                clearable
                placeholder="请输入生产商（选填）"
              />
            </el-form-item>
          </re-col>
          <re-col :value="8" :xs="12" :sm="8">
            <el-form-item label="食用方式" prop="heatingMethod">
              <el-input
                v-model="form.heatingMethod"
                maxlength="100"
                clearable
                placeholder="如：开袋即食（选填）"
              />
            </el-form-item>
          </re-col>

          <re-col>
            <el-form-item label="营养说明" prop="nutritionDesc">
              <el-input
                v-model="form.nutritionDesc"
                maxlength="255"
                clearable
                placeholder="请输入营养说明（选填）"
              />
            </el-form-item>
          </re-col>

          <re-col :value="12" :xs="24" :sm="24">
            <el-form-item label="过敏原" prop="allergens">
              <el-select
                v-model="form.allergens"
                class="w-full"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车创建，可多选"
              >
                <el-option
                  v-for="tag in form.allergens"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </el-form-item>
          </re-col>
          <re-col :value="12" :xs="24" :sm="24">
            <el-form-item label="健康标签" prop="healthTags">
              <el-select
                v-model="form.healthTags"
                class="w-full"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车创建，可多选"
              >
                <el-option
                  v-for="tag in form.healthTags"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </el-form-item>
          </re-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="规格" name="spec">
        <specs-section :form-inline="form" />
      </el-tab-pane>

      <el-tab-pane label="选项组" name="option">
        <options-section :form-inline="form" />
      </el-tab-pane>

      <el-tab-pane label="图集" name="image">
        <images-section :form-inline="form" />
      </el-tab-pane>
    </el-tabs>
  </el-form>
</template>

<!-- 分区块样式为 specs/options/images 子组件共用，故不加 scoped -->
<style lang="scss">
.product-form-block {
  padding: 12px 16px 4px;
  margin-bottom: 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;

  &--item {
    padding-bottom: 0;
    border-style: solid;
    background: var(--el-fill-color-lighter);
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.image-row {
  display: flex;
  gap: 12px;

  &--divider {
    padding-top: 8px;
    margin-top: 8px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  &__thumb {
    flex: 0 0 auto;
    width: 56px;
    height: 56px;
    border-radius: 4px;
  }

  &__body {
    flex: 1 1 auto;
    min-width: 0;
  }
}

.empty-tip {
  padding: 8px 0 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
