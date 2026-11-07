package com.school.classboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 电子班牌系统启动类。
 *
 * <p>后端：Spring Boot + Spring Data JPA + Spring Security。
 * <p>前端：Vaadin Flow，界面完全由 Java 编写、服务端渲染，无需手写 JS/HTML。
 */
@EnableScheduling
@SpringBootApplication
public class ClassBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassBoardApplication.class, args);
    }
}
