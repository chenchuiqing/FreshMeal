package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** 基于 V3 种子数据（12 个商品）验证用户端浏览、搜索与组合筛选契约。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void categoryTreeReturnsTwoLevelEnabledCategories() throws Exception {
        mockMvc.perform(get("/api/shop/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("轻食沙拉"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("蛋白沙拉"));
    }

    @Test
    void pageReturnsSeedProductsWithAvailableFlag() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/shop/product/page")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(12))
                .andReturn();

        JsonNode records = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("records");
        assertThat(records).hasSize(12);
        for (JsonNode record : records) {
            assertThat(record.path("id").asLong()).isPositive();
            assertThat(record.path("categoryName").asText()).isNotBlank();
            assertThat(record.path("healthTags").isArray()).isTrue();
            assertThat(record.path("status").asInt()).isIn(0, 1);
        }
        // 下架商品保留在列表中但标记为不可购买
        JsonNode offShelf = null;
        for (JsonNode record : records) {
            if (record.path("id").asLong() == 112L) {
                offShelf = record;
            }
        }
        assertThat(offShelf).isNotNull();
        assertThat(offShelf.path("status").asInt()).isZero();
        assertThat(offShelf.path("available").asBoolean()).isFalse();
    }

    @Test
    void pageFiltersByCaloriesAndHealthTagsAndKeyword() throws Exception {
        mockMvc.perform(get("/api/shop/product/page")
                        .param("maxCalories", "400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6));

        MultiValueMap<String, String> intersection = new LinkedMultiValueMap<>();
        intersection.add("healthTags", "低卡,高蛋白");
        mockMvc.perform(get("/api/shop/product/page").params(intersection))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(get("/api/shop/product/page")
                        .param("categoryId", "1")
                        .param("keyword", "鸡胸")
                        .param("maxCalories", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void pageSortsBySalesDescending() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/shop/product/page")
                        .param("sort", "sales")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode records = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("records");
        assertThat(records.get(0).path("name").asText()).isEqualTo("香煎鸡胸沙拉");
        int previous = Integer.MAX_VALUE;
        for (JsonNode record : records) {
            int sales = record.path("salesCount").asInt();
            assertThat(sales).isLessThanOrEqualTo(previous);
            previous = sales;
        }
    }

    @Test
    void detailReturnsNutritionStorageAndChildren() throws Exception {
        mockMvc.perform(get("/api/shop/product/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("香煎鸡胸沙拉"))
                .andExpect(jsonPath("$.data.storageLabel").value("冷藏"))
                .andExpect(jsonPath("$.data.shelfLifeDays").value(3))
                .andExpect(jsonPath("$.data.allergens[0]").value("鸡蛋"))
                .andExpect(jsonPath("$.data.healthTags[0]").value("低卡"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.specs.length()").value(2))
                .andExpect(jsonPath("$.data.options.length()").value(1))
                .andExpect(jsonPath("$.data.options[0].items.length()").value(3))
                .andExpect(jsonPath("$.data.images.length()").value(3));
    }

    @Test
    void offShelfAndSoldOutDetailsAreUnavailable() throws Exception {
        mockMvc.perform(get("/api/shop/product/112"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.available").value(false));

        mockMvc.perform(get("/api/shop/product/110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(0))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void hotAndRecommendListsOnlyReturnOnShelfProducts() throws Exception {
        MvcResult hotResult = mockMvc.perform(get("/api/shop/product/hot")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode hot = objectMapper.readTree(hotResult.getResponse().getContentAsString()).path("data");
        assertThat(hot).hasSize(5);
        for (JsonNode item : hot) {
            assertThat(item.path("status").asInt()).isEqualTo(1);
        }
        assertThat(hot.get(0).path("name").asText()).isEqualTo("香煎鸡胸沙拉");

        MvcResult recommendResult = mockMvc.perform(get("/api/shop/product/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode recommend = objectMapper.readTree(recommendResult.getResponse().getContentAsString()).path("data");
        List<Long> ids = new java.util.ArrayList<>();
        recommend.forEach(item -> ids.add(item.path("id").asLong()));
        assertThat(ids).containsExactly(104L, 106L, 108L);
    }

    @Test
    void healthTagsReturnsDistinctCandidateSet() throws Exception {
        mockMvc.perform(get("/api/shop/product/health-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    void invalidPaginationAndMissingProductReturnErrors() throws Exception {
        mockMvc.perform(get("/api/shop/product/page").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(get("/api/shop/product/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }
}
