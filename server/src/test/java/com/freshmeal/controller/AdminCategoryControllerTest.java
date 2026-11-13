package com.freshmeal.controller;

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
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createdCategoryId;

    @AfterEach
    void cleanUp() throws Exception {
        if (createdCategoryId != null) {
            mockMvc.perform(delete("/api/admin/category/{id}", createdCategoryId)
                            .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
            createdCategoryId = null;
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
    void categoryLifecycleFromCreateToUpdateToDelete() throws Exception {
        String token = adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/admin/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"[TEST]测试分类\",\"sort\":99,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        createdCategoryId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").asLong();

        mockMvc.perform(get("/api/admin/category/tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(put("/api/admin/category/{id}", createdCategoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"[TEST]测试分类改\",\"sort\":98,\"status\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/shop/category/tree"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/category/{id}", createdCategoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        createdCategoryId = null;
    }

    @Test
    void duplicateSiblingNameIsRejected() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/api/admin/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"轻食沙拉\",\"status\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void deleteGuardsRejectCategoriesWithChildrenOrProducts() throws Exception {
        String token = adminToken();

        // 分类 1 存在子分类
        mockMvc.perform(delete("/api/admin/category/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));

        // 分类 11 存在商品
        mockMvc.perform(delete("/api/admin/category/11")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    void categoryEndpointsRequireAdminAuth() throws Exception {
        mockMvc.perform(get("/api/admin/category/tree"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(post("/api/admin/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
