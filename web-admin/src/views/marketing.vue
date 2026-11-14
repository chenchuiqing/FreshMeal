<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createAdminBanner,
  createAdminCoupon,
  createAdminNotice,
  decideAdminRefund,
  deleteAdminBanner,
  deleteAdminNotice,
  deleteAdminReview,
  getAdminBanners,
  getAdminCoupons,
  getAdminNotices,
  getAdminRefunds,
  getAdminReviews,
  issueAdminCoupon,
  replyAdminReview,
  updateAdminBanner,
  updateAdminCouponStatus,
  updateAdminNotice
} from "@/api/admin-business";
import type {
  AdminBanner,
  AdminCoupon,
  AdminNotice,
  AdminRefund,
  AdminReview
} from "@/api/admin-business";

defineOptions({ name: "CommerceMarketing" });

const activeTab = ref("coupon");
const loading = ref(false);
const coupons = ref<AdminCoupon[]>([]);
const reviews = ref<AdminReview[]>([]);
const banners = ref<AdminBanner[]>([]);
const notices = ref<AdminNotice[]>([]);
const refunds = ref<AdminRefund[]>([]);

const couponDialog = ref(false);
const couponSaving = ref(false);
const couponForm = reactive({
  id: 0,
  name: "",
  type: 1,
  threshold: 0,
  amount: 1,
  startTime: "",
  endTime: "",
  totalCount: 100
});

const issueDialog = ref(false);
const issueCouponId = ref(0);
const issueUserId = ref<number | null>(null);
const issueSaving = ref(false);

const reviewDialog = ref(false);
const reviewTarget = ref<AdminReview | null>(null);
const reviewReply = ref("");
const reviewSaving = ref(false);

const bannerDialog = ref(false);
const bannerSaving = ref(false);
const bannerForm = reactive({
  id: 0,
  title: "",
  image: "",
  link: "",
  sort: 0,
  status: 1
});

const noticeDialog = ref(false);
const noticeSaving = ref(false);
const noticeForm = reactive({
  id: 0,
  title: "",
  content: "",
  type: 1,
  status: 1
});

const refundDialog = ref(false);
const refundTarget = ref<AdminRefund | null>(null);
const refundDecision = ref(1);
const refundReply = ref("");
const refundSaving = ref(false);

const money = (value: number | null | undefined) =>
  Number(value ?? 0).toFixed(2);

async function loadAll() {
  loading.value = true;
  try {
    const [
      couponResult,
      reviewResult,
      bannerResult,
      noticeResult,
      refundResult
    ] = await Promise.all([
      getAdminCoupons(),
      getAdminReviews({ current: 1, size: 100 }),
      getAdminBanners(),
      getAdminNotices(),
      getAdminRefunds({ current: 1, size: 100 })
    ]);
    coupons.value = couponResult.data.records;
    reviews.value = reviewResult.data.records;
    banners.value = bannerResult.data.records;
    notices.value = noticeResult.data.records;
    refunds.value = refundResult.data.records;
  } catch (error) {
    ElMessage.error(
      error instanceof Error ? error.message : "营销数据加载失败"
    );
  } finally {
    loading.value = false;
  }
}

function openCoupon(coupon?: AdminCoupon) {
  Object.assign(couponForm, {
    id: coupon?.id ?? 0,
    name: coupon?.name ?? "",
    type: coupon?.type ?? 1,
    threshold: coupon?.threshold ?? 0,
    amount: coupon?.amount ?? 1,
    startTime: coupon?.startTime ?? "",
    endTime: coupon?.endTime ?? "",
    totalCount: coupon?.totalCount ?? 100
  });
  couponDialog.value = true;
}

async function saveCoupon() {
  if (!couponForm.name.trim() || !couponForm.startTime || !couponForm.endTime) {
    ElMessage.warning("请完整填写优惠券信息");
    return;
  }
  couponSaving.value = true;
  try {
    await createAdminCoupon({
      name: couponForm.name.trim(),
      type: couponForm.type,
      threshold: couponForm.threshold,
      amount: couponForm.amount,
      startTime: couponForm.startTime,
      endTime: couponForm.endTime,
      totalCount: couponForm.totalCount
    });
    ElMessage.success("优惠券已创建");
    couponDialog.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "优惠券创建失败");
  } finally {
    couponSaving.value = false;
  }
}

