<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createAdminRider,
  getAdminRiderPage,
  getDeliveryAreas,
  updateAdminRider,
  updateAdminRiderStatus,
  updateAdminRiderWork
} from "@/api/admin-business";
import type { AdminRider, DeliveryArea } from "@/api/admin-business";

defineOptions({ name: "CommerceRider" });

const loading = ref(false);
const rows = ref<AdminRider[]>([]);
const total = ref(0);
const areas = ref<DeliveryArea[]>([]);
const dialogVisible = ref(false);
const saving = ref(false);
const editingId = ref<number | null>(null);

const form = reactive({
  current: 1,
  size: 10,
  keyword: "",
  status: undefined as number | undefined,
  onlineStatus: undefined as number | undefined
});

const editor = reactive({
  username: "",
  password: "",
  realName: "",
  phone: "",
  maxOrderNum: 5,
  serviceAreaId: null as number | null,
  status: 1
});

const workStatusOptions = [
  { label: "空闲", value: 0 },
  { label: "配送中", value: 1 },
  { label: "休息", value: 2 }
];

async function loadPage() {
  loading.value = true;
  try {
    const result = await getAdminRiderPage({
      current: form.current,
      size: form.size,
      keyword: form.keyword.trim() || undefined,
      status: form.status,
      onlineStatus: form.onlineStatus
    });
    rows.value = result.data.records;
    total.value = result.data.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "配送员加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadAreas() {
  try {
    const result = await getDeliveryAreas();
    areas.value = result.data;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "服务区域加载失败"
    );
  }
}

function search() {
  form.current = 1;
  void loadPage();
}

function reset() {
  form.keyword = "";
  form.status = undefined;
  form.onlineStatus = undefined;
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

function openCreate() {
  editingId.value = null;
  Object.assign(editor, {
    username: "",
    password: "",
    realName: "",
    phone: "",
    maxOrderNum: 5,
    serviceAreaId: null,
    status: 1
  });
  dialogVisible.value = true;
}

function openEdit(row: AdminRider) {
  editingId.value = row.id;
  Object.assign(editor, {
    username: row.username,
    password: "",
    realName: row.realName,
    phone: row.phone,
    maxOrderNum: row.maxOrderNum,
    serviceAreaId: row.serviceAreaId,
    status: row.status
  });
  dialogVisible.value = true;
}

async function save() {
  if (editingId.value === null && !editor.password) {
    ElMessage.warning("请输入初始密码");
    return;
  }
  saving.value = true;
  try {
    const payload = {
      username: editor.username.trim(),
      password: editor.password || undefined,
      realName: editor.realName.trim(),
      phone: editor.phone.trim(),
      maxOrderNum: editor.maxOrderNum,
      serviceAreaId: editor.serviceAreaId,
      status: editor.status
    };
    if (editingId.value === null) {
      await createAdminRider(payload);
    } else {
      await updateAdminRider(editingId.value, payload);
    }
    ElMessage.success(
      editingId.value === null ? "配送员已创建" : "配送员已更新"
    );
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}

async function changeStatus(row: AdminRider, status: number) {
  const action = status === 0 ? "禁用" : "启用";
  try {
    await ElMessageBox.confirm(
      `确认${action}配送员 ${row.realName}？`,
      "账号状态",
      {
        type: "warning"
      }
    );
    await updateAdminRiderStatus(row.id, status);
    ElMessage.success(`账号已${action}`);
    await loadPage();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error instanceof Error ? error.message : "状态更新失败");
    }
    await loadPage();
  }
}

async function changeOnline(row: AdminRider, onlineStatus: number) {
  try {
    await updateAdminRiderWork(row.id, onlineStatus, row.workStatus);
    ElMessage.success("在线状态已更新");
    await loadPage();
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "在线状态更新失败"
    );
    await loadPage();
  }
}

async function changeWork(row: AdminRider, workStatus: number) {
  try {
    await updateAdminRiderWork(row.id, row.onlineStatus, workStatus);
    ElMessage.success("工作状态已更新");
    await loadPage();
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "工作状态更新失败"
    );
    await loadPage();
  }
}

onMounted(() => {
  void loadPage();
  void loadAreas();
});
</script>

<template>
  <div class="main">
    <el-form :inline="true" class="search-form">
      <el-form-item label="关键词：">
        <el-input
          v-model="form.keyword"
          placeholder="账号/姓名/手机号"
          clearable
          class="w-56"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="账号状态：">
        <el-select
          v-model="form.status"
          placeholder="全部"
          clearable
          class="w-32"
        >
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="在线状态：">
        <el-select
          v-model="form.onlineStatus"
          placeholder="全部"
          clearable
          class="w-32"
        >
          <el-option label="在线" :value="1" />
          <el-option label="离线" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="search">
          搜索
        </el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="openCreate">新增配送员</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" row-key="id" border stripe>
      <el-table-column label="配送员" min-width="150">
        <template #default="{ row }">
          <div class="font-medium">{{ row.realName }}</div>
          <div class="text-xs text-gray-400">{{ row.username }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="服务区域" min-width="130">
        <template #default="{ row }">
          {{ row.serviceAreaName || "全部区域" }}
        </template>
      </el-table-column>
      <el-table-column label="在线状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.onlineStatus"
            :active-value="1"
            :inactive-value="0"
            :disabled="row.status === 0"
            @change="changeOnline(row as AdminRider, $event as number)"
          />
        </template>
      </el-table-column>
      <el-table-column label="工作状态" width="120">
        <template #default="{ row }">
          <el-select
            :model-value="row.workStatus"
            :disabled="row.status === 0"
            size="small"
            @change="changeWork(row as AdminRider, $event as number)"
          >
            <el-option
              v-for="item in workStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="当前负载" width="110" align="center">
        <template #default="{ row }">
          {{ row.inTransitCount }}/{{ row.maxOrderNum }}
        </template>
      </el-table-column>
      <el-table-column
        prop="todayOrderCount"
        label="今日完成"
        width="90"
        align="center"
      />
      <el-table-column prop="rating" label="评分" width="80" align="center" />
      <el-table-column label="账号状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status"
            :active-value="1"
            :inactive-value="0"
            active-text="正常"
            inactive-text="禁用"
            @change="changeStatus(row as AdminRider, $event as number)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as AdminRider)">
            编辑
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
      v-model="dialogVisible"
      :title="editingId === null ? '新增配送员' : '编辑配送员'"
      width="560px"
      destroy-on-close
    >
      <el-form :model="editor" label-width="100px">
        <el-form-item label="登录账号" required>
          <el-input v-model="editor.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item
          :label="editingId === null ? '初始密码' : '重置密码'"
          :required="editingId === null"
        >
          <el-input
            v-model="editor.password"
            type="password"
            show-password
            :placeholder="editingId === null ? '至少6位' : '不修改请留空'"
          />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="editor.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="editor.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="负载上限" required>
          <el-input-number
            v-model="editor.maxOrderNum"
            :min="1"
            :max="99"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="服务区域">
          <el-select
            v-model="editor.serviceAreaId"
            placeholder="不限制区域"
            clearable
            class="w-full"
          >
            <el-option
              v-for="area in areas"
              :key="area.id"
              :label="area.name"
              :value="area.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-radio-group v-model="editor.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          保存
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
</style>
