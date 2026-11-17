import { apiClient } from '@/api/client';

export interface RiderDelivery {
  id: number;
  deliveryNo: string;
  orderNo: string;
  status: number;
  statusText: string;
  assignType: number;
  receiver: string;
  receiverPhone: string;
  receiverAddress: string;
  distance: number;
  freight: number;
  manFee: number;
  slotStart: string | null;
  createTime: string;
  assignTime: string | null;
  pickupTime: string | null;
  receiveTime: string | null;
}

export interface RiderStatistics {
  todayOrderCount: number;
  totalOrderCount: number;
  activeOrderCount: number;
  totalIncome: number;
  todayIncome: number;
  rating: number;
  onlineStatus: number;
  workStatus: number;
}

export interface RiderWorkStatus {
  onlineStatus: number;
  workStatus: number;
}

export function getTodoTasks(signal?: AbortSignal): Promise<RiderDelivery[]> {
  return apiClient.get<RiderDelivery[]>('/rider/task/todo', { signal });
}

export function getMyTasks(signal?: AbortSignal): Promise<RiderDelivery[]> {
  return apiClient.get<RiderDelivery[]>('/rider/task/my', { signal });
}

export function grabTask(
  deliveryNo: string,
  signal?: AbortSignal,
): Promise<RiderDelivery> {
  return apiClient.post<RiderDelivery, undefined>(
    `/rider/task/${deliveryNo}/grab`,
    undefined,
    { signal },
  );
}

export function pickupTask(
  deliveryNo: string,
  signal?: AbortSignal,
): Promise<RiderDelivery> {
  return apiClient.put<RiderDelivery, undefined>(
    `/rider/task/${deliveryNo}/pickup`,
    undefined,
    { signal },
  );
}

export function deliverTask(
  deliveryNo: string,
  signal?: AbortSignal,
): Promise<RiderDelivery> {
  return apiClient.put<RiderDelivery, undefined>(
    `/rider/task/${deliveryNo}/deliver`,
    undefined,
    { signal },
  );
}

export function getStatistics(signal?: AbortSignal): Promise<RiderStatistics> {
  return apiClient.get<RiderStatistics>('/rider/task/statistics', { signal });
}

export function updateWorkStatus(
  params: RiderWorkStatus,
  signal?: AbortSignal,
): Promise<RiderWorkStatus> {
  return apiClient.put<RiderWorkStatus, RiderWorkStatus>(
    '/rider/profile/work-status',
    params,
    { signal },
  );
}
