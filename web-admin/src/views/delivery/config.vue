<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  createDeliveryArea,
  createDeliverySlot,
  getDeliveryAreas,
  getDeliverySlots,
  updateDeliveryArea,
  updateDeliverySlot
} from "@/api/admin-business";
import type { DeliveryArea, DeliverySlot } from "@/api/admin-business";

defineOptions({ name: "CommerceDeliveryConfig" });

const activeTab = ref("area");
const areaLoading = ref(false);
const slotLoading = ref(false);
const areas = ref<DeliveryArea[]>([]);
const slots = ref<DeliverySlot[]>([]);
const areaDialogVisible = ref(false);
const slotDialogVisible = ref(false);
const saving = ref(false);
const editingAreaId = ref<number | null>(null);
const editingSlotId = ref<number | null>(null);

const areaEditor = reactive({
  name: "",
  city: "",
  district: "",
  baseFee: 5,
  perKmFee: 2,
  freeThreshold: 0,
  nightSurcharge: 0,
  sort: 0,
  status: 1
});

const slotEditor = reactive({
  name: "",
  startTime: "",
  endTime: "",
  orderDeadline: "",
  dailyCapacity: 50,
  advanceDays: 1,
  sort: 0,
  status: 1
});

async function loadAreas() {
  areaLoading.value = true;
  try {
    const result = await getDeliveryAreas();
    areas.value = result.data;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "配送区域加载失败"
    );
  } finally {
    areaLoading.value = false;
  }
}

async function loadSlots() {
  slotLoading.value = true;
  try {
    const result = await getDeliverySlots();
    slots.value = result.data;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "配送时段加载失败"
    );
  } finally {
    slotLoading.value = false;
  }
}

function openArea(area?: DeliveryArea) {
  editingAreaId.value = area?.id ?? null;
  Object.assign(
    areaEditor,
    area
      ? {
          name: area.name,
          city: area.city,
          district: area.district ?? "",
          baseFee: area.baseFee,
          perKmFee: area.perKmFee,
          freeThreshold: area.freeThreshold,
          nightSurcharge: area.nightSurcharge,
          sort: area.sort,
          status: area.status
        }
      : {
          name: "",
          city: "",
          district: "",
          baseFee: 5,
          perKmFee: 2,
          freeThreshold: 0,
          nightSurcharge: 0,
          sort: 0,
          status: 1
        }
  );
  areaDialogVisible.value = true;
}

async function saveArea() {
  saving.value = true;
  try {
    const payload = {
      ...areaEditor,
      district: areaEditor.district.trim() || null
    };
    if (editingAreaId.value === null) {
      await createDeliveryArea(payload);
    } else {
      await updateDeliveryArea(editingAreaId.value, payload);
    }
    ElMessage.success("配送区域已保存");
    areaDialogVisible.value = false;
    await loadAreas();
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "配送区域保存失败"
    );
  } finally {
    saving.value = false;
  }
}

function openSlot(slot?: DeliverySlot) {
  editingSlotId.value = slot?.id ?? null;
  Object.assign(
    slotEditor,
    slot
      ? {
          name: slot.name,
          startTime: slot.startTime,
          endTime: slot.endTime,
          orderDeadline: slot.orderDeadline,
          dailyCapacity: slot.dailyCapacity,
          advanceDays: slot.advanceDays,
          sort: slot.sort,
          status: slot.status
        }
      : {
          name: "",
          startTime: "",
          endTime: "",
          orderDeadline: "",
          dailyCapacity: 50,
          advanceDays: 1,
          sort: 0,
          status: 1
        }
  );
  slotDialogVisible.value = true;
}

async function saveSlot() {
  saving.value = true;
  try {
    if (editingSlotId.value === null) {
      await createDeliverySlot(slotEditor);
    } else {
      await updateDeliverySlot(editingSlotId.value, slotEditor);
    }
    ElMessage.success("配送时段已保存");
    slotDialogVisible.value = false;
    await loadSlots();
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "配送时段保存失败"
    );
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  void loadAreas();
  void loadSlots();
});
</script>

