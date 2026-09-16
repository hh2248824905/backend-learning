package com.itheima.controller;

import com.itheima.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 用户控制器，演示 Spring Boot 配置管理 + RESTful 接口。
 *
 * 重点演示 @Value 注解从 application.yml 读取配置：
 * - 语法必须是 @Value("${属性名}")，注意 $ 符号和 {} 花括号必须齐全
 * - 注解必须用 org.springframework.beans.factory.annotation.Value（springframework 包）
 * - 可以读取内置配置（server.port、spring.application.name）和自定义配置（app.user.*）
 */
@RestController
public class UserController {

    /**
     * 读取 application.yml 中的 server.port。
     * 演示：内置配置（Spring Boot 标准 key）的读取。
     */
    @Value("${server.port}")
    private String serverPort;

    /**
     * 读取 application.yml 中的 spring.application.name。
     * 演示：嵌套配置（spring 下 application 下 name）的读取。
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 读取自定义配置 app.user.name。
     * 演示：完全自定义的属性名依然能用 @Value 注入。
     */
    @Value("${app.user.name}")
    private String appUserName;

    /**
     * 读取自定义配置 app.user.job。
     */
    @Value("${app.user.job}")
    private String appUserJob;

    /**
     * 返回 application.yml 中所有通过 @Value 注入的配置值，
     * 用于一眼验证配置是否读取成功。
     */
    @GetMapping("/config/info")
    public String configInfo() {
        return "server.port=" + serverPort
                + ", spring.application.name=" + applicationName
                + ", app.user.name=" + appUserName
                + ", app.user.job=" + appUserJob;
    }

    /**
     * 返回 User 对象，对应培训中 getUserInfo 演示。
     */
    @GetMapping("/user/info")
    public User getUserInfo() {
        return new User(1L, "张三", 25, LocalDate.of(2001, 5, 20));
    }
}
