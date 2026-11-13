<template>
  <div class="home-page page">
    <header class="brand-row">
      <span class="brand">
        <SvgIcon name="logo" />
        <strong>FreshMeal</strong>
      </span>
      <div v-if="!userStore.token" class="guest-actions">
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink class="register-link" to="/register">注册</RouterLink>
      </div>
      <RouterLink v-else to="/member" class="member-link">我的</RouterLink>
    </header>

    <main>
      <section class="hero">
        <div class="hero__copy">
          <span>新鲜备餐 · 按需配送</span>
          <h1>今天，吃一顿<br />刚刚好的饭</h1>
          <p>按热量、营养和配送时段选择餐品，让每一餐都清楚可控。</p>
          <div class="hero__actions">
            <RouterLink class="primary-action" to="/catalog"
              >浏览菜单</RouterLink
            >
            <RouterLink class="secondary-action" to="/member"
              >查看订单</RouterLink
            >
          </div>
        </div>
        <SvgIcon class="hero__mark" name="logo" />
      </section>

      <section class="category-section" aria-labelledby="category-title">
        <div class="section-heading">
          <span>按分类挑</span>
          <h2 id="category-title">今天想吃哪一类</h2>
        </div>
        <div class="category-list">
          <RouterLink
            v-for="item in roots"
            :key="item.id"
            class="category-item"
            :to="{ name: 'catalog', query: { categoryId: item.id } }"
          >
            <span v-if="item.icon" class="category-item__icon" aria-hidden="true">{{
              item.icon
            }}</span>
            <span>{{ item.name }}</span>
          </RouterLink>
        </div>
      </section>

      <section
        v-if="recommended?.length"
        class="shelf-section"
        aria-labelledby="recommend-title"
      >
        <div class="section-heading">
          <span>今日推荐</span>
          <h2 id="recommend-title">为你挑好的几样</h2>
        </div>
        <div class="shelf">
          <div
            v-for="item in recommended"
            :key="item.id"
            class="shelf__item"
          >
            <ProductCard :product="item" />
          </div>
        </div>
      </section>

      <section v-if="hot?.length" class="hot-section" aria-labelledby="hot-title">
        <div class="section-heading">
          <span>热销榜</span>
          <h2 id="hot-title">大家都在点</h2>
        </div>
        <ol class="hot-list">
          <li v-for="(item, index) in hot" :key="item.id">
            <RouterLink
              class="hot-row"
              :to="{ name: 'productDetail', params: { id: item.id } }"
            >
              <span
                class="hot-row__rank"
                :class="{ 'hot-row__rank--top': index < 3 }"
                >{{ index + 1 }}</span
              >
              <img
                class="hot-row__cover"
                :src="item.cover"
                :alt="item.name"
                loading="lazy"
              />
              <span class="hot-row__info">
                <b>{{ item.name }}</b>
                <span class="hot-row__meta"
                  >{{ item.calories }} kcal · 已售 {{ item.salesCount }}</span
                >
              </span>
              <span class="hot-row__price">{{ formatPrice(item.price) }}</span>
            </RouterLink>
          </li>
        </ol>
      </section>

      <section class="goal-section" aria-labelledby="goal-title">
        <div class="section-heading">
          <span>按需要选择</span>
          <h2 id="goal-title">吃得合适，比吃得更多更重要</h2>
        </div>
        <div class="goal-list">
          <article>
            <b>低卡</b>
            <p>按热量区间快速缩小选择范围。</p>
          </article>
          <article>
            <b>高蛋白</b>
            <p>优先查看蛋白质与整单营养信息。</p>
          </article>
          <article>
            <b>准时送达</b>
            <p>结算时选择可预约的配送日期与时段。</p>
          </article>
        </div>
      </section>

      <section class="path-section">
        <div>
          <span>从下一餐开始</span>
          <h2>浏览菜单，加入购物车，再预约送达时间。</h2>
        </div>
        <RouterLink to="/catalog">开始选择 →</RouterLink>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query';
import ProductCard from '@/components/common/ProductCard.vue';
import {
  getHotProducts,
  getRecommendedProducts,
  getShopCategoryTree,
} from '@/api/modules/product';
import { useUserStore } from '@/store/modules/user';
import { formatPrice } from '@/utils/product';

defineOptions({ name: 'HomePage' });
const userStore = useUserStore();

const RECOMMEND_LIMIT = 10;
const HOT_LIMIT = 6;

const { data: categoryTree } = useQuery({
  queryKey: ['shop', 'categories'],
  queryFn: ({ signal }) => getShopCategoryTree(signal),
});
const { data: recommended } = useQuery({
  queryKey: ['shop', 'products', 'recommended'],
  queryFn: ({ signal }) => getRecommendedProducts(RECOMMEND_LIMIT, signal),
});
const { data: hot } = useQuery({
  queryKey: ['shop', 'products', 'hot'],
  queryFn: ({ signal }) => getHotProducts(HOT_LIMIT, signal),
});

const roots = computed(() => categoryTree.value ?? []);
</script>

