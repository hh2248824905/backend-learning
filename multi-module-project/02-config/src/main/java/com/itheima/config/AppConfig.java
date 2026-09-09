package com.itheima.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置类（位于 02-config 模块）。
 *
 * 演示 Spring Boot 配置文件管理：
 * 用 @ConfigurationProperties(prefix = "app") 把 application.yml 中
 * app.* 开头的配置自动注入到本类的同名属性。
 *
 * 注意：本类在 02-config 模块，包名 com.itheima.config，
 * 会被 01-quick-start 的 @SpringBootApplication 自动扫描装配。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /** 应用名称，对应 app.name */
    private String name;

    /** 应用版本，对应 app.version */
    private String version;

    /** 应用描述，对应 app.description */
    private String description;
}
