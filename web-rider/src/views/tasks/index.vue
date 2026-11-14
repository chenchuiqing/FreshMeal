<template>
  <section class="page">
    <header class="hero">
      <div class="hero__top">
        <span class="hero__greeting">{{ greeting }}</span>
        <span class="hero__date num">{{ todayLabel }}</span>
      </div>

      <p class="hero__label">今日收入</p>
      <p class="hero__amount num">
        <span class="hero__currency">¥</span>{{ incomeText }}
      </p>
      <p class="hero__caption">完成送达后实时累计配送收入</p>

      <dl class="hero__stats">
        <div>
          <dt>今日完成</dt>
          <dd><b class="num">{{ statistics?.todayOrderCount ?? 0 }}</b> 单</dd>
        </div>
        <div>
          <dt>进行中</dt>
          <dd><b class="num">{{ statistics?.activeOrderCount ?? 0 }}</b> 单</dd>
        </div>
        <div>
          <dt>服务评分</dt>
          <dd><b class="num">{{ ratingText }}</b></dd>
        </div>
      </dl>

      <button
        type="button"
        class="shift-switch"
        :class="{ 'shift-switch--online': isOnline }"
        :disabled="workStatusLoading"
        @click="toggleOnline"
      >
        <span>{{ isOnline ? '在线接单' : '已收工' }}</span>
        <small>{{ isOnline ? '点击下线' : '点击开始接单' }}</small>
      </button>
    </header>

    <nav class="tabs" aria-label="任务筛选">
      <button
        type="button"
        :class="{ active: activeTab === 'todo' }"
        @click="activeTab = 'todo'"
      >
        任务大厅
        <span class="num">{{ todoTasks?.length ?? 0 }}</span>
      </button>
      <button
        type="button"
        :class="{ active: activeTab === 'my' }"
        @click="activeTab = 'my'"
      >
        我的任务
        <span class="num">{{ myTasks?.length ?? 0 }}</span>
      </button>
    </nav>

    <section class="panel">
      <header class="panel__head">
        <h2>{{ activeTab === 'todo' ? '待接订单' : '配送任务' }}</h2>
        <button type="button" class="refresh" @click="refreshTasks">
          刷新
        </button>
      </header>

      <div v-if="tasksLoading" class="empty">
        <strong>任务加载中…</strong>
      </div>
      <div v-else-if="tasksError" class="empty empty--error" role="alert">
        <strong>任务加载失败</strong>
        <p>{{ tasksErrorText }}</p>
      </div>
      <div v-else-if="!visibleTasks.length" class="empty">
        <span class="empty__badge" aria-hidden="true">
          <van-icon name="logistics" size="1.75rem" />
        </span>
        <strong>{{ activeTab === 'todo' ? '暂无待接订单' : '暂无配送任务' }}</strong>
        <p>{{ activeTab === 'todo' ? '新订单会实时出现在任务大厅。' : '从任务大厅接单后可在这里推进取送。' }}</p>
      </div>

      <ul v-else class="task-list">
        <li v-for="task in visibleTasks" :key="task.deliveryNo" class="task-card">
          <div class="task-card__head">
            <span class="task-status" :class="`task-status--${task.status}`">
              {{ task.statusText }}
            </span>
            <small class="num">{{ task.distance.toFixed(1) }} km</small>
          </div>
          <strong>{{ task.receiver }} · {{ task.receiverPhone }}</strong>
          <p>{{ task.receiverAddress }}</p>
          <small v-if="task.slotStart" class="task-slot">
            预约 {{ formatDateTime(task.slotStart) }}
          </small>
          <div class="task-card__foot">
            <span class="num">配送费 ¥{{ task.freight.toFixed(2) }}</span>
            <van-button
              v-if="activeTab === 'todo'"
              size="small"
              type="primary"
              :loading="actingNo === task.deliveryNo"
              :disabled="!isOnline"
              @click="act(task, 'grab')"
            >
              立即抢单
            </van-button>
            <van-button
              v-else-if="task.status === 1"
              size="small"
              type="primary"
              :loading="actingNo === task.deliveryNo"
              @click="act(task, 'pickup')"
            >
              确认取货
            </van-button>
            <van-button
              v-else-if="task.status === 3"
              size="small"
              type="primary"
              :loading="actingNo === task.deliveryNo"
              @click="act(task, 'deliver')"
            >
              确认送达
            </van-button>
          </div>
        </li>
      </ul>
    </section>

    <p class="notice">
      <van-icon name="shield-o" size="1rem" />
      配送途中请保持手机畅通，注意交通安全
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { showToast } from 'vant';
import {
  deliverTask,
  getMyTasks,
  getStatistics,
  getTodoTasks,
  grabTask,
  pickupTask,
  updateWorkStatus,
} from '@/api/modules/task';
import type { RiderDelivery, RiderStatistics } from '@/api/modules/task';
import { isApiError } from '@/types/api/common';
import { useRiderStore } from '@/store/modules/rider';

