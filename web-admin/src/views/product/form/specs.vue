<script setup lang="ts">
import { ref } from "vue";
import type { FormProps } from "../utils/types";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import {
  specNameRules,
  specPriceRules,
  specStockRules,
  specCaloriesRules
} from "../utils/rule";

import AddFill from "~icons/ri/add-circle-line";

const props = defineProps<FormProps>();
const form = ref(props.formInline);

function addSpec() {
  const list = form.value.specs;
  list.push({
    name: "",
    priceDelta: 0,
    stock: 0,
    calories: 0,
    isDefault: 0,
    sort: list.length,
    status: 1
  });
}

function removeSpec(index: number) {
  if (form.value.specs.length <= 1) return;
  form.value.specs.splice(index, 1);
  // 删除后校正默认规格下标，保证仍指向有效行
  const max = form.value.specs.length - 1;
  let next = form.value.defaultSpecIndex;
  if (index < next) next -= 1;
  form.value.defaultSpecIndex = Math.min(next, max);
}
</script>

<template>
  <div>
    <div
      v-for="(spec, index) in form.specs"
      :key="index"
      class="product-form-block"
    >
      <div class="product-form-block__header">
        <span class="product-form-block__title">规格 {{ index + 1 }}</span>
        <div class="product-form-block__actions">
          <el-radio-group v-model="form.defaultSpecIndex" size="small">
            <el-radio-button :value="index">默认规格</el-radio-button>
          </el-radio-group>
          <el-button
            link
            type="danger"
            :disabled="form.specs.length <= 1"
            @click="removeSpec(index)"
          >
            删除
          </el-button>
        </div>
      </div>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item
            label="名称"
            :prop="`specs.${index}.name`"
            :rules="specNameRules"
          >
            <el-input
              v-model="spec.name"
              maxlength="50"
              clearable
              placeholder="规格名称，如：大份"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item
            label="加价"
            :prop="`specs.${index}.priceDelta`"
            :rules="specPriceRules"
          >
            <el-input-number
              v-model="spec.priceDelta"
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
            label="库存"
            :prop="`specs.${index}.stock`"
            :rules="specStockRules"
          >
            <el-input-number
              v-model="spec.stock"
              class="w-full!"
              :min="0"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item
            label="热量"
            :prop="`specs.${index}.calories`"
            :rules="specCaloriesRules"
          >
            <el-input-number
              v-model="spec.calories"
              class="w-full!"
              :min="0"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item label="排序" :prop="`specs.${index}.sort`">
            <el-input-number
              v-model="spec.sort"
              class="w-full!"
              :min="0"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-form-item label="状态" :prop="`specs.${index}.status`">
            <el-switch
              v-model="spec.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
    <el-button :icon="useRenderIcon(AddFill)" @click="addSpec">
      新增规格
    </el-button>
  </div>
</template>
