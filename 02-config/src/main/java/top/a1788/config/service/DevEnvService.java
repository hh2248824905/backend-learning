package top.a1788.config.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 开发环境实现，仅在 dev 环境生效
 *
 * @Service 语义化的注解，用它标注的类一样可以被 Spring 容器托管为唯一的 Bean
 * @Profile("dev") 容器只在 dev 环境激活时注册这个 Bean
 */
@Service
@Profile("dev")
public class DevEnvService implements EnvService {

    @Override
    public String envInfo() {
        return "我是 dev 环境专属的 Bean";
    }
}
