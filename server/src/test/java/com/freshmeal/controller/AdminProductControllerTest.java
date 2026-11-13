package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminProductControllerTest {

    private static final String CREATE_BODY = """
            {
              "categoryId": 11,
              "name": "[TEST]集成测试商品",
              "subtitle": "自动化测试专用",
              "cover": "/api/files/products/p101.svg",
              "productType": 1,
              "price": 12.90,
              "originalPrice": 15.90,
              "unit": "份",
              "stock": 20,
              "status": 1,
              "isRecommend": 0,
              "sort": 0,
              "calories": 380,
              "protein": 25.0,
              "fat": 11.0,
              "carbohydrate": 30.0,
              "sodium": 400.0,
              "storageType": 2,
              "shelfLifeDays": 3,
              "healthTags": ["低卡", "高蛋白"],
              "allergens": ["鸡蛋"],
              "specs": [
                {"name": "标准份", "priceDelta": 0.00, "stock": 20, "calories": 380, "isDefault": 1, "sort": 0, "status": 1}
              ],
              "images": [
                {"imageUrl": "/api/files/products/p101.svg", "sort": 0, "status": 1}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createdProductId;

    @AfterEach
    void cleanUp() throws Exception {
        if (createdProductId != null) {
            mockMvc.perform(delete("/api/admin/product/{id}", createdProductId)
                            .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
            createdProductId = null;
        }
    }

    private String adminToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    @Test
    void productLifecycleSynchronizesToShopSide() throws Exception {
        String token = adminToken();

        // 创建
        MvcResult createResult = mockMvc.perform(post("/api/admin/product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        createdProductId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").asLong();
        assertThat(createdProductId).isPositive();

        // 管理端详情回读
        mockMvc.perform(get("/api/admin/product/{id}", createdProductId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("[TEST]集成测试商品"))
                .andExpect(jsonPath("$.data.healthTags[1]").value("高蛋白"))
                .andExpect(jsonPath("$.data.storageType").value(2))
                .andExpect(jsonPath("$.data.specs.length()").value(1))
                .andExpect(jsonPath("$.data.images.length()").value(1));

        // 用户端可见且可购买
        mockMvc.perform(get("/api/shop/product/{id}", createdProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.storageLabel").value("冷藏"));

        // 下架同步
        mockMvc.perform(put("/api/admin/product/{id}/status", createdProductId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/shop/product/{id}", createdProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.available").value(false));

        // 恢复上架并推荐，出现在用户端推荐位
        mockMvc.perform(put("/api/admin/product/{id}/status", createdProductId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/product/{id}/recommend", createdProductId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isRecommend\":1}"))
                .andExpect(status().isOk());
        MvcResult recommendResult = mockMvc.perform(get("/api/shop/product/recommend"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode recommend = objectMapper.readTree(recommendResult.getResponse().getContentAsString())
                .path("data");
        boolean found = false;
        for (JsonNode item : recommend) {
            if (item.path("id").asLong() == createdProductId) {
                found = true;
            }
        }
        assertThat(found).isTrue();

        // 删除后用户端 404
        mockMvc.perform(delete("/api/admin/product/{id}", createdProductId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        Long deletedId = createdProductId;
        createdProductId = null;
        mockMvc.perform(get("/api/shop/product/{id}", deletedId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void productPageFiltersByCategoryAndNameForAdmin() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/admin/product/page")
                        .header("Authorization", "Bearer " + token)
                        .param("categoryId", "11")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/admin/product/page")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "藜麦"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/admin/product/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(12));
    }

    @Test
    void invalidStatusAndMissingFieldsAreRejected() throws Exception {
        String token = adminToken();

        mockMvc.perform(put("/api/admin/product/101/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(post("/api/admin/product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(put("/api/admin/product/101/recommend")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isRecommend\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminEndpointsRequireAdminAuth() throws Exception {
        mockMvc.perform(get("/api/admin/product/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/api/admin/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/product/101"))
                .andExpect(status().isUnauthorized());
    }
}
