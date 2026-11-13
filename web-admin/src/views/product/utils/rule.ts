import { reactive } from "vue";
import type { FormItemRule, FormRules } from "element-plus";

/** 多值标签输入的长度校验（过敏原、健康标签单项不超过 20 字符） */
function tagListRule(message: string): FormItemRule {
  return {
    validator: (_rule, value: string[], callback) => {
      const invalid = Array.isArray(value)
        ? value.find(item => typeof item === "string" && item.length > 20)
        : undefined;
      callback(invalid ? new Error(message) : undefined);
    },
    trigger: "change"
  };
}

function requiredNumber(message: string, trigger = "change"): FormItemRule {
  return { required: true, type: "number", message, trigger };
}

function minNumber(
  min: number,
  message: string,
  trigger = "change"
): FormItemRule {
  return { type: "number", min, message, trigger };
}

function requiredText(message: string): FormItemRule {
  return { required: true, whitespace: true, message, trigger: "blur" };
}

function maxText(max: number, message: string): FormItemRule {
  return { max, message, trigger: "blur" };
}

/** 弹窗表单顶层字段规则（对齐 ProductSaveRequest 的校验消息） */
export const formRules = reactive(<FormRules>{
  categoryId: [requiredNumber("商品分类不能为空", "change")],
  name: [
    requiredText("商品名称不能为空"),
    maxText(100, "商品名称不能超过100个字符")
  ],
  cover: [
    requiredText("商品主图不能为空"),
    maxText(255, "商品主图地址不能超过255个字符")
  ],
  productType: [requiredNumber("商品类型只能为1或2")],
  price: [
    requiredNumber("商品售价不能为空"),
    minNumber(0.01, "商品售价必须大于0")
  ],
  originalPrice: [minNumber(0, "划线原价不能小于0")],
  unit: [
    requiredText("商品单位不能为空"),
    maxText(20, "商品单位不能超过20个字符")
  ],
  stock: [
    requiredNumber("商品库存不能为空"),
    minNumber(0, "商品库存不能小于0")
  ],
  status: [requiredNumber("状态只能为0或1")],
  isRecommend: [requiredNumber("推荐标记只能为0或1")],
  sort: [minNumber(0, "排序值不能小于0")],
  calories: [requiredNumber("热量不能为空"), minNumber(0, "热量不能小于0")],
  protein: [requiredNumber("蛋白质不能为空"), minNumber(0, "蛋白质不能小于0")],
  fat: [requiredNumber("脂肪不能为空"), minNumber(0, "脂肪不能小于0")],
  carbohydrate: [
    requiredNumber("碳水化合物不能为空"),
    minNumber(0, "碳水化合物不能小于0")
  ],
  sodium: [requiredNumber("钠含量不能为空"), minNumber(0, "钠含量不能小于0")],
  nutritionDesc: [maxText(255, "营养说明不能超过255个字符")],
  allergens: [tagListRule("过敏原名称不能超过20个字符")],
  storageType: [requiredNumber("储存方式不能为空")],
  shelfLifeDays: [
    requiredNumber("保质期不能为空"),
    minNumber(0, "保质期不能小于0"),
    {
      type: "number",
      max: 3650,
      message: "保质期不能超过3650天",
      trigger: "change"
    }
  ],
  producer: [maxText(100, "生产商不能超过100个字符")],
  heatingMethod: [maxText(100, "食用方式不能超过100个字符")],
  healthTags: [tagListRule("健康标签不能超过20个字符")]
});

/** 规格行字段规则（对齐 ProductSpecRequest 的校验消息） */
export const specNameRules: FormItemRule[] = [
  requiredText("规格名称不能为空"),
  maxText(50, "规格名称不能超过50个字符")
];
export const specPriceRules: FormItemRule[] = [
  requiredNumber("规格加价不能为空"),
  minNumber(0, "规格加价不能小于0")
];
export const specStockRules: FormItemRule[] = [
  requiredNumber("规格库存不能为空"),
  minNumber(0, "规格库存不能小于0")
];
export const specCaloriesRules: FormItemRule[] = [
  requiredNumber("规格热量不能为空"),
  minNumber(0, "规格热量不能小于0")
];

/** 选项组字段规则（对齐 ProductOptionRequest 的校验消息） */
export const optionGroupNameRules: FormItemRule[] = [
  requiredText("选项组名称不能为空"),
  maxText(50, "选项组名称不能超过50个字符")
];
export const optionGroupTypeRules: FormItemRule[] = [
  requiredNumber("选项组类型不能为空")
];

/** 选项明细字段规则（对齐 ProductOptionItemRequest 的校验消息） */
export const optionItemNameRules: FormItemRule[] = [
  requiredText("选项名称不能为空"),
  maxText(50, "选项名称不能超过50个字符")
];
export const optionItemPriceRules: FormItemRule[] = [
  requiredNumber("选项加价不能为空"),
  minNumber(0, "选项加价不能小于0")
];
export const optionItemCaloriesRules: FormItemRule[] = [
  requiredNumber("选项热量不能为空"),
  minNumber(0, "选项热量不能小于0")
];

/** 图集行字段规则（对齐 ProductImageRequest 的校验消息） */
export const imageRules: FormItemRule[] = [
  requiredText("图片地址不能为空"),
  maxText(255, "图片地址不能超过255个字符")
];
