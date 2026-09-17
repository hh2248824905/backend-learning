# 11 接口文档

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\11-doc`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8011`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

::: tip 这个模块已有实践基础
`02-config` 模块的接口已经手动同步到了 Apifox（通过 IDEA 的 Apifox Helper 插件）。

11 模块要做的是**把这件事自动化** —— 让文档从代码生成，而不是手动维护。
:::

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 手写 Markdown 接口文档，代码改了文档忘了改，怎么破？
2. OpenAPI 3 是什么？和 Swagger 什么关系？
3. 用注解标注接口，怎么做到「代码即文档」？
4. 生成的文档怎么导入 Apifox / Postman 给前端用？
5. 生产环境为什么要把接口文档关掉？

## 1. 模块定位

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 04 Web：接口规范定型 | **11：把接口规范变成可读的文档** | 前后端联调、对外交付 |

**核心价值**：把「写文档」这件事从「额外工作」变成「写代码的副产品」。

## 2. 计划覆盖的知识点

### 2.1 Swagger 家族的版本关系（容易搞混）

| 名称 | 是什么 | 现状 |
|---|---|---|
| **Swagger 2.0** | 早期的接口描述规范 + 工具集 | 规范已过时（2017 年被 OpenAPI 3 取代） |
| **OpenAPI 3** | 现在的**规范标准**（Linux 基金会维护） | 当前标准，版本 3.x |
| **springfox** | 老一代 Spring 集成库 | ❌ **停止维护**，不兼容 Spring Boot 3 |
| **springdoc-openapi** | 新一代集成库 | ✅ 推荐，支持 Spring Boot 3 |
| **Knife4j** | 在 springdoc 基础上做了**增强 UI** | ✅ 国内常用，界面比原生好看 |
| **Apifox / Postman** | API 协作平台，可导入 OpenAPI 文件 | 团队协作、Mock、自动化测试 |

::: danger Spring Boot 3 不要用 springfox
`springfox-boot-starter` 最后一次更新是 2020 年，**不支持 Spring Boot 3**（因为 Spring Boot 3 把 `javax.*` 换成了 `jakarta.*`，路径匹配也换成了 `PathPatternParser`）。

强行用会报 `NullPointerException` 或接口扫不出来。

**Spring Boot 3 的选择：`springdoc-openapi` 或 `knife4j-openapi3-jakarta-spring-boot-starter`。**
:::

### 2.2 springdoc-openapi 集成

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x.x</version>
</dependency>
```

**零配置**：加完依赖重启，就能访问：

| 地址 | 内容 |
|---|---|
| `/swagger-ui.html` | 可视化接口调试页面 |
| `/v3/api-docs` | OpenAPI 3 的 JSON 描述文件（**这个就是给 Apifox 导入的**） |

### 2.3 全局信息配置

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("前后端工程化课程 API")
                        .version("1.0.0")
                        .description("Spring Boot 学习项目接口文档")
                        .contact(new Contact().name("a1788")))
                .components(new Components()
                        .addSecuritySchemes("bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

或者用 `application.yml` 配：

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha          # 标签按字母排序
    operations-sorter: method   # 接口按 HTTP 方法排序
  packages-to-scan: top.a1788.doc.controller    # 只扫指定包
```

::: tip `packages-to-scan` 一定要配
不配的话，springdoc 会扫**所有** Controller，包括：
- Spring Boot 自带的 `/error` 端点
- 第三方 starter 引入的端点（比如 Actuator 的）

结果文档里混进一堆和你无关的接口。

**只扫自己的包**，文档才干净。
:::

### 2.4 常用注解

```java
@Tag(name = "用户管理", description = "用户的增删改查")
@RestController
@RequestMapping("/users")
public class UserController {

    @Operation(summary = "查询用户", description = "按 ID 查询用户详情，不存在返回 404")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{id}")
    public Result<UserVO> get(
            @Parameter(description = "用户 ID", example = "1")
            @PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid UserDTO dto) { }
}
```

| 注解 | 加在哪 | 作用 |
|---|---|---|
| `@Tag` | 类 | 给一组接口分组（对应 UI 上的折叠面板） |
| `@Operation` | 方法 | 接口名称和说明 |
| `@Parameter` | 参数 | 参数说明、是否必填、示例值 |
| `@ApiResponse` / `@ApiResponses` | 方法 | 声明可能的响应码 |
| `@Schema` | 类 / 字段 | **DTO 字段的说明**（最有用，能让前端看懂每个字段） |
| `@Hidden` | 类 / 方法 | 隐藏不导出（内部接口、废弃接口） |

### 2.5 `@Schema` 是性价比最高的注解

```java
@Data
@Schema(description = "用户新增请求")
public class UserDTO {

    @Schema(description = "用户名", example = "zhangsan", requiredMode = REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
    private Integer age;

    @Schema(description = "角色", example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;
}
```

**加了 `@Schema` 之后，前端在文档里能直接看到**：字段含义、示例值、取值范围、是否必填。

**不加的话**，前端只能看到 `username`、`age` 这种字段名，得反复问你。

::: tip 有了 `@Schema`，字段上的 Javadoc 注释就没那么必要了
`@Schema(description = "...")` 已经承担了说明职责，而且**会出现在文档里**。Javadoc 只对读源码的人可见。

两者写一样的内容会重复。**约定：对外 DTO 用 `@Schema`，内部类用 Javadoc。**
:::

### 2.6 分组与隐藏

```java
// 按分组导出不同文档
@Bean
public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
            .group("public")                       // 访问 /v3/api-docs/public
            .pathsToMatch("/api/public/**")
            .build();
}

@Bean
public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/admin/**")
            .build();
}
```

