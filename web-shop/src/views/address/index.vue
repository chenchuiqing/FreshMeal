<template>
  <div class="address-page page">
    <header class="page-header">
      <div>
        <span>收货地址</span>
        <h1>管理配送位置</h1>
        <p>经纬度用于预估配送距离、运费计算和后续派单。</p>
      </div>
      <van-button size="small" type="primary" @click="openCreate">
        新增
      </van-button>
    </header>

    <div v-if="isLoading" class="state-panel">
      <van-loading vertical>地址加载中…</van-loading>
    </div>
    <div v-else-if="isError" class="state-panel" role="alert">
      <p>{{ errorText }}</p>
      <van-button plain type="primary" @click="refetch">重新加载</van-button>
    </div>
    <van-empty
      v-else-if="addresses && addresses.length === 0"
      description="还没有收货地址"
    >
      <van-button type="primary" @click="openCreate">新增地址</van-button>
    </van-empty>

    <ul v-else-if="addresses" class="address-list">
      <li
        v-for="address in addresses"
        :key="address.id"
        class="address-card surface-card"
        :class="{ 'address-card--default': address.isDefault }"
      >
        <div class="address-card__body">
          <div class="address-card__identity">
            <strong>{{ address.receiver }}</strong>
            <span>{{ address.phone }}</span>
            <van-tag v-if="address.isDefault" type="primary">默认</van-tag>
            <van-tag v-if="address.tag" plain>{{ address.tag }}</van-tag>
          </div>
          <p>{{ address.fullAddress }}</p>
          <small v-if="address.longitude != null && address.latitude != null">
            坐标 {{ address.longitude }}, {{ address.latitude }}
          </small>
        </div>
        <div class="address-card__actions">
          <button
            v-if="!address.isDefault"
            type="button"
            @click="makeDefault(address.id)"
          >
            设为默认
          </button>
          <button type="button" @click="openEdit(address)">编辑</button>
          <button type="button" class="danger" @click="removeAddress(address.id)">
            删除
          </button>
        </div>
      </li>
    </ul>

    <van-popup
      v-model:show="editorVisible"
      position="bottom"
      round
      safe-area-inset-bottom
      class="address-editor"
    >
      <form @submit.prevent="saveAddress">
        <header>
          <div>
            <span>{{ editingId == null ? '新增地址' : '编辑地址' }}</span>
            <h2>收货信息</h2>
          </div>
          <button type="button" aria-label="关闭" @click="editorVisible = false">
            关闭
          </button>
        </header>

        <div class="form-grid">
          <label>
            <span>收货人</span>
            <input
              v-model.trim="form.receiver"
              type="text"
              maxlength="50"
              autocomplete="name"
              placeholder="请输入姓名"
              required
            />
          </label>
          <label>
            <span>手机号</span>
            <input
              v-model.trim="form.phone"
              type="tel"
              maxlength="11"
              autocomplete="tel"
              inputmode="numeric"
              placeholder="11位手机号"
              required
            />
          </label>
          <label>
            <span>省份</span>
            <input v-model.trim="form.province" type="text" maxlength="30" required />
          </label>
          <label>
            <span>城市</span>
            <input v-model.trim="form.city" type="text" maxlength="30" required />
          </label>
          <label>
            <span>区县</span>
            <input v-model.trim="form.district" type="text" maxlength="30" required />
          </label>
          <label>
            <span>地址标签</span>
            <input v-model.trim="form.tag" type="text" maxlength="20" placeholder="家、公司等" />
          </label>
          <label class="form-grid__wide">
            <span>详细地址</span>
            <textarea
              v-model.trim="form.detail"
              rows="2"
              maxlength="200"
              placeholder="街道、楼栋、门牌号"
              required
            />
          </label>
          <label>
            <span>经度</span>
            <input
              v-model.trim="form.longitude"
              type="number"
              step="0.0000001"
              min="-180"
              max="180"
              inputmode="decimal"
              placeholder="例如 121.473701"
              required
            />
          </label>
          <label>
            <span>纬度</span>
            <input
              v-model.trim="form.latitude"
              type="number"
              step="0.0000001"
              min="-90"
              max="90"
              inputmode="decimal"
              placeholder="例如 31.230416"
              required
            />
          </label>
        </div>

        <label class="default-switch">
          <span>
            <strong>设为默认地址</strong>
            <small>结算页将优先选择该地址</small>
          </span>
          <van-switch v-model="form.isDefault" size="28px" />
        </label>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <van-button
          block
          type="primary"
          size="large"
          native-type="submit"
          :loading="saving"
          loading-text="保存中…"
        >
          保存地址
        </van-button>
      </form>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { useQuery, useQueryClient } from '@tanstack/vue-query';
