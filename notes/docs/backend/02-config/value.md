# @Value 注入详解

<NoteStatus level="done" />

## 0. 本篇要解决的问题

1. `@Value` 怎么读配置？能读哪些来源？
2. `${}` 是占位符，`#{}` 是 SpEL，两者有什么区别？能嵌套吗？
3. 配置项不存在时怎么给默认值，避免启动直接崩？
4. 随机值有什么用？`random.uuid` 和 `random.int(1,100)` 怎么写？
5. `@Value` 有哪些做不了的事？

## 1. 基本用法

```java
@Value("${配置项的 key}")
private 字段类型 变量名;
```

Spring 在**创建 Bean 时**解析这个注解：拿括号里的 key 去 `Environment` 里查，查到就转成字段类型注入，查不到就抛异常。

```java [ConfigController.java]
@Value("${server.port}")
private Integer serverPort;

@Value("${spring.application.name}")
private String appName;

@Value("${mxu.name}")
private String myName;

@Value("${mxu.job}")
private String myJob;
```

对应的配置：

```yaml [application.yml]
server:
  port: 8002

spring:
  application:
    name: 02-配置管理

mxu:
  name: a1788
  job: 学习者
```

::: tip yml 的层级用 `.` 连接
yml 里是嵌套结构，`@Value` 里写**扁平的 key 路径**：

```
yml：  mxu:  →  name: a1788
写：   ${mxu.name}
```

`server.port` 也一样 —— `server:` 下面缩进的 `port:`，写成 `${server.port}`。
:::

### 1.1 能读哪些来源

`@Value` 读的是 `Environment`，所以**前面讲的整个优先级链它都能读到**：

| 来源 | 例子 |
|---|---|
| Spring Boot 内置配置 | `${server.port}`、`${spring.application.name}` |
| 自定义 yml 配置 | `${mxu.name}` |
| 环境变量 | `${JAVA_HOME}`（`@Value` 能读系统环境变量） |
| 系统属性 | `${user.home}` |
| 命令行参数 | 用 `--key=value` 传进来的值 |
| Spring 内置随机值 | `${random.uuid}` |

### 1.2 类型转换

`@Value` 会按字段类型自动转换字符串：

```java
@Value("${app.port}")
private Integer port;        // "8002" → 8002

@Value("${app.enabled:true}")
private Boolean enabled;     // "true" → true

@Value("${app.ratio:0.75}")
private Double ratio;
```

**转换失败会在启动时报错**（fail-fast），这是好事 —— 配置写错了立刻知道，而不是等运行时某次请求才炸。

```
Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'
```

## 2. 案例实测

本模块的 `ConfigController` 覆盖了 `@Value` 的全部用法。以下输出都是实际跑出来的。

### 2.1 基础读取

```java [ConfigController.java]
@GetMapping("/basic")
public String getBasicInfo() {
    return "服务器端口是：" + this.serverPort + "，应用名称是：" + appName;
}
```

```bash
curl http://localhost:8002/config/basic
```

```
服务器端口是：8002，应用名称是：02-配置管理
```

读的是 Spring Boot 自己的配置项。`server.port` 是框架内置的，改它的值会真的换端口。

### 2.2 自定义配置

```java [ConfigController.java]
@GetMapping("/my")
public String getMyInfo() {
    return "我的姓名是：" + this.myName + "，职业是：" + this.myJob;
}
```

```bash
curl http://localhost:8002/config/my
```

```
我的姓名是：a1788，职业是：学习者
```

::: warning 自定义配置不要占 `spring.*` 前缀
`spring.*` 是 Spring Boot 保留命名空间。往里塞自己的 key 会失去 IDE 提示，还可能和未来版本的官方配置冲突。

用独立前缀（本项目用 `mxu`，或用自己的域名反写 `top.a1788.xxx`）。
:::

### 2.3 四种进阶写法

这段是本篇的核心，四种写法在同一个方法里对照：

```java [ConfigController.java]
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
```

```bash
curl http://localhost:8002/config/value
```

