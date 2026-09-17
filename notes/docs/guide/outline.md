# 笔记内容规划

::: tip 这一页的作用
写笔记最容易犯的错是「想到哪写到哪」。这一页**先于写作存在**：每个模块写什么、写到什么深度、用什么代码验证，全部提前定好。

写作时只做一件事 —— 把表里的「计划内容」变成正文。写完把状态从 <NoteStatus level="todo" /> 改成 <NoteStatus level="done" />。
:::

## 1. 写作标准（所有笔记统一遵守）

| 维度 | 要求 | 反例 |
|---|---|---|
| **边界** | 开篇用 3~5 条列出本篇要解决的问题 | 「本篇介绍配置管理」这种没有信息量的开场 |
| **可复现** | 每个结论配一段可运行的代码或命令 | 「配置会被自动注入」——哪个配置？注入到哪？ |
| **真实输出** | 运行结果必须是实际跑出来的，包含报错原文 | 「结果应该是 xxx」 |
| **有对比** | 每个知识点说清「什么时候用它、什么时候不用」 | 只列 API 清单，不说适用场景 |
| **有坑** | 至少记录一个自己踩过的坑（含报错原文 + 原因 + 解法） | 复述官方文档的注意事项 |
| **可跳转** | 笔记里出现的代码路径，在仓库里真实存在 | 指向不存在的文件 |

**硬门槛**：没有「运行验证」小节的笔记，状态只能是 <NoteStatus level="wip" />，不能标 <NoteStatus level="done" />。

---

## 2. 后端笔记规划（01~13）

### 基础篇

#### 01 快速入门 <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | Spring Boot 项目由什么构成？为什么不用写 web.xml 就能跑起来？ |
| **核心知识点** | 起步依赖 `spring-boot-starter-web`、自动配置原理、`@SpringBootApplication` 的三个注解合成、内嵌 Tomcat、`application.yml` 基础写法 |
| **对应代码** | `houduan/01-quickstart/`：`QuickStartApplication`、`controller/UserController`、`entity/User`、`application.yml` |
| **验收标准** | 能说清 `@SpringBootApplication` 拆开是哪三个注解、各自干什么；能独立新建一个返回 JSON 的接口 |
| **依赖** | 无（起点） |

#### 02 配置管理 <NoteStatus level="done" />（拆 4 篇）

内容量最大，单独拆成子目录。

| 子篇 | 解决什么问题 | 核心知识点 | 验收标准 |
|---|---|---|---|
| **模块总览** | 配置从哪来？一份配置怎么支撑多环境？ | 配置文件优先级链、配置文件加载顺序、`@Value` vs `@ConfigurationProperties` 选型 | 能画出配置来源优先级顺序，并说明每层怎么覆盖下一层 |
| **@Value 注入详解** | 单个值怎么读？占位符、默认值、随机值怎么写？ | `${}` 占位符、`${key:default}` 默认值、`${random.uuid}` / `${random.int(1,100)}`、`#{}` SpEL 表达式、`@Value` 不能注复杂结构 | 能手写一个含占位符 + 默认值 + SpEL 三元表达式的 `@Value` 并解释解析顺序 |
| **@ConfigurationProperties** | 一组同前缀配置怎么一次性绑定到对象？ | 前缀绑定、松散绑定（`max-count` ↔ `maxCount`）、List / Map / 嵌套对象 / 对象列表四种形态、`@Component` 注册 Bean vs `@EnableConfigurationProperties`、构造器绑定 | 能独立写一个含四种数据形态的配置类并跑通测试 |
| **多环境与 Profile** | dev / test / prod 配置怎么分离和切换？ | `spring.profiles.active`、`application-{profile}.yml` 覆盖规则、`@Profile` 按环境注册 Bean、命令行覆盖优先级 | 能说出「主配置写什么、环境配置写什么」的划分原则 |
| **配置校验与最佳实践** | 配置写错了怎么在启动时就发现？ | `@Validated` + JSR-380 注解（`@NotBlank` / `@Min` / `@Max`）、fail-fast、配置类命名与分层约定 | 能故意配错一个值并让应用启动失败，看到断言消息 |

| 项 | 内容 |
|---|---|
| **对应代码** | `houduan/02-config/`：`properties/{StudentProperties, AppProperties}`、`controller/{ConfigController, StudentController}`、`service/{EnvService, DevEnvService, ProdEnvService}`、`application{,-dev,-prod}.yml`、`test/.../StudentPropertiesTest` |
| **依赖** | 01 |

