import { http } from "@/utils/http";

/** 后端统一响应结构 */
export interface CategoryResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/** 分类树节点（GET /api/admin/category/tree，最多两级）
 * 用 type 而非 interface 声明，使其具备隐式索引签名，可直接作为 el-cascader 的 options 传入 */
export type CategoryVO = {
  id: number;
  parentId: number;
  name: string;
  icon: string | null;
  sort: number | null;
  status: number | null;
  children?: CategoryVO[] | null;
};

/** 新增/编辑分类入参（POST/PUT /api/admin/category） */
export interface CategorySaveRequest {
  parentId: number;
  name: string;
  icon?: string | null;
  sort?: number | null;
  status?: number | null;
}

/** 获取分类树 */
export const getCategoryTree = () => {
  return http.request<CategoryResult<CategoryVO[]>>(
    "get",
    "/api/admin/category/tree"
  );
};

/** 新增分类 */
export const createCategory = (data: CategorySaveRequest) => {
  return http.request<CategoryResult<number>>("post", "/api/admin/category", {
    data
  });
};

/** 编辑分类 */
export const updateCategory = (id: number, data: CategorySaveRequest) => {
  return http.request<CategoryResult<null>>(
    "put",
    `/api/admin/category/${id}`,
    {
      data
    }
  );
};

/** 更新分类状态（0 下架、1 上架） */
export const updateCategoryStatus = (id: number, status: number) => {
  return http.request<CategoryResult<null>>(
    "put",
    `/api/admin/category/${id}/status`,
    { data: { status } }
  );
};

/** 删除分类（存在子分类/商品时返回 40901/40902） */
export const deleteCategory = (id: number) => {
  return http.request<CategoryResult<null>>(
    "delete",
    `/api/admin/category/${id}`
  );
};
