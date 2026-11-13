import { reactive } from "vue";
import type { FormRules } from "element-plus";

/** 自定义表单规则校验（对齐 CategorySaveRequest 的校验消息） */
export const formRules = reactive(<FormRules>{
  parentId: [
    {
      required: true,
      type: "number",
      message: "请选择父分类",
      trigger: "change"
    }
  ],
  name: [
    {
      required: true,
      whitespace: true,
      message: "分类名称不能为空",
      trigger: "blur"
    },
    { max: 50, message: "分类名称不能超过50个字符", trigger: "blur" }
  ],
  icon: [
    { max: 255, message: "分类图标地址不能超过255个字符", trigger: "blur" }
  ],
  sort: [
    {
      required: true,
      type: "number",
      min: 0,
      message: "排序值不能小于0",
      trigger: "change"
    }
  ],
  status: [
    {
      required: true,
      type: "number",
      message: "状态只能为0或1",
      trigger: "change"
    }
  ]
});
