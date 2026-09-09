package com.itheima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 主启动类。
 * @SpringBootApplication 默认扫描本类所在包（com.itheima）及其子包，
 * 因此 com.itheima.entity、com.itheima.controller、com.itheima.config
 * 都会被自动装配（包括 02-config 模块里的 com.itheima.config.AppConfig）。
 */
@SpringBootApplication
public class QuickStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApplication.class, args);
    }
}
