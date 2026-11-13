import editForm from "../form.vue";
import { message } from "@/utils/message";
import { addDialog } from "@/components/ReDialog";
import { deviceDetection } from "@pureadmin/utils";
import {
  type CategoryVO,
  type CategorySaveRequest,
  getCategoryTree,
  createCategory,
  updateCategory,
  updateCategoryStatus,
  deleteCategory
} from "@/api/category";
import { ref, h, onMounted } from "vue";
import type { CategoryFormInline } from "./types";

export function useCategory() {
  const formRef = ref();
  const dataList = ref<CategoryVO[]>([]);
  const loading = ref(true);
  /** 正在提交状态变更的分类 id（用于开关 loading 与防重复提交） */
  const statusLoadingId = ref<number | null>(null);

  const columns: TableColumnList = [
    {
      label: "分类名称",
      prop: "name",
      align: "left",
      minWidth: 220
    },
    {
      label: "图标",
      prop: "icon",
      width: 90,
      formatter: ({ icon }) => icon || "-"
    },
    {
      label: "排序",
      prop: "sort",
      width: 80
    },
    {
      label: "状态",
      width: 90,
      slot: "status"
    },
    {
      label: "操作",
      fixed: "right",
      width: 240,
      slot: "operation"
    }
  ];

  /** 接口返回的部分节点 children 可能为 null，统一补齐为空数组便于树表渲染 */
  function normalizeTree(list: CategoryVO[]): CategoryVO[] {
    return list.map(item => ({
      ...item,
      children: item.children?.length ? normalizeTree(item.children) : []
    }));
  }

  function failText(error: unknown, fallback: string): string {
    return error instanceof Error && error.message ? error.message : fallback;
  }

  async function onSearch() {
    loading.value = true;
    try {
      const { code, message: msg, data } = await getCategoryTree();
      if (code !== 200) {
        message(msg || "分类树加载失败", { type: "error" });
        return;
      }
      dataList.value = normalizeTree(data ?? []);
    } catch (error) {
      message(failText(error, "分类树加载失败"), { type: "error" });
    } finally {
      loading.value = false;
    }
  }

  /** 状态开关：先乐观更新，失败回滚并提示后端 message */
  async function onStatusChange(
    row: CategoryVO,
    val: string | number | boolean
  ) {
    const next = Number(val);
    if (statusLoadingId.value !== null) return;
    const prev = row.status ?? 0;
    row.status = next;
    statusLoadingId.value = row.id;
    try {
      const { code, message: msg } = await updateCategoryStatus(row.id, next);
      if (code !== 200) throw new Error(msg || "状态更新失败");
      message("分类状态更新成功", { type: "success" });
    } catch (error) {
      row.status = prev;
      message(failText(error, "状态更新失败"), { type: "error" });
    } finally {
      statusLoadingId.value = null;
    }
  }

  /** 删除分类（40901 有子分类、40902 有商品，提示后端 message） */
  async function handleDelete(row: CategoryVO) {
    try {
      const { code, message: msg } = await deleteCategory(row.id);
      if (code !== 200) throw new Error(msg || "删除失败");
      message(`已删除分类「${row.name}」`, { type: "success" });
      await onSearch();
    } catch (error) {
      message(failText(error, "删除失败"), { type: "error" });
    }
  }

  /** 可选父分类：顶级 + 一级分类（接口最多两级，排除自身避免自引用） */
  function parentOptions(excludeId?: number) {
    const options: Array<{ label: string; value: number }> = [
      { label: "顶级分类", value: 0 }
    ];
    for (const item of dataList.value) {
      if (item.id === excludeId) continue;
      options.push({ label: item.name, value: item.id });
    }
    return options;
  }

  /**
   * 新增/编辑分类弹窗
   * @param title 操作名称
   * @param row 编辑时的目标行
   * @param presetParentId 新增子级时预设的父分类 id（仅一级分类可新增子级）
   */
  function openDialog(
    title = "新增",
    row?: CategoryVO,
    presetParentId?: number
  ) {
    const formInline: CategoryFormInline = {
      id: row?.id,
      parentId: presetParentId ?? row?.parentId ?? 0,
      name: row?.name ?? "",
      icon: row?.icon ?? "",
      sort: row?.sort ?? 0,
      status: row?.status ?? 1,
      hasChildren: Boolean(row?.children?.length),
      parentOptions: parentOptions(row?.id)
    };
    addDialog({
      title: `${title}分类`,
      props: { formInline },
      width: "45%",
      draggable: true,
      fullscreen: deviceDetection(),
      fullscreenIcon: true,
      closeOnClickModal: false,
      sureBtnLoading: true,
      // 表单数据由 ReDialog 的 v-bind="options.props" 注入，此处仅满足必填类型
      contentRenderer: () => h(editForm, { ref: formRef, formInline: null }),
      beforeSure: async (done, { options, closeLoading }) => {
        const formInstance = formRef.value?.getRef();
        if (!formInstance) return;
        try {
          await formInstance.validate();
        } catch {
          closeLoading();
          return;
        }
        const inline = options.props.formInline as CategoryFormInline;
        const payload: CategorySaveRequest = {
          parentId: inline.parentId,
          name: inline.name,
          icon: inline.icon || null,
          sort: inline.sort,
          status: inline.status
        };
        try {
          const { code, message: msg } = inline.id
            ? await updateCategory(inline.id, payload)
            : await createCategory(payload);
          if (code !== 200) throw new Error(msg || "保存失败");
          message(`已${title}分类「${inline.name}」`, { type: "success" });
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
  });

  return {
    loading,
    columns,
    dataList,
    statusLoadingId,
    onSearch,
    onStatusChange,
    handleDelete,
    openDialog
  };
}
