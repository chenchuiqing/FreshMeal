<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useDark, useECharts } from "@pureadmin/utils";
import { ElMessage } from "element-plus";
import {
  getDashboardOverview,
  getDashboardRank,
  getDashboardRegion,
  getDashboardTrend
} from "@/api/admin-business";
import type {
  DashboardOverview,
  DashboardRank,
  DashboardRegion,
  DashboardTrend
} from "@/api/admin-business";

defineOptions({ name: "Welcome" });

const loading = ref(false);
const overview = ref<DashboardOverview>({
  todayRevenue: 0,
  todayOrderCount: 0,
  todayUserCount: 0,
  pendingDeliveryCount: 0,
  finishedTodayCount: 0
});
const trend = ref<DashboardTrend[]>([]);
const rank = ref<DashboardRank[]>([]);
const region = ref<DashboardRegion[]>([]);

const { isDark } = useDark();
const theme = computed(() => (isDark.value ? "dark" : "light"));
const chartRef = ref();
const { setOptions } = useECharts(chartRef, {
  theme,
  renderer: "svg"
});

const money = (value: number | null | undefined) =>
  Number(value ?? 0).toFixed(2);

const maxRegionOrders = computed(() =>
  Math.max(1, ...region.value.map(item => item.orderCount))
);

async function loadAll() {
  loading.value = true;
  try {
    const [overviewResult, trendResult, rankResult, regionResult] =
      await Promise.all([
        getDashboardOverview(),
        getDashboardTrend(7),
        getDashboardRank(10),
        getDashboardRegion()
      ]);
    overview.value = overviewResult.data;
    trend.value = trendResult.data;
    rank.value = rankResult.data;
    region.value = regionResult.data;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "数据看板加载失败"
    );
  } finally {
    loading.value = false;
  }
}

watch(
  trend,
  async () => {
    await nextTick();
    setOptions({
      container: ".trend-card",
      tooltip: {
        trigger: "axis",
        valueFormatter: value => `¥${money(Number(value))}`
      },
      grid: {
        top: 30,
        left: 60,
        right: 20,
        bottom: 45
      },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: trend.value.map(item => item.statDate.slice(5)),
        axisLabel: { fontSize: 12 }
      },
      yAxis: {
        type: "value",
        axisLabel: {
          fontSize: 12,
          formatter: value => `¥${value}`
        }
      },
      series: [
        {
          name: "营业额",
          type: "line",
          smooth: true,
          symbolSize: 7,
          areaStyle: { opacity: 0.12 },
          data: trend.value.map(item => Number(item.revenue))
        },
        {
          name: "订单数",
          type: "bar",
          barWidth: 8,
          yAxisIndex: 0,
          data: trend.value.map(item => item.orderCount)
        }
      ]
    });
  },
  { deep: true, immediate: true }
);

onMounted(loadAll);
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <div class="header">
      <div>
        <h2 class="title">经营数据看板</h2>
        <p class="subtitle">数据来源于订单、用户与配送业务表的实时汇总</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新数据</el-button>
    </div>

    <el-row :gutter="16">
      <el-col
        v-for="card in [
          {
            label: '今日营业额',
            value: `¥${money(overview.todayRevenue)}`,
            type: 'revenue'
          },
          { label: '今日订单', value: overview.todayOrderCount, type: 'order' },
          {
            label: '今日新增用户',
            value: overview.todayUserCount,
            type: 'user'
          },
          {
            label: '待配送订单',
            value: overview.pendingDeliveryCount,
            type: 'pending'
          },
          {
            label: '今日完成订单',
            value: overview.finishedTodayCount,
            type: 'finished'
          }
        ]"
        :key="card.type"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="8"
        :xl="8"
        class="mb-4"
      >
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16" class="mb-4">
        <el-card shadow="never" class="trend-card h-full">
          <div class="card-title">近7日营业额与订单趋势</div>
          <div ref="chartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8" class="mb-4">
        <el-card shadow="never" class="h-full">
          <div class="card-title">区域配送分布</div>
          <el-empty
            v-if="region.length === 0"
            description="暂无已支付配送订单"
            :image-size="70"
          />
          <div v-else class="region-list">
            <div v-for="item in region" :key="item.areaId" class="region-item">
              <div class="region-head">
                <span>{{ item.areaName }}</span>
                <strong>{{ item.orderCount }} 单</strong>
              </div>
              <el-progress
                :percentage="
                  Math.round((item.orderCount / maxRegionOrders) * 100)
                "
                :stroke-width="9"
              />
              <div class="region-revenue">
                营业额 ¥{{ money(item.revenue) }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div class="card-title">商品销量 Top 10</div>
      <el-table :data="rank" row-key="productId" stripe>
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <el-tag :type="$index < 3 ? 'danger' : 'info'" effect="plain">
              {{ $index + 1 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品" min-width="220" />
        <el-table-column
          prop="quantity"
          label="销量"
          width="120"
          align="center"
        />
        <el-table-column label="销售额" min-width="140" align="right">
          <template #default="{ row }">¥{{ money(row.revenue) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无已支付订单销量" :image-size="70" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  min-height: 100%;
}

.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.subtitle {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.metric-card {
  border: 1px solid var(--el-border-color-lighter);
}

.metric-label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.metric-value {
  margin-top: 12px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.card-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}

.chart {
  width: 100%;
  height: 330px;
}

.region-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.region-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.region-revenue {
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
