package com.freshmeal.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 商品图片访问映射：{@code /api/files/**} 优先读取本地上传目录，其次读取种子图片（classpath）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${freshmeal.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(location, "classpath:/static/files/");
    }
}