import {
  createAddress,
  deleteAddress,
  listAddresses,
  setDefaultAddress,
  updateAddress,
} from '@/api/modules/trade';
import type { Address, AddressSaveRequest } from '@/api/modules/trade';
import { isApiError } from '@/types/api/common';

defineOptions({ name: 'AddressPage' });

interface AddressForm {
  receiver: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  longitude: string;
  latitude: string;
  tag: string;
  isDefault: boolean;
}

const queryClient = useQueryClient();
const editorVisible = ref(false);
const editingId = ref<number | null>(null);
const saving = ref(false);
const formError = ref('');
const form = reactive<AddressForm>(emptyForm());

const {
  data: addresses,
  isLoading,
  isError,
  error,
  refetch,
} = useQuery({
  queryKey: ['shop', 'addresses'],
  queryFn: ({ signal }) => listAddresses(signal),
});

const errorText = computed(() =>
  isApiError(error.value) ? error.value.message : '地址加载失败',
);

function emptyForm(): AddressForm {
  return {
    receiver: '',
    phone: '',
    province: '上海市',
    city: '上海市',
    district: '',
    detail: '',
    longitude: '',
    latitude: '',
    tag: '',
    isDefault: false,
  };
}

function assignForm(value: Partial<AddressForm>) {
  Object.assign(form, emptyForm(), value);
}

function openCreate() {
  editingId.value = null;
  assignForm({});
  formError.value = '';
  editorVisible.value = true;
}

function openEdit(address: Address) {
  editingId.value = address.id;
  assignForm({
    receiver: address.receiver,
    phone: address.phone,
    province: address.province,
    city: address.city,
    district: address.district,
    detail: address.detail,
    longitude:
      typeof address.longitude === 'number'
        ? String(address.longitude)
        : '',
    latitude:
      typeof address.latitude === 'number' ? String(address.latitude) : '',
    tag: address.tag ?? '',
    isDefault: address.isDefault,
  });
  formError.value = '';
  editorVisible.value = true;
}

function buildRequest(): AddressSaveRequest {
  const longitude = Number(form.longitude);
  const latitude = Number(form.latitude);
  if (!form.receiver || !form.phone || !form.province || !form.city) {
    throw new Error('请完整填写收货人、电话和省市区');
  }
  if (!/^1\d{10}$/.test(form.phone)) {
    throw new Error('请输入正确的11位手机号');
  }
  if (!form.district || !form.detail) {
    throw new Error('请填写区县和详细地址');
  }
  if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    throw new Error('经度必须在 -180 到 180 之间');
  }
  if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90) {
    throw new Error('纬度必须在 -90 到 90 之间');
  }
  return {
    receiver: form.receiver,
    phone: form.phone,
    province: form.province,
    city: form.city,
    district: form.district,
    detail: form.detail,
    longitude,
    latitude,
    tag: form.tag,
    isDefault: form.isDefault,
  };
}

