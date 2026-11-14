<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  assignDelivery,
  getDeliveryPage,
  getDeliveryRiders
} from "@/api/delivery";
import type { DeliveryOrder, DeliveryRider } from "@/api/delivery";

defineOptions({ name: "CommerceDelivery" });

const loading = ref(false);
const rows = ref<DeliveryOrder[]>([]);
const total = ref(0);
const riders = ref<DeliveryRider[]>([]);
const assignDialogVisible = ref(false);
const assignLoading = ref(false);
const selectedRiderId = ref<number | null>(null);
const selectedDelivery = ref<DeliveryOrder | null>(null);
const form = reactive({
  current: 1,
  size: 10,
  status: undefined as number | undefined,
  orderNo: ""
});

const statusOptions = [
  { label: "待分配", value: 0 },
  { label: "已分配", value: 1 },
  { label: "取货中", value: 2 },
  { label: "配送中", value: 3 },
  { label: "已送达", value: 4 }
];

async function loadPage() {
  loading.value = true;
  try {
    const result = await getDeliveryPage({
      current: form.current,
      size: form.size,
      status: form.status,
      orderNo: form.orderNo.trim() || undefined
    });
    rows.value = result.data.records;
    total.value = result.data.total;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "配送单加载失败"
    );
  } finally {
    loading.value = false;
  }
}

function search() {
  form.current = 1;
  void loadPage();
}

function reset() {
  form.status = undefined;
  form.orderNo = "";
  search();
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

async function openAssign(row: DeliveryOrder) {
  selectedDelivery.value = row;
  selectedRiderId.value = null;
  assignDialogVisible.value = true;
  try {
    const result = await getDeliveryRiders();
    riders.value = result.data;
  } catch (error) {
    assignDialogVisible.value = false;
    ElMessage.error(
      error instanceof Error ? error.message : "配送员加载失败"
    );
  }
}

async function confirmAssign() {
  if (!selectedDelivery.value || !selectedRiderId.value) return;
  assignLoading.value = true;
  try {
    await assignDelivery(
      selectedDelivery.value.id,
      selectedRiderId.value
    );
    ElMessage.success("人工指派成功");
    assignDialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "人工指派失败"
    );
  } finally {
    assignLoading.value = false;
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
      <el-form-item label="配送状态：">
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
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="search">
          搜索
        </el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="id"
      border
      stripe
    >
      <el-table-column prop="deliveryNo" label="配送单号" min-width="190" />
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 4 ? 'success' : row.status === 0 ? 'warning' : 'primary'">
            {{ row.statusText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分配方式" width="110">
        <template #default="{ row }">{{ row.assignTypeText }}</template>
      </el-table-column>
      <el-table-column label="配送员" min-width="130">
        <template #default="{ row }">
          {{ row.deliveryManName || "待分配" }}
        </template>
      </el-table-column>
      <el-table-column prop="distance" label="距离(km)" width="100" />
      <el-table-column prop="freight" label="配送费" width="100" />
      <el-table-column label="创建时间" min-width="165">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.status !== 0"
            @click="openAssign(row as DeliveryOrder)"
          >
            人工指派
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
      v-model="assignDialogVisible"
      title="人工指派配送员"
      width="520px"
    >
      <el-alert
        v-if="selectedDelivery"
        type="info"
        :closable="false"
        :title="`配送单 ${selectedDelivery.deliveryNo}`"
        class="mb-3"
      />
      <el-table
        :data="riders"
        max-height="360"
        highlight-current-row
        @current-change="(row: DeliveryRider | null) => (selectedRiderId = row?.id ?? null)"
      >
        <el-table-column label="选择" width="60">
          <template #default="{ row }">
            <el-radio :model-value="selectedRiderId" :value="row.id">
              <span class="sr-only">选择 {{ row.realName }}</span>
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="配送员" min-width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            {{ row.onlineStatus === 1 ? "在线" : "离线" }}
          </template>
        </el-table-column>
        <el-table-column label="负载" width="90">
          <template #default="{ row }">
            {{ row.inTransitCount }}/{{ row.maxOrderNum }}
          </template>
        </el-table-column>
        <el-table-column label="可指派" width="90">
          <template #default="{ row }">
            <el-tag :type="row.assignable ? 'success' : 'danger'" size="small">
              {{ row.assignable ? "是" : "否" }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="assignLoading"
          :disabled="!selectedRiderId"
          @click="confirmAssign"
        >
          确认指派
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

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
