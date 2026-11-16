package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmeal.entity.DeliveryManLog;
import com.freshmeal.entity.DeliveryOrder;
import com.freshmeal.entity.Order;
import com.freshmeal.enums.DeliveryStatus;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.DeliveryManLogMapper;
import com.freshmeal.mapper.DeliveryOrderMapper;
import com.freshmeal.mapper.OrderMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;

    @Autowired
    private DeliveryManLogMapper deliveryManLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String testOrderNo;
    private Long testRiderId;
    private Long testAreaId;
    private Long testSlotId;
    private Long testBatchId;
    private Long testDeliveryId;
    private Long testProductId;
    private Integer originalProductStock;

    @AfterEach
    void cleanUp() {
        if (testOrderNo != null) {
            jdbcTemplate.update(
                    "DELETE FROM t_order_status_log WHERE order_no = ?",
                    testOrderNo);
            jdbcTemplate.update(
                    "DELETE FROM t_order WHERE order_no = ?", testOrderNo);
            testOrderNo = null;
        }
        if (testRiderId != null) {
            jdbcTemplate.update(
                    "DELETE FROM t_delivery_man WHERE id = ?", testRiderId);
            testRiderId = null;
        }
        if (testAreaId != null) {
            jdbcTemplate.update(
                    "DELETE FROM t_delivery_area WHERE id = ?", testAreaId);
            testAreaId = null;
        }
        if (testSlotId != null) {
            jdbcTemplate.update(
                    "DELETE FROM t_delivery_slot WHERE id = ?", testSlotId);
            testSlotId = null;
        }
        if (testDeliveryId != null) {
            jdbcTemplate.update(
                    "DELETE FROM t_delivery_man_log WHERE delivery_order_id = ?",
                    testDeliveryId);
            jdbcTemplate.update(
                    "DELETE FROM t_delivery_order WHERE id = ?", testDeliveryId);
            testDeliveryId = null;
        }
        if (testBatchId != null) {
            jdbcTemplate.update(
                    "DELETE FROM t_inventory_log WHERE batch_id = ?", testBatchId);
            jdbcTemplate.update(
                    "DELETE FROM t_product_batch WHERE id = ?", testBatchId);
            testBatchId = null;
        }
        if (testProductId != null && originalProductStock != null) {
            jdbcTemplate.update(
                    "UPDATE t_product SET stock = ? WHERE id = ?",
                    originalProductStock, testProductId);
            testProductId = null;
            originalProductStock = null;
        }
    }

    @Test
    void adminCanQueryAndInterveneOrdersWhileIllegalTransitionIsRejected()
            throws Exception {
        String token = adminToken();
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_user WHERE username = 'user01'",
                Long.class);
        Order order = new Order();
        order.setOrderNo("TEST-ADM-" + System.nanoTime());
        order.setUserId(userId);
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        order.setPayStatus(0);
        order.setDeliveryType(2);
        order.setGoodsAmount(new BigDecimal("10.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("10.00"));
        order.setReceiver("测试用户");
        order.setReceiverPhone("13800000001");
        order.setReceiverAddress("测试地址");
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);
        testOrderNo = order.getOrderNo();

        mockMvc.perform(get("/api/admin/order/page")
                        .header("Authorization", "Bearer " + token)
                        .param("orderNo", testOrderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].statusText").value("待支付"));

        mockMvc.perform(get("/api/admin/order/{orderNo}", testOrderNo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.orderNo").value(testOrderNo))
                .andExpect(jsonPath("$.data.timeline").isArray());

        mockMvc.perform(put("/api/admin/order/{orderNo}/status", testOrderNo)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":1,\"reason\":\"管理员接单\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.payStatus").value(1));

        mockMvc.perform(put("/api/admin/order/{orderNo}/status", testOrderNo)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":6}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));
    }

    @Test
    void adminPageQueriesWithoutOptionalFiltersReturnOk() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/order/page")
                        .header("Authorization", "Bearer " + token)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());

        mockMvc.perform(get("/api/admin/delivery/order/page")
                        .header("Authorization", "Bearer " + token)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());

        mockMvc.perform(get("/api/admin/batch/page")
                        .header("Authorization", "Bearer " + token)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void adminCanMaintainDeliveryConfigurationAndRiderAccounts()
            throws Exception {
        String token = adminToken();
        String suffix = String.valueOf(System.nanoTime()).substring(0, 8);

        MvcResult areaResult = mockMvc.perform(post("/api/admin/delivery/area")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试区域-%s",
                                  "city": "上海市",
                                  "district": "测试区",
                                  "baseFee": 6.50,
                                  "perKmFee": 2.20,
                                  "freeThreshold": 88.00,
                                  "nightSurcharge": 5.00,
                                  "sort": 99,
                                  "status": 1
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        testAreaId = readId(areaResult);

        mockMvc.perform(put("/api/admin/delivery/area/{id}", testAreaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试区域-%s",
                                  "city": "上海市",
                                  "district": "测试区",
                                  "baseFee": 7.77,
                                  "perKmFee": 2.20,
                                  "freeThreshold": 88.00,
                                  "nightSurcharge": 5.00,
                                  "sort": 99,
                                  "status": 1
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseFee").value(7.77));

        mockMvc.perform(post("/api/admin/delivery/slot")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试时段-%s",
                                  "startTime": "11:00:00",
                                  "endTime": "13:00:00",
                                  "orderDeadline": "14:00:00",
                                  "dailyCapacity": 20,
                                  "advanceDays": 1,
                                  "sort": 99,
                                  "status": 1
                                }
                                """.formatted(suffix)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        MvcResult slotResult = mockMvc.perform(post("/api/admin/delivery/slot")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试时段-%s",
                                  "startTime": "11:00:00",
                                  "endTime": "13:00:00",
                                  "orderDeadline": "10:30:00",
                                  "dailyCapacity": 20,
                                  "advanceDays": 1,
                                  "sort": 99,
                                  "status": 1
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andReturn();
        testSlotId = readId(slotResult);

        MvcResult riderResult = mockMvc.perform(post("/api/admin/delivery/man")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test_rider_%s",
                                  "password": "test-password",
                                  "realName": "测试配送员",
                                  "phone": "13900000001",
                                  "maxOrderNum": 3,
                                  "serviceAreaId": %d,
                                  "status": 1
                                }
                                """.formatted(suffix, testAreaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.serviceAreaId").value(testAreaId))
                .andReturn();
        testRiderId = readId(riderResult);

        mockMvc.perform(get("/api/admin/delivery/man/page")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "test_rider_" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(put("/api/admin/delivery/man/{id}/work", testRiderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onlineStatus\":1,\"workStatus\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onlineStatus").value(1));

        mockMvc.perform(put("/api/admin/delivery/man/{id}/work", testRiderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onlineStatus\":0,\"workStatus\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void batchEntryLossAndInventoryLogsRemainConsistent() throws Exception {
        String token = adminToken();
        testProductId = 101L;
        originalProductStock = jdbcTemplate.queryForObject(
                "SELECT stock FROM t_product WHERE id = ?",
                Integer.class, testProductId);
        String batchNo = "TESTB" + System.nanoTime();

        MvcResult batchResult = mockMvc.perform(post("/api/admin/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 101,
                                  "batchNo": "%s",
                                  "productionDate": "%s",
                                  "expireDate": "%s",
                                  "stock": 5,
                                  "lockStock": 0,
                                  "remark": "自动化测试批次"
                                }
                                """.formatted(
                                batchNo,
                                LocalDate.now().minusDays(1),
                                LocalDate.now().plusDays(30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(5))
                .andReturn();
        testBatchId = readId(batchResult);

        Integer stockAfterEntry = jdbcTemplate.queryForObject(
                "SELECT stock FROM t_product WHERE id = ?",
                Integer.class, testProductId);
        assertThat(stockAfterEntry).isEqualTo(originalProductStock + 5);

        mockMvc.perform(get("/api/admin/batch/page")
                        .header("Authorization", "Bearer " + token)
                        .param("batchNo", batchNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].productName").value("香煎鸡胸沙拉"));

        mockMvc.perform(put("/api/admin/batch/{id}/loss", testBatchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2,\"remark\":\"包装破损\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(3));

        Integer stockAfterLoss = jdbcTemplate.queryForObject(
                "SELECT stock FROM t_product WHERE id = ?",
                Integer.class, testProductId);
        assertThat(stockAfterLoss).isEqualTo(originalProductStock + 3);

        mockMvc.perform(get("/api/admin/inventory/log")
                        .header("Authorization", "Bearer " + token)
                        .param("batchId", String.valueOf(testBatchId))
                        .param("changeType", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].changeTypeText").value("报损"))
                .andExpect(jsonPath("$.data.records[0].beforeStock").value(5))
                .andExpect(jsonPath("$.data.records[0].afterStock").value(3));
    }

    @Test
    void dashboardAndDispatchLogExposeRealBusinessData() throws Exception {
        String token = adminToken();
        DeliveryOrder delivery = new DeliveryOrder();
        delivery.setDeliveryNo("TESTD" + System.nanoTime());
        delivery.setOrderId(System.nanoTime());
        delivery.setOrderNo("TESTO" + System.nanoTime());
        delivery.setStatus(DeliveryStatus.WAIT_ASSIGN.getCode());
        delivery.setAssignType(1);
        delivery.setDistance(BigDecimal.ZERO);
        delivery.setFreight(BigDecimal.ZERO);
        delivery.setManFee(BigDecimal.ZERO);
        delivery.setCreateTime(LocalDateTime.now());
        deliveryOrderMapper.insert(delivery);
        testDeliveryId = delivery.getId();

        DeliveryManLog log = new DeliveryManLog();
        log.setDeliveryOrderId(delivery.getId());
        log.setCandidateId(999999L);
        log.setCandidateName("测试候选配送员");
        log.setTotalScore(new BigDecimal("92.5000"));
        log.setDistanceScore(new BigDecimal("35.0000"));
        log.setLoadScore(new BigDecimal("30.0000"));
        log.setRatingScore(new BigDecimal("27.5000"));
        log.setDistanceValue(new BigDecimal("2.35"));
        log.setLoadValue(1);
        log.setIsFinal(1);
        log.setCreateTime(LocalDateTime.now());
        deliveryManLogMapper.insert(log);

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.todayRevenue").exists())
                .andExpect(jsonPath("$.data.pendingDeliveryCount").exists());

        mockMvc.perform(get("/api/admin/dashboard/trend")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/admin/dashboard/rank")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/admin/dashboard/region")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/admin/dispatch/log/page")
                        .header("Authorization", "Bearer " + token)
                        .param("deliveryNo", delivery.getDeliveryNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].candidateName")
                        .value("测试候选配送员"))
                .andExpect(jsonPath("$.data.records[0].isFinal").value(1));

        mockMvc.perform(get("/api/admin/dispatch/log/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    private String adminToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long readId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("id").asLong();
    }
}
