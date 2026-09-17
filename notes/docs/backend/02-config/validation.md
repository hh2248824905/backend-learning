# 配置校验与最佳实践

<NoteStatus level="done" />

## 0. 本篇要解决的问题

1. 配置写错了（比如端口写成 `99999`），怎么让它在**启动阶段**就报错，而不是上线后某个请求才炸？
2. `@Validated` 加在配置类上，具体做了什么？
3. 校验失败的报错长什么样？能定位到是哪个配置项吗？
4. 不是所有配置都该校验 —— 哪些该校验、哪些不校验？
5. 配置类的命名和分层怎么组织才不乱？

## 1. 为什么需要配置校验

**没有校验时的问题**：配置错误会「潜伏」到运行时才爆发。

```
配置写错 → 应用正常启动 → 跑到某个业务分支 → 抛出难以定位的异常
```

典型例子：

| 配置错误 | 不校验的后果 |
|---|---|
| `app.port: 99999` | 如果这个值被用来构造 URL，运行时才报端口非法 |
| `app.name:`（留空） | 某个逻辑依赖应用名，运行时 NPE |
| `app.max-count: -1` | 分页查询时 `LIMIT -1` 报 SQL 异常 |
| 数据库地址写错 | 启动能过，第一次查库才失败 |

**有校验时**：`@Validated` 让这些错误在**容器启动阶段**就抛出来，`APPLICATION FAILED TO START`，根本跑不起来。

::: tip fail-fast 的价值
**「启动失败」远好于「启动成功但功能异常」**。

启动失败：CI 立刻红灯、部署流水线立刻中止、开发本地立刻发现 —— 问题暴露在**没有任何用户受影响**的时刻。

启动成功但功能异常：上线后跑了几个小时，某个定时任务触发才炸 —— 这时候已经在生产环境造成影响了。
:::

## 2. 实现方式

### 2.1 加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

::: warning 缺这个依赖会怎样
`@Validated` 注解本身来自 Spring（`org.springframework.validation.annotation`），但**校验逻辑的执行者**是 Jakarta Validation 的实现（Hibernate Validator）。

少了这个 starter，**校验静默失效** —— 不报错、不提示，配了个非法值照样启动。这比报错更危险。

这个 starter 不在 `spring-boot-starter-web` 里，必须单独引入。（Spring Boot 2.3 之前是含在 web starter 里的，之后拆出来了，很多人升级后踩这个坑。）
:::

### 2.2 配置类

```java [AppProperties.java]
@Data
@Validated                                                // ① 开启 JSR-380 校验
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 应用名称（不能为空） */
    @NotBlank(message = "app.name 不能为空")               // ② 字段级约束
    private String name;

    /** 作者（占位符引用 yml 中 mxu.name 的值） */
    private String author;                                // 无约束

    /** 端口（合法端口范围 1~65535） */
    @Min(value = 1, message = "app.port 必须大于等于 1")
    @Max(value = 65535, message = "app.port 必须小于等于 65535")
    private Integer port;

    /** 最大数量 */
    @Min(value = 1, message = "app.max-count 必须大于等于 1")
    @Max(value = 1000, message = "app.max-count 不能超过 1000")
    private Integer maxCount;
}
```

| 元素 | 作用 |
|---|---|
| `@Validated` | 类级别，告诉 Spring「这个 Bean 绑定完配置后跑一遍校验」 |
| `@NotBlank` / `@Min` / `@Max` | 字段级别，具体约束 |
| `message = "..."` | 校验失败时的提示信息，**不写会用默认的英文提示** |

### 2.3 对应的配置

```yaml [application.yml]
app:
  name: 配置管理模块
  author: ${mxu.name}
  port: 8002          # 在 1~65535 范围内 ✅
  max-count: 100      # 在 1~1000 范围内 ✅
```

正常启动时校验通过，接口正常返回：

```bash
curl http://localhost:8002/config/app
```

```json
{"name":"配置管理模块","author":"a1788","port":8002,"maxCount":100}
```

## 3. 校验失败长什么样（真实输出）

故意传一个非法端口启动：

```bash
java -jar 02-config/target/02-config-0.0.1-SNAPSHOT.jar --app.port=99999
```

