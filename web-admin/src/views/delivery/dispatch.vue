<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getDispatchLogs } from "@/api/admin-business";
import type { DispatchLog } from "@/api/admin-business";

defineOptions({ name: "CommerceDispatchLog" });

const loading = ref(false);
const rows = ref<DispatchLog[]>([]);
const total = ref(0);

const form = reactive({
  current: 1,
  size: 10,
  deliveryNo: "",
  isFinal: undefined as number | undefined
});

async function loadPage() {
  loading.value = true;
  try {
    const result = await getDispatchLogs({
      current: form.current,
      size: form.size,
      deliveryNo: form.deliveryNo.trim() || undefined,
      isFinal: form.isFinal
    });
    rows.value = result.data.records;
    total.value = result.data.total;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "派单日志加载失败"
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
  form.deliveryNo = "";
  form.isFinal = undefined;
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

const score = (value: number | null | undefined) =>
  Number(value ?? 0).toFixed(4);

onMounted(loadPage);
</script>

<template>
  <div class="main">
    <el-form :inline="true" class="search-form">
      <el-form-item label="配送单号：">
        <el-input
          v-model="form.deliveryNo"
          placeholder="请输入配送单号"
          clearable
          class="w-56"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="结果：">
        <el-select
          v-model="form.isFinal"
          placeholder="全部候选"
          clearable
          class="w-40"
        >
          <el-option label="最终选中" :value="1" />
          <el-option label="未选中" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="search">
          搜索
        </el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      title="同一配送单下按综合得分降序展示全部候选配送员，最终选中记录会高亮显示。"
      class="mb-4"
    />

    <el-table v-loading="loading" :data="rows" row-key="id" border stripe>
      <el-table-column prop="deliveryNo" label="配送单号" min-width="190" />
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column
        prop="candidateName"
        label="候选配送员"
        min-width="130"
      />
      <el-table-column label="综合得分" width="110" align="center">
        <template #default="{ row }">
          <strong>{{ score(row.totalScore) }}</strong>
        </template>
      </el-table-column>
      <el-table-column label="距离分" width="100" align="center">
        <template #default="{ row }">{{ score(row.distanceScore) }}</template>
      </el-table-column>
      <el-table-column label="负载分" width="100" align="center">
        <template #default="{ row }">{{ score(row.loadScore) }}</template>
      </el-table-column>
      <el-table-column label="评分分" width="100" align="center">
        <template #default="{ row }">{{ score(row.ratingScore) }}</template>
      </el-table-column>
      <el-table-column label="距离" width="100" align="center">
        <template #default="{ row }">{{ row.distanceValue }} km</template>
      </el-table-column>
      <el-table-column label="在途负载" width="100" align="center">
        <template #default="{ row }">{{ row.loadValue }}</template>
      </el-table-column>
      <el-table-column label="选中结果" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isFinal === 1 ? 'success' : 'info'">
            {{ row.isFinal === 1 ? "最终选中" : "候选" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="派单时间" min-width="165" />
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
