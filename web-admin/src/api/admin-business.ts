import { http } from "@/utils/http";

export interface AdminResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface AdminPageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface AdminOrder {
  id: number;
  orderNo: string;
  userId: number;
  username: string | null;
  nickname: string | null;
  phone: string | null;
  status: number;
  statusText: string;
  payStatus: number;
  payType: number;
  deliveryType: number;
  goodsAmount: number;
  freightAmount: number;
  discountAmount: number;
  payAmount: number;
  receiver: string;
  receiverPhone: string;
  receiverAddress: string;
  deliverySlotName: string | null;
  deliverySlotDate: string | null;
  createTime: string;
  payTime: string | null;
  finishTime: string | null;
  cancelTime: string | null;
  cancelReason: string | null;
}

export interface AdminOrderItem {
  productId: number;
  productName: string;
  productCover: string | null;
  specName: string | null;
  optionsDesc: string | null;
  price: number;
  quantity: number;
  subtotal: number;
  calories: number;
}

export interface AdminOrderTimeline {
  fromStatus: number | null;
  toStatus: number;
  toStatusText: string;
  operatorName: string;
  remark: string | null;
  createTime: string;
}

export interface AdminOrderDetail {
  order: AdminOrder;
  items: AdminOrderItem[];
  timeline: AdminOrderTimeline[];
}

export interface AdminOrderPageQuery {
  current?: number;
  size?: number;
  orderNo?: string;
  username?: string;
  status?: number;
  startTime?: string;
  endTime?: string;
}

export interface AdminRider {
  id: number;
  username: string;
  realName: string;
  phone: string;
  serviceAreaId: number | null;
  serviceAreaName: string | null;
  onlineStatus: number;
  workStatus: number;
  maxOrderNum: number;
  inTransitCount: number;
  todayOrderCount: number;
  totalOrderCount: number;
  totalIncome: number;
  rating: number;
  status: number;
  assignable: boolean;
  createTime: string;
  updateTime: string;
}

export interface AdminRiderPageQuery {
  current?: number;
  size?: number;
  keyword?: string;
  status?: number;
  onlineStatus?: number;
}

export interface AdminRiderPayload {
  username: string;
  password?: string;
  realName: string;
  phone: string;
  maxOrderNum: number;
  serviceAreaId: number | null;
  status: number;
}

