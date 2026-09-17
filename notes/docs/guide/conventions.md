# 工程规范约定

规范的价值在于**不用每次重新决策**。定下来之后照着做，省下的是反复纠结的时间。

以下是这个项目里已经生效的约定，改动前先想清楚代价。

## 1. 命名规范

### 1.1 包名

```
top.a1788.<模块简称>
```

| 项 | 值 | 说明 |
|---|---|---|
| 域名前缀 | `top.a1788` | 个人标识，**不用 `com.itheima`**（那是教程作者的） |
| 模块段 | 见下表 | 和模块目录名去掉编号后的部分对应 |

| 模块 | 包名 |
|---|---|
| 01 快速入门 | `top.a1788.quickstart` |
| 02 配置管理 | `top.a1788.config` |
| 03 日志 | `top.a1788.logging` |
| 04 Web | `top.a1788.web` |
| 05 MySQL | `top.a1788.mysql` |
| 06 MyBatis | `top.a1788.mybatis` |

::: warning 历史遗留：`com.itheima`
早期跟着教程敲的代码用了 `com.itheima` 包名，`01-quickstart` 和 `02-config` 里还残留一部分。

**处理策略**：不主动批量改。这些类能正常运行，改动它们的收益低于风险。新写的代码一律用 `top.a1788`。

代价是 `02-config` 里现在有**两个启动类**（`com.itheima.Application` 和 `top.a1788.config.ConfigApplication`），扫包范围不重叠 —— 参见 [踩坑记录](/engineering/troubleshooting)。
:::

### 1.2 模块目录名

```
<两位编号>-<英文小写短横线>
```

- `01-quickstart`、`02-config`、`03-logging` …… 编号连续
- 编号是**顺序标识**，不是序号占位。目前 01~13 连续无跳号
- 英语单词用短横线连接（`06-mybatis`，不是 `06_mybatis` 或 `06MyBatis`）

### 1.3 类名

| 类型 | 规范 | 示例 |
|---|---|---|
| 启动类 | `<模块名>Application` | `ConfigApplication` |
| 配置属性类 | `<业务>Properties` | `StudentProperties` |
| 控制器 | `<业务>Controller` | `StudentController` |
| 服务接口 | `<业务>Service` | `EnvService` |
| 服务实现 | `<环境/场景><业务>Service` | `DevEnvService` |
| 测试类 | `<被测类>Test` | `StudentPropertiesTest` |

## 2. 端口分配

**每个模块固定一个端口**，避免同时启动时冲突。

| 模块 | 端口 | 备注 |
|---|---|---|
| 01-quickstart | `8080` | 默认端口 |
| 02-config | `8002` | 与教程一致 |
| 03~13 | 待分配 | 建议按 `80XX` 规律顺延 |

::: tip 端口写在配置里，不写在代码里
统一在 `application.yml` 的 `server.port` 声明。不要在 Java 代码里硬编码端口。

需要临时改端口时用命令行参数覆盖，不动文件：

```bash
java -jar app.jar --server.port=9000
```
:::

## 3. 配置文件组织

### 3.1 主配置写什么、环境配置写什么

| 文件 | 写什么 | 不写什么 |
|---|---|---|
| `application.yml` | 所有环境**共用**的配置、默认值、自定义业务配置 | 环境特有的值（端口、日志级别、数据源地址） |
| `application-dev.yml` | 开发环境差异：日志宽松、连本地数据库 | 重复主配置里已有的 key |
| `application-prod.yml` | 生产环境差异：日志收紧、连生产数据库 | 调试开关（生产不该有） |

**判断标准**：这个值在不同环境**不一样**吗？不一样就放环境文件，一样就放主配置。

### 3.2 key 命名

| 场景 | 写法 | 说明 |
|---|---|---|
| 多单词 key | `kebab-case` | `max-count`，不用 `maxCount` 或 `max_count` |
| 自定义业务前缀 | 单独一段命名空间 | 如 `student:`、`app:`、`mxu:`，不要塞进 `spring:` 下 |
| 简单值 | `@Value("${key}")` | 散装、个数少 |
| 一组同前缀配置 | `@ConfigurationProperties` | 结构化、个数多、有嵌套 |

::: tip 为什么自定义配置不用 `spring.` 前缀
`spring.*` 是 Spring Boot 保留命名空间。往里塞自己的 key 会：
1. 在 IDE 里失去自动补全提示（IDE 只认官方 schema）
2. 未来 Spring 新增同名配置时静默冲突

自定义配置用**自己的域名反写**或独立前缀最安全。
:::

## 4. Git 提交规范

```
<type>(<scope>): <中文描述>
```

