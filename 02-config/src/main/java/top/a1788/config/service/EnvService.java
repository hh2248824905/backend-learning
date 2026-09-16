package top.a1788.config.service;

/**
 * 环境信息服务：用于演示 @Profile 注解
 *
 * 同一个接口，不同环境下注入不同的实现：
 * - dev 环境注入 DevEnvService
 * - prod 环境注入 ProdEnvService
 * 两个实现不会同时存在，启动时按 spring.profiles.active 二选一
 */
public interface EnvService {

    String envInfo();
}