真实输出：

```
占位符引用 author=a1788；默认值 remark=暂无备注；随机 UUID=e4753b36-...；随机整数=19；SpEL adult=成年
```

#### ① 占位符：`${app.author}`

配置里这样写：

```yaml
mxu:
  name: a1788           # 定义处（唯一）
app:
  author: ${mxu.name}   # 引用处：运行时替换成 a1788
```

**解析过程**：Spring 遇到 `${app.author}` → 查到这个值是字符串 `"${mxu.name}"` → **发现里面还有占位符，继续解析** → 查到 `mxu.name = a1788` → 最终得到 `a1788`。

这解决了「同一个值在多处引用」的问题：改名只改一处，所有引用自动跟着变。

| 用途 | 做法 |
|---|---|
| 一处定义多处引用 | 上例 |
| 不同环境用不同值 | `application-dev.yml` 写 `mxu.name: 开发组`，主配置的 `app.author: ${mxu.name}` 一个字不用改 |
| 部署时外部注入 | `java -jar app.jar --mxu.name=张三` 覆盖 |

#### ② 默认值：`${app.remark:暂无备注}`

**冒号后面是兜底值**，找不到 key 就用它。

```yaml
# 注意：application.yml 里故意没有 app.remark 这个 key
```

所以输出 `remark=暂无备注`。

::: danger 不写默认值又找不到 key，应用直接启动失败
```
IllegalArgumentException: Could not resolve placeholder 'app.remark' in value "${app.remark}"
```

这是新手最常见的启动报错。**排查三步**：
1. key 拼写对不对（`mxu.name` 别写成 `mxu.username`）
2. 缩进层级对不对（yml 靠缩进定层级，缩进错了 key 就变了）
3. 该 key 是不是定义在某个 profile 文件里，但那个 profile 没激活
:::

**什么时候该写默认值**：

| 场景 | 写不写默认值 |
|---|---|
| 配置项在**所有**环境的 yml 里都有 | 不用写（写了反而掩盖配置缺失） |
| 配置项可能缺失，且缺失时有合理行为 | 写 |
| 敏感配置（密钥、密码） | **绝对不写默认值** —— 宁可启动失败，也不要静默用一个错误的默认值跑起来 |

#### ③ 随机值：`${random.uuid}` / `${random.int(1,100)}`

Spring Boot 内置的 `RandomValuePropertySource` 提供的，不需要在 yml 里定义。

| 写法 | 结果 | 用途 |
|---|---|---|
| `${random.value}` | 32 位随机十六进制串 | 生成随机密钥 |
| `${random.uuid}` | 标准 UUID | 实例标识、traceId |
| `${random.int}` | 随机 int（含负数） | 测试数据 |
| `${random.int(10)}` | `0 ~ 9` 的随机数 | 上限不含 |
| `${random.int(1,100)}` | `1 ~ 99` 的随机数 | 区间不含上限 |
| `${random.long}` | 随机 long | 测试数据 |
| `${random.long(100,200)}` | 区间随机 long | 测试数据 |

::: warning 随机值只在 Bean 创建时算一次
`e4753b36-...` 这个 UUID 在**应用启动时**生成一次，之后就固定了。同一个 Bean 里反复读这个字段，值不变。

**想让每次请求都变**，得在方法里调 `UUID.randomUUID()`，`@Value("${random.uuid}")` 做不到。
:::

#### ④ SpEL 表达式：`#{}`

```java
@Value("#{${student.age} >= 18 ? '成年' : '未成年'}")
private String adult;
```

**这是占位符和 SpEL 的嵌套**，看着吓人，拆开很简单：

```
#{                              ← 外层：SpEL 表达式，能算数、能调方法
  ${student.age}                ← 内层：占位符，先被替换成 18
  >= 18 ? '成年' : '未成年'      ← 三元表达式
}
```

**解析顺序**：

| 步骤 | 结果 |
|---|---|
| 1. 解析内层占位符 `${student.age}` | `#{18 >= 18 ? '成年' : '未成年'}` |
| 2. 计算 SpEL 表达式 | `成年` |