<style scoped lang="scss">
.home-page {
  padding-top: calc(var(--space-4) + env(safe-area-inset-top));
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.brand {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  font-size: 1.0625rem;
  letter-spacing: -0.02em;

  .svg-icon {
    font-size: 1.4rem;
    color: var(--color-primary);
  }
}

.guest-actions {
  display: flex;
  gap: var(--space-3);
  align-items: center;

  a {
    display: inline-flex;
    align-items: center;
    min-height: 2.25rem;
    color: var(--color-text-secondary);
    text-decoration: none;
  }

  .register-link {
    padding: 0 var(--space-3);
    color: var(--color-primary-contrast);
    background: var(--color-primary);
    border-radius: var(--radius-md);
  }
}

.member-link {
  display: inline-flex;
  align-items: center;
  min-height: 2.25rem;
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: none;
}

.hero {
  position: relative;
  display: grid;
  min-height: 23rem;
  padding: var(--space-8) var(--space-6);
  margin-top: var(--space-5);
  overflow: hidden;
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-radius: var(--radius-lg);
}

.hero__copy {
  position: relative;
  z-index: 1;
  align-self: center;

  > span,
  .path-section span {
    font-size: var(--text-caption);
    font-weight: 700;
    letter-spacing: 0.08em;
  }

  h1 {
    margin: var(--space-3) 0;
    font-size: clamp(2rem, 8vw, 3.4rem);
    font-weight: 780;
    line-height: 1.08;
    letter-spacing: -0.055em;
  }

  p {
    max-width: 28rem;
    margin: 0;
    line-height: 1.7;
    color: rgb(255 255 255 / 80%);
  }
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-6);

  a {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: var(--touch-target);
    padding: 0 var(--space-4);
    font-weight: 700;
    text-decoration: none;
    border-radius: var(--radius-md);
  }
}

.primary-action {
  color: var(--color-primary);
  background: var(--color-primary-contrast);
}

.secondary-action {
  color: var(--color-primary-contrast);
  border: 1px solid rgb(255 255 255 / 55%);
}

.hero__mark {
  position: absolute;
  right: -2rem;
  bottom: -2.5rem;
  font-size: 13rem;
  color: rgb(255 255 255 / 10%);
  transform: rotate(-8deg);
}

.goal-section {
  padding-top: var(--space-8);
}

.section-heading span,
.path-section span {
  color: var(--color-primary);
}

.section-heading h2,
.path-section h2 {
  margin: var(--space-2) 0 0;
  font-size: var(--text-section-title);
  font-weight: 750;
  line-height: 1.35;
}

.goal-list {
  display: grid;
  gap: 0;
  margin-top: var(--space-5);
  border-top: 1px solid var(--color-border);

  article {
    display: grid;
    grid-template-columns: 5rem 1fr;
    gap: var(--space-4);
    padding: var(--space-4) 0;
    border-bottom: 1px solid var(--color-border);
  }

  b {
    color: var(--color-primary);
  }

  p {
    margin: 0;
    color: var(--color-text-secondary);
  }
}

.path-section {
  display: flex;
  gap: var(--space-5);
  align-items: end;
  justify-content: space-between;
  padding: var(--space-7, 1.75rem) 0 var(--space-6);

  h2 {
    max-width: 34rem;
  }

  > a {
    flex: 0 0 auto;
    min-height: var(--touch-target);
    font-weight: 750;
    line-height: var(--touch-target);
    color: var(--color-primary);
    text-decoration: none;
  }
}

.category-section {
  padding-top: var(--space-7, 1.75rem);
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding-top: var(--space-4);
}

.category-item {
  display: inline-flex;
  gap: var(--space-1);
  align-items: center;
  min-height: var(--touch-target);
  padding: 0 var(--space-3);
  font-size: var(--text-secondary);
  font-weight: 600;
  color: var(--color-text);
  text-decoration: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-full);

  &:hover {
    border-color: var(--color-primary);
  }
}

.category-item__icon {
  font-size: 1.125rem;
}

.shelf-section,
.hot-section {
  padding-top: var(--space-7, 1.75rem);
}

.shelf {
  display: flex;
  gap: var(--space-3);
  padding-top: var(--space-4);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.shelf__item {
  flex: 0 0 10rem;
}

.hot-list {
  padding: var(--space-4) 0 0;
  margin: 0;
  list-style: none;
}

.hot-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  min-height: var(--touch-target);
  padding: var(--space-2) 0;
  color: var(--color-text);
  text-decoration: none;
  border-bottom: 1px solid var(--color-border);
}

.hot-row__rank {
  flex: 0 0 auto;
  width: 1.5rem;
  font-weight: 750;
  color: var(--color-text-muted);
  text-align: center;
}

.hot-row__rank--top {
  color: var(--color-accent);
}

.hot-row__cover {
  flex: 0 0 auto;
  width: 3.25rem;
  height: 3.25rem;
  object-fit: cover;
  background: var(--color-background-soft);
  border-radius: var(--radius-md);
}

.hot-row__info {
  display: grid;
  flex: 1;
  min-width: 0;

  b {
    overflow: hidden;
    text-overflow: ellipsis;
    font-weight: 700;
    white-space: nowrap;
  }
}

.hot-row__meta {
  font-size: var(--text-caption);
  color: var(--color-text-muted);
}

.hot-row__price {
  flex: 0 0 auto;
  font-weight: 700;
  color: var(--color-danger);
}

@media (max-width: 560px) {
  .hero {
    min-height: 27rem;
    padding: var(--space-7) var(--space-5);
  }

  .hero__mark {
    right: -3.5rem;
    bottom: -3rem;
    font-size: 11rem;
  }

  .goal-list article {
    grid-template-columns: 4rem 1fr;
  }

  .path-section {
    flex-direction: column;
    align-items: stretch;
  }

  .path-section > a {
    align-self: flex-start;
  }
}
</style>
