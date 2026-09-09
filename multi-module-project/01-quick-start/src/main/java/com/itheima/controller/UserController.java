package com.itheima.controller;

import com.itheima.config.AppConfig;
import com.itheima.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 用户控制器，演示 RESTful 接口开发。
 * @RestController = @Controller + @ResponseBody（返回值直接序列化为 JSON）。
 */
@RestController
public class UserController {

    /** 注入 02-config 模块的配置类，演示跨模块调用 */
    private final AppConfig appConfig;

    public UserController(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 查询用户信息，返回 User 对象的 JSON。
     * 浏览器访问 http://localhost:8080/user/info 即可验证。
     */
    @GetMapping("/user/info")
    public User getUserInfo() {
        return new User(1L, "张三", 25, LocalDate.of(2001, 5, 20));
    }

    /**
     * 返回应用配置信息（来自 02-config 模块 + application.yml），
     * 验证配置读取和跨模块依赖是否生效。
     * 浏览器访问 http://localhost:8080/app/info。
     */
    @GetMapping("/app/info")
    public AppConfig getAppInfo() {
        return appConfig;
    }
}
