package top.a1788.config.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 生产环境实现，仅在 prod 环境生效
 *
 * @Profile("prod") 容器只在 prod 环境激活时注册这个 Bean，
 * 此时 DevEnvService 不存在，EnvService 注入的是本类
 */
@Service
@Profile("prod")
public class ProdEnvService implements EnvService {

    @Override
    public String envInfo() {
        return "我是 prod 环境专属的 Bean";
    }
}