**为什么要分组**：接口多了以后（100+），一份文档里找起来很痛苦。按「对外开放 / 内部管理」拆开，各给各的。

### 2.7 生产环境必须关闭文档

::: danger 生产环境暴露 Swagger UI 是很严重的问题
**风险**：

| 风险 | 说明 |
|---|---|
| **接口结构全泄露** | 攻击者拿到所有接口路径、参数、返回结构，攻击面一览无余 |
| **敏感接口被发现** | 管理接口、内部接口、调试接口暴露在文档里 |
| **能直接调接口** | Swagger UI 是个调试器，可以发真实请求 |
| **`/v3/api-docs` 泄露 DTO 结构** | 数据库字段名、枚举值都能推出来 |

**正确做法**：用 profile 控制。

```yaml
# application-dev.yml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

# 或者干脆不引入依赖（用 Maven profile 控制）
```

**注意**：如果用了 swagger-ui 的原生资源路径，还要确保 Spring Security 放行了 `/swagger-ui/**`、`/v3/api-docs/**`（开发环境）—— 这又是 [09 安全](/backend/09-security) 的知识。
:::

### 2.8 导出与导入 Apifox

**流程**：

```
后端代码（带注解）
    ↓ springdoc 生成
/v3/api-docs（OpenAPI 3 JSON）
    ↓ 下载或 URL 导入
Apifox / Postman
    ↓
前端联调、Mock、自动化测试
```

**Apifox 里怎么导**：

| 方式 | 操作 |
|---|---|
| **URL 导入（推荐）** | Apifox → 项目设置 → 导入数据 → OpenAPI/Swagger → 填 `http://localhost:8011/v3/api-docs` |
| **文件导入** | 浏览器访问 `/v3/api-docs`，另存为 JSON，再上传到 Apifox |

**URL 导入的好处**：后端改了接口重启后，Apifox 里点一下「同步」就能拉最新的，不用来回传文件。

::: warning 导入是单向的，Apifox 里手改会被覆盖
下次同步时，Apifox 里手动改过的字段会被代码生成的内容盖掉（按「路径 + 方法」匹配接口）。

**约定**：**接口结构以代码为准**。Apifox 里只放测试用例、环境变量、Mock 数据这些「生成的文档里没有的东西」。
:::

### 2.9 与 04 模块的衔接

11 模块的文档质量**完全取决于 04 模块的接口规范**：

| 04 里做的事 | 11 里的效果 |
|---|---|
| 统一返回体 `Result<T>` | 文档里所有接口的响应结构一致，前端好处理 |
| DTO / VO 分离 | 文档里「请求体」和「响应体」结构清晰 |
| 参数校验注解 | springdoc 能自动把校验规则（必填、范围）渲染进文档 |
| 全局异常处理 | 文档里能声明统一的错误响应结构 |

**如果 04 的接口写得乱，11 生成的文档也一定乱** —— 文档只是把代码结构可视化，不创造信息。

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | 引 `springdoc-openapi-starter-webmvc-ui` 依赖 |
| 代码 | `OpenApiConfig`：全局信息 + JWT 认证方案声明 |
| 代码 | 给 `UserController` 全套加注解（`@Tag`/`@Operation`/`@Parameter`/`@Schema`） |
| 代码 | 一个 `@Hidden` 的内部接口，验证不会出现在文档里 |
| 配置 | `springdoc.packages-to-scan` 精确限定扫描包 |
| 配置 | dev 开文档、prod 关文档 |
| 笔记 | **实测截图项还原**：`/v3/api-docs` 返回的 JSON 结构解读 |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8011 端口
- [ ] 引 springdoc 依赖，访问 `/swagger-ui.html` 确认能打开
- [ ] 配 `packages-to-scan`，确认文档里没有 Spring 自带的 `/error` 接口
- [ ] 写 `OpenApiConfig`，加全局标题/版本/联系方式
- [ ] 给 DTO 字段加 `@Schema`（含 example、requiredMode）
- [ ] 给接口加 `@Operation` / `@ApiResponses`
- [ ] 加一个 `@Hidden` 的接口，验证它不出现在文档里
- [ ] 在 Apifox 里用 **URL 方式**导入 `/v3/api-docs`，看接口是否完整
- [ ] 起 prod profile，验证文档接口返回 404
- [ ] 贴真实输出：`/v3/api-docs` 的 JSON 片段、关闭文档后的响应
- [ ] 写踩坑：springfox 不兼容 Boot 3、文档里混入无关接口、生产忘了关
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| springdoc 还是 Knife4j？ | springdoc（原生，够用）/ Knife4j（UI 更友好，国内团队习惯） |
| 注解写多细？ | DTO 必写 `@Schema`；接口写 `@Operation`；`@ApiResponse` 只在有非 200 响应时写 |
| 分几组？ | 按「对外开放 / 内部」两组，或按业务域分组 |
| 文档地址 | 默认 `/swagger-ui.html`，或改到不易猜的路径（**安全性靠关闭，不靠隐藏路径**） |
| Apifox 怎么同步 | URL 导入（推荐）/ 文件导入 |
| 生产怎么关 | profile 配置 / Maven profile 不引依赖 |

## 6. 预习要点

1. **OpenAPI 是规范，Swagger UI 是渲染器**：前者是 JSON 描述文件，后者把它渲染成网页
2. **`/v3/api-docs` 才是核心产物**：Swagger UI 只是它的一个消费者，Apifox 也是
3. **注解不写也有文档**：springdoc 能从方法签名和类型信息推断出大部分结构，注解只是补充说明
4. **Spring Boot 3 + `jakarta.*`**：所有老一代 Swagger 相关库的坑都源于这个包名变更

## 下一步

[12 单元测试](/backend/12-test) —— 文档说接口是什么样，测试证明接口真的是这样。
