package com.school.classboard.config;

import com.school.classboard.service.ImageStorageService;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把上传图片目录挂到 /uploads/*，与 Vaadin 路由互不影响。
 */
@Configuration
public class UploadConfig {

    @Bean
    public ServletRegistrationBean<UploadServlet> uploadServlet(ImageStorageService storage) {
        ServletRegistrationBean<UploadServlet> registration =
                new ServletRegistrationBean<>(new UploadServlet(storage.getRoot()), "/uploads/*");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
