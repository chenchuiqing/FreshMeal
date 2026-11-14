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
import com.freshmeal.entity.Address;
import com.freshmeal.mapper.AddressMapper;
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
class ShopAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddressMapper addressMapper;

    private final List<Long> createdAddressIds = new ArrayList<>();

    @AfterEach
    void restoreSeedAddress() {
        createdAddressIds.forEach(addressMapper::deleteById);
        createdAddressIds.clear();
        Address seed = addressMapper.selectById(1L);
        if (seed != null && seed.getIsDefault() != 1) {
            seed.setIsDefault(1);
            addressMapper.updateById(seed);
        }
    }

    @Test
    void addressCreateEditDefaultAndDeleteMaintainSingleDefault() throws Exception {
        String token = login();
        long seedId = readFirstAddressId(token);

        String requestBody = """
                {"receiver":"阶段三测试","phone":"13800000011",
                 "province":"上海市","city":"上海市","district":"徐汇区",
                 "detail":"漕溪北路88号","longitude":121.436000,
                 "latitude":31.192000,"tag":"测试","isDefault":true}
                """;
        MvcResult createResult = mockMvc.perform(post("/api/shop/address")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andReturn();
        JsonNode created = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).path("data");
        long createdId = created.path("id").asLong();
        createdAddressIds.add(createdId);

        mockMvc.perform(get("/api/shop/address")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(createdId))
                .andExpect(jsonPath("$.data[1].id").value(seedId))
                .andExpect(jsonPath("$.data[1].isDefault").value(false));

        mockMvc.perform(put("/api/shop/address/" + seedId + "/default")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(seedId))
                .andExpect(jsonPath("$.data.isDefault").value(true));

        String updateBody = """
                {"receiver":"阶段三测试","phone":"13800000011",
                 "province":"上海市","city":"上海市","district":"徐汇区",
                 "detail":"漕溪北路99号","longitude":121.437000,
                 "latitude":31.193000,"tag":"更新","isDefault":false}
                """;
        mockMvc.perform(put("/api/shop/address/" + createdId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detail").value("漕溪北路99号"))
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.longitude").value(121.437));

        mockMvc.perform(delete("/api/shop/address/" + createdId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value((Object) null));

        MvcResult listResult = mockMvc.perform(get("/api/shop/address")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn();
        JsonNode remaining = objectMapper.readTree(
                listResult.getResponse().getContentAsString()).path("data").get(0);
        assertThat(remaining.path("id").asLong()).isEqualTo(seedId);
        assertThat(remaining.path("isDefault").asBoolean()).isTrue();
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

    private long readFirstAddressId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/shop/address")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();
    }
}