### 数据篇

#### 03 日志管理 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 为什么不能用 `System.out.println`？生产环境日志怎么分级、怎么归档？ |
| **核心知识点** | SLF4J 门面 + Logback 实现的分层设计、日志级别（TRACE→ERROR）、`{}` 占位符写法（相比字符串拼接的优势）、`logging.level.*` 分环境配置、日志格式与滚动策略、`@Slf4j` |
| **对应代码** | `houduan/03-logging/`（待建）：`logback-spring.xml` + 分级演示 Controller |
| **验收标准** | 能配置出「控制台彩色 + 文件按天滚动 + 保留 7 天」的日志方案；能解释为什么用 `log.info("x={}", x)` 而不是 `"x=" + x` |
| **依赖** | 02（要会写配置文件） |

#### 04 Web 开发 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 一个 HTTP 请求从进来到返回，Spring MVC 中间做了哪些事？ |
| **核心知识点** | `@RestController` / `@RequestMapping` 家族、`@RequestParam` / `@PathVariable` / `@RequestBody` 三种取参方式、参数校验 `@Valid` + 分组、统一返回体 `Result<T>`、全局异常 `@RestControllerAdvice`、静态资源与拦截器 |
| **对应代码** | `houduan/04-web/`（待建） |
| **验收标准** | 能写出「统一返回体 + 全局异常处理 + 参数校验」三件套；能说清 `@RequestParam` 和 `@PathVariable` 的取舍 |
| **依赖** | 02、03 |

#### 05 MySQL <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 数据怎么存进数据库？连接池是什么，为什么要它？ |
| **核心知识点** | 建库建表与字符集（`utf8mb4`）、索引基础、JDBC 六步走、`DataSource` 与连接池（HikariCP）、多数据源配置、SQL 注入与预编译 |
| **对应代码** | `houduan/05-mysql/`（待建）：`application.yml` 数据源配置 + JDBC 演示 |
| **验收标准** | 能解释「为什么必须用 `PreparedStatement` 而不是拼 SQL」；能配好一个能连上的数据源 |
| **依赖** | 02 |

#### 06 MyBatis <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 每次写 JDBC 太啰嗦，ORM 怎么简化又不失控？ |
| **核心知识点** | MyBatis-Plus 与原生 MyBatis 的关系、`BaseMapper` 通用 CRUD、条件构造器 `LambdaQueryWrapper`、分页插件、驼峰与下划线自动映射、`@MapperScan`、XML 与注解两种写法的取舍 |
| **对应代码** | `houduan/06-mybatis/`（待建） |
| **验收标准** | 能不写一行 XML 完成一个带条件查询 + 分页的接口；能说清什么时候必须回到 XML |
| **依赖** | 05 |

#### 07 消息队列 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 什么业务不该同步做？队列怎么做到「不丢、不重、不乱序」？ |
| **核心知识点** | 异步解耦 / 削峰填谷 / 最终一致性的适用边界、RabbitMQ 或 RocketMQ 核心模型（交换机 / 队列 / 路由键 / 消费者组）、消息确认与重试、幂等消费、死信队列 |
| **对应代码** | `houduan/07-mq/`（待建） |
| **验收标准** | 能判断一个业务该不该上队列；能实现一个幂等的消费者 |
| **依赖** | 06（要有数据落地） |

#### 12 单元测试 <NoteStatus level="wip" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 怎么证明代码是对的，而不是「我点了一遍没问题」？ |
| **核心知识点** | `@SpringBootTest` vs `@WebMvcTest` vs 纯单元测试（启动成本差异）、`@Resource` 注入真实 Bean、Mockito 打桩、`MockMvc` 测接口、断言库 AssertJ、测试命名与结构（given/when/then） |
| **对应代码** | `houduan/02-config/src/test/.../StudentPropertiesTest.java`（已有）；`houduan/12-test/`（待建） |
| **验收标准** | 能给一个 Service 写出「正常 + 边界 + 异常」三类测试；能说清什么时候**不该**用 `@SpringBootTest` |
| **依赖** | 02（已有实例）、06 |

### 进阶篇

#### 08 定时任务 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 每天凌晨跑统计，怎么让它自动跑？多实例部署时怎么避免跑两遍？ |
| **核心知识点** | `@EnableScheduling` + `@Scheduled`（cron / fixedRate / fixedDelay 的区别）、cron 表达式速查、异步执行 `@Async` 与线程池、分布式锁避免重复执行、任务失败重试 |
| **对应代码** | `houduan/08-schedule/`（待建） |
| **验收标准** | 能写出正确的 cron 表达式；能说清 `fixedRate` 和 `fixedDelay` 在任务耗时超过间隔时的行为差异 |
| **依赖** | 02、07 |