async function toggleCoupon(coupon: AdminCoupon) {
  try {
    await updateAdminCouponStatus(coupon.id, coupon.status === 1 ? 0 : 1);
    ElMessage.success("优惠券状态已更新");
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "状态更新失败");
  }
}

function openIssue(coupon: AdminCoupon) {
  issueCouponId.value = coupon.id;
  issueUserId.value = null;
  issueDialog.value = true;
}

async function saveIssue() {
  if (!issueUserId.value) {
    ElMessage.warning("请输入用户ID");
    return;
  }
  issueSaving.value = true;
  try {
    await issueAdminCoupon(issueCouponId.value, issueUserId.value);
    ElMessage.success("优惠券已发放");
    issueDialog.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "优惠券发放失败");
  } finally {
    issueSaving.value = false;
  }
}

function openReply(review: AdminReview) {
  reviewTarget.value = review;
  reviewReply.value = review.reply ?? "";
  reviewDialog.value = true;
}

async function saveReply() {
  if (!reviewTarget.value) return;
  reviewSaving.value = true;
  try {
    await replyAdminReview(reviewTarget.value.id, reviewReply.value.trim());
    ElMessage.success("商家回复已保存");
    reviewDialog.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "回复保存失败");
  } finally {
    reviewSaving.value = false;
  }
}

async function removeReview(review: AdminReview) {
  try {
    await ElMessageBox.confirm("确定删除这条评价吗？", "删除评价", {
      type: "warning"
    });
    await deleteAdminReview(review.id);
    ElMessage.success("评价已删除");
    await loadAll();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error instanceof Error ? error.message : "评价删除失败");
    }
  }
}

function openBanner(banner?: AdminBanner) {
  Object.assign(bannerForm, {
    id: banner?.id ?? 0,
    title: banner?.title ?? "",
    image: banner?.image ?? "",
    link: banner?.link ?? "",
    sort: banner?.sort ?? 0,
    status: banner?.status ?? 1
  });
  bannerDialog.value = true;
}

async function saveBanner() {
  if (!bannerForm.title.trim() || !bannerForm.image.trim()) {
    ElMessage.warning("请填写轮播标题和图片地址");
    return;
  }
  bannerSaving.value = true;
  try {
    const payload = {
      title: bannerForm.title.trim(),
      image: bannerForm.image.trim(),
      link: bannerForm.link.trim() || undefined,
      sort: bannerForm.sort,
      status: bannerForm.status
    };
    if (bannerForm.id) {
      await updateAdminBanner(bannerForm.id, payload);
    } else {
      await createAdminBanner(payload);
    }
    ElMessage.success("轮播图已保存");
    bannerDialog.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "轮播图保存失败");
  } finally {
    bannerSaving.value = false;
  }
}

async function removeBanner(banner: AdminBanner) {
  try {
    await ElMessageBox.confirm("确定删除这张轮播图吗？", "删除轮播图", {
      type: "warning"
    });
    await deleteAdminBanner(banner.id);
    ElMessage.success("轮播图已删除");
    await loadAll();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(
        error instanceof Error ? error.message : "轮播图删除失败"
      );
    }
  }
}

function openNotice(notice?: AdminNotice) {
  Object.assign(noticeForm, {
    id: notice?.id ?? 0,
    title: notice?.title ?? "",
    content: notice?.content ?? "",
    type: notice?.type ?? 1,
    status: notice?.status ?? 1
  });
  noticeDialog.value = true;
}

async function saveNotice() {
  if (!noticeForm.title.trim()) {
    ElMessage.warning("请填写公告标题");
    return;
  }
  noticeSaving.value = true;
  try {
    const payload = {
      title: noticeForm.title.trim(),
      content: noticeForm.content.trim() || undefined,
      type: noticeForm.type,
      status: noticeForm.status
    };
    if (noticeForm.id) {
      await updateAdminNotice(noticeForm.id, payload);
    } else {
      await createAdminNotice(payload);
    }
    ElMessage.success("公告已保存");
    noticeDialog.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "公告保存失败");
  } finally {
    noticeSaving.value = false;
  }
}

