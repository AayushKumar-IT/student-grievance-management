package com.grievance.management.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Serve evidence/upload files from the local filesystem
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");

        // Serve Angular static files from classpath:/static/
        // For any path that doesn't match a real file, fall back to index.html
        // This enables Angular client-side routing to work correctly.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        // If the file exists (JS, CSS, images, etc.) serve it directly
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        // Otherwise fall back to index.html for Angular routing
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
