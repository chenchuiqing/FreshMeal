<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  createProductBatch,
  getExpireWarnings,
  getInventoryLogs,
  getProductBatches,
  reportBatchLoss
} from "@/api/admin-business";
import type { InventoryLog, ProductBatch } from "@/api/admin-business";
import { getProductPage } from "@/api/product";
import type { AdminProductVO } from "@/api/product";

defineOptions({ name: "CommerceInventory" });

const activeTab = ref("batch");
const batchLoading = ref(false);
const logLoading = ref(false);
const warningLoading = ref(false);
const batchRows = ref<ProductBatch[]>([]);
const logRows = ref<InventoryLog[]>([]);
const warningRows = ref<ProductBatch[]>([]);
const batchTotal = ref(0);
const logTotal = ref(0);
const products = ref<AdminProductVO[]>([]);
const createVisible = ref(false);
const lossVisible = ref(false);
const saving = ref(false);
const selectedBatch = ref<ProductBatch | null>(null);

const batchForm = reactive({
  current: 1,
  size: 10,
  productId: undefined as number | undefined,
  batchNo: "",
  status: undefined as number | undefined
});

const logForm = reactive({
  current: 1,
  size: 10,
  productId: undefined as number | undefined,
  changeType: undefined as number | undefined,
  dateRange: [] as string[]
});

const warningDays = ref(3);

const editor = reactive({
  productId: undefined as number | undefined,
  batchNo: "",
  productionDate: "",
  expireDate: "",
  stock: 0,
  lockStock: 0,
  remark: ""
});

const loss = reactive({
  quantity: 1,
  remark: ""
});

const batchStatusOptions = [
  { label: "正常", value: 1 },
  { label: "临期", value: 2 },
  { label: "过期", value: 3 },
  { label: "售罄", value: 4 }
];

const changeTypeOptions = [
  { label: "入库", value: 1 },
  { label: "出库", value: 2 },
  { label: "退货", value: 3 },
  { label: "报损", value: 4 },
  { label: "盘点", value: 5 },
  { label: "取消回补", value: 6 }
];

const batchTagType = (status: number) => {
  if (status === 1) return "success";
  if (status === 2) return "warning";
  if (status === 3) return "danger";
  return "info";
};

async function loadBatches() {
  batchLoading.value = true;
  try {
    const result = await getProductBatches({
      current: batchForm.current,
      size: batchForm.size,
      productId: batchForm.productId,
      batchNo: batchForm.batchNo.trim() || undefined,
      status: batchForm.status
    });
    batchRows.value = result.data.records;
    batchTotal.value = result.data.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "批次加载失败");
  } finally {
    batchLoading.value = false;
  }
}

async function loadLogs() {
  logLoading.value = true;
  try {
    const result = await getInventoryLogs({
      current: logForm.current,
      size: logForm.size,
      productId: logForm.productId,
      changeType: logForm.changeType,
      startTime: logForm.dateRange[0],
      endTime: logForm.dateRange[1]
        ? `${logForm.dateRange[1].slice(0, 10)}T23:59:59`
        : undefined
    });
    logRows.value = result.data.records;
    logTotal.value = result.data.total;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "库存流水加载失败"
    );
  } finally {
    logLoading.value = false;
  }
}

async function loadWarnings() {
  warningLoading.value = true;
  try {
    const result = await getExpireWarnings(warningDays.value);
    warningRows.value = result.data;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "临期预警加载失败"
    );
  } finally {
    warningLoading.value = false;
  }
}

async function loadProducts() {
  try {
    const result = await getProductPage({ current: 1, size: 100 });
    products.value = result.data.records;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "商品加载失败");
  }
}

function searchBatches() {
  batchForm.current = 1;
  void loadBatches();
}

function resetBatches() {
  batchForm.productId = undefined;
  batchForm.batchNo = "";
  batchForm.status = undefined;
  searchBatches();
}

function searchLogs() {
  logForm.current = 1;
  void loadLogs();
}

function resetLogs() {
  logForm.productId = undefined;
  logForm.changeType = undefined;
  logForm.dateRange = [];
  searchLogs();
}

function changeBatchCurrent(current: number) {
  batchForm.current = current;
  void loadBatches();
}

function changeBatchSize(size: number) {
  batchForm.size = size;
  batchForm.current = 1;
  void loadBatches();
}

function changeLogCurrent(current: number) {
  logForm.current = current;
  void loadLogs();
}

