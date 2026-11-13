package com.freshmeal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class RiderAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededRiderCanLoginAndLogout() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/rider/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"rider01","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("rider01"))
                .andExpect(jsonPath("$.data.realName").value("Rider 01"))
                .andExpect(jsonPath("$.data.phone").value("13800000003"))
                .andExpect(jsonPath("$.data.roles[0]").value("rider"))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();
        Instant expires = Instant.parse(
                loginBody.path("data").path("expires").asText());

        assertThat(accessToken.split("\\.")).hasSize(3);
        assertThat(expires).isAfter(Instant.now());

        mockMvc.perform(post("/api/rider/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(post("/api/rider/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void invalidPasswordReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(post("/api/rider/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"rider01","password":"invalid-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void logoutWithoutRiderTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/rider/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void riderTokenCannotAuthenticateAdminEndpoint() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/rider/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"rider01","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/admin/auth/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void riderRegistrationEndpointDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/rider/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"rider01","password":"123456"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }
}
