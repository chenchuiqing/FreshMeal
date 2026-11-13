import type {
  ProductSpecRequest,
  ProductOptionRequest,
  ProductImageRequest
} from "@/api/product";
import type { CategoryVO } from "@/api/category";

/** 商品新增/编辑弹窗表单数据（基于详情回填或默认值构造） */
export interface ProductFormInline {
  id?: number;
  categoryId: number | null;
  name: string;
  subtitle: string;
  cover: string;
  detail: string;
  productType: number;
  price: number | null;
  originalPrice: number | null;
  unit: string;
  stock: number | null;
  status: number;
  isRecommend: number;
  sort: number;
  calories: number | null;
  protein: number | null;
  fat: number | null;
  carbohydrate: number | null;
  sodium: number | null;
  nutritionDesc: string;
  allergens: string[];
  storageType: number | null;
  shelfLifeDays: number | null;
  producer: string;
  heatingMethod: string;
  healthTags: string[];
  /** 默认规格所在行下标，保存时该行 isDefault=1、其余为 0 */
  defaultSpecIndex: number;
  specs: ProductSpecRequest[];
  options: ProductOptionRequest[];
  images: ProductImageRequest[];
}

export interface FormProps {
  formInline: ProductFormInline;
}

/** 弹窗表单根组件额外接收分类树（商品分类级联选择用） */
export interface ShellProps extends FormProps {
  categoryTree: CategoryVO[];
}