export interface DeliveryArea {
  id: number;
  name: string;
  city: string;
  district: string | null;
  baseFee: number;
  perKmFee: number;
  freeThreshold: number;
  nightSurcharge: number;
  sort: number;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface DeliveryAreaPayload {
  name: string;
  city: string;
  district: string | null;
  baseFee: number;
  perKmFee: number;
  freeThreshold: number;
  nightSurcharge: number;
  sort: number;
  status: number;
}

export interface DeliverySlot {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  orderDeadline: string;
  dailyCapacity: number;
  advanceDays: number;
  sort: number;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface DeliverySlotPayload {
  name: string;
  startTime: string;
  endTime: string;
  orderDeadline: string;
  dailyCapacity: number;
  advanceDays: number;
  sort: number;
  status: number;
}

export interface ProductBatch {
  id: number;
  productId: number;
  productName: string | null;
  batchNo: string;
  productionDate: string;
  expireDate: string;
  stock: number;
  lockStock: number;
  status: number;
  statusText: string;
  remark: string | null;
  createTime: string;
  updateTime: string;
}

export interface InventoryLog {
  id: number;
  productId: number;
  productName: string | null;
  batchId: number | null;
  batchNo: string | null;
  changeType: number;
  changeTypeText: string;
  changeNum: number;
  beforeStock: number;
  afterStock: number;
  refType: string | null;
  refNo: string | null;
  operatorId: number | null;
  remark: string | null;
  createTime: string;
}

export interface BatchPageQuery {
  current?: number;
  size?: number;
  productId?: number;
  batchNo?: string;
  status?: number;
}

export interface InventoryLogPageQuery {
  current?: number;
  size?: number;
  productId?: number;
  batchId?: number;
  changeType?: number;
  startTime?: string;
  endTime?: string;
}

export interface DispatchLog {
  id: number;
  deliveryOrderId: number;
  deliveryNo: string | null;
  orderNo: string | null;
  candidateId: number;
  candidateName: string;
  totalScore: number;
  distanceScore: number;
  loadScore: number;
  ratingScore: number;
  distanceValue: number;
  loadValue: number;
  isFinal: number;
  createTime: string;
}

export interface DashboardOverview {
  todayRevenue: number;
  todayOrderCount: number;
  todayUserCount: number;
  pendingDeliveryCount: number;
  finishedTodayCount: number;
}

export interface DashboardTrend {
  statDate: string;
  revenue: number;
  orderCount: number;
}

export interface DashboardRank {
  productId: number;
  productName: string;
  quantity: number;
  revenue: number;
}

export interface DashboardRegion {
  areaId: number;
  areaName: string;
  orderCount: number;
  revenue: number;
}

export interface AdminCoupon {
  id: number;
  name: string;
  type: number;
  threshold: number;
  amount: number;
  startTime: string;
  endTime: string;
  totalCount: number;
  receivedCount: number;
  usedCount: number;
  status: number;
}

export interface AdminCouponPayload {
  name: string;
  type: number;
  threshold: number;
  amount: number;
  startTime: string;
  endTime: string;
  totalCount: number;
}

export interface AdminReview {
  id: number;
  userId: number;
  username: string | null;
  nickname: string | null;
  orderId: number;
  orderNo: string | null;
  productId: number;
  productName: string;
  rating: number;
  content: string | null;
  images: string[];
  reply: string | null;
  createTime: string;
}

export interface AdminRefund {
  id: number;
  orderId: number;
  orderNo: string;
  userId: number;
  username: string | null;
  nickname: string | null;
  amount: number;
  reason: string;
  status: number;
  previousStatus: number;
  adminReply: string | null;
  createTime: string;
  updateTime: string;
}

export interface AdminBanner {
  id: number;
  title: string;
  image: string;
  link: string | null;
  sort: number;
  status: number;
  createTime: string;
}

export interface AdminBannerPayload {
  title: string;
  image: string;
  link?: string;
  sort?: number;
  status?: number;
}

export interface AdminNotice {
  id: number;
  title: string;
  content: string | null;
  type: number;
  status: number;
  createTime: string;
}

export interface AdminNoticePayload {
  title: string;
  content?: string;
  type?: number;
  status?: number;
}

const request = <T>(
  method: "get" | "post" | "put" | "delete",
  url: string,
  data?: object
) => http.request<AdminResult<T>>(method, url, data ? { data } : undefined);

export const getAdminOrderPage = (params: AdminOrderPageQuery) =>
  http.request<AdminResult<AdminPageResult<AdminOrder>>>(
    "get",
    "/api/admin/order/page",
    { params }
  );

export const getAdminOrderDetail = (orderNo: string) =>
  request<AdminOrderDetail>("get", `/api/admin/order/${orderNo}`);

export const updateAdminOrderStatus = (
  orderNo: string,
  targetStatus: number,
  reason?: string
) =>
  request<AdminOrder>("put", `/api/admin/order/${orderNo}/status`, {
    targetStatus,
    reason
  });

export const getAdminRiderList = () =>
  request<AdminRider[]>("get", "/api/admin/delivery/man");

export const getAdminRiderPage = (params: AdminRiderPageQuery) =>
  http.request<AdminResult<AdminPageResult<AdminRider>>>(
    "get",
    "/api/admin/delivery/man/page",
    { params }
  );

export const createAdminRider = (data: AdminRiderPayload) =>
  request<AdminRider>("post", "/api/admin/delivery/man", data);

export const updateAdminRider = (id: number, data: AdminRiderPayload) =>
  request<AdminRider>("put", `/api/admin/delivery/man/${id}`, data);

export const updateAdminRiderStatus = (id: number, status: number) =>
  request<AdminRider>("put", `/api/admin/delivery/man/${id}/status`, {
    status
  });

export const updateAdminRiderWork = (
  id: number,
  onlineStatus: number,
  workStatus: number
) =>
  request<AdminRider>("put", `/api/admin/delivery/man/${id}/work`, {
    onlineStatus,
    workStatus
  });

export const getDeliveryAreas = () =>
  request<DeliveryArea[]>("get", "/api/admin/delivery/area");

export const createDeliveryArea = (data: DeliveryAreaPayload) =>
  request<DeliveryArea>("post", "/api/admin/delivery/area", data);

export const updateDeliveryArea = (id: number, data: DeliveryAreaPayload) =>
  request<DeliveryArea>("put", `/api/admin/delivery/area/${id}`, data);

export const getDeliverySlots = () =>
  request<DeliverySlot[]>("get", "/api/admin/delivery/slot");

export const createDeliverySlot = (data: DeliverySlotPayload) =>
  request<DeliverySlot>("post", "/api/admin/delivery/slot", data);

export const updateDeliverySlot = (id: number, data: DeliverySlotPayload) =>
  request<DeliverySlot>("put", `/api/admin/delivery/slot/${id}`, data);

export const getProductBatches = (params: BatchPageQuery) =>
  http.request<AdminResult<AdminPageResult<ProductBatch>>>(
    "get",
    "/api/admin/batch/page",
    { params }
  );

export const getExpireWarnings = (days = 3) =>
  http.request<AdminResult<ProductBatch[]>>(
    "get",
    "/api/admin/batch/expire-warning",
    { params: { days } }
  );

export const createProductBatch = (data: {
  productId: number;
  batchNo: string;
  productionDate: string;
  expireDate: string;
  stock: number;
  lockStock: number;
  remark?: string;
}) => request<ProductBatch>("post", "/api/admin/batch", data);

export const reportBatchLoss = (id: number, quantity: number, remark: string) =>
  request<ProductBatch>("put", `/api/admin/batch/${id}/loss`, {
    quantity,
    remark
  });

export const getInventoryLogs = (params: InventoryLogPageQuery) =>
  http.request<AdminResult<AdminPageResult<InventoryLog>>>(
    "get",
    "/api/admin/inventory/log",
    { params }
  );

export const getDispatchLogs = (params: {
  current?: number;
  size?: number;
  deliveryNo?: string;
  isFinal?: number;
}) =>
  http.request<AdminResult<AdminPageResult<DispatchLog>>>(
    "get",
    "/api/admin/dispatch/log/page",
    { params }
  );

export const getDashboardOverview = () =>
  request<DashboardOverview>("get", "/api/admin/dashboard/overview");

export const getDashboardTrend = (days = 7) =>
  http.request<AdminResult<DashboardTrend[]>>(
    "get",
    "/api/admin/dashboard/trend",
    { params: { days } }
  );

export const getDashboardRank = (limit = 10) =>
  http.request<AdminResult<DashboardRank[]>>(
    "get",
    "/api/admin/dashboard/rank",
    { params: { limit } }
  );

export const getDashboardRegion = () =>
  request<DashboardRegion[]>("get", "/api/admin/dashboard/region");

export const getAdminCoupons = () =>
  http.request<AdminResult<AdminPageResult<AdminCoupon>>>(
    "get",
    "/api/admin/coupon/page"
  );

export const createAdminCoupon = (data: AdminCouponPayload) =>
  request<number>("post", "/api/admin/coupon", data);

export const updateAdminCouponStatus = (id: number, status: number) =>
  request<void>("put", `/api/admin/coupon/${id}/status`, { status });

export const issueAdminCoupon = (id: number, userId: number) =>
  request<void>("post", `/api/admin/coupon/${id}/issue`, { userId });

export const getAdminReviews = (params: {
  current?: number;
  size?: number;
  orderNo?: string;
  rating?: number;
}) =>
  http.request<AdminResult<AdminPageResult<AdminReview>>>(
    "get",
    "/api/admin/review/page",
    { params }
  );

export const replyAdminReview = (id: number, reply: string) =>
  request<void>("put", `/api/admin/review/${id}/reply`, { reply });

export const deleteAdminReview = (id: number) =>
  request<void>("delete", `/api/admin/review/${id}`);

export const getAdminBanners = () =>
  http.request<AdminResult<AdminPageResult<AdminBanner>>>(
    "get",
    "/api/admin/content/banner"
  );

export const createAdminBanner = (data: AdminBannerPayload) =>
  request<number>("post", "/api/admin/content/banner", data);

export const updateAdminBanner = (id: number, data: AdminBannerPayload) =>
  request<void>("put", `/api/admin/content/banner/${id}`, data);

export const deleteAdminBanner = (id: number) =>
  request<void>("delete", `/api/admin/content/banner/${id}`);

export const getAdminNotices = () =>
  http.request<AdminResult<AdminPageResult<AdminNotice>>>(
    "get",
    "/api/admin/content/notice"
  );

export const createAdminNotice = (data: AdminNoticePayload) =>
  request<number>("post", "/api/admin/content/notice", data);

export const updateAdminNotice = (id: number, data: AdminNoticePayload) =>
  request<void>("put", `/api/admin/content/notice/${id}`, data);

export const deleteAdminNotice = (id: number) =>
  request<void>("delete", `/api/admin/content/notice/${id}`);

export const getAdminRefunds = (params: {
  current?: number;
  size?: number;
  status?: number;
}) =>
  http.request<AdminResult<AdminPageResult<AdminRefund>>>(
    "get",
    "/api/admin/refund/page",
    { params }
  );

export const decideAdminRefund = (
  id: number,
  status: number,
  adminReply?: string
) =>
  request<void>("put", `/api/admin/refund/${id}/decision`, {
    status,
    adminReply
  });

export const exportAdminOrders = (params: {
  startTime?: string;
  endTime?: string;
}) =>
  http.request<Blob>(
    "get",
    "/api/admin/report/orders.xlsx",
    { params },
    { responseType: "blob" }
  );
