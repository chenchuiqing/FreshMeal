<script setup lang="ts">
import { ref } from "vue";
import ReCol from "@/components/ReCol";
import { formRules } from "./utils/rule";
import type { FormProps } from "./utils/types";

const props = withDefaults(defineProps<FormProps>(), {
  formInline: () => ({
    parentId: 0,
    name: "",
    icon: "",
    sort: 0,
    status: 1,
    hasChildren: false,
    parentOptions: []
  })
});

const ruleFormRef = ref();
const newFormInline = ref(props.formInline);

function getRef() {
  return ruleFormRef.value;
}

defineExpose({ getRef });
</script>

<template>
  <el-form
    ref="ruleFormRef"
    :model="newFormInline"
    :rules="formRules"
    label-width="82px"
  >
    <el-row :gutter="30">
      <re-col>
        <el-form-item label="父分类" prop="parentId">
          <el-select
            v-model="newFormInline.parentId"
            class="w-full"
            :disabled="newFormInline.hasChildren"
            placeholder="请选择父分类（顶级为一级分类）"
          >
            <el-option
              v-for="item in newFormInline.parentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div v-if="newFormInline.hasChildren" class="form-tip">
            该分类下已有子分类，接口最多两级，仅支持保持为顶级分类
          </div>
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="newFormInline.name"
            maxlength="50"
            clearable
            placeholder="请输入分类名称"
          />
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="图标" prop="icon">
          <el-input
            v-model="newFormInline.icon"
            maxlength="255"
            clearable
            placeholder="请输入 emoji 或图标地址"
          />
        </el-form-item>
      </re-col>

      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="newFormInline.sort"
            class="w-full!"
            :min="0"
            :max="9999"
            controls-position="right"
          />
        </el-form-item>
      </re-col>
      <re-col :value="12" :xs="24" :sm="24">
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="newFormInline.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </re-col>
    </el-row>
  </el-form>
</template>

<style lang="scss" scoped>
.form-tip {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-color-warning);
}
</style>