#### 09 安全认证 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 接口不能随便调，怎么识别「谁在调」以及「他能不能调」？ |
| **核心知识点** | 认证 vs 授权、会话 vs JWT（无状态）、Spring Security 过滤器链、密码加密（BCrypt）、方法级权限 `@PreAuthorize`、CORS 与 CSRF |
| **对应代码** | `houduan/09-security/`（待建） |
| **验收标准** | 能画出 JWT 方案的完整时序（登录发 token → 携带 token → 校验）；能说清为什么 JWT 不适合做「立即失效」 |
| **依赖** | 04、06 |

#### 10 文件处理 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 上传大文件怎么不撑爆内存？下载怎么不卡住线程？ |
| **核心知识点** | `MultipartFile` 上传、文件大小与类型限制、流式处理大文件、本地存储 vs 对象存储（OSS/MinIO）、下载时的文件名编码（中文乱码）、断点续传思路 |
| **对应代码** | `houduan/10-file/`（待建） |
| **验收标准** | 能实现上传 + 下载闭环，中文文件名不乱码；能说清为什么上传要限制大小和类型 |
| **依赖** | 04、06 |

#### 11 接口文档 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 接口改了文档没改，怎么让文档永远和代码一致？ |
| **核心知识点** | OpenAPI 3 规范、springdoc-openapi 或 Knife4j 集成、`@Operation` / `@Schema` 注解、分组与鉴权头配置、导出与云端同步（Apifox 导入 OpenAPI）、生产环境关闭文档 |
| **对应代码** | `houduan/11-doc/`（待建）；已有实践：`02-config` 的接口同步到 Apifox |
| **验收标准** | 能生成一份可导入 Apifox/Postman 的 OpenAPI 文件；能说清生产环境为什么应关闭 `swagger-ui` |
| **依赖** | 04 |

#### 13 监控运维 <NoteStatus level="todo" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 服务上线后是死是活、慢在哪，怎么不用登服务器就知道？ |
| **核心知识点** | `spring-boot-starter-actuator` 端点清单（`health` / `info` / `metrics` / `env`）、端点暴露与安全（只暴露必要端点）、自定义健康指示器、Micrometer 对接 Prometheus、优雅停机 `server.shutdown=graceful` |
| **对应代码** | `houduan/13-actuator/`（待建） |
| **验收标准** | 能配出「只暴露 health + info + metrics」的安全配置；能说清 `/actuator/env` 为什么绝不能对外 |
| **依赖** | 02、04 |

---

## 3. 前端笔记规划

#### 01 HTML 与 CSS <NoteStatus level="wip" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 页面结构和样式怎么写才不互相打架？ |
| **核心知识点** | 语义化标签、盒模型与 `box-sizing`、选择器优先级、Flex 一维布局、Grid 二维布局、定位 `position` 五种值、响应式媒体查询 |
| **验收标准** | 能用 Flex 实现水平垂直居中；能说清 `position: absolute` 相对谁定位 |

#### 02 JavaScript 与 ES6+ <NoteStatus level="wip" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 数组对象怎么高效处理？异步代码怎么写才不嵌套成金字塔？ |
| **核心知识点** | `let`/`const` 与块级作用域、数组七件套（`map`/`filter`/`reduce`/`find`/`some`/`every`/`forEach`）、解构与展开、可选链 `?.` 与空值合并 `??`、模块化、`Promise` 与 `async/await`、`fetch` |
| **验收标准** | 能用 `reduce` 完成分组统计；能说清 `map` 和 `forEach` 的返回值差异 |

#### 03 CDN 字面量入门 <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 不装任何构建工具，怎么最快看到 Vue 的响应式效果？ |
| **核心知识点** | CDN 引入 `vue.global.js`、`createApp().mount()`、选项式 API（`data` / `computed` / `methods`）、七个核心指令（插值 <code v-pre>{{}}</code> / `v-bind` / `v-on` / `v-model` / `v-if` / `v-for` / `:class`） |
| **对应代码** | `qianduan/01-vue-literal/index.html`（单文件，双击即用） |
| **验收标准** | 能说清 `:class` 和 `class` 的区别；能解释为什么 `v-for` 必须加 `:key` |

