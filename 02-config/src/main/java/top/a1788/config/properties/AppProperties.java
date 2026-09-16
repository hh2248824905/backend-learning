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

    /** 端口（不能小于 1024，避免占用系统端口） */
    @Min(value = 1024, message = "app.port 不能小于 1024")
    private Integer port;

    /** 最大数量（不能超过 1000；yml 中为 max-count，松散绑定自动映射） */
    @Max(value = 1000, message = "app.max-count 不能超过 1000")
    private Integer maxCount;
}
