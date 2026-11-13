import { describe, expect, it } from 'vitest';
import type {
  ProductImage,
  ProductOption,
  ProductOptionItem,
  ProductSpec,
  ShopProductDetail,
} from '@/api/modules/product';
import {
  collectDetailGallery,
  formatDelta,
  formatPrice,
  missingRequiredOptions,
  pickDefaultSpec,
  resolveUnavailableText,
} from '@/utils/product';

function createDetail(
  overrides: Partial<ShopProductDetail> = {},
): ShopProductDetail {
  return {
    id: 101,
    categoryId: 11,
    categoryName: '蛋白沙拉',
    name: '香煎鸡胸沙拉',
    subtitle: '低脂高蛋白，饱腹无负担',
    cover: '/api/files/products/p101.svg',
    detail: '低温慢煎鸡胸配混合生菜。',
    productType: 1,
    price: 28,
    originalPrice: 34,
    unit: '份',
    stock: 150,
    salesCount: 320,
    status: 1,
    calories: 352,
    protein: 38.5,
    fat: 12.3,
    carbohydrate: 28.6,
    sodium: 420,
    nutritionDesc: '蛋白质 38.5g',
    allergens: ['鸡蛋'],
    storageType: 2,
    storageLabel: '0-4℃ 冷藏',
    shelfLifeDays: 3,
    producer: 'FreshMeal 中央厨房',
    heatingMethod: '开盖即食',
    healthTags: ['低卡', '高蛋白'],
    available: true,
    specs: [],
    options: [],
    images: [],
    ...overrides,
  };
}

function createSpec(overrides: Partial<ProductSpec> = {}): ProductSpec {
  return {
    id: 1001,
    name: '标准份',
    priceDelta: 0,
    stock: 150,
    calories: 352,
    isDefault: 1,
    sort: 0,
    status: 1,
    ...overrides,
  };
}

function createImage(overrides: Partial<ProductImage> = {}): ProductImage {
  return { id: 4001, imageUrl: '/a.svg', sort: 0, status: 1, ...overrides };
}

function createItem(overrides: Partial<ProductOptionItem> = {}): ProductOptionItem {
  return {
    id: 3001,
    name: '加溏心蛋',
    priceDelta: 3,
    calories: 78,
    sort: 0,
    status: 1,
    ...overrides,
  };
}

function createOption(overrides: Partial<ProductOption> = {}): ProductOption {
  return {
    id: 2001,
    name: '加料站',
    type: 2,
    required: 0,
    sort: 10,
    items: [],
    ...overrides,
  };
}

describe('shop product display helpers', () => {
  it('formats prices with two decimals', () => {
    expect(formatPrice(28)).toBe('¥28.00');
    expect(formatPrice(19.9)).toBe('¥19.90');
  });

  it('formats price deltas with an explicit sign', () => {
    expect(formatDelta(0)).toBe('');
    expect(formatDelta(3)).toBe('+¥3.00');
    expect(formatDelta(-2.5)).toBe('-¥2.50');
  });

  it('distinguishes off-shelf and sold-out texts for unavailable products', () => {
    expect(resolveUnavailableText(createDetail({ available: true }))).toBeNull();
    expect(
      resolveUnavailableText(
        createDetail({ available: false, status: 0, stock: 10 }),
      ),
    ).toBe('商品已下架');
    expect(
      resolveUnavailableText(
        createDetail({ available: false, status: 1, stock: 0 }),
      ),
    ).toBe('商品已售罄');
    // 分类停用等其余不可购买场景按已下架提示
    expect(
      resolveUnavailableText(
        createDetail({ available: false, status: 1, stock: 5 }),
      ),
    ).toBe('商品已下架');
  });

  it('collects active gallery images in backend order with cover fallback', () => {
    const detail = createDetail({
      images: [
        createImage({ id: 1, imageUrl: '/a.svg', sort: 0 }),
        createImage({ id: 2, imageUrl: '/b.svg', sort: 1, status: 0 }),
        createImage({ id: 3, imageUrl: '/c.svg', sort: 2 }),
      ],
    });
    expect(collectDetailGallery(detail)).toEqual(['/a.svg', '/c.svg']);

    expect(
      collectDetailGallery(
        createDetail({ images: [createImage({ status: 0 })] }),
      ),
    ).toEqual(['/api/files/products/p101.svg']);
    expect(
      collectDetailGallery(createDetail({ cover: '', images: [] })),
    ).toEqual([]);
  });

  it('picks the default spec among active specs only', () => {
    expect(
      pickDefaultSpec([
        createSpec({ id: 1, name: '停用默认', isDefault: 1, status: 0 }),
        createSpec({ id: 2, name: '标准份', isDefault: 0 }),
        createSpec({ id: 3, name: '大份', isDefault: 1 }),
      ])?.id,
    ).toBe(3);

    expect(
      pickDefaultSpec([
        createSpec({ id: 4, isDefault: 0 }),
        createSpec({ id: 5, isDefault: 0 }),
      ])?.id,
    ).toBe(4);

    expect(pickDefaultSpec([createSpec({ status: 0 })])).toBeUndefined();
    expect(pickDefaultSpec([])).toBeUndefined();
  });

  it('lists required option groups that have no selection yet', () => {
    const requiredSingle = createOption({
      id: 1,
      name: '主食选择',
      type: 1,
      required: 1,
      items: [createItem({ id: 11 })],
    });
    const requiredMulti = createOption({
      id: 2,
      name: '加料站',
      type: 2,
      required: 1,
      items: [createItem({ id: 21 })],
    });
    const optional = createOption({
      id: 3,
      name: '饮品搭配',
      type: 1,
      required: 0,
      items: [createItem({ id: 31 })],
    });
    const noActiveItems = createOption({
      id: 4,
      name: '停用组',
      type: 1,
      required: 1,
      items: [createItem({ id: 41, status: 0 })],
    });
    const options = [requiredSingle, requiredMulti, optional, noActiveItems];

    expect(
      missingRequiredOptions(options, { radio: {}, checkbox: {} }),
    ).toEqual(['主食选择', '加料站']);

    expect(
      missingRequiredOptions(options, {
        radio: { 1: 11 },
        checkbox: { 2: [21] },
      }),
    ).toEqual([]);

    expect(
      missingRequiredOptions([requiredMulti], {
        radio: {},
        checkbox: { 2: [] },
      }),
    ).toEqual(['加料站']);
  });
});
