<script setup lang="ts">
import { onErrorCaptured, ref } from 'vue';

const error = ref<Error | null>(null);

onErrorCaptured((captured) => {
  error.value = captured;
  return false;
});

const recover = () => window.location.reload();
</script>

<template>
  <main v-if="error" class="app-error" role="alert">
    <h1>页面暂时无法显示</h1>
    <p>刷新页面后可以重新加载当前内容。</p>
    <button type="button" @click="recover">重新加载</button>
  </main>
  <RouterView v-else />
</template>

<style scoped lang="scss">
.app-error {
  display: grid;
  place-content: center;
  min-height: 100dvh;
  padding: var(--space-8);
  text-align: center;

  h1 {
    margin: 0;
    font-size: var(--text-title);
  }

  p {
    color: var(--color-text-secondary);
  }

  button {
    min-height: var(--touch-target);
    padding: 0 var(--space-4);
    color: var(--color-primary-contrast);
    background: var(--color-primary);
    border: 0;
    border-radius: var(--radius-md);
  }
}
</style>