**进程退出码 `1`，应用启动失败**，控制台输出：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Binding to target top.a1788.config.properties.AppProperties failed:

    Property: app.port
    Value: "99999"
    Origin: "app.port" from property source "commandLineArgs"
    Reason: app.port 必须小于等于 65535


Action:

Update your application's configuration
```

### 3.1 这段报错为什么好用

它把定位问题需要的信息全给齐了：

| 字段 | 值 | 作用 |
|---|---|---|
| `Property` | `app.port` | 哪个配置项错了（精确到 key 路径） |
| `Value` | `"99999"` | 错误的值是什么（本来以为配对了，一看就知道） |
| `Origin` | `commandLineArgs` | **这个值从哪来的** —— 命令行参数 |
| `Reason` | `app.port 必须小于等于 65535` | 违反了什么约束（这里用了自定义 message） |

::: tip `Origin` 字段是排查多来源冲突的利器
配置项在多个地方都定义了，不校验的话你不知道最终生效的是哪个。

有了 `Origin`，报错直接告诉你「这个值来自 `commandLineArgs`」，你就知道去命令行而不是 yml 里找问题。

常见的 `Origin` 值：

| Origin | 来源 |
|---|---|
| `commandLineArgs` | 命令行参数 `--key=value` |
| `systemProperties` | JVM 系统属性 `-Dkey=value` |
| `systemEnvironment` | 操作系统环境变量 |
| `applicationConfig: [classpath:/application-dev.yml]` | 具体某个 yml 文件 |
:::

### 3.2 不写 `message` 会怎样

```java
@Max(65535)   // 没写 message
```

报错会变成默认提示：

```
Reason: must be less than or equal to 65535
```

**强烈建议写中文 `message`**。原因：默认提示不带 key 名，看到 `must be less than or equal to 65535` 还得回头看是哪个字段。

写成 `"app.port 必须小于等于 65535"` 自带上下文，排查时省一步。

## 4. 常用校验注解

Jakarta Validation（JSR-380）提供的注解，全部可用：

### 4.1 空值校验

| 注解 | 规则 | 适用类型 |
|---|---|---|
| `@NotNull` | 不能是 `null` | 任意 |
| `@NotEmpty` | 不能是 `null`，且长度/集合大小 > 0 | `String`、`Collection`、`Map`、数组 |
| `@NotBlank` | 不能是 `null`，且**去掉首尾空格后**长度 > 0 | **只能用于 `String`** |

::: warning `@NotEmpty` 和 `@NotBlank` 的区别最容易搞混
```java
@NotEmpty private String name;   // "   "（三个空格）→ 通过校验！因为长度是 3
@NotBlank private String name;   // "   " → 校验失败
```

**配置项用 `@NotBlank`**，因为 yml 里写 `name: ""` 或 `name: "  "` 都是明显的错误配置。
:::

### 4.2 数值校验

| 注解 | 规则 |
|---|---|
| `@Min(value)` | ≥ value |
| `@Max(value)` | ≤ value |
| `@DecimalMin("0.1")` | 用于 `BigDecimal`，支持字符串写小数 |
| `@DecimalMax("100.00")` | 同上 |
| `@Positive` | > 0 |
| `@PositiveOrZero` | ≥ 0 |
| `@Negative` / `@NegativeOrZero` | < 0 / ≤ 0 |
| `@Digits(integer=6, fraction=2)` | 整数部分最多 6 位，小数最多 2 位 |

### 4.3 字符串与其他

| 注解 | 规则 |
|---|---|
| `@Size(min=, max=)` | 长度/大小范围，可用于 `String`、集合、数组 |
| `@Pattern(regexp="...")` | 正则匹配 |
| `@Email` | 邮箱格式 |
| `@Past` / `@Future` | 时间必须在过去 / 未来 |

### 4.4 实战例子

```java
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank(message = "app.name 不能为空")
    @Size(max = 50, message = "app.name 长度不能超过 50")
    private String name;

    @Min(value = 1, message = "app.port 必须大于等于 1")
    @Max(value = 65535, message = "app.port 必须小于等于 65535")
    private Integer port;

    @Positive(message = "app.timeout 必须为正数")
    private Integer timeout;

    @Pattern(regexp = "^https?://.+", message = "app.base-url 必须是 http/https 地址")
    private String baseUrl;
}
```

## 5. 哪些配置该校验，哪些不用

不是所有配置项都值得加约束。加太多会维护成本高，加太少起不到保护作用。

| 类型 | 建议 | 理由 |
|---|---|---|
| **关键的、有明确合法范围的** | ✅ 必须校验 | 端口、线程数、超时时间、比例值、URL 格式 |
| **必填的标识类配置** | ✅ 加 `@NotBlank` | 应用名、环境标识、第三方 appKey |
| **可选的、有合理默认行为的** | ❌ 不校验 | 备注、描述、开关（有默认值就行） |
| **敏感配置（密码、密钥）** | ⚠️ **只校验收非空，不给默认值** | 见下 |
| **复杂结构** | ❌ 不校验（或自定义校验器） | List / Map 内部的约束不好用注解表达 |

::: danger 敏感配置绝对不要写默认值
```java
// ❌ 绝对不要这样
@Value("${db.password:123456}")
private String dbPassword;
```

**后果**：生产环境的配置文件漏配了密码，应用**不会报错**，而是用 `123456` 静默启动，连着一个错误的数据库。

**正确做法**：

```java
@NotBlank(message = "db.password 必须配置，且不能为空")
private String dbPassword;
```

**宁可启动失败，也不要静默用一个错误的默认值跑起来。**
:::

## 6. 配置类的组织方式

### 6.1 命名规范

```
<业务域>Properties
```

| 例子 | 对应前缀 | 说明 |
|---|---|---|
| `AppProperties` | `app` | 应用级配置 |
| `StudentProperties` | `student` | 学生业务配置 |
| `JwtProperties` | `jwt` | JWT 认证配置 |
| `OssProperties` | `oss` | 对象存储配置 |
| `DataSourceProperties` | 框架自带 | 不要和框架类重名 |

### 6.2 一个前缀一个类，不要塞在一起

```java
// ❌ 反例：什么都往里塞，一个类管所有配置
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;          // 应用名
    private String jwtSecret;     // JWT 密钥（不同业务域）
    private String ossEndpoint;   // OSS 地址（不同业务域）
    private Integer threadPoolSize;
}

