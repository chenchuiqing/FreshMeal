<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  getAdminOrderDetail,
  getAdminOrderPage,
  updateAdminOrderStatus,
  exportAdminOrders
} from "@/api/admin-business";
import type { AdminOrder, AdminOrderDetail } from "@/api/admin-business";

defineOptions({ name: "CommerceOrder" });

const loading = ref(false);
const rows = ref<AdminOrder[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<AdminOrderDetail | null>(null);
const actionVisible = ref(false);
const actionLoading = ref(false);
const selectedOrder = ref<AdminOrder | null>(null);
const targetStatus = ref<number | null>(null);
const reason = ref("");
const exporting = ref(false);

const form = reactive({
  current: 1,
  size: 10,
  orderNo: "",
  username: "",
  status: undefined as number | undefined,
  dateRange: [] as string[]
});

const statusOptions = [
  { label: "待支付", value: 0 },
  { label: "待接单", value: 1 },
  { label: "备餐中", value: 2 },
  { label: "待配送时段", value: 3 },
  { label: "待配送", value: 4 },
  { label: "配送中", value: 5 },
  { label: "已完成", value: 6 },
  { label: "退款中", value: 7 },
  { label: "已退款", value: 8 },
  { label: "已取消", value: 9 }
];

const transitions: Record<number, number[]> = {
  0: [1, 9],
  1: [2, 9],
  2: [3, 4],
  3: [4],
  4: [5, 9],
  5: [6],
  6: [],
  7: [],
  8: [],
  9: []
};

const statusTagType = (status: number) => {
  if (status === 6) return "success";
  if (status === 9 || status === 8) return "danger";
  if (status === 0 || status === 3 || status === 4) return "warning";
  return "primary";
};

const money = (value: number | null | undefined) =>
  Number(value ?? 0).toFixed(2);

async function loadPage() {
  loading.value = true;
  try {
    const result = await getAdminOrderPage({
      current: form.current,
      size: form.size,
      orderNo: form.orderNo.trim() || undefined,
      username: form.username.trim() || undefined,
      status: form.status,
      startTime: form.dateRange[0],
      endTime: form.dateRange[1]
        ? `${form.dateRange[1].slice(0, 10)}T23:59:59`
        : undefined
    });
    rows.value = result.data.records;
    total.value = result.data.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "订单加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  form.current = 1;
  void loadPage();
}

function reset() {
  form.orderNo = "";
  form.username = "";
  form.status = undefined;
  form.dateRange = [];
  search();
}

async function exportReport() {
  exporting.value = true;
  try {
    const blob = await exportAdminOrders({
      startTime: form.dateRange[0]
        ? `${form.dateRange[0]}T00:00:00`
        : undefined,
      endTime: form.dateRange[1]
        ? `${form.dateRange[1]}T23:59:59`
        : undefined
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "FreshMeal-订单报表.xlsx";
    anchor.click();
    URL.revokeObjectURL(url);
    ElMessage.success("订单报表已导出");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "报表导出失败");
  } finally {
    exporting.value = false;
  }
}

function changeCurrent(current: number) {
  form.current = current;
  void loadPage();
}

function changeSize(size: number) {
  form.size = size;
  form.current = 1;
  void loadPage();
}

async function openDetail(row: AdminOrder) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const result = await getAdminOrderDetail(row.orderNo);
    detail.value = result.data;
  } catch (error) {
    detailVisible.value = false;
    ElMessage.error(
      error instanceof Error ? error.message : "订单详情加载失败"
    );
  } finally {
    detailLoading.value = false;
  }
}

function openAction(row: AdminOrder) {
  selectedOrder.value = row;
  targetStatus.value = transitions[row.status][0] ?? null;
  reason.value = "";
  actionVisible.value = true;
}

async function confirmAction() {
  if (!selectedOrder.value || targetStatus.value === null) return;
  if (targetStatus.value === 9 && !reason.value.trim()) {
    ElMessage.warning("请填写取消原因");
    return;
  }
  actionLoading.value = true;
  try {
    await updateAdminOrderStatus(
      selectedOrder.value.orderNo,
      targetStatus.value,
      reason.value.trim() || undefined
    );
    ElMessage.success("订单状态已更新");
    actionVisible.value = false;
    await loadPage();
    if (detailVisible.value && detail.value) {
      await openDetail(selectedOrder.value);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "状态更新失败");
  } finally {
    actionLoading.value = false;
  }
}

onMounted(loadPage);
</script>

<template>
  <div class="main">
    <el-form :inline="true" class="search-form">
      <el-form-item label="订单号：">
        <el-input
          v-model="form.orderNo"
          placeholder="请输入订单号"
          clearable
          class="w-50"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="用户：">
        <el-input
          v-model="form.username"
          placeholder="账号/昵称/手机号"
          clearable
          class="w-44"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="状态：">
        <el-select
          v-model="form.status"
          placeholder="全部"
          clearable
          class="w-36"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间：">
        <el-date-picker
          v-model="form.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          class="w-70!"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="search">
          搜索
        </el-button>
        <el-button @click="reset">重置</el-button>
        <el-button :loading="exporting" @click="exportReport">
          导出Excel
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" row-key="id" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="185" />
      <el-table-column label="用户" min-width="150">
        <template #default="{ row }">
          <div>{{ row.nickname || row.username || "未知用户" }}</div>
          <div class="text-xs text-gray-400">{{ row.phone || "" }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ row.statusText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配送方式" width="100">
        <template #default="{ row }">
          {{ row.deliveryType === 2 ? "到店自提" : "外卖配送" }}
        </template>
      </el-table-column>
      <el-table-column label="实付金额" width="110" align="right">
        <template #default="{ row }">¥{{ money(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column prop="receiver" label="收货人" width="100" />
      <el-table-column prop="createTime" label="下单时间" min-width="165" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as AdminOrder)">
            详情
          </el-button>
          <el-button
            link
            type="warning"
            :disabled="transitions[row.status].length === 0"
            @click="openAction(row as AdminOrder)"
          >
            状态干预
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="form.current"
      v-model:page-size="form.size"
      class="pagination"
      background
      layout="total, sizes, prev, pager, next"
      :total="total"
      @current-change="changeCurrent"
      @size-change="changeSize"
    />

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="860px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="订单号">
              {{ detail.order.orderNo }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(detail.order.status)">
                {{ detail.order.statusText }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">
              {{ detail.order.createTime }}
            </el-descriptions-item>
            <el-descriptions-item label="用户">
              {{ detail.order.nickname || detail.order.username }}
            </el-descriptions-item>
            <el-descriptions-item label="收货人">
              {{ detail.order.receiver }} {{ detail.order.receiverPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="配送时段">
              {{
                detail.order.deliveryType === 1 &&
                !detail.order.deliverySlotDate
                  ? "立即配送"
                  : detail.order.deliverySlotDate
                    ? `${detail.order.deliverySlotDate} ${detail.order.deliverySlotName}`
                    : "未预约"
              }}
            </el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="3">
              {{ detail.order.receiverAddress }}
            </el-descriptions-item>
            <el-descriptions-item label="商品金额">
              ¥{{ money(detail.order.goodsAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="配送费">
              ¥{{ money(detail.order.freightAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">
              ¥{{ money(detail.order.payAmount) }}
            </el-descriptions-item>
          </el-descriptions>

          <h3 class="section-title">商品明细</h3>
          <el-table :data="detail.items" border size="small">
            <el-table-column prop="productName" label="商品" min-width="180" />
            <el-table-column prop="specName" label="规格" width="110" />
            <el-table-column prop="optionsDesc" label="选项" min-width="130" />
            <el-table-column label="单价" width="90" align="right">
              <template #default="{ row }">¥{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="70" />
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">¥{{ money(row.subtotal) }}</template>
            </el-table-column>
          </el-table>

          <h3 class="section-title">状态时间轴</h3>
          <el-timeline>
            <el-timeline-item
              v-for="item in detail.timeline"
              :key="item.createTime"
              :timestamp="item.createTime"
              placement="top"
            >
              <el-card shadow="never">
                <strong>{{ item.toStatusText }}</strong>
                <span class="ml-2 text-gray-500">{{ item.operatorName }}</span>
                <p v-if="item.remark" class="text-sm text-gray-500">
                  {{ item.remark }}
                </p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>
    </el-dialog>

    <el-dialog
      v-model="actionVisible"
      title="订单状态干预"
      width="480px"
      destroy-on-close
    >
      <el-alert
        v-if="selectedOrder"
        type="info"
        :closable="false"
        :title="`订单 ${selectedOrder.orderNo}：${selectedOrder.statusText}`"
        class="mb-4"
      />
      <el-form label-width="90px">
        <el-form-item label="目标状态：">
          <el-select v-model="targetStatus" placeholder="请选择" class="w-full">
            <el-option
              v-for="value in selectedOrder
                ? transitions[selectedOrder.status]
                : []"
              :key="value"
              :label="statusOptions.find(item => item.value === value)?.label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="targetStatus === 9 ? '取消原因：' : '备注：'"
          :required="targetStatus === 9"
        >
          <el-input
            v-model="reason"
            type="textarea"
            :rows="3"
            :placeholder="targetStatus === 9 ? '请输入取消原因' : '可选'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="actionLoading"
          :disabled="targetStatus === null"
          @click="confirmAction"
        >
          确认执行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.search-form {
  margin-bottom: 16px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.section-title {
  margin: 22px 0 12px;
  font-size: 16px;
  font-weight: 600;
}
</style>