async function saveAddress() {
  if (saving.value) return;
  formError.value = '';
  let request: AddressSaveRequest;
  try {
    request = buildRequest();
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '地址填写不完整';
    return;
  }
  saving.value = true;
  try {
    await (editingId.value === null
      ? createAddress(request)
      : updateAddress(editingId.value, request));
    await queryClient.invalidateQueries({ queryKey: ['shop', 'addresses'] });
    editorVisible.value = false;
    showToast({ message: '地址已保存', icon: 'success' });
  } catch (error) {
    formError.value = isApiError(error) ? error.message : '地址保存失败';
  } finally {
    saving.value = false;
  }
}

async function makeDefault(id: number) {
  try {
    await setDefaultAddress(id);
    await queryClient.invalidateQueries({ queryKey: ['shop', 'addresses'] });
    showToast({ message: '默认地址已更新', icon: 'success' });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '设置默认地址失败');
  }
}

async function removeAddress(id: number) {
  try {
    await showConfirmDialog({
      title: '删除地址',
      message: '删除后无法用于新的结算订单。',
      confirmButtonText: '确认删除',
      confirmButtonColor: 'var(--color-danger)',
    });
  } catch {
    return;
  }
  try {
    await deleteAddress(id);
    await queryClient.invalidateQueries({ queryKey: ['shop', 'addresses'] });
    showToast({ message: '地址已删除', icon: 'success' });
  } catch (error) {
    showToast(isApiError(error) ? error.message : '删除地址失败');
  }
}
</script>

<style scoped lang="scss">
.address-page {
  display: grid;
  gap: var(--space-4);
  padding-top: var(--space-5);
}

.page-header :deep(.van-button) {
  align-self: center;
}

.address-list {
  display: grid;
  gap: var(--space-3);
  padding: 0;
  margin: 0;
  list-style: none;
}

.address-card {
  display: grid;
  gap: var(--space-3);
  border-left: 3px solid var(--color-border);

  &--default {
    border-left-color: var(--color-primary);
  }
}

.address-card__identity {
  display: flex;
  gap: var(--space-2);
  align-items: center;

  span {
    color: var(--color-text-secondary);
  }
}

.address-card__body {
  display: grid;
  gap: var(--space-1);

  p {
    margin: 0;
    line-height: var(--leading-body);
  }

  small {
    color: var(--color-text-muted);
  }
}

.address-card__actions {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);

  button {
    min-height: var(--touch-target);
    padding: 0 var(--space-3);
    color: var(--color-primary);
    background: transparent;
    border: 0;

    &.danger {
      color: var(--color-danger);
    }
  }
}

.address-editor {
  max-height: 92dvh;
  overflow: hidden auto;
}

.address-editor form {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-4)
    calc(var(--space-5) + env(safe-area-inset-bottom));
}

.address-editor header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  span {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }

  h2 {
    margin: var(--space-1) 0 0;
    font-size: var(--text-section-title);
  }

  button {
    min-width: var(--touch-target);
    min-height: var(--touch-target);
    color: var(--color-text-secondary);
    background: transparent;
    border: 0;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);

  label {
    display: grid;
    gap: var(--space-1);
    min-width: 0;

    > span {
      font-size: var(--text-caption);
      font-weight: 650;
    }
  }

  input,
  textarea {
    width: 100%;
    min-height: var(--control-height);
    padding: var(--space-2) var(--space-3);
    color: var(--color-text);
    resize: vertical;
    background: var(--color-surface);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-md);
  }

  input:focus,
  textarea:focus {
    outline: 0;
    border-color: var(--color-primary);
  }

  &__wide {
    grid-column: 1 / -1;
  }
}

.default-switch {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  background: var(--color-background-soft);
  border-radius: var(--radius-md);

  span {
    display: grid;
    gap: var(--space-1);
  }

  small {
    color: var(--color-text-secondary);
  }
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}

@media (max-width: 374px) {
  .form-grid {
    grid-template-columns: 1fr;

    &__wide {
      grid-column: auto;
    }
  }
}
</style>