// ✅ 正例：按业务域拆开，各自管自己的前缀
@ConfigurationProperties(prefix = "app")  class AppProperties { ... }
@ConfigurationProperties(prefix = "jwt")  class JwtProperties { ... }
@ConfigurationProperties(prefix = "oss")  class OssProperties { ... }
```

**拆分的判断标准**：这个配置项换个业务模块还要用吗？不用就属于那个模块的前缀。

### 6.3 建议给未配置的嵌套对象设默认值

```java
private Address address = new Address();   // ✅ 即使 yml 里没配也不会 NPE

private Address address;                   // ⚠️ 依赖这个字段的代码要处处判空
```

### 6.4 配置类的包位置

```
top.a1788.config.properties/     ← 本项目的位置
```

**约定**：放在业务模块的 `properties`（或 `config`）子包下，和 `controller` / `service` 平级。

不要放进 `entity` —— `entity` 通常是对应数据库表的实体，语义不同。

## 7. 踩过的坑

### 7.1 校验静默失效：缺 `spring-boot-starter-validation`

**现象**：配了 `@NotBlank` 和 `@Min`，但传非法值照样启动。

**原因**：少了 `spring-boot-starter-validation` 依赖。

**为什么危险**：不报错、不警告，你以为有保护，实际是裸奔。

**排查**：

```bash
mvn -pl 02-config dependency:tree | grep validation
```

能搜到 `hibernate-validator` 才算配好了。

::: tip 怎么快速验证校验真的生效了
故意传个非法值启动，看是否 `APPLICATION FAILED TO START`：

```bash
java -jar app.jar --app.port=99999
```

**能起来就说明校验没生效**，去查依赖。
:::

### 7.2 校验报错看不出是哪个配置项

**现象**：`Reason: must not be blank` —— 不知道是哪个字段空的。

**原因**：注解上没写 `message`。

**解法**：每个约束都写清楚 key 名。

```java
@NotBlank(message = "app.name 不能为空")   // ✅ 报错直接说明是 app.name
@NotBlank                                  // ❌ 只有 "must not be blank"
```

### 7.3 加 `@Validated` 后启动直接挂了

**现象**：加完校验注解，应用起不来了。

**这不一定是 bug，很多时候是校验抓到了真问题** —— 比如某个必填配置项确实没配。

**排查顺序**：

1. 看报错里的 `Property` 是哪个 key
2. 看 `Origin` 确认这个值来自哪个文件或参数
3. **判断是「配置错了」还是「约束写太严了」**

如果是约束不合理（比如把可选配置标成了 `@NotBlank`），调整注解而不是去补一个假值。

### 7.4 `@Validated` 加错位置

```java
@Validated                    // 加在类上 ✅ —— 校验该类绑定的配置
@ConfigurationProperties(prefix = "app")
public class AppProperties { }
```

有的人会加在字段上或方法上，那是给 `@RequestBody` 参数校验用的，对配置绑定不起作用。

### 7.5 课后实践：新增 email / create-date 两个校验字段

在 `AppProperties` 原有基础上补了两个属性，把常用约束凑齐：

```java
/** 联系邮箱（@Email 校验格式，@NotBlank 保证非空） */
@NotBlank(message = "app.email 不能为空")
@Email(message = "app.email 邮箱格式不正确")
private String email;

