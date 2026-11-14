package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.freshmeal.entity.InventoryLog;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.entity.Payment;
import com.freshmeal.entity.Product;
import com.freshmeal.mapper.CartMapper;
import com.freshmeal.mapper.InventoryLogMapper;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.mapper.ProductBatchMapper;
import com.freshmeal.service.InventoryService;
import com.freshmeal.service.OrderService;
import java.time.LocalDateTime;
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

    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<String> createdOrderNumbers = new ArrayList<>();

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
            orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId()));
            orderMapper.deleteById(order.getId());
        }
        createdOrderNumbers.clear();
        createdCartIds.forEach(cartMapper::deleteById);
        createdCartIds.clear();
    }

    @Test
    void completeTradeFlowRepricesDeductsPaysIdempotentlyAndRestoresOnTimeout()
            throws Exception {
        String token = login();
        int productBefore = requireProduct().getStock();
        int firstBatchBefore = requireBatchStock(5001L);

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
        assertThat(requireBatchStock(5001L)).isEqualTo(firstBatchBefore - 1);

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
        assertThat(requireBatchStock(5001L)).isEqualTo(firstBatchBefore - 1);

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
        MvcResult result = mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":%s,
                                 "remark":"阶段3自动化验证"}
                                """.formatted(addressId, cartIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.payStatus").value(0))
                .andReturn();
        String orderNo = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderNo").asText();
        createdOrderNumbers.add(orderNo);
        return orderNo;
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

    private int requireBatchStock(Long batchId) {
        com.freshmeal.entity.ProductBatch batch = productBatchMapper.selectById(batchId);
        assertThat(batch).isNotNull();
        return batch.getStock();
    }
}
