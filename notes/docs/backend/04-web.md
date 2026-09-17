# 04 Web 开发

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\04-web`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8004`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容，并补上真实的运行验证。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 一个 HTTP 请求从进来到返回，Spring MVC 中间做了哪些事？
2. 路径参数、查询参数、请求体，三种取参方式怎么选？
3. 参数校验失败怎么返回统一的错误结构，而不是 Spring 默认的那坨 JSON？
4. 每个 Controller 都写 `try-catch` 太蠢，怎么统一处理异常？
5. 前后端约定返回格式时，怎么设计一个能装下成功/失败/分页的返回体？

## 1. 模块定位

04 是整个后端课程里**代码量最大、最贴近实际开发**的模块。

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 02 配置：能读配置 | **04 Web：能对外提供接口** | 05 数据库、06 ORM、09 安全、10 文件、11 文档 |
| 03 日志：能看清运行状态 | | 所有后续模块的接口都建立在这里的规范上 |

**核心价值**：做完这个模块，你的接口应该**不需要每个方法都写 try-catch**，错误信息是结构化的，前端能直接根据 `code` 判断处理方式。

## 2. 计划覆盖的知识点

### 2.1 请求映射注解家族

| 注解 | 等价于 | 说明 |
|---|---|---|
| `@RequestMapping` | —— | 通用映射，可指定 `method` |
| `@GetMapping` | `@RequestMapping(method = GET)` | 查询 |
| `@PostMapping` | `@RequestMapping(method = POST)` | 新增 |
| `@PutMapping` | `@RequestMapping(method = PUT)` | 全量更新 |
| `@DeleteMapping` | `@RequestMapping(method = DELETE)` | 删除 |
| `@PatchMapping` | `@RequestMapping(method = PATCH)` | 局部更新 |

**类级 + 方法级路径拼接**：

```java
@RestController
@RequestMapping("/users")            // 类级：统一前缀
public class UserController {

    @GetMapping("/{id}")              // 拼接后：GET /users/{id}
    public User get(@PathVariable Long id) { }
}
```

### 2.2 三种取参方式

| 方式 | 注解 | 请求形态 | 适用 |
|---|---|---|---|
| 路径参数 | `@PathVariable` | `GET /users/1` | RESTful 风格，标识资源 |
| 查询参数 | `@RequestParam` | `GET /users?page=1&size=10` | 分页、筛选、可选参数 |
| 请求体 | `@RequestBody` | `POST` + JSON body | 提交复杂对象 |

**选型判断**：

```
这个参数是用来「定位资源」的吗？ → @PathVariable
参数个数少且语义简单吗？        → @RequestParam
参数是个对象（多个字段）吗？     → @RequestBody
```

**几个易错点**：

| 场景 | 问题 | 说明 |
|---|---|---|
| `@RequestParam` 必填 | 不传报 400 | `required = false` 或给 `defaultValue` 设为可选 |
| 参数名对不上 | 拿不到值 | `@RequestParam("pageNum") Integer page` 显式指定名称 |
| `@PathVariable` 类型 | 传了非数字 | 自动转 `Long` 失败报 400，`@ExceptionHandler` 里能接住 |

### 2.3 参数校验

```java
@PostMapping
public Result<Void> create(@RequestBody @Valid UserDTO dto) { }
```

| 要点 | 说明 |
|---|---|
| `@Valid` vs `@Validated` | `@Valid` 是标准注解；`@Validated` 是 Spring 的，支持**分组校验** |
| 嵌套对象校验 | 内层对象字段上加 `@Valid` 才会级联校验 |
| 校验失败处理 | 默认抛 `MethodArgumentNotValidException`，要用全局异常处理器转成统一返回体 |

### 2.4 统一返回体

```java
@Data
public class Result<T> {
    private Integer code;      // 业务状态码
    private String message;
    private T data;

    public static <T> Result<T> success(T data) { }
    public static <T> Result<T> fail(Integer code, String message) { }
}
```

**要回答的设计问题**（写笔记时必须给出结论）：

| 问题 | 需要权衡什么 |
|---|---|
| 用 HTTP 状态码还是业务状态码？ | HTTP 状态码表达「传输层」成败，业务状态码表达「业务」成败。两者可以都用 |
| 分页数据怎么装？ | `data` 里放 `{ list, total, page, size }`，还是单独定义 `PageResult<T>`？ |
| 成功时 `message` 填什么？ | 空着？填 `"success"`？ |
| 泛型 `T` 和 `Object` 怎么选？ | 泛型能获得编译期类型检查，但静态工厂方法里签名会啰嗦 |

