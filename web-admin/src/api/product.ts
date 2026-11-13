import { http } from "@/utils/http";

/** 后端统一响应结构 */
export interface ProductResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/** 分页结果（common/PageResult） */
export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

/** 商品分页查询参数（dto/admin/AdminProductQuery） */
export interface ProductPageQuery {
  current?: number;
  size?: number;
  name?: string;
  categoryId?: number;
  status?: number;
  productType?: number;
}

/** 商品列表行（vo/admin/AdminProductVO） */
export interface AdminProductVO {
  id: number;
  categoryId: number;
  categoryName: string | null;
  name: string;
  subtitle: string | null;
  cover: string;
  unit: string;
  price: number;
  originalPrice: number | null;
  stock: number;
  salesCount: number;
  status: number;
  isRecommend: number;
  sort: number;
  calories: number;
  healthTags: string[] | null;
  updateTime: string;
}

/** 规格（vo/product/ProductSpecVO） */
export interface ProductSpecVO {
  id: number;
  name: string;
  priceDelta: number;
  stock: number;
  calories: number;
  isDefault: number;
  sort: number;
  status: number;
}

/** 选项明细（vo/product/ProductOptionItemVO） */
export interface ProductOptionItemVO {
  id: number;
  name: string;
  priceDelta: number;
  calories: number;
  sort: number;
  status: number;
}

/** 选项组（vo/product/ProductOptionVO） */
export interface ProductOptionVO {
  id: number;
  name: string;
  type: number;
  required: number;
  sort: number;
  items: ProductOptionItemVO[] | null;
}

/** 图集（vo/product/ProductImageVO） */
export interface ProductImageVO {
  id: number;
  imageUrl: string;
  sort: number;
  status: number;
}

/** 商品详情（vo/admin/AdminProductDetailVO，编辑回填用） */
export interface AdminProductDetailVO {
  id: number;
  categoryId: number;
  categoryName: string | null;
  name: string;
  subtitle: string | null;
  cover: string;
  detail: string | null;
  productType: number;
  price: number;
  originalPrice: number | null;
  unit: string;
  stock: number;
  salesCount: number;
  status: number;
  isRecommend: number;
  sort: number;
  calories: number;
  protein: number;
  fat: number;
  carbohydrate: number;
  sodium: number;
  nutritionDesc: string | null;
  allergens: string[] | null;
  storageType: number;
  shelfLifeDays: number;
  producer: string | null;
  heatingMethod: string | null;
  healthTags: string[] | null;
  specs: ProductSpecVO[] | null;
  options: ProductOptionVO[] | null;
  images: ProductImageVO[] | null;
}

/** 保存商品入参：规格（dto/admin/ProductSpecRequest） */
export interface ProductSpecRequest {
  id?: number | null;
  name: string;
  priceDelta: number;
  stock: number;
  calories: number;
  isDefault: number;
  sort: number;
  status: number;
}

/** 保存商品入参：选项明细（dto/admin/ProductOptionItemRequest） */
export interface ProductOptionItemRequest {
  id?: number | null;
  name: string;
  priceDelta: number;
  calories: number;
  sort: number;
  status: number;
}

/** 保存商品入参：选项组（dto/admin/ProductOptionRequest） */
export interface ProductOptionRequest {
  id?: number | null;
  name: string;
  type: number;
  required: number;
  sort: number;
  items: ProductOptionItemRequest[];
}

/** 保存商品入参：图集（dto/admin/ProductImageRequest） */
export interface ProductImageRequest {
  id?: number | null;
  imageUrl: string;
  sort: number;
  status: number;
}

/** 保存商品入参（dto/admin/ProductSaveRequest） */
export interface ProductSaveRequest {
  categoryId: number;
  name: string;
  subtitle: string | null;
  cover: string;
  detail: string | null;
  productType: number;
  price: number;
  originalPrice: number | null;
  unit: string;
  stock: number;
  status: number;
  isRecommend: number;
  sort: number;
  calories: number;
  protein: number;
  fat: number;
  carbohydrate: number;
  sodium: number;
  nutritionDesc: string | null;
  allergens: string[] | null;
  storageType: number;
  shelfLifeDays: number;
  producer: string | null;
  heatingMethod: string | null;
  healthTags: string[] | null;
  specs: ProductSpecRequest[];
  options: ProductOptionRequest[] | null;
  images: ProductImageRequest[] | null;
}

/** 商品分页列表 */
export const getProductPage = (params: ProductPageQuery) => {
  return http.request<ProductResult<PageResult<AdminProductVO>>>(
    "get",
    "/api/admin/product/page",
    { params }
  );
};

/** 商品详情（含 specs/options/images 全量子资源） */
export const getProductDetail = (id: number) => {
  return http.request<ProductResult<AdminProductDetailVO>>(
    "get",
    `/api/admin/product/${id}`
  );
};

/** 新增商品 */
export const createProduct = (data: ProductSaveRequest) => {
  return http.request<ProductResult<number>>("post", "/api/admin/product", {
    data
  });
};

/** 编辑商品（全量子资源整体替换） */
export const updateProduct = (id: number, data: ProductSaveRequest) => {
  return http.request<ProductResult<null>>("put", `/api/admin/product/${id}`, {
    data
  });
};

/** 更新商品状态（0 下架、1 上架） */
export const updateProductStatus = (id: number, status: number) => {
  return http.request<ProductResult<null>>(
    "put",
    `/api/admin/product/${id}/status`,
    { data: { status } }
  );
};

/** 更新商品推荐标记（0 不推荐、1 推荐） */
export const updateProductRecommend = (id: number, isRecommend: number) => {
  return http.request<ProductResult<null>>(
    "put",
    `/api/admin/product/${id}/recommend`,
    { data: { isRecommend } }
  );
};

/** 删除商品 */
export const deleteProduct = (id: number) => {
  return http.request<ProductResult<null>>(
    "delete",
    `/api/admin/product/${id}`
  );
};

/** 上传商品图片（multipart 字段名 file，仅 JPG/PNG/WebP 且不超过 10MB） */
export const uploadProductImage = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.request<ProductResult<{ url: string }>>(
    "post",
    "/api/admin/file/upload",
    {
      data: formData,
      // 去掉实例默认的 application/json，交由浏览器生成带 boundary 的 multipart 头
      headers: { "Content-Type": null }
    }
  );
};
