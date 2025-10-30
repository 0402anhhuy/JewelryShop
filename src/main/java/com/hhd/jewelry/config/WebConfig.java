package com.hhd.jewelry.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 1. Tiêm giá trị từ application.properties
    //    (Nó sẽ tự động lấy từ dev hay prod tùy profile)
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 2. Giải quyết đường dẫn tuyệt đối
        //    (ví dụ: "./uploads/" -> "/var/app/current/uploads/")
        Path resolvedUploadPath = Paths.get(uploadDir).toAbsolutePath();

        // 3. Chuyển nó thành định dạng URI "file:/..."
        String resourceLocation = resolvedUploadPath.toUri().toString();

        System.out.println("✅ Ánh xạ URL /uploads/** tới thư mục: " + resourceLocation);

        // 4. Đăng ký resource handler
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}