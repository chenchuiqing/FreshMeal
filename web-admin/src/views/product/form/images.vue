<script setup lang="ts">
import { ref } from "vue";
import { message } from "@/utils/message";
import type { FormProps } from "../utils/types";
import { imageRules } from "../utils/rule";
import { uploadProductImage } from "@/api/product";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import type { UploadRequestOptions } from "element-plus";

import UploadIcon from "~icons/ep/upload";

const props = defineProps<FormProps>();
const form = ref(props.formInline);

function removeImage(index: number) {
  form.value.images.splice(index, 1);
}

/** 图集上传（成功后追加一行） */
async function handleUpload(options: UploadRequestOptions) {
  try {
    const { code, message: msg, data } = await uploadProductImage(options.file);
    if (code !== 200 || !data) throw new Error(msg || "图片上传失败");
    form.value.images.push({
      imageUrl: data.url,
      sort: form.value.images.length,
      status: 1
    });
    message("图片上传成功", { type: "success" });
  } catch (error) {
    message(
      error instanceof Error && error.message ? error.message : "图片上传失败",
      { type: "error" }
    );
  }
}
</script>

<template>
  <div class="product-form-block">
    <div class="product-form-block__header">
      <span class="product-form-block__title">
        图片列表（共 {{ form.images.length }} 张）
      </span>
      <el-upload
        :show-file-list="false"
        accept="image/jpeg,image/png,image/webp"
        :http-request="handleUpload"
      >
        <el-button type="primary" :icon="useRenderIcon(UploadIcon)">
          上传图片
        </el-button>
      </el-upload>
    </div>

    <div
      v-for="(img, index) in form.images"
      :key="index"
      class="image-row"
      :class="{ 'image-row--divider': index > 0 }"
    >
      <el-image
        v-if="img.imageUrl"
        class="image-row__thumb"
        :src="img.imageUrl"
        fit="cover"
        :preview-src-list="[img.imageUrl]"
        preview-teleported
      />
      <el-row :gutter="12" class="image-row__body">
        <el-col :xs="24" :sm="14" :md="12">
          <el-form-item
            label="图片地址"
            :prop="`images.${index}.imageUrl`"
            :rules="imageRules"
          >
            <el-input
              v-model="img.imageUrl"
              maxlength="255"
              clearable
              placeholder="图片地址"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="5" :md="4">
          <el-form-item label="排序" :prop="`images.${index}.sort`">
            <el-input-number
              v-model="img.sort"
              class="w-full!"
              :min="0"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="12" :sm="3" :md="4">
          <el-form-item label="显示" :prop="`images.${index}.status`">
            <el-switch
              v-model="img.status"
              :active-value="1"
              :inactive-value="0"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="2" :md="4">
          <el-form-item label="操作">
            <el-button link type="danger" @click="removeImage(index)">
              移除
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </div>

    <div v-if="!form.images.length" class="empty-tip">
      暂无图集图片，点击右上角上传（支持 JPG、PNG、WebP，单张不超过 10MB）
    </div>
  </div>
</template>
