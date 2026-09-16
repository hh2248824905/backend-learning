package top.a1788.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 学生配置属性类（案例 1：@ConfigurationProperties 类型安全绑定）。
 *
 * 演示四种绑定形态：
 * - 基本属性：name、age
 * - List 集合：hobbies（yml 中用 "- 值" 列表语法）
 * - Map 集合：scores（yml 中用 "key: value" 语法）
 * - 嵌套对象 + 对象列表：address、courses
 *
 * @Component + @ConfigurationProperties(prefix = "student")：
 * 把该类注册为 Bean，并把 yml 中 student 下的配置按属性名自动注入。
 * 这一步是视频里测试报 "No bean of type 'StudentProperties'" 的原因——
 * 少了 @Component（或不在启动类上加 @EnableConfigurationProperties）时 Bean 不存在。
 */
@Data
@Component
@ConfigurationProperties(prefix = "student")
public class StudentProperties {

    /** 学生姓名 */
    private String name;

    /** 年龄 */
    private Integer age;

    /** 爱好列表（List 绑定） */
    private List<String> hobbies;

    /** 各科成绩（Map 绑定：chinese/math/english） */
    private Map<String, Integer> scores;

    /** 住址（嵌套对象绑定） */
    private Address address = new Address();

    /** 课程列表（自定义对象列表绑定） */
    private List<Course> courses;

    /**
     * 嵌套对象：住址
     */
    @Data
    public static class Address {
        /** 省份 */
        private String province;
        /** 城市 */
        private String city;
    }

    /**
     * 自定义对象：课程
     */
    @Data
    public static class Course {
        /** 课程名 */
        private String name;
        /** 学分 */
        private Integer credit;
    }
}