const riderStore = useRiderStore();
const activeTab = ref<'todo' | 'my'>('todo');
const actingNo = ref<string | null>(null);
const workStatusLoading = ref(false);
const statistics = ref<RiderStatistics | null>(null);
const todoTasks = ref<RiderDelivery[]>([]);
const myTasks = ref<RiderDelivery[]>([]);
const tasksLoading = ref(true);
const tasksError = ref<unknown>(null);
let refreshTimer: number | undefined;

const visibleTasks = computed(() =>
  activeTab.value === 'todo' ? todoTasks.value ?? [] : myTasks.value ?? [],
);
const tasksErrorText = computed(() =>
  isApiError(tasksError.value)
    ? tasksError.value.message
    : '任务加载失败，请稍后重试',
);
const isOnline = computed(() => statistics.value?.onlineStatus === 1);
const incomeText = computed(() =>
  (statistics.value?.totalIncome ?? 0).toFixed(2),
);
const ratingText = computed(() =>
  statistics.value ? statistics.value.rating.toFixed(1) : '--',
);
const riderName = computed(
  () => riderStore.info?.realName || riderStore.info?.username || '骑手',
);
const greeting = computed(() => {
  const hour = new Date().getHours();
  const period =
    hour < 6
      ? '凌晨好'
      : hour < 11
        ? '早上好'
        : hour < 13
          ? '中午好'
          : hour < 18
            ? '下午好'
            : '晚上好';
  return `${period}，${riderName.value}`;
});
const todayLabel = computed(() => {
  const now = new Date();
  const date = new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
  }).format(now);
  const week = new Intl.DateTimeFormat('zh-CN', { weekday: 'short' }).format(
    now,
  );
  return `${date} ${week}`;
});

async function toggleOnline() {
  if (workStatusLoading.value) return;
  workStatusLoading.value = true;
  const nextOnline = isOnline.value ? 0 : 1;
  const nextWorkStatus =
    nextOnline === 0
      ? 2
      : (statistics.value?.activeOrderCount ?? 0) > 0
        ? 1
        : 0;
  try {
    await updateWorkStatus({
      onlineStatus: nextOnline,
      workStatus: nextWorkStatus,
    });
    await refreshTasks();
    showToast({
      message: nextOnline === 1 ? '已上线，可以接收任务' : '已下线休息',
      icon: 'success',
    });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '状态切换失败');
  } finally {
    workStatusLoading.value = false;
  }
}

async function act(
  task: RiderDelivery,
  action: 'grab' | 'pickup' | 'deliver',
) {
  if (actingNo.value) return;
  actingNo.value = task.deliveryNo;
  try {
    if (action === 'grab') await grabTask(task.deliveryNo);
    if (action === 'pickup') await pickupTask(task.deliveryNo);
    if (action === 'deliver') await deliverTask(task.deliveryNo);
    await refreshTasks();
    if (action === 'grab') activeTab.value = 'my';
    showToast({
      message:
        action === 'grab'
          ? '抢单成功'
          : action === 'pickup'
            ? '取货上报成功'
            : '送达上报成功',
      icon: 'success',
    });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '操作失败，请刷新后重试');
    await refreshTasks();
  } finally {
    actingNo.value = null;
  }
}

async function refreshTasks() {
  tasksLoading.value = true;
  try {
    const [nextStatistics, nextTodo, nextMy] = await Promise.all([
      getStatistics(),
      getTodoTasks(),
      getMyTasks(),
    ]);
    statistics.value = nextStatistics;
    todoTasks.value = nextTodo;
    myTasks.value = nextMy;
    tasksError.value = null;
  } catch (error) {
    tasksError.value = error;
  } finally {
    tasksLoading.value = false;
  }
}

onMounted(() => {
  void refreshTasks();
  refreshTimer = window.setInterval(() => void refreshTasks(), 10_000);
});

onUnmounted(() => {
  if (refreshTimer !== undefined) window.clearInterval(refreshTimer);
});

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped lang="scss">
.page {
  display: grid;
  gap: var(--space-4);
  padding: 0 var(--space-4) calc(4.5rem + env(safe-area-inset-bottom));
}

