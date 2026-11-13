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
import java.util.UUID;
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
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCanRegisterLoginAndLogout() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult registerResult = mockMvc.perform(post("/api/shop/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.roles[0]").value("user"))
                .andReturn();

        String accessToken = readAccessToken(registerResult);
        assertThat(accessToken.split("\\.")).hasSize(3);

        mockMvc.perform(post("/api/shop/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(post("/api/shop/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void seededUserCanLoginAndInvalidPasswordIsRejected() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/shop/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user01","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("user01"))
                .andExpect(jsonPath("$.data.nickname").value("FreshMeal User"))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        Instant expires = Instant.parse(
                loginBody.path("data").path("expires").asText());
        assertThat(expires).isAfter(Instant.now());

        mockMvc.perform(post("/api/shop/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user01","password":"invalid-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void duplicateRegistrationReturnsUnifiedBadRequest() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {"username":"%s","password":"123456"}
                """.formatted(username);

        mockMvc.perform(post("/api/shop/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/shop/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void logoutWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/shop/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void userTokenCannotAuthenticateAdminEndpoint() throws Exception {
        String username = "it-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult registerResult = mockMvc.perform(post("/api/shop/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = readAccessToken(registerResult);

        mockMvc.perform(get("/api/admin/auth/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    private String readAccessToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }
}
