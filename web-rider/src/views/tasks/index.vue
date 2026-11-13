<template>
  <section class="page">
    <header class="hero">
      <div class="hero__top">
        <span class="hero__greeting">{{ greeting }}</span>
        <span class="hero__date num">{{ todayLabel }}</span>
      </div>

      <p class="hero__label">今日收入</p>
      <p class="hero__amount num"><span class="hero__currency">¥</span>0.00</p>
      <p class="hero__caption">业绩数据将在配送业务上线后开始统计</p>

      <dl class="hero__stats">
        <div>
          <dt>今日完成</dt>
          <dd><b class="num">0</b> 单</dd>
        </div>
        <div>
          <dt>累计完成</dt>
          <dd><b class="num">0</b> 单</dd>
        </div>
        <div>
          <dt>服务评分</dt>
          <dd><b class="num">--</b></dd>
        </div>
      </dl>

      <div class="shift-switch" role="group" aria-disabled="true">
        <span class="shift-switch__track">
          <span class="shift-switch__knob">已收工</span>
          <span class="shift-switch__target">开始接单</span>
        </span>
        <span class="shift-switch__note">接单开关即将上线</span>
      </div>
    </header>

    <section class="panel">
      <header class="panel__head">
        <h2>待接订单</h2>
        <span class="panel__count num">0 单</span>
      </header>

      <div class="empty">
        <span class="empty__badge" aria-hidden="true">
          <van-icon name="logistics" size="1.75rem" />
        </span>
        <strong>暂无待接订单</strong>
        <p>订单大厅上线后，新订单会实时推送到这里。</p>
      </div>
    </section>

    <p class="notice">
      <van-icon name="shield-o" size="1rem" />
      配送途中请保持手机畅通，注意交通安全
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRiderStore } from '@/store/modules/rider';

const riderStore = useRiderStore();
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
</script>

<style scoped lang="scss">
.page {
  display: grid;
  gap: var(--space-4);
  padding: 0 var(--space-4) calc(4.5rem + env(safe-area-inset-bottom));
}

/* 深色仪表盘：与顶栏连成一体 */
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
  font-size: var(--text-body);
  font-weight: 700;
}

.hero__date {
  font-size: var(--text-secondary);
  color: var(--color-ink-muted);
}

.hero__label {
  margin: var(--space-4) 0 0;
  font-size: var(--text-secondary);
  color: var(--color-ink-muted);
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
  font-weight: 700;
}

.hero__caption {
  margin: var(--space-2) 0 0;
  font-size: var(--text-caption);
  color: var(--color-ink-muted);
}

.hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
  margin: var(--space-4) 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-ink-line);

  dt {
    font-size: var(--text-caption);
    color: var(--color-ink-muted);
  }

  dd {
    margin: var(--space-1) 0 0;
    font-size: var(--text-secondary);
    color: var(--color-ink-text);
  }

  b {
    font-size: var(--text-section);
    font-weight: 700;
  }
}

/* 接单开关占位：置灰表示尚未开放 */
.shift-switch {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
}

.shift-switch__track {
  display: inline-flex;
  gap: var(--space-1);
  align-items: center;
  padding: var(--space-1);
  background: rgb(255 255 255 / 8%);
  border-radius: var(--radius-pill);
}

.shift-switch__knob,
.shift-switch__target {
  padding: var(--space-1) var(--space-4);
  font-size: var(--text-secondary);
  border-radius: var(--radius-pill);
}

.shift-switch__knob {
  font-weight: 700;
  color: var(--color-ink-text);
  background: var(--color-ink-line);
}

.shift-switch__target {
  color: var(--color-ink-muted);
}

.shift-switch__note {
  font-size: var(--text-caption);
  color: var(--color-ink-muted);
}

.panel {
  padding: var(--space-4);
  background: var(--color-paper);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.panel__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: var(--text-section);
    font-weight: 700;
  }
}

.panel__count {
  font-size: var(--text-secondary);
  color: var(--color-text-muted);
}

.empty {
  display: grid;
  gap: var(--space-2);
  place-items: center;
  padding: var(--space-6) var(--space-4);
  text-align: center;

  strong {
    font-size: var(--text-body);
  }

  p {
    margin: 0;
    font-size: var(--text-secondary);
    color: var(--color-text-secondary);
  }
}

.empty__badge {
  display: grid;
  width: 3.5rem;
  height: 3.5rem;
  place-items: center;
  color: var(--color-ink-text);
  background: var(--color-ink);
  border-radius: 50%;
}

.notice {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  margin: 0;
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-secondary);
  color: var(--color-text-secondary);
  background: var(--color-paper);
  border-left: 3px solid var(--color-brand);
  border-radius: var(--radius-sm);
}
</style>