/** 创建日期（@Past 只能是过去的日期；yml 写 2024-09-01 即可绑定 LocalDate） */
@Past(message = "app.create-date 必须是过去的日期")
private LocalDate createDate;
```

对应 yml：

```yaml
app:
  email: a1788@example.com
  create-date: 2024-09-01
```

**两个实测的失败现场**（命令行 `--app.email=not-an-email` / `--app.create-date=2099-01-01` 启动，均为退出代码 1）：

```text
APPLICATION FAILED TO START
    Property: app.email
    Origin: "app.email" from property source "commandLineArgs"
    Reason: app.email 邮箱格式不正确
```

```text
APPLICATION FAILED TO START
    Origin: "app.create-date" from property source "commandLineArgs"
    Reason: app.create-date 必须是过去的日期
```

三个收获：

1. `LocalDate` 不用任何转换器注解，yml 写 ISO 格式 `2024-09-01` 就能直接绑定
2. 一个字段可以叠加多个约束（`@NotBlank` + `@Email`），报错时命中的是**第一个**违反的约束
3. 命令行传入的非法值，`Origin` 明确显示 `from property source "commandLineArgs"`——值从哪来一目了然

配置合法时 `/config/app` 返回新增字段：

```json
{"name":"配置管理模块","author":"a1788","port":8002,"maxCount":100,
 "email":"a1788@example.com","createDate":"2024-09-01"}
```

## 8. 小结与自检

### 核心结论

1. 配置校验把错误**前置到启动阶段**，fail-fast 远好于运行时异常
2. 三件套：`spring-boot-starter-validation` 依赖 + `@Validated` 类注解 + JSR-380 约束注解
3. 校验失败的报错会给出 `Property` / `Value` / `Origin` / `Reason`，定位信息完整
4. **`Origin` 能告诉你值来自哪个配置源**，是排查多来源冲突的关键
5. 敏感配置只校验收非空，**绝对不给默认值**
6. 一个前缀一个配置类，按业务域拆分

### 自检清单

- [ ] 少了 `spring-boot-starter-validation`，`@Validated` 的行为是什么？
- [ ] `@NotEmpty` 和 `@NotBlank` 对 `"   "`（纯空格）的判断有什么不同？
- [ ] 校验失败输出里的 `Origin` 字段有什么用？
- [ ] 为什么 `@Value("${db.password:123456}")` 是危险写法？
- [ ] 哪些配置项**不值得**加校验？
- [ ] 怎么快速验证一个配置类上的校验真的生效了？

### 本模块完成

02 配置管理四个子篇到此结束。回顾一下这条链路：

```
配置文件（从哪来、多来源怎么覆盖）      → 模块总览
      ↓
单个值注入（@Value + 占位符 + 默认值）  → @Value 注入详解
      ↓
一组值绑定（@ConfigurationProperties）  → @ConfigurationProperties
      ↓
多环境切换（profile + @Profile）        → 多环境与 Profile
      ↓
配置正确性保障（@Validated fail-fast）  → 本篇
```

### 下一步

[03 日志管理](/backend/03-logging) —— 配置搞定后，下一个问题是「应用运行起来后，我看得见它发生了什么吗」。