#### 04 综合案例 TodoList <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 八个语法点怎么在一个真实需求里串起来？ |
| **核心知识点** | 组合式 API `<script setup>`、`ref` 与 `.value`、`computed` 缓存与依赖追踪、`watch` 侦听 + `deep`、`localStorage` 持久化、条件渲染与列表渲染配合、动态 class |
| **对应代码** | `qianduan/vue-app/src/App.vue` |
| **验收标准** | 能说清 `computed` 和 `methods` 的区别（缓存机制）；能解释 `watch` 为什么需要 `deep: true` |

#### 05 Vite 工程化项目 <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 为什么要有构建工具？`.vue` 文件浏览器不认识，怎么就跑了？ |
| **核心知识点** | SFC 单文件组件三段式（`template` / `script` / `style`）、`scoped` 样式作用域原理、Vite 冷启动为什么快（原生 ESM + esbuild 预构建）、`vite.config.js` 常用配置、环境变量 `import.meta.env`、`dev` 与 `build` 的差异 |
| **对应代码** | `qianduan/vue-app/`（`main.js` / `App.vue` / `vite.config.js` / `package.json`） |
| **验收标准** | 能说清 `npm run dev` 和 `npm run build` 产物的区别；能解释 `scoped` 是怎么做到样式隔离的 |

---

## 4. 工程化笔记规划

#### Maven 多模块工程 <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 十三个模块怎么放在一个工程里、共享依赖和构建标准？ |
| **核心知识点** | 聚合（`<modules>`）与继承（`<parent>`）的区别、`<packaging>pom</packaging>`、父工程 `<dependencyManagement>` 管版本、子模块不写 version、`spring-boot-starter-parent` 继承链、多模块构建命令（`-pl` / `-am`） |
| **对应代码** | `houduan/pom.xml` + `01-quickstart/pom.xml` + `02-config/pom.xml` |
| **验收标准** | 能说清 `dependencyManagement` 和 `dependencies` 的区别；能新建一个子模块并正确挂到父工程 |

#### Git 工作流 <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 怎么让每次改动都可追溯、可回滚？ |
| **核心知识点** | 工作区/暂存区/版本库三区模型、提交信息规范（`feat` / `fix` / `refactor` / `chore` / `docs`）、`.gitignore` 与 `git rm --cached`、git 不追踪空目录与 `.gitkeep`、SSH 走 443 端口绕过防火墙、查看与回滚历史 |
| **验收标准** | 能说清 `git rm --cached` 和 `git rm` 的区别；能在提交前正确判断哪些文件不该入库 |

#### IDEA 工程配置 <NoteStatus level="done" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 代码没错但 IDEA 报 `ClassNotFoundException`，问题出在哪？ |
| **核心知识点** | `.idea` 目录各文件职责（`modules.xml` / `misc.xml` / `workspace.xml` / `compiler.xml`）、Maven 工程导入与 `MavenProjectsManager`、运行配置的外部存储、缓存失效与重建、`.idea` 该不该进 Git |
| **验收标准** | 遇到「代码没问题但 IDE 跑不起来」时，知道先查哪几个文件 |

#### 踩坑与排错记录 <NoteStatus level="done" text="6 个真实问题" />

| 项 | 内容 |
|---|---|
| **解决什么问题** | 同一个坑不踩第二次 |
| **收录范围** | 环境类（Maven 脚本、Node 版本、端口占用）、构建类（多主类打包失败）、配置类（占位符解析失败、Bean 找不到）、IDE 类（索引损坏、模块未导入） |
| **格式** | 报错原文 → 原因分析 → 解决步骤 → 如何避免 |

---

## 5. 维护纪律

写完一篇文章后，必须同步更新三处，否则笔记会慢慢腐烂：

| 要更新的地方 | 更新什么 |
|---|---|
| 本页对应条目 | 状态标记 <NoteStatus level="todo" /> → <NoteStatus level="done" /> |
| [学习路线 · 进度快照](/guide/roadmap#_5-当前进度快照) | 该模块的「代码」「笔记」两列状态 |
| [后端模块地图](/backend/) 或对应总览页 | 新增的文件路径、接口列表 |

::: warning 状态别乱标
<NoteStatus level="done" /> 的含义是「这篇笔记的内容已经写完，且里面的运行输出都真实复现过」。

代码写完了但笔记没写 → 笔记标 <NoteStatus level="todo" />
笔记写了一半 → 标 <NoteStatus level="wip" />

标错的代价是：三个月后你自己也不知道哪些是真学过的。
:::