因为 `student.age` 配的是 `18`，`18 >= 18` 为真，所以输出 `成年`。

**`${}` 和 `#{}` 的区别**（这个必须分清）：

| 语法 | 名称 | 在哪生效 | 能做什么 |
|---|---|---|---|
| `${}` | 占位符 | **配置文件里也能用**，注解里也能用 | 取值替换 |
| `#{}` | SpEL 表达式 | **只在注解里生效**，写在 yml 里不解析 | 算数、调方法、三元、集合操作 |

```java
// SpEL 能做的事：算数
@Value("#{2 * 3}")                  // 6
// 调静态方法
@Value("#{T(java.lang.Math).PI}")   // 3.141592653589793
// 读其他 Bean 的属性
@Value("#{appProperties.maxCount}") // 调用 appProperties 的 getMaxCount()
```

::: tip `#{appProperties.maxCount}` 是 `@Value` 和 `@ConfigurationProperties` 的桥
它不查配置，而是**直接读 Spring 容器里那个 Bean 的属性**。意味着「一组配置用 `@ConfigurationProperties` 绑成对象，个别地方用 `@Value` 读它的字段」这种混合用法是可行的。
:::

## 3. `@Value` 做不了什么

这部分比「能做什么」更重要，因为它决定了什么时候必须换 `@ConfigurationProperties`。

| 做不到的事 | 表现 |
|---|---|
| **绑 List** | `@Value("${student.hobbies}")` 只能得到 `"篮球,编程,阅读"` 一个字符串，不是 `List<String>` |
| **绑 Map** | 同样只能得到原始字符串，无法变成 `Map` |
| **绑嵌套对象** | 完全不行 |
| **松散绑定** | yml 写 `max-count`，Java 字段叫 `maxCount`，`@Value("${app.maxCount}")` **取不到值**（`@Value` 要求 key 完全一致） |
| **配置校验** | 加不了 `@NotBlank`/`@Min`，只能自己写 `if` 判断 |
| **IDE 提示** | 写 yml 时没有补全提示 |

**一个具体的坑**：

```java
// 想读 app.max-count，但写成了驼峰
@Value("${app.maxCount}")     // ❌ 报 Could not resolve placeholder
private Integer maxCount;

// 必须和 yml 里的 key 完全一致
@Value("${app.max-count}")    // ✅
private Integer maxCount;
```

这就是为什么 `AppProperties` 那个类用 `@ConfigurationProperties` 而不是 `@Value` —— 它支持松散绑定，`max-count` 能自动绑到 `maxCount`。

## 4. 小结与自检

### 核心结论

1. `@Value("${key}")` 从 `Environment` 取值，支持自动类型转换，转换失败启动即报错
2. 占位符 `${}` 可以嵌套引用其他 key，用于「一处定义多处引用」和「环境覆盖」
3. `${key:default}` 给默认值；**敏感配置不要给默认值**
4. `${random.*}` 是内置随机值源，但只在 Bean 创建时算一次
5. `#{}` 是 SpEL，只在注解里生效；`${}` 可以嵌在 `#{}` 里用
6. `@Value` 不适合 List / Map / 嵌套对象 / 校验场景 → 换 `@ConfigurationProperties`

### 自检清单

- [ ] `${}` 和 `#{}` 分别是什么？各自在哪生效？
- [ ] `appliction-dev.yml` 里的 key 和 `application.yml` 里的同名 key 冲突，谁赢？
- [ ] 配置里写 `${app.remark}` 但没定义这个 key，会发生什么？
- [ ] `${random.uuid}` 在同一次运行中反复读同一个字段，值会变吗？
- [ ] 为什么 `@Value("${app.maxCount}")` 读不到 yml 里的 `max-count`？
- [ ] 哪些配置项**不应该**写默认值？

### 下一步

[@ConfigurationProperties](/backend/02-config/properties) —— 解决 `@Value` 处理不了的结构化绑定问题。
