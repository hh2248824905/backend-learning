package top.a1788.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 应用配置属性类（案例 4：@ConfigurationProperties + @Validated 配置校验）。
 *
 * - @Validated：开启 JSR-380 校验，配置值不满足约束时启动直接报错（fail-fast）
 * - max-count 在 yml 中是 max-count，Java 驼峰 maxCount，@ConfigurationProperties 默认支持松散绑定
 * - author 使用 yml 占位符 ${mxu.name} 注入
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 应用名称（不能为空） */
    @NotBlank(message = "app.name 不能为空")
    private String name;

    /** 作者（占位符引用 yml 中 mxu.name 的值） */
    private String author;

    /** 端口（合法端口范围 1~65535） */
    @Min(value = 1, message = "app.port 必须大于等于 1")
    @Max(value = 65535, message = "app.port 必须小于等于 65535")
    private Integer port;

    /** 最大数量（yml 中为 max-count，松散绑定自动映射为 maxCount） */
    @Min(value = 1, message = "app.max-count 必须大于等于 1")
    @Max(value = 1000, message = "app.max-count 不能超过 1000")
    private Integer maxCount;
}