async function removeNotice(notice: AdminNotice) {
  try {
    await ElMessageBox.confirm("确定删除这条公告吗？", "删除公告", {
      type: "warning"
    });
    await deleteAdminNotice(notice.id);
    ElMessage.success("公告已删除");
    await loadAll();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error instanceof Error ? error.message : "公告删除失败");
    }
  }
}

function openRefund(refund: AdminRefund) {
  refundTarget.value = refund;
  refundDecision.value = 1;
  refundReply.value = "";
  refundDialog.value = true;
}

async function saveRefundDecision() {
  if (!refundTarget.value) return;
  refundSaving.value = true;
  try {
    await decideAdminRefund(
      refundTarget.value.id,
      refundDecision.value,
      refundReply.value.trim() || undefined
    );
    ElMessage.success("售后申请已处理");
    refundDialog.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "售后处理失败");
  } finally {
    refundSaving.value = false;
  }
}

const reviewStatusText = (status: number) => {
  if (status === 0) return "处理中";
  return status === 1 ? "已退款" : "已驳回";
};

const noticeTypeText = (type: number) => {
  if (type === 1) return "系统";
  return type === 2 ? "配送" : "活动";
};

const refundTagType = (status: number) => {
  if (status === 0) return "warning";
  return status === 1 ? "success" : "danger";
};

onMounted(loadAll);
</script>

