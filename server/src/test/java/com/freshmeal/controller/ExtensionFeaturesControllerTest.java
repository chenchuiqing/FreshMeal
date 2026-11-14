package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmeal.entity.BrowsingHistory;
import com.freshmeal.entity.Favorite;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.entity.Refund;
import com.freshmeal.entity.Review;
import com.freshmeal.entity.UserCoupon;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.BrowsingHistoryMapper;
import com.freshmeal.mapper.CouponMapper;
import com.freshmeal.mapper.FavoriteMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.RefundMapper;
import com.freshmeal.mapper.ReviewMapper;
import com.freshmeal.mapper.UserCouponMapper;
import com.freshmeal.service.CouponService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtensionFeaturesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private BrowsingHistoryMapper historyMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private RefundMapper refundMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private CouponService couponService;

    private final List<Long> createdCouponIds = new ArrayList<>();
    private final List<Long> createdUserCouponIds = new ArrayList<>();
    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdRefundIds = new ArrayList<>();
    private final List<Long> createdItemIds = new ArrayList<>();
    private final List<Long> createdReviewIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdRefundIds.forEach(refundMapper::deleteById);
        createdReviewIds.forEach(reviewMapper::deleteById);
        createdItemIds.forEach(orderItemMapper::deleteById);
        createdOrderIds.forEach(orderId -> {
            orderStatusLogMapper.delete(Wrappers.<OrderStatusLog>lambdaQuery()
                    .eq(OrderStatusLog::getOrderId, orderId));
            orderMapper.deleteById(orderId);
        });
        createdUserCouponIds.forEach(userCouponMapper::deleteById);
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, 1L)
                .eq(Favorite::getProductId, 102L));
        historyMapper.delete(Wrappers.<BrowsingHistory>lambdaQuery()
                .eq(BrowsingHistory::getUserId, 1L)
                .eq(BrowsingHistory::getProductId, 102L));
        createdCouponIds.forEach(id -> {
            userCouponMapper.delete(Wrappers.<UserCoupon>lambdaQuery()
                    .eq(UserCoupon::getCouponId, id));
            couponMapper.deleteById(id);
        });
        createdCouponIds.clear();
        createdUserCouponIds.clear();
        createdOrderIds.clear();
        createdRefundIds.clear();
        createdItemIds.clear();
        createdReviewIds.clear();
    }

    @Test
    void couponFavoriteHistoryAndContentEndpointsWork() throws Exception {
        String userToken = login();

        mockMvc.perform(get("/api/shop/coupon/available")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").exists());

        MvcResult receiveResult = mockMvc.perform(post("/api/shop/coupon/1/receive")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponId").value(1))
                .andReturn();
        long userCouponId = objectMapper.readTree(
                        receiveResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        createdUserCouponIds.add(userCouponId);

        assertThat(couponService.calculateDiscount(
                1L, userCouponId, new BigDecimal("56.00")))
                .isEqualByComparingTo("5.00");
        couponService.markUsed(userCouponId, 999999L);
        assertThat(userCouponMapper.selectById(userCouponId).getStatus()).isEqualTo(1);
        couponService.release(userCouponId);
        assertThat(userCouponMapper.selectById(userCouponId).getStatus()).isEqualTo(0);

        mockMvc.perform(get("/api/shop/coupon/list")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + userCouponId + ")]").exists());

        mockMvc.perform(post("/api/shop/favorite/102")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/shop/favorite/ids")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@ == 102)]").exists());

        mockMvc.perform(post("/api/shop/history/102")
                        .header("Authorization", userToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/shop/history/page?current=1&size=20")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.id == 102)]").exists());

        mockMvc.perform(get("/api/shop/content/banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].image").exists());
        mockMvc.perform(get("/api/shop/content/notice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").exists());
    }

    @Test
    void adminMarketingRefundAndExcelEndpointsWork() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(get("/api/admin/coupon/page")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
        MvcResult couponResult = mockMvc.perform(post("/api/admin/coupon")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"自动化测试券","type":1,"threshold":10,
                                 "amount":2,"startTime":"2026-01-01T00:00:00",
                                 "endTime":"2030-01-01T00:00:00","totalCount":10}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long couponId = objectMapper.readTree(
                        couponResult.getResponse().getContentAsString())
                .path("data").asLong();
        createdCouponIds.add(couponId);

        mockMvc.perform(get("/api/admin/review/page?current=1&size=20")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].productName").exists());
        mockMvc.perform(get("/api/admin/content/banner")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
        mockMvc.perform(get("/api/admin/refund/page?current=1&size=20")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].orderNo").exists());
        mockMvc.perform(get("/api/admin/report/orders.xlsx")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void userCanCancelPaidWaitingOrderAndRefundFlowCanBeRejected()
            throws Exception {
        String userToken = login();
        String adminToken = loginAdmin();
        Order cancelOrder = createPaidWaitingOrder("FMTESTCANCEL");
        mockMvc.perform(delete("/api/shop/order/FMTESTCANCEL")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"自动化取消测试\""))
                .andExpect(status().isOk());
        Order cancelled = orderMapper.selectById(cancelOrder.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());

        Order refundOrder = createPaidWaitingOrder("FMTESTREFUND");
        mockMvc.perform(post("/api/shop/refund/FMTESTREFUND")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"自动化退款测试\"}"))
                .andExpect(status().isOk());
        Refund refund = refundMapper.selectOne(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, refundOrder.getId()));
        assertThat(refund).isNotNull();
        createdRefundIds.add(refund.getId());
        assertThat(orderMapper.selectById(refundOrder.getId()).getStatus())
                .isEqualTo(OrderStatus.REFUNDING.getCode());

        mockMvc.perform(put("/api/admin/refund/" + refund.getId() + "/decision")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2,\"adminReply\":\"测试驳回\"}"))
                .andExpect(status().isOk());
        assertThat(orderMapper.selectById(refundOrder.getId()).getStatus())
                .isEqualTo(OrderStatus.WAIT_ACCEPT.getCode());
    }

    @Test
    void userCanSubmitReviewAndAdminCanReply() throws Exception {
        String userToken = login();
        String adminToken = loginAdmin();
        Order order = createPaidWaitingOrder("FMTESTREVIEW");
        order.setStatus(OrderStatus.FINISHED.getCode());
        order.setFinishTime(java.time.LocalDateTime.now());
        orderMapper.updateById(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setUserId(1L);
        item.setProductId(101L);
        item.setProductName("香煎鸡胸沙拉");
        item.setProductCover("/api/files/products/p101.svg");
        item.setSpecName("标准份");
        item.setPrice(new BigDecimal("28.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("28.00"));
        item.setCalories(352);
        orderItemMapper.insert(item);
        createdItemIds.add(item.getId());

        MvcResult reviewResult = mockMvc.perform(post(
                        "/api/shop/order/FMTESTREVIEW/review")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":101,"rating":5,
                                 "content":"自动化评价测试","images":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andReturn();
        long reviewId = objectMapper.readTree(
                        reviewResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        createdReviewIds.add(reviewId);

        mockMvc.perform(get("/api/shop/order/FMTESTREVIEW/review")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("自动化评价测试"));
        mockMvc.perform(put("/api/admin/review/" + reviewId + "/reply")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reply\":\"自动化商家回复\"}"))
                .andExpect(status().isOk());
        assertThat(reviewMapper.selectById(reviewId).getReply())
                .isEqualTo("自动化商家回复");
    }

    private Order createPaidWaitingOrder(String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(1L);
        order.setGoodsAmount(new BigDecimal("28.00"));
        order.setFreightAmount(new BigDecimal("5.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("33.00"));
        order.setDeliveryManFee(BigDecimal.ZERO);
        order.setReceiver("测试用户");
        order.setReceiverPhone("13800000001");
        order.setReceiverAddress("上海市浦东新区测试路1号");
        order.setDeliveryType(1);
        order.setStatus(OrderStatus.WAIT_ACCEPT.getCode());
        order.setPayStatus(1);
        order.setPayType(1);
        orderMapper.insert(order);
        createdOrderIds.add(order.getId());
        return order;
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shop/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user01","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(
                        result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(
                        result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
