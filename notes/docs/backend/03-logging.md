# 03 日志管理

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\03-logging`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8003`
:::

> **这一篇目前是「施工图」，不是成品笔记。** 下面的知识点清单和待办清单已经定好，动手写代码时按这个结构填内容。写完把状态改成 <NoteStatus level="done" />，并补上「运行验证」一节（真实输出）。

## 0. 这个模块要解决什么问题

**规划中的四条**

1. 为什么不能用 `System.out.println` 打日志？它到底差在哪？
2. SLF4J 和 Logback 是什么关系？为什么代码里 import 的是 SLF4J？
3. 日志级别怎么选？什么时候用 `info`、什么时候 `debug`、什么时候 `error`？
4. 生产环境的日志怎么落到文件、按天滚动、自动清理？

## 1. 模块定位

在整条学习链里，03 是**从「能跑」到「能查」的分水岭**。

| 上一个模块（02） | 本模块（03） | 下一个模块（04） |
|---|---|---|
| 让应用按预期配置启动 | 让应用运行时**可见** | 让应用对外提供接口 |
| 配错了启动就失败 | 出问题能定位到行 | 请求进来能追踪 |

**核心价值**：接口报错时，你能否在不加断点、不重启的情况下，仅靠日志定位到问题？

## 2. 计划覆盖的知识点

### 2.1 日志门面与实现的分层

这是理解 Java 日志体系的关键。**代码只依赖门面，实现可替换**。

```
业务代码
   ↓ 只 import 这一个
org.slf4j.Logger                    ← 日志门面（接口，SLF4J）
   ↓ 运行时绑定
ch.qos.logback.classic.Logger       ← 日志实现（Logback）
   ↓ 输出到
控制台 / 文件
```

| 概念 | 说明 |
|---|---|
| **门面（Facade）** | 只定义 API，不实现。SLF4J、Commons Logging |
| **实现（Binding）** | 真正干活的。Logback、Log4j2、JUL |
| **桥接（Bridge）** | 让用旧 API 的第三方库也走统一出口。`jul-to-slf4j`、`log4j-to-slf4j` |

**为什么要有门面**：你的代码如果用 Logback 的 API，将来想换 Log4j2 就得改所有 import。用 SLF4J 就没这个问题 —— 换实现只要换依赖，代码不动。

### 2.2 `{}` 占位符 vs 字符串拼接

| 写法 | 评价 |
|---|---|
| `log.info("用户: " + name + ", 年龄: " + age);` | ❌ 无论日志级别是否输出，**都会执行字符串拼接**，浪费性能 |
| `log.info("用户: {}, 年龄: {}", name, age);` | ✅ 先判断级别，级别不够**直接返回**，不拼接 |

**第二种写法的优势在 debug 日志里最明显**：生产环境日志级别是 `info`，那些 `log.debug(...)` 调用几乎零开销。如果用字符串拼接，即使不输出也要执行拼接。

### 2.3 日志级别

从低到高：

| 级别 | 用途 | 生产环境 |
|---|---|---|
| `TRACE` | 最细粒度，一般只在追特定问题时临时开 | 关 |
| `DEBUG` | 调试信息：参数值、分支走向、配置绑定过程 | 关（自己的包可开） |
| `INFO` | 关键业务节点：请求进入、业务完成、定时任务执行 | 开 |
| `WARN` | 可恢复的异常、降级、重试 | 开 |
| `ERROR` | 需要人工介入的错误 | 开 |

**级别是「开关」不是「标签」**：配成 `INFO` 时，`DEBUG` 和 `TRACE` 的日志**完全不输出**（连参数求值都不做）。

::: tip 生产环境的日志级别怎么定
```yaml
logging:
  level:
    root: warn                    # 全局压到 warn，过滤第三方噪音
    top.a1788: info               # 自己的业务包放开到 info
```

`root: warn` 是为了压掉 Spring、连接池、Hibernate 那些每次启动刷一屏的 info 日志。
:::

### 2.4 Logback 配置

Spring Boot 默认用 Logback，默认配置已经够用。需要定制时（比如写文件、彩色控制台）加 `logback-spring.xml`：

