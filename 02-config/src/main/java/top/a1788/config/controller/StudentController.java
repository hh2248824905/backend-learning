package top.a1788.config.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.a1788.config.properties.StudentProperties;

/**
 * 学生配置接口：把 @ConfigurationProperties 绑定好的 StudentProperties 直接以 JSON 返回
 *
 * 与 ConfigController 的 @Value 散装注入形成对比：
 * 一个 POJO 一次拿走 student 前缀下的全部嵌套结构
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentProperties studentProperties;

    /**
     * GET /student/info
     * 返回 student 前缀下的完整配置（简单字段 + List + Map + 嵌套对象 + 对象列表）
     */
    @GetMapping("/info")
    public StudentProperties getStudentInfo() {
        return studentProperties;
    }
}
