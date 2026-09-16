package top.a1788.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "学生控制器，演示 @ConfigurationProperties 类型安全绑定")
@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentProperties studentProperties;

    /**
     * GET /student/info
     * 返回 student 前缀下的完整配置（简单字段 + List + Map + 嵌套对象 + 对象列表）
     */
    @Operation(summary = "返回 student 前缀下的完整配置（含 List/Map/嵌套对象/对象列表）")
    @GetMapping("/info")
    public StudentProperties getStudentInfo() {
        return studentProperties;
    }
}
