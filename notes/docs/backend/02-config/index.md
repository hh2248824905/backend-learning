# 02 配置管理 · 模块总览

<NoteStatus level="done" />

::: info 模块坐标
源码：`E:\houduan\02-config`　·　端口：`8002`　·　包名：`top.a1788.config`（另有 `com.itheima` 遗留代码）
:::

## 0. 这个模块为什么单独拆成四篇

配置管理看着简单 —— 不就是往 `application.yml` 里写几个值吗？实际动手会发现它牵扯到四件独立的事：

| 问题 | 落到哪篇 |
|---|---|
| 单个值怎么读进来？占位符、默认值、随机值怎么写？ | [@Value 注入详解](/backend/02-config/value) |
| 一组同前缀的配置怎么绑成对象？List/Map/嵌套结构怎么办？ | [@ConfigurationProperties](/backend/02-config/properties) |
| dev/test/prod 怎么切？Bean 怎么按环境注册？ | [多环境与 Profile](/backend/02-config/profile) |
| 配错了怎么在启动时就发现，而不是上线后炸？ | [配置校验与最佳实践](/backend/02-config/validation) |

四篇共用的基础（配置文件从哪来、优先级怎么算）在**本页**讲完。

## 1. 配置文件从哪来

Spring Boot 启动时会按固定顺序去找配置，全部加载进一个叫 `Environment` 的对象里。

### 1.1 默认加载的四个位置

按优先级从高到低：

| 顺序 | 位置 | 说明 |
|---|---|---|
| 1 | `./config/`（jar 同级目录的 config 子目录） | 外部配置，优先级最高 |
| 2 | `./`（jar 同级目录） | 外部配置 |
| 3 | `classpath:/config/` | 打包进 jar 的 config 目录 |
| 4 | `classpath:/` | 打包进 jar 的根目录 |

后加载的**不会覆盖**先加载的 —— 而是**优先级高的先被查到**。查一个 key 时按顺序找，找到就返回。

::: tip 这个顺序的实际意义
1 和 2 是「外部配置」——打好的 jar 不用重新打包，把新的 `application.yml` 放到 jar 旁边就能改配置。生产环境常用这招。

3 和 4 是「内部配置」——`src/main/resources/` 下的文件最终会进 jar 的 `classpath:/`。

**开发时你写的是 4，部署时可以覆盖成 2。**
:::

### 1.2 同一个 key 在多个来源都有，听谁的

完整的优先级链（从高到低，上面覆盖下面）：

```
命令行参数             --server.port=9000
      ↓
Java 系统属性          -Dserver.port=9000
      ↓
操作系统环境变量        SERVER_PORT=9000
      ↓
application-{profile}.yml   ← 环境专属配置
      ↓
application.yml             ← 主配置
      ↓
@PropertySource 引入的文件
      ↓
默认值（Spring Boot 内置的）
```

**两条实战推论**：

1. **环境文件覆盖主配置**：同名 key，`application-dev.yml` 里的值赢。所以主配置写通用值，环境文件写差异值。
2. **命令行覆盖一切**：`java -jar app.jar --server.port=9000` 能盖掉任何 yml 里的值。这是运维改配置的标准手段，不动文件、不重新打包。

### 1.3 本项目实际加载的文件

```
02-config/src/main/resources/
├── application.yml         主配置：所有环境共用 + 默认值
├── application-dev.yml     开发环境：日志宽松、env.name=开发环境
└── application-prod.yml    生产环境：日志收紧、env.name=生产环境
```

主配置里显式声明了激活哪个：

```yaml [application.yml]
spring:
  profiles:
    active: dev
```

启动日志会打印实际生效的 profile：

```
The following 1 profile is active: "dev"
```

::: warning 生产环境不要在配置文件里写死 `active: prod`
更稳的做法是**打包时不含 prod 配置，部署时用命令行指定**：

```bash
java -jar app.jar --spring.profiles.active=prod
```

原因：万一 prod 配置里有密钥，写死在仓库里就泄露了。用命令行或环境变量注入更安全。
:::

## 2. `@Value` 和 `@ConfigurationProperties` 怎么选

这是配置模块最核心的一个决策。两者都能读配置，但适用场景完全不同。

| 维度 | `@Value` | `@ConfigurationProperties` |
|---|---|---|
| 读取粒度 | **一个一个读**，每个字段一个注解 | **一组一起读**，按前缀整块绑定 |
| 复杂结构 | ❌ 不支持 List/Map/嵌套对象的自然绑定 | ✅ List / Map / 嵌套对象 / 对象列表全支持 |
| 松散绑定 | ❌ 必须 key 完全一致（`max-count` 不能绑到 `maxCount`） | ✅ `max-count` ↔ `maxCount` ↔ `MAX_COUNT` 自动匹配 |
| 校验支持 | ❌ 不能直接用 JSR-380 注解 | ✅ 配 `@Validated` 就能用 `@NotBlank`/`@Min` 等 |
| 元数据提示 | ❌ IDE 里写 yml 无提示 | ✅ 生成元数据，yml 里能自动补全 |
| 适用场景 | 散装的一两个值、内置配置项 | 一组业务配置（数据库、第三方 SDK、业务规则） |

