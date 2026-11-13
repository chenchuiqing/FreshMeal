import editForm from "../form/index.vue";
import dayjs from "dayjs";
import { message } from "@/utils/message";
import { addDialog } from "@/components/ReDialog";
import { deviceDetection } from "@pureadmin/utils";
import type { PaginationProps } from "@pureadmin/table";
import {
  type AdminProductVO,
  getProductPage,
  getProductDetail,
  createProduct,
  updateProduct,
  updateProductStatus,
  updateProductRecommend,
  deleteProduct
} from "@/api/product";
import { type CategoryVO, getCategoryTree } from "@/api/category";
import { reactive, ref, h, onMounted } from "vue";
import type { ProductFormInline } from "./types";
import { createDefaultForm, mapDetailToForm, toSaveRequest } from "./form";

/** 顶层字段中归属「营养与储存」页签的字段名（校验失败时据此自动切换页签） */
const NUTRITION_FIELDS = new Set([
  "calories",
  "protein",
  "fat",
  "carbohydrate",
  "sodium",
  "nutritionDesc",
  "allergens",
  "storageType",
  "shelfLifeDays",
  "producer",
  "heatingMethod",
  "healthTags"
]);

type TabName = "base" | "nutrition" | "spec" | "option" | "image";

export function useProduct() {
  const formRef = ref();
  const form = reactive({
    name: "",
    categoryId: null as number | null,
    status: null as number | null,
    productType: null as number | null
  });
  const dataList = ref<AdminProductVO[]>([]);
  const categoryTree = ref<CategoryVO[]>([]);
  const loading = ref(true);
  /** 正在提交状态变更的商品 id，用于开关 loading 与防重复提交 */
  const statusLoadingId = ref<number | null>(null);
  const recommendLoadingId = ref<number | null>(null);

  const pagination = reactive<PaginationProps>({
    total: 0,
    pageSize: 10,
    currentPage: 1,
    background: true
  });

  const columns: TableColumnList = [
    {
      label: "封面",
      prop: "cover",
      width: 80,
      slot: "cover"
    },
    {
      label: "商品名称",
      prop: "name",
      align: "left",
      minWidth: 180,
      showOverflowTooltip: true
    },
    {
      label: "分类",
      prop: "categoryName",
      width: 110,
      formatter: ({ categoryName }) => categoryName || "-"
    },
    {
      label: "售价",
      prop: "price",
      width: 100,
      formatter: ({ price }) => `¥${Number(price).toFixed(2)}`
    },
    {
      label: "库存",
      prop: "stock",
      width: 80
    },
    {
      label: "销量",
      prop: "salesCount",
      width: 80
    },
    {
      label: "热量",
      prop: "calories",
      width: 90,
      formatter: ({ calories }) => `${calories} kcal`
    },
    {
      label: "健康标签",
      prop: "healthTags",
      minWidth: 160,
      slot: "healthTags"
    },
    {
      label: "状态",
      width: 90,
      slot: "status"
    },
    {
      label: "推荐",
      width: 90,
      slot: "recommend"
    },
    {
      label: "排序",
      prop: "sort",
      width: 70
    },
    {
      label: "更新时间",
      prop: "updateTime",
      width: 170,
      formatter: ({ updateTime }) =>
        updateTime ? dayjs(updateTime).format("YYYY-MM-DD HH:mm:ss") : "-"
    },
    {
      label: "操作",
      fixed: "right",
      width: 160,
      slot: "operation"
    }
  ];

  function failText(error: unknown, fallback: string): string {
    return error instanceof Error && error.message ? error.message : fallback;
  }

  /** 分类树节点补齐空 children，供级联面板稳定渲染 */
  function normalizeTree(list: CategoryVO[]): CategoryVO[] {
    return list.map(item => ({
      ...item,
      children: item.children?.length ? normalizeTree(item.children) : []
    }));
  }

  async function loadCategoryTree() {
    try {
      const { code, message: msg, data } = await getCategoryTree();
      if (code !== 200) {
        message(msg || "分类树加载失败", { type: "error" });
        return;
      }
      categoryTree.value = normalizeTree(data ?? []);
    } catch (error) {
      message(failText(error, "分类树加载失败"), { type: "error" });
    }
  }

  async function onSearch(resetPage = false) {
    if (resetPage) pagination.currentPage = 1;
    loading.value = true;
    try {
      const {
        code,
        message: msg,
        data
      } = await getProductPage({
        current: pagination.currentPage,
        size: pagination.pageSize,
        name: form.name || undefined,
        categoryId: form.categoryId ?? undefined,
        status: form.status ?? undefined,
        productType: form.productType ?? undefined
      });
      if (code !== 200 || !data) {
        message(msg || "商品列表加载失败", { type: "error" });
        return;
      }
      dataList.value = data.records;
      pagination.total = data.total;
      pagination.currentPage = data.current;
      pagination.pageSize = data.size;
    } catch (error) {
      message(failText(error, "商品列表加载失败"), { type: "error" });
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    onSearch(true);
  }

  function resetForm(formEl) {
    if (!formEl) return;
    formEl.resetFields();
    handleSearch();
  }

  function handleSizeChange(size: number) {
    pagination.pageSize = size;
    pagination.currentPage = 1;
    onSearch();
  }

  function handleCurrentChange(current: number) {
    pagination.currentPage = current;
    onSearch();
  }

  /** 上下架开关：先乐观更新，失败回滚并提示后端 message */
  async function onStatusChange(
    row: AdminProductVO,
    val: string | number | boolean
  ) {
    const next = Number(val);
    if (statusLoadingId.value !== null) return;
    const prev = row.status ?? 0;
    row.status = next;
    statusLoadingId.value = row.id;
    try {
      const { code, message: msg } = await updateProductStatus(row.id, next);
      if (code !== 200) throw new Error(msg || "状态更新失败");
      message("商品状态更新成功", { type: "success" });
    } catch (error) {
      row.status = prev;
      message(failText(error, "状态更新失败"), { type: "error" });
    } finally {
      statusLoadingId.value = null;
    }
  }

  /** 推荐开关：先乐观更新，失败回滚并提示后端 message */
  async function onRecommendChange(
    row: AdminProductVO,
    val: string | number | boolean
  ) {
    const next = Number(val);
    if (recommendLoadingId.value !== null) return;
    const prev = row.isRecommend ?? 0;
    row.isRecommend = next;
    recommendLoadingId.value = row.id;
    try {
      const { code, message: msg } = await updateProductRecommend(row.id, next);
      if (code !== 200) throw new Error(msg || "推荐更新失败");
      message("商品推荐更新成功", { type: "success" });
    } catch (error) {
      row.isRecommend = prev;
      message(failText(error, "推荐更新失败"), { type: "error" });
    } finally {
      recommendLoadingId.value = null;
    }
  }

  async function handleDelete(row: AdminProductVO) {
    try {
      const { code, message: msg } = await deleteProduct(row.id);
      if (code !== 200) throw new Error(msg || "删除失败");
      message(`已删除商品「${row.name}」`, { type: "success" });
      await onSearch();
    } catch (error) {
      message(failText(error, "删除失败"), { type: "error" });
    }
  }

  /** 根据首个校验失败字段定位到对应表单页签 */
  function locateTab(field: string): TabName {
    if (field.startsWith("specs")) return "spec";
    if (field.startsWith("options")) return "option";
    if (field.startsWith("images")) return "image";
    if (NUTRITION_FIELDS.has(field)) return "nutrition";
    return "base";
  }

  function focusInvalidTab(fields?: Record<string, unknown>) {
    const first = fields ? Object.keys(fields)[0] : undefined;
    if (!first) return;
    formRef.value?.setActiveTab?.(locateTab(first));
  }

  /** 新增/编辑商品弹窗（编辑前拉取详情全量回填） */
  async function openDialog(title = "新增", row?: AdminProductVO) {
    if (!categoryTree.value.length) await loadCategoryTree();
    let formInline: ProductFormInline = createDefaultForm();
    if (row) {
      try {
        const { code, message: msg, data } = await getProductDetail(row.id);
        if (code !== 200 || !data) throw new Error(msg || "商品详情加载失败");
        formInline = mapDetailToForm(data);
      } catch (error) {
        message(failText(error, "商品详情加载失败"), { type: "error" });
        return;
      }
    }
    addDialog({
      title: `${title}商品`,
      props: { formInline, categoryTree: categoryTree.value },
      width: "70%",
      draggable: true,
      fullscreen: deviceDetection(),
      fullscreenIcon: true,
      closeOnClickModal: false,
      sureBtnLoading: true,
      // 表单数据与分类树由 ReDialog 的 v-bind="options.props" 注入，此处仅满足必填类型
      contentRenderer: () =>
        h(editForm, { ref: formRef, formInline: null, categoryTree: [] }),
      beforeSure: async (done, { options, closeLoading }) => {
        const formInstance = formRef.value?.getRef();
        if (!formInstance) return;
        const inline = options.props.formInline as ProductFormInline;
        if (!inline.specs.length) {
          closeLoading();
          formRef.value?.setActiveTab?.("spec");
          message("商品至少包含一个规格", { type: "error" });
          return;
        }
        try {
          await formInstance.validate();
        } catch (fields) {
          closeLoading();
          if (fields instanceof Error) {
            message(fields.message, { type: "error" });
            return;
          }
          focusInvalidTab(fields);
          return;
        }
        try {
          const { code, message: msg } = inline.id
            ? await updateProduct(inline.id, toSaveRequest(inline))
            : await createProduct(toSaveRequest(inline));
          if (code !== 200) throw new Error(msg || "保存失败");
          message(`已${title}商品「${inline.name}」`, { type: "success" });
          done();
          await onSearch();
        } catch (error) {
          closeLoading();
          message(failText(error, "保存失败"), { type: "error" });
        }
      }
    });
  }

  onMounted(() => {
    onSearch();
    loadCategoryTree();
  });

  return {
    form,
    loading,
    columns,
    dataList,
    categoryTree,
    pagination,
    statusLoadingId,
    recommendLoadingId,
    onSearch,
    handleSearch,
    resetForm,
    handleSizeChange,
    handleCurrentChange,
    onStatusChange,
    onRecommendChange,
    handleDelete,
    openDialog
  };
}