| type | 用于 | 示例 |
|---|---|---|
| `feat` | 新增功能 | `feat(02-config): 新增配置绑定案例` |
| `fix` | 修复缺陷 | `fix(02-config): 指定打包主类，修复双启动类打包失败` |
| `refactor` | 重构（不改行为） | `refactor: 目录结构对齐教学仓库` |
| `docs` | 文档变更 | `docs: 补充模块 README` |
| `chore` | 杂项（构建、配置、依赖） | `chore: 空目录添加 .gitkeep 占位` |
| `test` | 测试相关 | `test(02-config): 补充边界值用例` |

**scope** 用模块目录名，省略号后的描述用中文、动宾结构、说清「做了什么」。

::: warning 不要用 `update`、`修改` 这种无信息量的提交信息
半年后 `git log` 看到一排 `update`，等于没有历史。
:::

## 5. 目录与文件约定

### 5.1 一个模块的完整结构

```
02-config/
├── pom.xml
├── README.md                      模块说明（可选）
├── 子模块名.iml / 无               IDEA 生成，通常不进 Git
└── src/
    ├── main/
    │   ├── java/top/a1788/config/
    │   │   ├── ConfigApplication.java      启动类放包根
    │   │   ├── controller/                 控制器
    │   │   ├── service/                    服务
    │   │   ├── properties/                 配置属性类
    │   │   ├── entity/                     实体
    │   │   └── config/                     自定义配置类（可选）
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       └── application-prod.yml
    └── test/
        └── java/top/a1788/config/
            └── XxxTest.java
```

### 5.2 空目录与 `.gitkeep`

**Git 不追踪空目录。** 新建的模块目录如果里面什么都没有，`git push` 之后 GitHub 上看不到。

解法是放一个占位文件：

```bash
touch 03-logging/.gitkeep
```

| 项 | 约定 |
|---|---|
| 文件名 | `.gitkeep`（这不是 Git 官方功能，只是社区惯例，任意文件名都行） |
| 时机 | 目录建好但代码还没写时 |
| 清理 | 往目录里写了第一个真实文件后，**删掉 `.gitkeep`** |

### 5.3 哪些文件不进 Git

仓库根的 `.gitignore` 已覆盖：

| 类别 | 忽略项 |
|---|---|
| 依赖 | `node_modules/`、`vendor/`、`.venv/` |
| 构建产物 | `target/`、`dist/`、`build/`、`out/` |
| IDE 配置 | `.idea/`、`.vscode/`、`*.iml` |
| 环境与密钥 | `.env*`、`*.pem`、`*.key`、`secrets/` |
| 日志 | `*.log`、`logs/` |
| 数据库文件 | `*.db`、`*.sqlite` |

::: danger 一个必须记住的点
`.idea/` 被忽略了，所以 **IDEA 的配置不会跟着 Git 走**。

这意味着：换电脑 clone 仓库后，IDEA 的 Maven 工程配置、运行配置、Apifox 令牌**全都没有**，需要重新配一遍。这不是 bug，是刻意设计 —— IDE 配置里可能含令牌等敏感信息。
:::

## 6. 代码风格

| 项 | 约定 |
|---|---|
| 编码 | UTF-8（父 pom 里 `project.build.sourceEncoding=UTF-8`） |
| 缩进 | 4 空格（Java）/ 2 空格（JS、YAML、JSON） |
| 注释 | 类注释说明**用途**，字段注释说明**业务含义**，都不写"设置 xxx"这种废话 |
| 依赖注入 | 构造器注入优先（配合 Lombok `@RequiredArgsConstructor`），字段注入仅在测试或简单场景用 `@Resource` |
| Lombok | 实体和 Properties 类用 `@Data`；不要在 Controller/Service 上乱用 |

**构造器注入和字段注入怎么选**

```java
// 推荐：构造器注入（final 字段，依赖不可变，便于测试）
@RestController
@RequiredArgsConstructor
public class ConfigController {
    private final AppProperties appProperties;
}

// 可用：字段注入（代码更短，但依赖可被反射修改，测试时不便替换）
@RestController
public class StudentController {
    @Resource
    private StudentProperties studentProperties;
}
```

::: tip Spring 官方推荐构造器注入
原因有三：依赖不可变（`final`）、依赖关系在编译期就暴露（构造时缺依赖直接报错）、脱离 Spring 容器也能 `new` 出来做单元测试。
:::

## 7. 规范检查清单

提交前对着过一遍：

- [ ] 包名是 `top.a1788.xxx`，不是 `com.itheima.xxx`
- [ ] 模块目录名是 `<两位编号>-<英文>`，编号连续
- [ ] 端口已在配置里分配，没和已用端口冲突
- [ ] 环境差异项写在 `application-{profile}.yml`，没往主配置里塞
- [ ] 自定义配置 key 没占用 `spring.*`
- [ ] 提交信息符合 `<type>(<scope>): <描述>` 格式
- [ ] `.gitignore` 覆盖了新增的产物目录
- [ ] 空目录补了 `.gitkeep`，有内容的目录删了 `.gitkeep`
- [ ] 没有把 `.idea/`、`node_modules/`、`target/` 提交进去

<NoteStatus level="done" />