.hero {
  margin: 0 calc(-1 * var(--space-4));
  padding: var(--space-4) var(--space-4) var(--space-5);
  color: var(--color-ink-text);
  background: var(--color-ink);
  border-radius: 0 0 var(--radius-xl) var(--radius-xl);
}

.hero__top {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
}

.hero__greeting {
  font-weight: 700;
}

.hero__date,
.hero__label,
.hero__caption {
  color: var(--color-ink-muted);
  font-size: var(--text-caption);
}

.hero__label {
  margin: var(--space-4) 0 0;
}

.hero__amount {
  margin: var(--space-1) 0 0;
  font-size: var(--text-display);
  font-weight: 800;
  line-height: 1.1;
  color: var(--color-brand);
}

.hero__currency {
  margin-right: 0.15em;
  font-size: 1.25rem;
}

.hero__caption {
  margin: var(--space-2) 0 0;
}

.hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
  margin: var(--space-4) 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-ink-line);

  dt {
    color: var(--color-ink-muted);
    font-size: var(--text-caption);
  }

  dd {
    margin: var(--space-1) 0 0;
    color: var(--color-ink-text);
    font-size: var(--text-secondary);
  }

  b {
    font-size: var(--text-section);
    font-weight: 700;
  }
}

.shift-switch {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: var(--touch-target);
  margin-top: var(--space-4);
  padding: var(--space-2) var(--space-3);
  color: var(--color-ink-text);
  background: var(--color-ink-line);
  border: 0;
  border-radius: var(--radius-pill);

  span {
    font-weight: 700;
  }

  small {
    color: var(--color-ink-muted);
  }
}

.shift-switch--online {
  color: var(--color-brand-ink);
  background: var(--color-brand);

  small {
    color: color-mix(in srgb, var(--color-brand-ink) 70%, transparent);
  }
}

.tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2);
  padding: var(--space-1);
  background: var(--color-line);
  border-radius: var(--radius-pill);

  button {
    display: flex;
    gap: var(--space-2);
    align-items: center;
    justify-content: center;
    min-height: var(--touch-target);
    color: var(--color-text-secondary);
    background: transparent;
    border: 0;
    border-radius: var(--radius-pill);
    font-weight: 700;
  }

  .active {
    color: var(--color-text);
    background: var(--color-paper);
    box-shadow: var(--shadow-card);
  }

  span {
    min-width: 1.4rem;
    padding: 0 var(--space-1);
    color: var(--color-brand-ink);
    background: var(--color-brand);
    border-radius: var(--radius-pill);
    font-size: var(--text-caption);
  }
}

.panel {
  padding: var(--space-4);
  background: var(--color-paper);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);

  h2 {
    margin: 0;
    font-size: var(--text-section);
  }
}

.refresh {
  min-height: var(--touch-target);
  padding: 0;
  color: var(--color-text-secondary);
  background: transparent;
  border: 0;
}

.empty {
  display: grid;
  gap: var(--space-2);
  place-items: center;
  padding: var(--space-6) var(--space-4);
  text-align: center;

  p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-secondary);
  }
}

.empty__badge {
  display: grid;
  width: 3.5rem;
  height: 3.5rem;
  place-items: center;
  color: var(--color-brand-ink);
  background: var(--color-brand);
  border-radius: 50%;
}

.empty--error strong {
  color: var(--color-danger);
}

.task-list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  margin: 0;
  list-style: none;
}

.task-card {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-canvas);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);

  > strong {
    font-size: var(--text-body);
  }

  > p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: var(--text-secondary);
  }
}

.task-card__head,
.task-card__foot {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
}

.task-card__foot {
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-line);
  color: var(--color-text-secondary);
  font-size: var(--text-secondary);
}

.task-status {
  padding: 0 var(--space-2);
  color: var(--color-text-secondary);
  background: var(--color-line);
  border-radius: var(--radius-pill);
  font-size: var(--text-caption);
  font-weight: 700;

  &--1,
  &--2,
  &--3 {
    color: var(--color-brand-ink);
    background: var(--color-brand);
  }

  &--4 {
    color: var(--color-success);
    background: var(--color-success-soft);
  }
}

.task-slot {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
}

.notice {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  margin: 0;
  padding: var(--space-3) var(--space-4);
  color: var(--color-text-secondary);
  background: var(--color-paper);
  border-left: 3px solid var(--color-brand);
  border-radius: var(--radius-sm);
  font-size: var(--text-secondary);
}
</style>
