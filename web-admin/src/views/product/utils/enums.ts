/** 通用下拉选项（value 对应接口枚举值） */
export interface SelectOption {
  label: string;
  value: number;
}

/** 商品类型：1 单品、2 套餐 */
export const productTypeOptions: SelectOption[] = [
  { label: "单品", value: 1 },
  { label: "套餐", value: 2 }
];

/** 储存方式：1 常温、2 冷藏、3 冷冻 */
export const storageTypeOptions: SelectOption[] = [
  { label: "常温", value: 1 },
  { label: "冷藏", value: 2 },
  { label: "冷冻", value: 3 }
];

/** 上下架状态：1 上架、0 下架 */
export const statusOptions: SelectOption[] = [
  { label: "上架", value: 1 },
  { label: "下架", value: 0 }
];

/** 选项组类型：1 单选、2 多选 */
export const optionTypeOptions: SelectOption[] = [
  { label: "单选", value: 1 },
  { label: "多选", value: 2 }
];