### 选型口诀

```
一两个散值           → @Value
一组同前缀的配置      → @ConfigurationProperties
需要 List / Map / 嵌套 → @ConfigurationProperties
需要校验             → @ConfigurationProperties
```

本项目两种都用上了，正好对照：

```java [ConfigController.java —— 散装值用 @Value]
@Value("${server.port}")
private Integer serverPort;          // 就一个端口，用 @Value 够了

@Value("${spring.application.name}")
private String appName;
```

```java [ConfigController.java —— 一组配置用 @ConfigurationProperties]
private final AppProperties appProperties;   // name/author/port/maxCount 一组，绑成对象
```

## 3. 模块文件结构与接口

### 3.1 目录结构

```
02-config/src/main/
├── java/
│   ├── com/itheima/                        ← 遗留代码（跟教程早期版本敲的）
│   │   ├── Application.java
│   │   ├── controller/UserController.java
│   │   └── entity/User.java
│   └── top/a1788/config/
│       ├── ConfigApplication.java          启动类
│       ├── controller/
│       │   ├── ConfigController.java       @Value 全案例 + 多环境
│       │   └── StudentController.java      @ConfigurationProperties 案例
│       ├── properties/
│       │   ├── StudentProperties.java      四种绑定形态（List/Map/嵌套/对象列表）
│       │   └── AppProperties.java          带 @Validated 校验
│       └── service/
│           ├── EnvService.java             接口
│           ├── DevEnvService.java          @Profile("dev")
│           └── ProdEnvService.java         @Profile("prod")
└── resources/
    ├── application.yml
    ├── application-dev.yml
    └── application-prod.yml
```

### 3.2 接口清单

| 方法 | 路径 | 验证什么 | 对应章节 |
|---|---|---|---|
| GET | `/config/basic` | 基础 `@Value` 读内置配置 | [@Value](/backend/02-config/value#_2-1-基础读取) |
| GET | `/config/my` | 自定义前缀配置 | [@Value](/backend/02-config/value#_2-2-自定义配置) |
| GET | `/config/value` | 占位符 / 默认值 / 随机值 / SpEL | [@Value](/backend/02-config/value#_2-3-四种进阶写法) |
| GET | `/config/env` | 多环境 + `@Profile` Bean | [Profile](/backend/02-config/profile) |
| GET | `/config/app` | `@ConfigurationProperties` + 校验 | [校验](/backend/02-config/validation) |
| GET | `/student/info` | List / Map / 嵌套对象 / 对象列表 | [@ConfigurationProperties](/backend/02-config/properties) |

### 3.3 启动与验证

```bash
cd E:\houduan
mvn -pl 02-config -am clean package -DskipTests
java -jar 02-config/target/02-config-0.0.1-SNAPSHOT.jar

# 换个终端
curl http://localhost:8002/config/app
curl http://localhost:8002/student/info
```

## 4. 踩过的坑

### 4.1 双启动类导致打包失败

`02-config` 里有两个带 `@SpringBootApplication` 的类（历史遗留），导致 `spring-boot-maven-plugin` 无法确定用哪个做主类：

```
Unable to find a single main class from the following candidates
[com.itheima.Application, top.a1788.config.ConfigApplication]
```

**解法**：在 pom 里显式指定。

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>top.a1788.config.ConfigApplication</mainClass>
    </configuration>
</plugin>
```

详细分析见 [踩坑记录](/engineering/troubleshooting#_2-双启动类导致打包失败)。

### 4.2 占位符解析失败导致启动崩

```
IllegalArgumentException: Could not resolve placeholder 'mxu.name' in value "${mxu.name}"
```

**原因**：yml 里写了 `${mxu.name}` 引用，但没有定义 `mxu.name` 这个 key。

**排查顺序**：① 拼写 → ② 层级缩进 → ③ 是不是在另一个 profile 文件里定义的但没激活。

**保险做法**：给默认值 `${mxu.name:匿名}`，找不到也不崩。

## 5. 下一步

- 想搞清单个值怎么读 → [@Value 注入详解](/backend/02-config/value)
- 想搞清一组配置怎么绑对象 → [@ConfigurationProperties](/backend/02-config/properties)
- 想搞清多环境 → [多环境与 Profile](/backend/02-config/profile)
- 想知道怎么让配置错误暴露在启动阶段 → [配置校验与最佳实践](/backend/02-config/validation)
