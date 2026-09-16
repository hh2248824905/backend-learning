package top.a1788.config.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.a1788.config.properties.AppProperties;
import top.a1788.config.properties.StudentProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置读取控制器：把 @ConfigurationProperties 绑定好的 Bean 直接以 JSON 返回，
 * 用于在 Apifox 中同步接口并肉眼验证配置绑定结果。
 *
 * 注意对比：这里注入的是 POJO（StudentProperties），
 * 一次拿到全部字段；用 @Value 则每个字段都得写一个 ${} 注解。
 */
@RestController
@RequestMapping("/config")
public class ConfigController {

    @Resource
    private StudentProperties studentProperties;

    @Resource
    private AppProperties appProperties;

    /**
     * GET /config/student
     * 返回 student 前缀下的完整配置（简单字段 + List + Map + 嵌套对象 + 对象列表）。
     */
    @GetMapping("/student")
    public StudentProperties student() {
        return studentProperties;
    }

    /**
     * GET /config/app
     * 返回 app 前缀下的配置（含占位符注入的 author 与 @Validated 校验通过的 port/maxCount）。
     */
    @GetMapping("/app")
    public AppProperties app() {
        return appProperties;
    }

    /**
     * GET /config/summary
     * 汇总常用字段，方便在 Apifox 里直接看关键值，不用翻嵌套结构。
     */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("学生姓名", studentProperties.getName());
        result.put("学生年龄", studentProperties.getAge());
        result.put("爱好数量", studentProperties.getHobbies().size());
        result.put("语文成绩", studentProperties.getScores().get("chinese"));
        result.put("所在城市", studentProperties.getAddress().getCity());
        result.put("课程数量", studentProperties.getCourses().size());
        result.put("应用名称", appProperties.getName());
        result.put("作者", appProperties.getAuthor());
        return result;
    }
}