<template>
  <div v-loading="loading" class="main marketing-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="优惠券" name="coupon">
        <div class="toolbar">
          <el-button type="primary" @click="openCoupon()">新建优惠券</el-button>
        </div>
        <el-table :data="coupons" border stripe>
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              {{ row.type === 1 ? "满减券" : "折扣券" }}
            </template>
          </el-table-column>
          <el-table-column label="面值" width="120">
            <template #default="{ row }">
              {{
                row.type === 1
                  ? `¥${money(row.amount)}`
                  : `${Math.round(row.amount * 10)}折`
              }}
            </template>
          </el-table-column>
          <el-table-column label="门槛" width="100" align="right">
            <template #default="{ row }">¥{{ money(row.threshold) }}</template>
          </el-table-column>
          <el-table-column label="发放/使用" width="130">
            <template #default="{ row }">
              {{ row.receivedCount }} / {{ row.usedCount }}
            </template>
          </el-table-column>
          <el-table-column label="有效期" min-width="220">
            <template #default="{ row }">
              {{ row.startTime }} 至 {{ row.endTime }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openIssue(row as AdminCoupon)"
              >
                发放
              </el-button>
              <el-button
                link
                type="warning"
                @click="toggleCoupon(row as AdminCoupon)"
              >
                {{ row.status === 1 ? "停用" : "启用" }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="评价管理" name="review">
        <el-table :data="reviews" border stripe>
          <el-table-column prop="orderNo" label="订单号" min-width="170" />
          <el-table-column label="用户" min-width="130">
            <template #default="{ row }">
              {{ row.nickname || row.username || row.userId }}
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品" min-width="150" />
          <el-table-column prop="rating" label="评分" width="80" />
          <el-table-column prop="content" label="评价内容" min-width="220" />
          <el-table-column prop="reply" label="商家回复" min-width="200" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openReply(row as AdminReview)"
              >
                回复
              </el-button>
              <el-button
                link
                type="danger"
                @click="removeReview(row as AdminReview)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="轮播与公告" name="content">
        <div class="content-grid">
          <section class="content-panel">
            <div class="toolbar">
              <h3>轮播图</h3>
              <el-button type="primary" @click="openBanner()"
                >新建轮播</el-button
              >
            </div>
            <el-table :data="banners" border stripe>
              <el-table-column prop="title" label="标题" min-width="150" />
              <el-table-column prop="image" label="图片" min-width="220" />
              <el-table-column prop="sort" label="排序" width="80" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'">
                    {{ row.status === 1 ? "启用" : "停用" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    @click="openBanner(row as AdminBanner)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    @click="removeBanner(row as AdminBanner)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section class="content-panel">
            <div class="toolbar">
              <h3>公告</h3>
              <el-button type="primary" @click="openNotice()"
                >新建公告</el-button
              >
            </div>
            <el-table :data="notices" border stripe>
              <el-table-column prop="title" label="标题" min-width="160" />
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  {{ noticeTypeText(row.type) }}
                </template>
              </el-table-column>
              <el-table-column prop="content" label="内容" min-width="240" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'">
                    {{ row.status === 1 ? "启用" : "停用" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    @click="openNotice(row as AdminNotice)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    @click="removeNotice(row as AdminNotice)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="售后退款" name="refund">
        <el-table :data="refunds" border stripe>
          <el-table-column prop="orderNo" label="订单号" min-width="170" />
          <el-table-column label="用户" min-width="130">
            <template #default="{ row }">
              {{ row.nickname || row.username || row.userId }}
            </template>
          </el-table-column>
          <el-table-column label="退款金额" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="申请原因" min-width="220" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="refundTagType(row.status)">
                {{ reviewStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="adminReply" label="处理说明" min-width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="openRefund(row as AdminRefund)"
              >
                处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="couponDialog" title="新建优惠券" width="520px">
      <el-form label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="couponForm.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="couponForm.type" class="w-full">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="门槛">
          <el-input-number
            v-model="couponForm.threshold"
            :min="0"
            :precision="2"
          />
        </el-form-item>
        <el-form-item :label="couponForm.type === 1 ? '优惠金额' : '折扣率'">
          <el-input-number
            v-model="couponForm.amount"
            :min="0"
            :max="couponForm.type === 1 ? undefined : 0.99"
            :precision="2"
          />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="couponForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker
            v-model="couponForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="couponForm.totalCount" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="couponDialog = false">取消</el-button>
        <el-button type="primary" :loading="couponSaving" @click="saveCoupon">
          创建
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueDialog" title="发放优惠券" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户ID">
          <el-input-number v-model="issueUserId" :min="1" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialog = false">取消</el-button>
        <el-button type="primary" :loading="issueSaving" @click="saveIssue">
          发放
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialog" title="回复评价" width="520px">
      <el-input
        v-model="reviewReply"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入商家回复"
      />
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="reviewSaving" @click="saveReply">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bannerDialog"
      :title="bannerForm.id ? '编辑轮播图' : '新建轮播图'"
      width="520px"
    >
      <el-form label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="bannerForm.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="bannerForm.image" maxlength="255" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="bannerForm.link" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="bannerForm.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerDialog = false">取消</el-button>
        <el-button type="primary" :loading="bannerSaving" @click="saveBanner">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="noticeDialog"
      :title="noticeForm.id ? '编辑公告' : '新建公告'"
      width="560px"
    >
      <el-form label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="noticeForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="noticeForm.content"
            type="textarea"
            :rows="5"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="noticeForm.type" class="w-full">
            <el-option label="系统公告" :value="1" />
            <el-option label="配送公告" :value="2" />
            <el-option label="活动公告" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="noticeForm.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialog = false">取消</el-button>
        <el-button type="primary" :loading="noticeSaving" @click="saveNotice">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialog" title="处理售后申请" width="520px">
      <el-alert
        v-if="refundTarget"
        type="info"
        :closable="false"
        :title="`${refundTarget.orderNo} · ¥${money(refundTarget.amount)}`"
        class="mb-4"
      />
      <el-form label-width="90px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="refundDecision">
            <el-radio :value="1">退款通过</el-radio>
            <el-radio :value="2">驳回申请</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="refundReply"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="可选，会展示给用户"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="refundSaving"
          @click="saveRefundDecision"
        >
          确认处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.marketing-page {
  min-width: 0;
}

.toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 16px;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.content-panel {
  min-width: 0;
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
