package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.freshmeal.entity.User;
import com.freshmeal.mapper.CartMapper;
import com.freshmeal.mapper.InventoryLogMapper;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.mapper.UserMapper;
import com.freshmeal.service.DeliverySlotService;
import com.freshmeal.service.InventoryService;
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

/**
 * 立即购买链路：不经购物车直接结算与下单。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopDirectPurchaseControllerTest {

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
    private UserMapper userMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private DeliverySlotService deliverySlotService;

    private final List<String> createdOrderNumbers = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();

    @AfterEach
    void cleanUpCreatedOrders() {
        createdCartIds.forEach(cartMapper::deleteById);
        createdCartIds.clear();
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
            orderItemMapper.delete(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId()));
            orderMapper.deleteById(order.getId());
        }
        createdOrderNumbers.clear();
    }

    @Test
    void directPurchaseSettlesWithoutCartRows() throws Exception {
        String token = login();
        long addressId = readFirstAddressId(token);
        long cartCountBefore = cartCount(token);

        MvcResult settleResult = mockMvc.perform(post("/api/shop/cart/settle")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,
                                 "directItems":[{"productId":101,"specId":1001,
                                 "optionItemIds":[3001],"quantity":2}]}
                                """.formatted(addressId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].productId").value(101))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(31.00))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(62.00))
                .andExpect(jsonPath("$.data.goodsAmount").value(62.00))
                .andReturn();

        JsonNode settle = objectMapper.readTree(settleResult.getResponse().getContentAsString())
                .path("data");
        assertThat(settle.path("totalCalories").asInt()).isEqualTo(860);
        assertThat(settle.path("payAmount").decimalValue())
                .isEqualByComparingTo(settle.path("goodsAmount").decimalValue()
                        .add(settle.path("freightAmount").decimalValue()));

        assertThat(cartCount(token)).isEqualTo(cartCountBefore);
    }

    @Test
    void directPurchaseCreatesOrderAndLeavesCartUntouched() throws Exception {
        String token = login();
        long addressId = readFirstAddressId(token);
        int stockBefore = requireProduct().getStock();
        long cartCountBefore = cartCount(token);
        var slot = deliverySlotService.listAvailable().get(0);

        MvcResult result = mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,
                                 "directItems":[{"productId":101,"specId":1001,
                                 "optionItemIds":[3001],"quantity":2}],
                                 "deliverySlotId":%d,"deliverySlotDate":"%s",
                                 "remark":"立即购买"}
                                """.formatted(addressId, slot.id(), slot.date())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.goodsAmount").value(62.00))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(62.00))
                .andReturn();

        String orderNo = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderNo").asText();
        createdOrderNumbers.add(orderNo);

        assertThat(requireProduct().getStock()).isEqualTo(stockBefore - 2);
        assertThat(cartCount(token)).isEqualTo(cartCountBefore);
    }

    @Test
    void directPurchaseRejectsMissingRequiredOption() throws Exception {
        String token = login();
        long addressId = readFirstAddressId(token);

        mockMvc.perform(post("/api/shop/cart/settle")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,
                                 "directItems":[{"productId":106,"specId":1011,
                                 "optionItemIds":[],"quantity":1}]}
                                """.formatted(addressId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void directPurchaseRejectsUnavailableProduct() throws Exception {
        String token = login();
        long addressId = readFirstAddressId(token);

        mockMvc.perform(post("/api/shop/cart/settle")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,
                                 "directItems":[{"productId":999999,"specId":1001,
                                 "optionItemIds":[3001],"quantity":1}]}
                                """.formatted(addressId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void settlementWithoutAnyItemIsRejected() throws Exception {
        String token = login();
        long addressId = readFirstAddressId(token);

        mockMvc.perform(post("/api/shop/cart/settle")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1}
                                """.formatted(addressId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40005));
    }

    @Test
    void cartItemsAndDirectItemsCanBeSettledTogether() throws Exception {
        String token = login();
        long addressId = readFirstAddressId(token);
        long cartId = addItem(token, 1, List.of(3001L));
        int stockBefore = requireProduct().getStock();
        long cartCountBefore = cartCount(token);
        var slot = deliverySlotService.listAvailable().get(0);

        MvcResult result = mockMvc.perform(post("/api/shop/order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":%d,"deliveryType":1,"cartItemIds":[%d],
                                 "directItems":[{"productId":101,"specId":1002,
                                 "optionItemIds":[3002],"quantity":1}],
                                 "deliverySlotId":%d,"deliverySlotDate":"%s"}
                                """.formatted(addressId, cartId, slot.id(), slot.date())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn();

        String orderNo = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orderNo").asText();
        createdOrderNumbers.add(orderNo);

        assertThat(requireProduct().getStock()).isEqualTo(stockBefore - 2);
        assertThat(cartCount(token)).isEqualTo(cartCountBefore - 1);
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

    private long currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/shop/cart")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).isNotBlank();
        return userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, "user01")).getId();
    }

    private long cartCount(String token) throws Exception {
        return cartMapper.selectCount(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, currentUserId(token)));
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
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        createdCartIds.add(id);
        return id;
    }

    private Product requireProduct() {
        Product product = productMapper.selectById(101L);
        assertThat(product).isNotNull();
        return product;
    }
}