### 2.5 全局异常处理

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) { }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) { }

    @ExceptionHandler(Exception.class)     // 兜底
    public Result<Void> handleOther(Exception e) { }
}
```

**要理解的关键点**：

| 点 | 说明 |
|---|---|
| 匹配优先级 | Spring 选**最具体**的匹配 —— 有 `BusinessException` 处理器就不会走到 `Exception` 兜底 |
| 兜底处理器必须打 `error` 日志 | 未预期的异常要留下线索，不能悄悄吞掉 |
| 统一异常后 HTTP 状态码怎么定 | 全返回 200，还是按异常类型返回 4xx/5xx？两种做法都有实践，**选一个并在项目里保持一致** |

### 2.6 其他

| 知识点 | 说明 |
|---|---|
| 静态资源 | `src/main/resources/static/` 下的文件可直接访问 |
| 拦截器 `HandlerInterceptor` | `preHandle` / `postHandle` / `afterCompletion`，用于登录校验、日志埋点 |
| 过滤器 `Filter` | 比拦截器更靠外（在 Servlet 层），能拿到原始请求体 |
| `ResponseEntity` | 需要精确控制状态码和响应头时使用 |
| 跨域 CORS | `@CrossOrigin` 或全局 `WebMvcConfigurer.addCorsMappings` |

### 2.7 与 02 模块的衔接

全局 CORS 配置、静态资源配置都通过 `WebMvcConfigurer` 实现类来完成 —— 那是个 `@Configuration` 类，**本质还是配置**，延续 02 的知识。

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `04-web/`：`WebApplication` + `Result<T>` + `BusinessException` + `GlobalExceptionHandler` |
| 代码 | `UserController`：完整 CRUD，演示三种取参 + 参数校验 |
| 代码 | `UserDTO` / `UserVO`：请求体和返回体分开定义 |
| 代码 | `WebConfig implements WebMvcConfigurer`：CORS + 拦截器注册 |
| 笔记 | 三种取参方式的**真实请求/响应对照** |
| 笔记 | 参数校验失败 + 业务异常 + 系统异常的**三份真实错误响应** |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8004 端口
- [ ] 定义 `Result<T>` 统一返回体（含静态工厂方法）
- [ ] 定义 `BusinessException`（含业务码）
- [ ] 写 `GlobalExceptionHandler`，覆盖：业务异常、参数校验异常、兜底异常
- [ ] `UserController` 三个接口分别演示 `@PathVariable` / `@RequestParam` / `@RequestBody`
- [ ] 给 `@RequestBody` 参数加 `@Valid` + 校验注解，实测校验失败响应
- [ ] 配置 CORS，起一个前端页面实测跨域
- [ ] 写一个 `LoginInterceptor`（先用假 token 判断），演示拦截器生效
- [ ] **贴真实输出**：正常响应、参数校验失败响应、业务异常响应、404、500
- [ ] 写踩坑：比如 `@RequestBody` 和 `@RequestParam` 混用报错、跨域预检请求 `OPTIONS` 被拦
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| HTTP 状态码 | 全部返回 200，靠业务码区分 / 按语义返回 400、404、500 |
| 统一返回体的 code | 自定义业务码枚举 / 复用 HTTP 状态码数值 |
| 错误响应里要不要带 traceId | 要（便于日志追踪）/ 不要（简单） |
| DTO 和 VO 要不要分开 | 分（清晰，代码多）/ 不分（省事，字段泄露风险） |
| `@Valid` 还是 `@Validated` | 普通校验用 `@Valid`；需要分组校验用 `@Validated` |

::: tip 这些决策没有标准答案，但必须「选一个并写下来」
项目里最忌讳的是「一半接口返回 200 + 业务码，一半接口返回 4xx」。前端处理起来要写两套逻辑。

**写笔记时把这个决策明确记下来，后面所有模块都照着执行。**
:::

## 6. 预习要点

1. **HTTP 方法语义**：GET 查询（幂等、无副作用）、POST 新增、PUT 全量更新、PATCH 局部更新、DELETE 删除
2. **RESTful 风格**：URL 用名词复数表示资源（`/users` 而不是 `/getUserList`），动作用 HTTP 方法表达
3. **HTTP 状态码速记**：2xx 成功、3xx 重定向、4xx 客户端错、5xx 服务端错。最常用的：`200`、`400`、`401`、`403`、`404`、`500`
4. **`Content-Type`**：`application/json` 才会触发 `@RequestBody` 解析；表单提交是 `application/x-www-form-urlencoded`

## 下一步

[05 MySQL](/backend/05-mysql) —— 接口有了，接下来数据得存下来。
