import { http } from "@/utils/http";

export interface DeliveryResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface DeliveryOrder {
  id: number;
  deliveryNo: string;
  orderNo: string;
  status: number;
  statusText: string;
  assignType: number;
  assignTypeText: string;
  deliveryManId: number | null;
  deliveryManName: string;
  deliveryManPhone: string;
  distance: number;
  freight: number;
  manFee: number;
  assignTime: string | null;
  pickupTime: string | null;
  receiveTime: string | null;
  createTime: string;
}

export interface DeliveryRider {
  id: number;
  realName: string;
  phone: string;
  onlineStatus: number;
  workStatus: number;
  maxOrderNum: number;
  inTransitCount: number;
  serviceAreaId: number | null;
  assignable: boolean;
}

export interface DeliveryPageQuery {
  current?: number;
  size?: number;
  status?: number;
  orderNo?: string;
}

export function getDeliveryPage(params: DeliveryPageQuery) {
  return http.request<DeliveryResult<PageResult<DeliveryOrder>>>(
    "get",
    "/api/admin/delivery/order/page",
    { params }
  );
}

export function getDeliveryRiders() {
  return http.request<DeliveryResult<DeliveryRider[]>>(
    "get",
    "/api/admin/delivery/man"
  );
}

export function assignDelivery(deliveryId: number, riderId: number) {
  return http.request<DeliveryResult<DeliveryOrder>>(
    "put",
    `/api/admin/delivery/order/${deliveryId}/assign`,
    { data: { riderId } }
  );
}