function changeLogSize(size: number) {
  logForm.size = size;
  logForm.current = 1;
  void loadLogs();
}

function openCreate() {
  Object.assign(editor, {
    productId: products.value[0]?.id,
    batchNo: "",
    productionDate: "",
    expireDate: "",
    stock: 0,
    lockStock: 0,
    remark: ""
  });
  createVisible.value = true;
}

async function saveBatch() {
  if (!editor.productId) {
    ElMessage.warning("请选择商品");
    return;
  }
  saving.value = true;
  try {
    await createProductBatch({
      ...editor,
      remark: editor.remark.trim() || undefined
    });
    ElMessage.success("批次已录入");
    createVisible.value = false;
    await Promise.all([loadBatches(), loadWarnings(), loadProducts()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "批次录入失败");
  } finally {
    saving.value = false;
  }
}

function openLoss(row: ProductBatch) {
  selectedBatch.value = row;
  loss.quantity = 1;
  loss.remark = "";
  lossVisible.value = true;
}

async function saveLoss() {
  if (!selectedBatch.value) return;
  saving.value = true;
  try {
    await reportBatchLoss(
      selectedBatch.value.id,
      loss.quantity,
      loss.remark.trim()
    );
    ElMessage.success("报损已记录");
    lossVisible.value = false;
    await Promise.all([loadBatches(), loadLogs(), loadProducts()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "报损失败");
  } finally {
    saving.value = false;
  }
}

function onTabChange(name: string | number) {
  if (name === "log" && logRows.value.length === 0) void loadLogs();
  if (name === "warning" && warningRows.value.length === 0) void loadWarnings();
}

onMounted(() => {
  void loadBatches();
  void loadProducts();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="商品批次" name="batch">
          <el-form :inline="true" class="search-form">
            <el-form-item label="商品：">
              <el-select
                v-model="batchForm.productId"
                placeholder="全部商品"
                clearable
                filterable
                class="w-56"
              >
                <el-option
                  v-for="product in products"
                  :key="product.id"
                  :label="product.name"
                  :value="product.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="批次号：">
              <el-input
                v-model="batchForm.batchNo"
                placeholder="请输入批次号"
                clearable
                class="w-44"
                @keyup.enter="searchBatches"
              />
            </el-form-item>
            <el-form-item label="状态：">
              <el-select
                v-model="batchForm.status"
                placeholder="全部"
                clearable
                class="w-32"
              >
                <el-option
                  v-for="item in batchStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="batchLoading"
                @click="searchBatches"
              >
                搜索
              </el-button>
              <el-button @click="resetBatches">重置</el-button>
              <el-button type="success" @click="openCreate">录入批次</el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="batchLoading"
            :data="batchRows"
            row-key="id"
            border
            stripe
          >
            <el-table-column prop="batchNo" label="批次号" min-width="130" />
            <el-table-column prop="productName" label="商品" min-width="170" />
            <el-table-column
              prop="productionDate"
              label="生产日期"
              width="115"
            />
            <el-table-column prop="expireDate" label="到期日期" width="115" />
            <el-table-column
              prop="stock"
              label="可用库存"
              width="95"
              align="center"
            />
            <el-table-column
              prop="lockStock"
              label="锁定库存"
              width="95"
              align="center"
            />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="batchTagType(row.status)">
                  {{ row.statusText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="danger"
                  :disabled="row.stock <= 0"
                  @click="openLoss(row as ProductBatch)"
                >
                  报损
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="batchForm.current"
            v-model:page-size="batchForm.size"
            class="pagination"
            background
            layout="total, sizes, prev, pager, next"
            :total="batchTotal"
            @current-change="changeBatchCurrent"
            @size-change="changeBatchSize"
          />
        </el-tab-pane>

        <el-tab-pane label="库存流水" name="log">
          <el-form :inline="true" class="search-form">
            <el-form-item label="商品：">
              <el-select
                v-model="logForm.productId"
                placeholder="全部商品"
                clearable
                filterable
                class="w-56"
              >
                <el-option
                  v-for="product in products"
                  :key="product.id"
                  :label="product.name"
                  :value="product.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="变动类型：">
              <el-select
                v-model="logForm.changeType"
                placeholder="全部"
                clearable
                class="w-36"
              >
                <el-option
                  v-for="item in changeTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="时间：">
              <el-date-picker
                v-model="logForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                class="w-70!"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="logLoading"
                @click="searchLogs"
              >
                搜索
              </el-button>
              <el-button @click="resetLogs">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="logLoading"
            :data="logRows"
            row-key="id"
            border
            stripe
          >
            <el-table-column prop="createTime" label="时间" min-width="165" />
            <el-table-column prop="productName" label="商品" min-width="160" />
            <el-table-column prop="batchNo" label="批次" min-width="120">
              <template #default="{ row }">{{
                row.batchNo || "商品总库存"
              }}</template>
            </el-table-column>
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="
                    row.changeType === 4
                      ? 'danger'
                      : row.changeType === 2
                        ? 'warning'
                        : 'success'
                  "
                >
                  {{ row.changeTypeText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="变动" width="90" align="right">
              <template #default="{ row }">
                {{ row.changeType === 2 || row.changeType === 4 ? "-" : "+"
                }}{{ row.changeNum }}
              </template>
            </el-table-column>
            <el-table-column label="库存变化" width="130" align="center">
              <template #default="{ row }">
                {{ row.beforeStock }} → {{ row.afterStock }}
              </template>
            </el-table-column>
            <el-table-column prop="refNo" label="关联单据" min-width="150" />
            <el-table-column prop="remark" label="备注" min-width="170" />
          </el-table>

          <el-pagination
            v-model:current-page="logForm.current"
            v-model:page-size="logForm.size"
            class="pagination"
            background
            layout="total, sizes, prev, pager, next"
            :total="logTotal"
            @current-change="changeLogCurrent"
            @size-change="changeLogSize"
          />
        </el-tab-pane>

        <el-tab-pane label="临期预警" name="warning">
          <div class="toolbar">
            <span class="mr-3 self-center text-gray-500">预警范围：</span>
            <el-select
              v-model="warningDays"
              class="w-36"
              @change="loadWarnings"
            >
              <el-option label="未来3天" :value="3" />
              <el-option label="未来7天" :value="7" />
              <el-option label="未来14天" :value="14" />
              <el-option label="未来30天" :value="30" />
            </el-select>
            <el-button
              class="ml-3"
              :loading="warningLoading"
              @click="loadWarnings"
            >
              刷新
            </el-button>
          </div>
          <el-alert
            type="warning"
            :closable="false"
            title="列表包含已过期或将在所选范围内到期、且仍有可用库存的批次"
            class="mb-4"
          />
          <el-table
            v-loading="warningLoading"
            :data="warningRows"
            row-key="id"
            border
            stripe
          >
            <el-table-column prop="batchNo" label="批次号" min-width="130" />
            <el-table-column prop="productName" label="商品" min-width="180" />
            <el-table-column prop="expireDate" label="到期日期" width="120" />
            <el-table-column
              prop="stock"
              label="可用库存"
              width="100"
              align="center"
            />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="batchTagType(row.status)">
                  {{ row.statusText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="createVisible"
      title="录入商品批次"
      width="600px"
      destroy-on-close
    >
      <el-form :model="editor" label-width="110px">
        <el-form-item label="商品" required>
          <el-select
            v-model="editor.productId"
            placeholder="请选择商品"
            filterable
            class="w-full"
          >
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号" required>
          <el-input v-model="editor.batchNo" placeholder="最多32个字符" />
        </el-form-item>
        <el-form-item label="生产日期" required>
          <el-date-picker
            v-model="editor.productionDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择生产日期"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="到期日期" required>
          <el-date-picker
            v-model="editor.expireDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择到期日期"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="批次库存" required>
          <el-input-number v-model="editor.stock" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item label="锁定库存" required>
          <el-input-number v-model="editor.lockStock" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="editor.remark"
            type="textarea"
            :rows="3"
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBatch">
          确认入库
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="lossVisible"
      title="批次报损"
      width="480px"
      destroy-on-close
    >
      <el-alert
        v-if="selectedBatch"
        type="warning"
        :closable="false"
        :title="`${selectedBatch.productName} / ${selectedBatch.batchNo}`"
        :description="`当前可用库存 ${selectedBatch.stock}`"
        class="mb-4"
      />
      <el-form label-width="90px">
        <el-form-item label="报损数量" required>
          <el-input-number
            v-model="loss.quantity"
            :min="1"
            :max="selectedBatch?.stock ?? 1"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="报损原因" required>
          <el-input
            v-model="loss.remark"
            type="textarea"
            :rows="3"
            placeholder="如：包装破损、温度异常"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lossVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="saving"
          :disabled="!loss.remark.trim()"
          @click="saveLoss"
        >
          确认报损
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

.toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 14px;
}
</style>
