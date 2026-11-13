/** 分类新增/编辑弹窗表单数据 */
export interface CategoryFormInline {
  id?: number;
  parentId: number;
  name: string;
  icon: string;
  sort: number;
  status: number;
  /** 当前分类是否存在子分类（存在时父分类固定为顶级） */
  hasChildren: boolean;
  /** 可选父分类（顶级 + 所有一级分类，接口最多两级） */
  parentOptions: Array<{ label: string; value: number }>;
}

export interface FormProps {
  formInline: CategoryFormInline;
}
