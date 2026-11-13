import type {
  AdminProductDetailVO,
  ProductSaveRequest,
  ProductSpecRequest
} from "@/api/product";
import type { ProductFormInline } from "./types";

/** 新增商品的默认表单数据 */
export function createDefaultForm(): ProductFormInline {
  return {
    categoryId: null,
    name: "",
    subtitle: "",
    cover: "",
    detail: "",
    productType: 1,
    price: null,
    originalPrice: null,
    unit: "份",
    stock: null,
    status: 1,
    isRecommend: 0,
    sort: 0,
    calories: null,
    protein: null,
    fat: null,
    carbohydrate: null,
    sodium: null,
    nutritionDesc: "",
    allergens: [],
    storageType: 1,
    shelfLifeDays: null,
    producer: "",
    heatingMethod: "",
    healthTags: [],
    defaultSpecIndex: 0,
    specs: [
      {
        name: "",
        priceDelta: 0,
        stock: 0,
        calories: 0,
        isDefault: 1,
        sort: 0,
        status: 1
      }
    ],
    options: [],
    images: []
  };
}

/** 商品详情 → 编辑表单（子资源含 status 全量回填） */
export function mapDetailToForm(
  detail: AdminProductDetailVO
): ProductFormInline {
  const specs: ProductSpecRequest[] = (detail.specs ?? []).map(spec => ({
    id: spec.id,
    name: spec.name,
    priceDelta: spec.priceDelta,
    stock: spec.stock,
    calories: spec.calories,
    isDefault: spec.isDefault ?? 0,
    sort: spec.sort ?? 0,
    status: spec.status ?? 1
  }));
  const defaultIndex = specs.findIndex(spec => spec.isDefault === 1);
  return {
    id: detail.id,
    categoryId: detail.categoryId,
    name: detail.name,
    subtitle: detail.subtitle ?? "",
    cover: detail.cover,
    detail: detail.detail ?? "",
    productType: detail.productType ?? 1,
    price: detail.price,
    originalPrice: detail.originalPrice,
    unit: detail.unit,
    stock: detail.stock,
    status: detail.status ?? 1,
    isRecommend: detail.isRecommend ?? 0,
    sort: detail.sort ?? 0,
    calories: detail.calories,
    protein: detail.protein,
    fat: detail.fat,
    carbohydrate: detail.carbohydrate,
    sodium: detail.sodium,
    nutritionDesc: detail.nutritionDesc ?? "",
    allergens: detail.allergens ?? [],
    storageType: detail.storageType,
    shelfLifeDays: detail.shelfLifeDays,
    producer: detail.producer ?? "",
    heatingMethod: detail.heatingMethod ?? "",
    healthTags: detail.healthTags ?? [],
    defaultSpecIndex: defaultIndex >= 0 ? defaultIndex : 0,
    specs,
    options: (detail.options ?? []).map(option => ({
      id: option.id,
      name: option.name,
      type: option.type,
      required: option.required ?? 0,
      sort: option.sort ?? 0,
      items: (option.items ?? []).map(item => ({
        id: item.id,
        name: item.name,
        priceDelta: item.priceDelta,
        calories: item.calories,
        sort: item.sort ?? 0,
        status: item.status ?? 1
      }))
    })),
    images: (detail.images ?? []).map(image => ({
      id: image.id,
      imageUrl: image.imageUrl,
      sort: image.sort ?? 0,
      status: image.status ?? 1
    }))
  };
}

/** 编辑表单 → ProductSaveRequest（默认规格行 isDefault=1，其余置 0） */
export function toSaveRequest(form: ProductFormInline): ProductSaveRequest {
  return {
    categoryId: form.categoryId,
    name: form.name,
    subtitle: form.subtitle || null,
    cover: form.cover,
    detail: form.detail || null,
    productType: form.productType,
    price: form.price,
    originalPrice: form.originalPrice ?? null,
    unit: form.unit,
    stock: form.stock,
    status: form.status,
    isRecommend: form.isRecommend,
    sort: form.sort,
    calories: form.calories,
    protein: form.protein,
    fat: form.fat,
    carbohydrate: form.carbohydrate,
    sodium: form.sodium,
    nutritionDesc: form.nutritionDesc || null,
    allergens: form.allergens,
    storageType: form.storageType,
    shelfLifeDays: form.shelfLifeDays,
    producer: form.producer || null,
    heatingMethod: form.heatingMethod || null,
    healthTags: form.healthTags,
    specs: form.specs.map((spec, index) => ({
      ...spec,
      isDefault: index === form.defaultSpecIndex ? 1 : 0
    })),
    options: form.options,
    images: form.images
  };
}
