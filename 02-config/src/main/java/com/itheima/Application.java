package com.itheima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 02-config 模块启动类。
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * 默认扫描当前包及其子包（com.itheima.**），所以 controller、entity 不需要额外配置。
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