| 配置点 | 说明 |
|---|---|
| 命名必须叫 `logback-spring.xml` | 叫 `logback.xml` 也能生效，但**不能使用 Spring 的 `<springProfile>` 标签**（因为它在 Spring 初始化之前就被加载了） |
| 控制台彩色输出 | `<conversionRule>` 或 `spring.output.ansi.enabled` |
| 文件输出与滚动 | `RollingFileAppender` + `TimeBasedRollingPolicy` |
| 按环境区分 | `<springProfile name="dev">` / `<springProfile name="prod">` |
| 保留天数 | `<maxHistory>7</maxHistory>` |
| 单文件大小上限 | `<maxFileSize>100MB</maxFileSize>` |

### 2.5 与 02 模块的衔接

日志级别**本来就是配置项**，所以 03 会直接复用 02 学到的多环境机制：

```yaml
# application-dev.yml
logging:
  level:
    top.a1788: debug      # 开发期看细节

# application-prod.yml
logging:
  level:
    root: warn
    top.a1788: info       # 生产只留必要信息
```

**这就是 02 和 03 的衔接点**：环境差异配置的典型应用场景。

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `03-logging/src/main/java/.../LoggingApplication.java` |
| 代码 | 一个 `LogController`，演示五个级别各自的输出 |
| 代码 | 一个 `@Slf4j` 的 Service，演示 `{}` 占位符正确用法 |
| 配置 | `logback-spring.xml`：控制台彩色 + 文件滚动 + 保留 7 天 |
| 配置 | `application-dev.yml` / `application-prod.yml` 的 `logging.level` 差异 |
| 笔记 | **「运行验证」一节必须贴真实的日志输出**（控制台 + 落盘文件内容） |

## 4. 待办清单

- [ ] 建模块，挂到父 pom 的 `<modules>`
- [ ] 删掉 `03-logging/.gitkeep`
- [ ] `application.yml` 配 `server.port: 8003`、`spring.application.name`
- [ ] 用 `@Slf4j` 写五个级别的日志输出，接口触发打印
- [ ] 对比 `log.info("a" + b)` 和 `log.info("a={}", b)`，用实测证明后者在低级别下不拼接
- [ ] 配 `logback-spring.xml`：控制台彩色 + 按天滚动 + 保留 7 天
- [ ] 起服务，**贴出真实日志输出**（控制台片段 + 落盘的 `.log` 文件内容）
- [ ] 写「踩坑」：定位一个真实遇到的日志问题（比如中文乱码、日志重复输出、级别配置不生效）
- [ ] 更新 [内容规划表](/guide/outline) 和 [学习路线进度快照](/guide/roadmap#_5-当前进度快照)

## 5. 关键决策（写笔记时要回答）

| 问题 | 需要给出的结论 |
|---|---|
| 一共配几个 appender？ | 控制台 + 文件，还是只有控制台？ |
| 日志文件放哪？ | 项目内 `logs/`（要进 `.gitignore`）还是系统目录？ |
| 错误日志单独分文件吗？ | 有些方案会把 `ERROR` 单独落一个文件，方便告警采集 |
| `logback-spring.xml` 还是纯 `application.yml` 配置？ | 简单需求（只调级别、只改格式）用 yml 就够；需要滚动策略和自定义 appender 才上 XML |

## 6. 预习要点

动手前可以先想清楚这几个问题：

1. **日志是给谁看的？** 给自己排错、给运维告警、给审计合规 —— 目的不同，内容和级别都不同
2. **一行好日志包含什么？** 时间、级别、线程、类名、业务标识（请求 ID / 用户 ID）、消息、上下文
3. **什么时候不该打日志？** 循环里逐条打 info、打印完整敏感信息（密码、身份证、token）、把异常吞掉只打一行 `e.getMessage()`（丢掉堆栈）

::: warning 一个必须避免的写法
```java
try {
    // 业务代码
} catch (Exception e) {
    log.error("出错了");          // ❌ 堆栈丢了，排查时无从下手
}
```

```java
try {
    // 业务代码
} catch (Exception e) {
    log.error("处理订单失败, orderId={}", orderId, e);   // ✅ 带上上下文 + 堆栈
}
```

**注意最后那个 `e` 的位置** —— SLF4J 会把最后一个 `Throwable` 参数当成异常，打印完整堆栈，且它**不占 `{}` 的位置**。
:::

## 下一步

[04 Web 开发](/backend/04-web) —— 有了配置（02）和日志（03）两块地基，可以正式做接口了。
