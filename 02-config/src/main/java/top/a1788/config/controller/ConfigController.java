package top.a1788.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.a1788.config.properties.AppProperties;
import top.a1788.config.service.EnvService;

/**
 * 配置管理案例：@Value、占位符、随机值、SpEL、多环境、@Profile、配置校验
 *
 * 注意两种注入风格的对比：
 * - 散装单个值用 @Value 逐个注入（本类上半部分）
 * - 一组同前缀的配置用 @ConfigurationProperties 绑成 POJO（appProperties），
 *   所以此处用构造器注入 final 字段，而不是 @Value
 *
 * @Tag 的 name 会成为 Apifox 里的接口分组名（Apifox Helper 上传时读取）
 */
@Tag(name = "配置读取控制器，演示 @Value 的四种常见形态")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final AppProperties appProperties;
    private final EnvService envService;

    // ---------- 基础 @Value 注入 ----------
    @Value("${server.port}")
    private Integer serverPort;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${mxu.name}")
    private String myName;

    @Value("${mxu.job}")
    private String myJob;

    // ---------- 占位符引用：app.author 在 yml 中引用了 mxu.name ----------
    @Value("${app.author}")
    private String author;

    // ---------- 默认值：app.remark 未配置时使用冒号后的默认值 ----------
    @Value("${app.remark:暂无备注}")
    private String remark;

    // ---------- 随机值 ----------
    @Value("${random.uuid}")
    private String randomUuid;

    @Value("${random.int(1,100)}")
    private Integer randomInt;

    // ---------- SpEL 表达式：先解析 ${student.age} 再计算三元表达式 ----------
    @Value("#{${student.age} >= 18 ? '成年' : '未成年'}")
    private String adult;

    // ---------- 多环境配置：值来自 application-{profile}.yml ----------
    @Value("${env.name}")
    private String envName;

    @Value("${env.description}")
    private String envDescription;

    @Operation(summary = "基础 @Value：读取 server.port 和 spring.application.name")
    @GetMapping("/basic")
    public String getBasicInfo() {
        return "服务器端口是：" + this.serverPort + "，应用名称是：" + appName;
    }

    @Operation(summary = "@Value 读取自定义配置 mxu.name / mxu.job")
    @GetMapping("/my")
    public String getMyInfo() {
        return "我的姓名是：" + this.myName + "，职业是：" + myJob;
    }

    @Operation(summary = "@Value 四种形态：占位符引用、默认值、随机值、SpEL 表达式")
    @GetMapping("/value")
    public String getValueCases() {
        return "占位符引用 author=" + author
                + "；默认值 remark=" + remark
                + "；随机 UUID=" + randomUuid
                + "；随机整数=" + randomInt
                + "；SpEL adult=" + adult;
    }

    @Operation(summary = "多环境配置 + @Profile Bean，值随 spring.profiles.active 切换")
    @GetMapping("/env")
    public String getEnv() {
        return "当前环境：" + envName + "，" + envDescription + "；Profile Bean：" + envService.envInfo();
    }

    @Operation(summary = "@ConfigurationProperties 绑定的 AppProperties（含 @Email/@Past 校验字段）")
    @GetMapping("/app")
    public AppProperties getApp() {
        return appProperties;
    }
}
