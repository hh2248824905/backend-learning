package top.a1788.config.properties;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StudentProperties 配置绑定测试。
 *
 * @SpringBootTest 会从测试类所在包向上查找 @SpringBootConfiguration，
 * 在 top.a1788.config 包找到 ConfigApplication 后启动完整上下文，
 * 因此 properties 包下注册的 StudentProperties Bean 可以直接 @Resource 注入。
 */
@Slf4j
@SpringBootTest
public class StudentPropertiesTest {

    @Resource
    private StudentProperties studentProperties;

    @Test
    public void printStudentInfo() {
        log.info("学生信息：{}", studentProperties);
        //打印语文
        log.info("学生姓名: {}", studentProperties.getName());
        log.info("学生语文成绩: {}", studentProperties.getScores().get("chinese"));

        // 断言绑定结果，确认类型安全绑定生效
        assertEquals("张三", studentProperties.getName());
        assertEquals(18, studentProperties.getAge());
        assertEquals(90, studentProperties.getScores().get("chinese"));
        assertEquals("江苏省", studentProperties.getAddress().getProvince());
        assertEquals("高等数学", studentProperties.getCourses().get(0).getName());
    }
}