<template>
  <div class="main">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="配送区域与运费规则" name="area">
          <div class="toolbar">
            <el-button type="primary" @click="openArea()">新增区域</el-button>
          </div>
          <el-table
            v-loading="areaLoading"
            :data="areas"
            row-key="id"
            border
            stripe
          >
            <el-table-column prop="name" label="区域名称" min-width="150" />
            <el-table-column prop="city" label="城市" width="110" />
            <el-table-column label="覆盖区县" min-width="130">
              <template #default="{ row }">
                {{ row.district || "全市" }}
              </template>
            </el-table-column>
            <el-table-column label="起步费" width="90" align="right">
              <template #default="{ row }">¥{{ row.baseFee }}</template>
            </el-table-column>
            <el-table-column label="每公里" width="90" align="right">
              <template #default="{ row }">¥{{ row.perKmFee }}</template>
            </el-table-column>
            <el-table-column label="免运费门槛" width="120" align="right">
              <template #default="{ row }">
                {{
                  Number(row.freeThreshold) > 0
                    ? `¥${row.freeThreshold}`
                    : "不免"
                }}
              </template>
            </el-table-column>
            <el-table-column label="夜间附加" width="100" align="right">
              <template #default="{ row }">¥{{ row.nightSurcharge }}</template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? "启用" : "停用" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openArea(row as DeliveryArea)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="预约配送时段" name="slot">
          <div class="toolbar">
            <el-button type="primary" @click="openSlot()">新增时段</el-button>
          </div>
          <el-table
            v-loading="slotLoading"
            :data="slots"
            row-key="id"
            border
            stripe
          >
            <el-table-column prop="name" label="时段名称" min-width="180" />
            <el-table-column label="配送时间" width="150" align="center">
              <template #default="{ row }">
                {{ row.startTime.slice(0, 5) }}-{{ row.endTime.slice(0, 5) }}
              </template>
            </el-table-column>
            <el-table-column label="下单截止" width="110" align="center">
              <template #default="{ row }">
                {{ row.orderDeadline.slice(0, 5) }}
              </template>
            </el-table-column>
            <el-table-column label="每日容量" width="100" align="center">
              <template #default="{ row }">
                {{ row.dailyCapacity === 0 ? "不限" : row.dailyCapacity }}
              </template>
            </el-table-column>
            <el-table-column label="可预约天数" width="110" align="center">
              <template #default="{ row }"
                >今天+{{ row.advanceDays }}天</template
              >
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? "启用" : "停用" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openSlot(row as DeliverySlot)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="areaDialogVisible"
      :title="editingAreaId === null ? '新增配送区域' : '编辑配送区域'"
      width="620px"
      destroy-on-close
    >
      <el-form :model="areaEditor" label-width="120px">
        <el-form-item label="区域名称" required>
          <el-input v-model="areaEditor.name" placeholder="如：浦东核心区" />
        </el-form-item>
        <el-form-item label="所属城市" required>
          <el-input v-model="areaEditor.city" placeholder="如：上海市" />
        </el-form-item>
        <el-form-item label="覆盖区县">
          <el-input
            v-model="areaEditor.district"
            placeholder="留空表示全市覆盖"
          />
        </el-form-item>
        <el-form-item label="起步配送费" required>
          <el-input-number
            v-model="areaEditor.baseFee"
            :min="0"
            :precision="2"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="每公里加价" required>
          <el-input-number
            v-model="areaEditor.perKmFee"
            :min="0"
            :precision="2"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="免运费门槛" required>
          <el-input-number
            v-model="areaEditor.freeThreshold"
            :min="0"
            :precision="2"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="夜间附加费" required>
          <el-input-number
            v-model="areaEditor.nightSurcharge"
            :min="0"
            :precision="2"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="排序" required>
          <el-input-number v-model="areaEditor.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="areaEditor.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="areaDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveArea">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="slotDialogVisible"
      :title="editingSlotId === null ? '新增配送时段' : '编辑配送时段'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="slotEditor" label-width="130px">
        <el-form-item label="时段名称" required>
          <el-input
            v-model="slotEditor.name"
            placeholder="如：午市 11:00-13:00"
          />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-picker
            v-model="slotEditor.startTime"
            format="HH:mm:ss"
            value-format="HH:mm:ss"
            placeholder="选择开始时间"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-picker
            v-model="slotEditor.endTime"
            format="HH:mm:ss"
            value-format="HH:mm:ss"
            placeholder="选择结束时间"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="下单截止时间" required>
          <el-time-picker
            v-model="slotEditor.orderDeadline"
            format="HH:mm:ss"
            value-format="HH:mm:ss"
            placeholder="须早于开始时间"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="每日容量" required>
          <el-input-number
            v-model="slotEditor.dailyCapacity"
            :min="0"
            :max="100000"
            class="w-full"
          />
          <div class="form-tip">0 表示不限容量</div>
        </el-form-item>
        <el-form-item label="可预约天数" required>
          <el-input-number
            v-model="slotEditor.advanceDays"
            :min="0"
            :max="30"
            class="w-full"
          />
          <div class="form-tip">0 表示仅今天，1 表示今天和明天</div>
        </el-form-item>
        <el-form-item label="排序" required>
          <el-input-number v-model="slotEditor.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="slotEditor.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="slotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSlot">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.form-tip {
  width: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
