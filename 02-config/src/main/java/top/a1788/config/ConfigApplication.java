package top.a1788.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 02-config 模块启动类（配置管理案例，包名 top.a1788.config）。
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * 默认扫描当前包及其子包（top.a1788.config.**），
 * 所以 properties 下的 @Component 配置属性类会被自动注册为 Bean。
 */
@SpringBootApplication
public class ConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
