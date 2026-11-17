package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmeal.entity.Cart;
import com.freshmeal.entity.DeliveryMan;
import com.freshmeal.entity.DeliveryManLog;
import com.freshmeal.entity.DeliveryOrder;
import com.freshmeal.entity.DeliveryTrace;
import com.freshmeal.entity.InventoryLog;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.entity.Payment;
import com.freshmeal.entity.Product;
import com.freshmeal.mapper.CartMapper;
import com.freshmeal.mapper.DeliveryManLogMapper;
import com.freshmeal.mapper.DeliveryManMapper;
import com.freshmeal.mapper.DeliveryOrderMapper;
import com.freshmeal.mapper.DeliveryTraceMapper;
import com.freshmeal.mapper.InventoryLogMapper;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.mapper.ProductBatchMapper;
import com.freshmeal.service.InventoryService;
import com.freshmeal.service.DeliverySlotService;
import com.freshmeal.service.OrderService;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopTradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private InventoryLogMapper inventoryLogMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductBatchMapper productBatchMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DeliverySlotService deliverySlotService;

    @Autowired
    private DeliveryOrderMapper deliveryOrderMapper;

    @Autowired
    private DeliveryTraceMapper deliveryTraceMapper;

    @Autowired
    private DeliveryManLogMapper deliveryManLogMapper;

    @Autowired
    private DeliveryManMapper deliveryManMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<String> createdOrderNumbers = new ArrayList<>();
    private final List<String> createdUsernames = new ArrayList<>();
    private DeliveryMan originalRider;

    @AfterEach
    void cleanUpCreatedTradeData() {
        for (String orderNo : createdOrderNumbers) {
            Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                    .eq(Order::getOrderNo, orderNo));
            if (order == null) {
                continue;
            }
            List<OrderItem> items = orderItemMapper.selectList(
                    Wrappers.<OrderItem>lambdaQuery()
                            .eq(OrderItem::getOrderId, order.getId()));
            if (order.getStatus() != 9) {
                inventoryService.restoreForOrder(orderNo, order.getUserId(), "测试清理");
            }
            // 销量仅在支付成功时递增，只有付过款的订单才需要回退，否则会凭空扣减。
            if (order.getPayStatus() != null && order.getPayStatus() == 1) {
                for (OrderItem item : items) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null && product.getSalesCount() != null) {
                        product.setSalesCount(Math.max(0,
                                product.getSalesCount() - item.getQuantity()));
                        productMapper.updateById(product);
                    }
                }
            }
            inventoryLogMapper.delete(Wrappers.<InventoryLog>lambdaQuery()
                    .eq(InventoryLog::getRefNo, orderNo));
            paymentMapper.delete(Wrappers.<Payment>lambdaQuery()
                    .eq(Payment::getOrderId, order.getId()));
            orderStatusLogMapper.delete(Wrappers.<OrderStatusLog>lambdaQuery()
                    .eq(OrderStatusLog::getOrderId, order.getId()));
            DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                    Wrappers.<DeliveryOrder>lambdaQuery()
                            .eq(DeliveryOrder::getOrderId, order.getId()));
            if (delivery != null) {
                deliveryTraceMapper.delete(Wrappers.<DeliveryTrace>lambdaQuery()
                        .eq(DeliveryTrace::getDeliveryOrderId, delivery.getId()));
                deliveryManLogMapper.delete(Wrappers.<DeliveryManLog>lambdaQuery()
                        .eq(DeliveryManLog::getDeliveryOrderId, delivery.getId()));
                deliveryOrderMapper.deleteById(delivery.getId());
            }
            orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId()));
            orderMapper.deleteById(order.getId());
        }
        createdOrderNumbers.clear();
        createdCartIds.forEach(cartMapper::deleteById);
        createdCartIds.clear();
        createdUsernames.forEach(username -> jdbcTemplate.update(
                "DELETE FROM t_user WHERE username = ?", username));
        createdUsernames.clear();
        if (originalRider != null) {
            deliveryManMapper.updateById(originalRider);
            originalRider = null;
        }
    }

    @Test
    void usersCannotReadOrdersOwnedByAnotherUser() throws Exception {
        String ownerToken = login();
        long cartId = addItem(ownerToken, 1, List.of());
        long addressId = readFirstAddressId(ownerToken);
        String ownerOrderNo = createOrder(ownerToken, addressId,
                "[" + cartId + "]");

        String intruderUsername = "it-"
                + java.util.UUID.randomUUID().toString().substring(0, 8);
        MvcResult registerResult = mockMvc.perform(post("/api/shop/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456"}
                                """.formatted(intruderUsername)))
                .andExpect(status().isOk())
                .andReturn();
        createdUsernames.add(intruderUsername);
        String intruderToken = "Bearer " + objectMapper.readTree(
                        registerResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/shop/order/" + ownerOrderNo)
                        .header("Authorization", intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40405));

        mockMvc.perform(post("/api/shop/auth/logout")
                        .header("Authorization", intruderToken))
                .andExpect(status().isOk());
    }

    @Test
    void reservationActivatesDispatchesAndCompletesDeliveryWithTrace()
            throws Exception {
        String userToken = login();
        DeliveryMan rider = deliveryManMapper.selectOne(
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getUsername, "rider01"));
        assertThat(rider).isNotNull();
        originalRider = copyRider(rider);
        rider.setOnlineStatus(1);
        rider.setWorkStatus(0);
        deliveryManMapper.updateById(rider);

        long cartId = addItem(userToken, 1, List.of());
        long addressId = readFirstAddressId(userToken);
        String orderNo = createOrder(userToken, addressId, "[" + cartId + "]");
        String payNo = initiatePayment(userToken, orderNo);
        notifyPayment(userToken, payNo, 1);

        Order order = requireOrder(orderNo);
        // 骑手提成在下单时按配送费全额写入。
        assertThat(order.getDeliveryManFee()).isPositive();
        assertThat(order.getDeliveryManFee()).isEqualByComparingTo(order.getFreightAmount());
        order.setPayTime(LocalDateTime.now().minusMinutes(2));
        orderMapper.updateById(order);
        orderService.advanceReadyOrders();
        assertThat(requireOrder(orderNo).getStatus()).isEqualTo(3);

        order = requireOrder(orderNo);
        order.setDeliverySlotDate(LocalDate.now().minusDays(1));
        orderMapper.updateById(order);
        orderService.activateDueSlotOrders();
        assertThat(requireOrder(orderNo).getStatus()).isEqualTo(4);

        DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getOrderId, requireOrder(orderNo).getId()));
        assertThat(delivery).isNotNull();
        assertThat(delivery.getStatus()).isEqualTo(1);
        assertThat(delivery.getAssignType()).isEqualTo(1);
        List<DeliveryManLog> dispatchLogs = deliveryManLogMapper.selectList(
                Wrappers.<DeliveryManLog>lambdaQuery()
                        .eq(DeliveryManLog::getDeliveryOrderId, delivery.getId()));
        assertThat(dispatchLogs).isNotEmpty();
        assertThat(dispatchLogs).anySatisfy(log -> {
            assertThat(log.getIsFinal()).isEqualTo(1);
            assertThat(log.getTotalScore()).isBetween(
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE);
            assertThat(log.getDistanceScore()).isBetween(
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE);
            assertThat(log.getLoadScore()).isBetween(
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE);
            assertThat(log.getRatingScore()).isBetween(
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE);
        });

        String riderToken = loginRider();
        JsonNode statsBefore = readRiderStatistics(riderToken);
        mockMvc.perform(put("/api/rider/task/" + delivery.getDeliveryNo() + "/pickup")
                        .header("Authorization", riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(3));
        assertThat(requireOrder(orderNo).getStatus()).isEqualTo(5);

        mockMvc.perform(put("/api/rider/task/" + delivery.getDeliveryNo() + "/deliver")
                        .header("Authorization", riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(4));
        assertThat(requireOrder(orderNo).getStatus()).isEqualTo(6);

        // 送达后骑手收入按本单提成累加，今日口径同步增长。
        JsonNode statsAfter = readRiderStatistics(riderToken);
        var manFee = delivery.getManFee();
        assertThat(manFee).isPositive();
        assertThat(statsAfter.path("totalIncome").decimalValue())
                .isEqualByComparingTo(statsBefore.path("totalIncome").decimalValue().add(manFee));
        assertThat(statsAfter.path("todayIncome").decimalValue())
                .isEqualByComparingTo(statsBefore.path("todayIncome").decimalValue().add(manFee));
        assertThat(statsAfter.path("todayOrderCount").asInt())
                .isEqualTo(statsBefore.path("todayOrderCount").asInt() + 1);

        // 管理端骑手列表与骑手端 statistics 的今日单数同口径。
        MvcResult adminRiders = mockMvc.perform(get("/api/admin/delivery/man")
                        .header("Authorization", loginAdmin()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode riderRows = objectMapper
                .readTree(adminRiders.getResponse().getContentAsString()).path("data");
        JsonNode adminRow = null;
        for (JsonNode row : riderRows) {
            if (row.path("id").asLong() == rider.getId()) {
                adminRow = row;
            }
        }
        assertThat(adminRow).isNotNull();
        assertThat(adminRow.path("todayOrderCount").asInt())
                .isEqualTo(statsAfter.path("todayOrderCount").asInt());

        mockMvc.perform(get("/api/shop/order/" + orderNo + "/trace")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(
                        deliveryTraceMapper.selectCount(
                                Wrappers.<DeliveryTrace>lambdaQuery()
                                        .eq(DeliveryTrace::getDeliveryOrderId,
                                                delivery.getId())).intValue()));
    }

    @Test
    void instantDeliveryOrderSkipsSlotAndActivatesAfterPreparation()
            throws Exception {
        String userToken = login();
        // 固定骑手离线，配送任务停留在待分配态，断言不依赖派单结果。
        DeliveryMan rider = deliveryManMapper.selectOne(
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getUsername, "rider01"));
        assertThat(rider).isNotNull();
        originalRider = copyRider(rider);
        rider.setOnlineStatus(0);
        rider.setWorkStatus(2);
        deliveryManMapper.updateById(rider);

        long cartId = addItem(userToken, 1, List.of());
        long addressId = readFirstAddressId(userToken);

        MvcResult result = mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":[%d]}
                                """.formatted(addressId, cartId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String orderNo = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderNo").asText();
        createdOrderNumbers.add(orderNo);

        Order order = requireOrder(orderNo);
        assertThat(order.getDeliverySlotId()).isNull();
        assertThat(order.getDeliverySlotDate()).isNull();

        String payNo = initiatePayment(userToken, orderNo);
        notifyPayment(userToken, payNo, 1);

        order = requireOrder(orderNo);
        order.setPayTime(LocalDateTime.now().minusMinutes(2));
        orderMapper.updateById(order);
        orderService.advanceReadyOrders();
        // 立即配送单备餐完成直接进入待配送，不经过等待预约时段。
        assertThat(requireOrder(orderNo).getStatus()).isEqualTo(4);

        DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getOrderId, requireOrder(orderNo).getId()));
        assertThat(delivery).isNotNull();
        assertThat(delivery.getStatus()).isZero();
    }

    @Test
    void partialDeliverySlotFieldsAreRejected() throws Exception {
        String userToken = login();
        long cartId = addItem(userToken, 1, List.of());
        long addressId = readFirstAddressId(userToken);
        var slot = deliverySlotService.listAvailable().get(0);

        mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":[%d],
                                 "deliverySlotId":%d}
                                """.formatted(addressId, cartId, slot.id())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":[%d],
                                 "deliverySlotDate":"%s"}
                                """.formatted(addressId, cartId, slot.date())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void adminCanManuallyAssignUnclaimedDelivery() throws Exception {
        DeliveryScenario scenario = createUnassignedDelivery();
        String riderToken = loginRider();
        mockMvc.perform(put("/api/rider/profile/work-status")
                        .header("Authorization", riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onlineStatus\":1,\"workStatus\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onlineStatus").value(1))
                .andExpect(jsonPath("$.data.workStatus").value(0));
        String adminToken = loginAdmin();

        mockMvc.perform(put("/api/admin/delivery/order/"
                        + scenario.delivery().getId() + "/assign")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.assignType").value(3))
                .andExpect(jsonPath("$.data.deliveryManId").value(1));

        assertThat(deliveryManLogMapper.selectCount(
                Wrappers.<DeliveryManLog>lambdaQuery()
                        .eq(DeliveryManLog::getDeliveryOrderId,
                                scenario.delivery().getId()))).isPositive();
    }

    @Test
    void optimisticClaimAllowsOneWinnerAndRiderCanGrabFromTodo()
            throws Exception {
        DeliveryScenario scenario = createUnassignedDelivery();
        String riderToken = loginRider();
        DeliveryMan rider = deliveryManMapper.selectById(1L);
        rider.setOnlineStatus(1);
        rider.setWorkStatus(0);
        deliveryManMapper.updateById(rider);

        assertThat(deliveryOrderMapper.claimForRider(
                scenario.delivery().getId(), 1L, "Rider 01",
                "13800000003", 1)).isEqualTo(1);
        assertThat(deliveryOrderMapper.claimForRider(
                scenario.delivery().getId(), 2L, "Rider 02",
                "13800000003", 2)).isZero();
        assertThat(deliveryOrderMapper.resetClaim(scenario.delivery().getId()))
                .isEqualTo(1);

        mockMvc.perform(get("/api/rider/task/todo")
                        .header("Authorization", riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.deliveryNo == '%s')].deliveryNo"
                                .formatted(scenario.delivery().getDeliveryNo()))
                        .isNotEmpty());

        mockMvc.perform(post("/api/rider/task/"
                        + scenario.delivery().getDeliveryNo() + "/grab")
                        .header("Authorization", riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.assignType").value(2));

        mockMvc.perform(get("/api/rider/task/my")
                        .header("Authorization", riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.deliveryNo == '%s')].deliveryNo"
                                .formatted(scenario.delivery().getDeliveryNo()))
                        .isNotEmpty());

        mockMvc.perform(post("/api/rider/task/"
                        + scenario.delivery().getDeliveryNo() + "/grab")
                        .header("Authorization", riderToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));

        mockMvc.perform(put("/api/rider/profile/work-status")
                        .header("Authorization", riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"onlineStatus\":0,\"workStatus\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onlineStatus").value(0))
                .andExpect(jsonPath("$.data.workStatus").value(2));
    }

    @Test
    void completeTradeFlowRepricesDeductsPaysIdempotentlyAndRestoresOnTimeout()
            throws Exception {
        String token = login();
        int productBefore = requireProduct().getStock();
        Long fefoBatchId = requireFefoBatchId();
        int firstBatchBefore = requireBatchStock(fefoBatchId);

        long cartId = addItem(token, 1, List.of(3001L));
        mockMvc.perform(get("/api/shop/cart")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(31.00))
                .andExpect(jsonPath("$.data.items[0].subtotalCalories").value(430))
                .andExpect(jsonPath("$.data.goodsAmount").value(31.00));

        long addressId = readFirstAddressId(token);
        String cartIds = "[" + cartId + "]";
        MvcResult settleResult = mockMvc.perform(post("/api/shop/cart/settle")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":%s}
                                """.formatted(addressId, cartIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goodsAmount").value(31.00))
                .andExpect(jsonPath("$.data.totalCalories").value(430))
                .andReturn();
        JsonNode settle = objectMapper.readTree(settleResult.getResponse().getContentAsString())
                .path("data");
        assertThat(settle.path("freightAmount").decimalValue()).isPositive();
        assertThat(settle.path("payAmount").decimalValue())
                .isEqualByComparingTo(settle.path("goodsAmount").decimalValue()
                        .add(settle.path("freightAmount").decimalValue()));

        String paidOrderNo = createOrder(token, addressId, cartIds);
        assertThat(requireProduct().getStock()).isEqualTo(productBefore - 1);
        assertThat(requireBatchStock(fefoBatchId)).isEqualTo(firstBatchBefore - 1);

        String payNo = initiatePayment(token, paidOrderNo);
        notifyPayment(token, payNo, 1);
        int stockAfterFirstCallback = requireProduct().getStock();
        int salesAfterFirstCallback = requireProduct().getSalesCount();
        notifyPayment(token, payNo, 1);
        assertThat(requireProduct().getStock()).isEqualTo(stockAfterFirstCallback);
        assertThat(requireProduct().getSalesCount()).isEqualTo(salesAfterFirstCallback);

        mockMvc.perform(get("/api/shop/order/" + paidOrderNo)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.payment.status").value(1))
                .andExpect(jsonPath("$.data.timeline.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotalCalories").value(430));

        long timeoutCartId = addItem(token, 1, List.of());
        String timeoutOrderNo = createOrder(token, addressId, "[" + timeoutCartId + "]");
        Order timeoutOrder = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, timeoutOrderNo));
        timeoutOrder.setCreateTime(LocalDateTime.now().minusMinutes(20));
        orderMapper.updateById(timeoutOrder);

        orderService.cancelExpiredUnpaidOrders();
        Order cancelled = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, timeoutOrderNo));
        assertThat(cancelled.getStatus()).isEqualTo(9);
        assertThat(requireProduct().getStock()).isEqualTo(stockAfterFirstCallback);
        assertThat(requireBatchStock(fefoBatchId)).isEqualTo(firstBatchBefore - 1);

    }

    @Test
    void shopOrderPageWithoutOrderNoFilterReturnsOk() throws Exception {
        String userToken = login();
        mockMvc.perform(get("/api/shop/order/page")
                        .header("Authorization", userToken)
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shop/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user01","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String loginRider() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rider/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"rider01","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
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
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private DeliveryScenario createUnassignedDelivery() throws Exception {
        String userToken = login();
        DeliveryMan rider = deliveryManMapper.selectOne(
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getUsername, "rider01"));
        assertThat(rider).isNotNull();
        originalRider = copyRider(rider);
        rider.setOnlineStatus(0);
        rider.setWorkStatus(2);
        deliveryManMapper.updateById(rider);

        long cartId = addItem(userToken, 1, List.of());
        long addressId = readFirstAddressId(userToken);
        String orderNo = createOrder(userToken, addressId, "[" + cartId + "]");
        String payNo = initiatePayment(userToken, orderNo);
        notifyPayment(userToken, payNo, 1);

        Order order = requireOrder(orderNo);
        order.setPayTime(LocalDateTime.now().minusMinutes(2));
        orderMapper.updateById(order);
        orderService.advanceReadyOrders();
        order = requireOrder(orderNo);
        order.setDeliverySlotDate(LocalDate.now().minusDays(1));
        orderMapper.updateById(order);
        orderService.activateDueSlotOrders();

        DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getOrderId, requireOrder(orderNo).getId()));
        assertThat(delivery).isNotNull();
        assertThat(delivery.getStatus()).isZero();
        return new DeliveryScenario(orderNo, delivery);
    }

    private DeliveryMan copyRider(DeliveryMan rider) {
        DeliveryMan copy = new DeliveryMan();
        copy.setId(rider.getId());
        copy.setOnlineStatus(rider.getOnlineStatus());
        copy.setWorkStatus(rider.getWorkStatus());
        copy.setTodayOrderCount(rider.getTodayOrderCount());
        copy.setTotalOrderCount(rider.getTotalOrderCount());
        copy.setTotalIncome(rider.getTotalIncome());
        return copy;
    }

    private Order requireOrder(String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
        assertThat(order).isNotNull();
        return order;
    }

    private long addItem(String token, int quantity, List<Long> optionIds) throws Exception {
        String options = objectMapper.writeValueAsString(optionIds);
        MvcResult result = mockMvc.perform(post("/api/shop/cart")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":101,"specId":1001,"optionItemIds":%s,
                                 "quantity":%d}
                                """.formatted(options, quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        createdCartIds.add(id);
        return id;
    }

    private long readFirstAddressId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/shop/address")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isDefault").value(true))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();
    }

    private String createOrder(String token, long addressId, String cartIds) throws Exception {
        var slot = deliverySlotService.listAvailable().get(0);
        MvcResult result = mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":%s,
                                 "deliverySlotId":%d,"deliverySlotDate":"%s",
                                 "remark":"阶段7自动化验证",
                                 "goodsAmount":0.01,"freightAmount":0,
                                 "discountAmount":0,"payAmount":0.01}
                                """.formatted(addressId, cartIds,
                                slot.id(), slot.date())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.payStatus").value(0))
                .andExpect(jsonPath("$.data.goodsAmount")
                        .value(greaterThan(0.01)))
                .andExpect(jsonPath("$.data.payAmount")
                        .value(greaterThan(0.01)))
                .andReturn();
        String orderNo = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderNo").asText();
        createdOrderNumbers.add(orderNo);
        return orderNo;
    }

    private JsonNode readRiderStatistics(String riderToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rider/task/statistics")
                        .header("Authorization", riderToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String initiatePayment(String token, String orderNo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shop/order/" + orderNo + "/pay")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("payNo").asText();
    }

    private void notifyPayment(String token, String payNo, int payType) throws Exception {
        mockMvc.perform(post("/api/pay/mock/notify")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payNo":"%s","payType":%d,"success":true,
                                 "thirdPartyNo":"MOCK-VERIFY"}
                                """.formatted(payNo, payType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));
    }

    private Product requireProduct() {
        Product product = productMapper.selectById(101L);
        assertThat(product).isNotNull();
        return product;
    }

    private Long requireFefoBatchId() {
        List<com.freshmeal.entity.ProductBatch> batches =
                productBatchMapper.selectAvailableFefo(101L);
        assertThat(batches).isNotEmpty();
        return batches.get(0).getId();
    }

    private int requireBatchStock(Long batchId) {
        com.freshmeal.entity.ProductBatch batch = productBatchMapper.selectById(batchId);
        assertThat(batch).isNotNull();
        return batch.getStock();
    }

    private record DeliveryScenario(String orderNo, DeliveryOrder delivery) {
    }
}
