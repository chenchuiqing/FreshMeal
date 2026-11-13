<script setup lang="ts">
import { ref } from "vue";
import type { ProductOptionItemRequest } from "@/api/product";
import type { FormProps } from "../utils/types";
import { optionTypeOptions } from "../utils/enums";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import {
  optionGroupNameRules,
  optionGroupTypeRules,
  optionItemNameRules,
  optionItemPriceRules,
  optionItemCaloriesRules
} from "../utils/rule";

import AddFill from "~icons/ri/add-circle-line";

const props = defineProps<FormProps>();
const form = ref(props.formInline);

function createItem(): ProductOptionItemRequest {
  return { name: "", priceDelta: 0, calories: 0, sort: 0, status: 1 };
}

function addGroup() {
  form.value.options.push({
    name: "",
    type: 1,
    required: 0,
    sort: form.value.options.length,
    items: [createItem()]
  });
}

function removeGroup(index: number) {
  form.value.options.splice(index, 1);
}

function addItem(groupIndex: number) {
  form.value.options[groupIndex].items.push(createItem());
}

function removeItem(groupIndex: number, itemIndex: number) {
  const items = form.value.options[groupIndex].items;
  if (items.length <= 1) return;
  items.splice(itemIndex, 1);
}
</script>

<template>
  <div>
    <div
      v-for="(group, index) in form.options"
      :key="index"
      class="product-form-block"
    >
      <div class="product-form-block__header">
        <span class="product-form-block__title">选项组 {{ index + 1 }}</span>
        <div class="product-form-block__actions">
          <el-button link type="danger" @click="removeGroup(index)">
            删除组
          </el-button>
        </div>
      </div>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item
            label="组名"
            :prop="`options.${index}.name`"
            :rules="optionGroupNameRules"
          >
            <el-input
              v-model="group.name"
              maxlength="50"
              clearable
              placeholder="如：甜度"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item
            label="类型"
            :prop="`options.${index}.type`"
            :rules="optionGroupTypeRules"
          >
            <el-radio-group v-model="group.type">
              <el-radio-button
                v-for="item in optionTypeOptions"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-form-item label="必选" :prop="`options.${index}.required`">
            <el-switch
              v-model="group.required"
              :active-value="1"
              :inactive-value="0"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="4">
          <el-form-item label="排序" :prop="`options.${index}.sort`">
            <el-input-number
              v-model="group.sort"
              class="w-full!"
              :min="0"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <div
        v-for="(item, itemIndex) in group.items"
        :key="itemIndex"
        class="product-form-block product-form-block--item"
      >
        <div class="product-form-block__header">
          <span class="product-form-block__title">
            选项 {{ index + 1 }}-{{ itemIndex + 1 }}
          </span>
          <div class="product-form-block__actions">
            <el-button
              link
              type="danger"
              :disabled="group.items.length <= 1"
              @click="removeItem(index, itemIndex)"
            >
              移除
            </el-button>
          </div>
        </div>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item
              label="名称"
              :prop="`options.${index}.items.${itemIndex}.name`"
              :rules="optionItemNameRules"
            >
              <el-input
                v-model="item.name"
                maxlength="50"
                clearable
                placeholder="如：少糖"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item
              label="加价"
              :prop="`options.${index}.items.${itemIndex}.priceDelta`"
              :rules="optionItemPriceRules"
            >
              <el-input-number
                v-model="item.priceDelta"
                class="w-full!"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item
              label="热量"
              :prop="`options.${index}.items.${itemIndex}.calories`"
              :rules="optionItemCaloriesRules"
            >
              <el-input-number
                v-model="item.calories"
                class="w-full!"
                :min="0"
                :precision="0"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item
              label="排序"
              :prop="`options.${index}.items.${itemIndex}.sort`"
            >
              <el-input-number
                v-model="item.sort"
                class="w-full!"
                :min="0"
                :precision="0"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item
              label="状态"
              :prop="`options.${index}.items.${itemIndex}.status`"
            >
              <el-switch
                v-model="item.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="停用"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <el-button :icon="useRenderIcon(AddFill)" @click="addItem(index)">
        新增选项
      </el-button>
    </div>
    <el-button
      type="primary"
      plain
      :icon="useRenderIcon(AddFill)"
      @click="addGroup"
    >
      新增选项组
    </el-button>
  </div>
</template>
