package com.freshmeal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFileControllerTest {

    /** 1x1 真实 PNG，魔数 89 50 4E 47 0D 0A 1A 0A。 */
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uploadedUrl;

    @AfterEach
    void cleanUp() throws Exception {
        if (uploadedUrl != null) {
            String filename = uploadedUrl.substring(uploadedUrl.lastIndexOf('/') + 1);
            Path file = Paths.get("uploads", "products", filename);
            Files.deleteIfExists(file);
            uploadedUrl = null;
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
    void uploadValidPngReturnsFileUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pixel.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
        MvcResult result = mockMvc.perform(multipart("/api/admin/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        uploadedUrl = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("url").asText();
        org.assertj.core.api.Assertions.assertThat(uploadedUrl)
                .startsWith("/api/files/products/")
                .endsWith(".png");
    }

    @Test
    void uploadUnsupportedTypeReturnsBusinessError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.svg", "image/svg+xml", "<svg/>".getBytes());
        mockMvc.perform(multipart("/api/admin/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    void uploadEmptyFileReturnsBusinessError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
        mockMvc.perform(multipart("/api/admin/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40003));
    }

    @Test
    void uploadOversizeFileReturnsBusinessError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.png", MediaType.IMAGE_PNG_VALUE, new byte[10 * 1024 * 1024 + 1]);
        mockMvc.perform(multipart("/api/admin/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void uploadRequiresAdminAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pixel.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
        mockMvc.perform(multipart("/api/admin/file/upload").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }
}
