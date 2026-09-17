# 后端模块地图

后端仓库：`E:\houduan` → `github.com/hh2248824905/backend-learning`

所有模块在一个 Maven 多模块工程里，父 `pom.xml` 聚合全部子模块，共享 JDK 版本和编码设置。

## 1. 目录结构

```
houduan/                          ← 父工程（packaging=pom，只有 pom.xml）
├── pom.xml
├── README.md
├── 01-quickstart/                ← 快速入门
├── 02-config/                    ← 配置管理（内容最多，已拆 4 篇笔记）
├── 03-logging/                   ← 日志管理
├── 04-web/                       ← Web 开发
├── 05-mysql/                     ← 数据库
├── 06-mybatis/                   ← 持久层框架
├── 07-mq/                        ← 消息队列
├── 08-schedule/                  ← 定时任务
├── 09-security/                  ← 安全认证
├── 10-file/                      ← 文件处理
├── 11-doc/                       ← 接口文档
├── 12-test/                      ← 单元测试
└── 13-actuator/                  ← 监控运维
```

::: tip 关于 03~13 目录
这些目录**已经建好但里面只有 `.gitkeep` 占位文件**，代码还没写。

建目录先于写代码，好处是「模块清单」在文件系统层面就是可见的 —— 打开 IDEA 就知道还有多少没做。开始写某个模块时，删掉对应的 `.gitkeep`。
:::

## 2. 模块清单

| 模块 | 主题 | 端口 | 代码 | 笔记 | 关键产出 |
|---|---|---|---|---|---|
| [01](/backend/01-quickstart) | 快速入门 | 8080 | <NoteStatus level="done" /> | <NoteStatus level="done" /> | 第一个 Web 接口、自动配置理解 |
| [02](/backend/02-config/) | 配置管理 | 8002 | <NoteStatus level="done" /> | <NoteStatus level="done" /> | 配置注入全链路、多环境、配置校验 |
| [03](/backend/03-logging) | 日志管理 | 8003 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 分级日志 + 滚动归档 |
| [04](/backend/04-web) | Web 开发 | 8004 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 统一返回体 + 全局异常 + 参数校验 |
| [05](/backend/05-mysql) | MySQL | 8005 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 数据源 + JDBC |
| [06](/backend/06-mybatis) | MyBatis | 8006 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | ORM 增删改查 + 分页 |
| [07](/backend/07-mq) | 消息队列 | 8007 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 异步解耦 + 幂等消费 |
| [08](/backend/08-schedule) | 定时任务 | 8008 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | cron 任务 + 防重复执行 |
| [09](/backend/09-security) | 安全认证 | 8009 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 认证 + 授权 |
| [10](/backend/10-file) | 文件处理 | 8010 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 上传下载闭环 |
| [11](/backend/11-doc) | 接口文档 | 8011 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | OpenAPI 自动生成 |
| [12](/backend/12-test) | 单元测试 | 8012 | <NoteStatus level="wip" /> | <NoteStatus level="todo" /> | 三类测试用例 |
| [13](/backend/13-actuator) | 监控运维 | 8013 | <NoteStatus level="todo" /> | <NoteStatus level="todo" /> | 健康检查与指标 |

> 端口从 03 开始按 `80XX` 顺延，与模块编号对齐，方便记忆。当前 01 用 8080、02 用 8002 是历史原因（跟教程保持一致）。

## 3. 各模块对外接口

已经实现的接口（可用于 Apifox 联调）：

### 01-quickstart（端口 8080）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/user/info` | 返回 `User` 对象 JSON |

### 02-config（端口 8002）

| 方法 | 路径 | 说明 | 验证的知识点 |
|---|---|---|---|
| GET | `/config/basic` | 服务器端口 + 应用名 | 基础 `@Value` 注入 |
| GET | `/config/my` | 自定义配置项 | 自定义前缀 `@Value` |
| GET | `/config/value` | 占位符/默认值/随机值/SpEL | `@Value` 进阶四种写法 |
| GET | `/config/env` | 当前环境 + Profile Bean | 多环境 + `@Profile` |
| GET | `/config/app` | `AppProperties` 整体 | `@ConfigurationProperties` + 校验 |
| GET | `/student/info` | student 全量嵌套配置 | List / Map / 嵌套对象 / 对象列表 |
| GET | `/config/info` | 旧版配置演示 | `com.itheima` 遗留代码 |
| GET | `/user/info` | 旧版 User 对象 | 遗留代码 |

::: warning `/config/info` 和 `/user/info` 是遗留接口
它们属于早期 `com.itheima` 包下的代码，和新的 `top.a1788.config` 是**两个独立应用上下文**（两个启动类，扫包范围不重叠）。保留是为了不破坏已同步到 Apifox 的接口，新代码不要参照这两个。
:::

## 4. 每个模块的通用结构

不管是哪个模块，代码组织方式一致：

```
0X-模块名/
├── pom.xml
└── src/
    ├── main/java/top/a1788/<模块简称>/
    │   ├── <模块>Application.java    ← 启动类，放在包根
    │   ├── controller/               ← 接收请求，不写业务逻辑
    │   ├── service/                  ← 业务逻辑
    │   ├── properties/               ← 配置属性类
    │   └── entity/                   ← 实体
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-dev.yml
    │   └── application-prod.yml
    └── test/java/top/a1788/<模块简称>/
        └── XxxTest.java
```

**分层原则**：Controller 只做参数接收和结果返回，Service 承载业务逻辑，Properties 承载配置。不要把业务逻辑写进 Controller。

## 5. 常用命令

::: warning Git Bash 下 `mvn` 脚本有 bug
直接敲 `mvn` 会报找不到主类，见 [开发环境 · Maven 问题](/guide/env#_2-4-git-bash-下-mvn-脚本的问题)。

下面命令**在 IDEA 的 Maven 面板里点**，或用 `java -classpath` 方式执行。
:::

```bash
# 全量构建（跳过测试）
mvn clean package -DskipTests

# 全量测试
mvn test

# 只构建某个模块（-pl 指定模块，-am 连同它依赖的模块）
mvn -pl 02-config -am clean package -DskipTests

# 只跑某个模块的某个测试类
mvn -pl 02-config test -Dtest=StudentPropertiesTest
```

**启动单个模块**

```bash
java -jar 02-config/target/02-config-0.0.1-SNAPSHOT.jar
```

**临时覆盖端口和配置文件**

```bash
java -jar 02-config/target/02-config-0.0.1-SNAPSHOT.jar \
     --server.port=9000 \
     --spring.profiles.active=prod
```

## 6. 相关工程化笔记

- [Maven 多模块工程](/engineering/maven-multimodule) —— 父工程怎么聚合、版本怎么统一管
- [工程规范约定](/guide/conventions) —— 包名、端口、提交信息规范
- [踩坑与排错记录](/engineering/troubleshooting) —— 双启动类、打包失败等实际问题
