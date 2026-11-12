package com.freshmeal.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.freshmeal.dto.common.PageQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerTest.TestControllerConfiguration.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Test
    void successResponseContainsAllContractFields() throws Exception {
        mockMvc.perform(get("/__common-test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("ok"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @ParameterizedTest
    @CsvSource({
            "40000, 400",
            "40101, 401",
            "40301, 403",
            "40400, 404",
            "40900, 409",
            "50000, 500"
    })
    void businessErrorCodesMapToHttpStatus(int code, int status) throws Exception {
        mockMvc.perform(get("/__common-test/business/{code}", code))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value("test business error"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void invalidPaginationReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/__common-test/page").param("current", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message", containsString("current")));
    }

    @Test
    void missingRequiredFieldReturnsFirstValidationMessage() throws Exception {
        mockMvc.perform(post("/__common-test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("name: 名称不能为空"));
    }

    @Test
    void invalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/__common-test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void unknownPathReturnsUnifiedNotFoundResponse() throws Exception {
        mockMvc.perform(get("/__common-test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("请求资源不存在"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(get("/__common-test/validated"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/__common-test/validated")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<request/>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("不支持的请求内容类型"));
    }

    @Test
    void unexpectedExceptionDoesNotLeakInternalDetails() throws Exception {
        mockMvc.perform(get("/__common-test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("系统异常，请稍后重试"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("secret database failure"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("IllegalStateException"))));
    }

    @Test
    void paginationInterceptorUsesMysqlAndMaxLimit() {
        List<com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor> interceptors =
                mybatisPlusInterceptor.getInterceptors();

        org.assertj.core.api.Assertions.assertThat(interceptors)
                .hasSize(1)
                .first()
                .isInstanceOf(PaginationInnerInterceptor.class)
                .extracting(interceptor -> ((PaginationInnerInterceptor) interceptor).getDbType())
                .isEqualTo(com.baomidou.mybatisplus.annotation.DbType.MYSQL);

        PaginationInnerInterceptor pagination = (PaginationInnerInterceptor) interceptors.get(0);
        org.assertj.core.api.Assertions.assertThat(pagination.getMaxLimit()).isEqualTo(100L);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestControllerConfiguration {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    @RequestMapping("/__common-test")
    static class TestController {

        @GetMapping("/success")
        public Result<String> success() {
            return Result.ok("ok");
        }

        @GetMapping("/business/{code}")
        public Result<Void> business(@PathVariable int code) {
            throw new BusinessException(code, "test business error");
        }

        @GetMapping("/page")
        public Result<PageResult<String>> page(@Valid PageQuery query) {
            return Result.ok(PageResult.of(List.of(), 0, query.getCurrent(), query.getSize()));
        }

        @PostMapping("/validated")
        public Result<Void> validated(@Valid @RequestBody ValidatedRequest request) {
            return Result.ok();
        }

        @GetMapping("/boom")
        public Result<Void> boom() {
            throw new IllegalStateException("secret database failure");
        }
    }

    record ValidatedRequest(@NotBlank(message = "名称不能为空") String name) {
    }
}
