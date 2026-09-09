package com.itheima.controller;

import com.itheima.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 用户控制器，演示 RESTful 接口开发。
 * @RestController = @Controller + @ResponseBody（返回值直接序列化为 JSON）。
 *
 * 注：培训 2 之后，01-quick-start 不再依赖 02-config 模块，
 * 所以删除了原 /app/info 端点和 AppConfig 注入，模块完全自包含。
 */
@RestController
public class UserController {

    /**
     * 查询用户信息，返回 User 对象的 JSON。
     * 浏览器访问 http://localhost:8080/user/info 即可验证。
     */
    @GetMapping("/user/info")
    public User getUserInfo() {
        return new User(1L, "张三", 25, LocalDate.of(2001, 5, 20));
    }
}
