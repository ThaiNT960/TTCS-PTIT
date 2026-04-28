package com.example.ptitsocialchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Spring MVC để serve thư mục uploads/ dưới dạng static resource.
 * Khi truy cập /uploads/posts/abc.jpg → Spring sẽ tìm file trong thư mục uploads/posts/abc.jpg
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve thư mục uploads/ từ working directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
