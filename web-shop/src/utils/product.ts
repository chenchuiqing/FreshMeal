import type { ProductOption, ProductSpec, ShopProductDetail } from '@/api/modules/product';

export interface ProductOptionSelections {
  radio: Record<number, number | undefined>;
  checkbox: Record<number, number[]>;
}

/** 价格展示为保留两位小数的人民币文本。 */
export function formatPrice(value: number): string {
  return `¥${value.toFixed(2)}`;
}

/** 价格增量展示：0 返回空串，正数带 +，负数带 -。 */
export function formatDelta(value: number): string {
  if (value === 0) return '';
  return `${value > 0 ? '+' : '-'}${formatPrice(Math.abs(value))}`;
}

/**
 * 不可购买原因文案：下架优先，其次售罄；其余情况（如分类停用）按已下架提示。
 * available 为 true 时返回 null。
 */
export function resolveUnavailableText(detail: ShopProductDetail): string | null {
  if (detail.available) return null;
  if (detail.status === 0) return '商品已下架';
  if (detail.stock <= 0) return '商品已售罄';
  return '商品已下架';
}

/** 图集：仅取启用图片（顺序由后端按 sort 升序返回）；无启用图片时回退封面，封面也为空则返回空数组。 */
export function collectDetailGallery(detail: ShopProductDetail): string[] {
  const images = detail.images
    .filter((image) => image.status === 1)
    .map((image) => image.imageUrl);
  if (images.length > 0) return images;
  return detail.cover ? [detail.cover] : [];
}

/** 规格初始选中：启用规格中标记默认者，无默认则取第一个。 */
export function pickDefaultSpec(specs: ProductSpec[]): ProductSpec | undefined {
  const active = specs.filter((spec) => spec.status === 1);
  return active.find((spec) => spec.isDefault === 1) ?? active[0];
}

/** 必选但尚未选择的选项组名称列表，按传入顺序返回。 */
export function missingRequiredOptions(
  options: ProductOption[],
  selections: ProductOptionSelections,
): string[] {
  const missing: string[] = [];
  for (const group of options) {
    if (group.type !== 1 && group.type !== 2) continue;
    if (group.required !== 1) continue;
    const activeItems = group.items.filter((item) => item.status === 1);
    if (activeItems.length === 0) continue;
    const selected =
      group.type === 1
        ? selections.radio[group.id] !== undefined
        : (selections.checkbox[group.id]?.length ?? 0) > 0;
    if (!selected) missing.push(group.name);
  }
  return missing;
}
