# 多环境与 Profile

<NoteStatus level="done" />

## 0. 本篇要解决的问题

1. 开发环境连本地数据库、生产连线上数据库，同一份代码怎么跑出两套配置？
2. 主配置和 `application-{profile}.yml` 各写什么？怎么划分？
3. `@Profile` 能让一个 Bean 只在某个环境注册，怎么用？
4. 已经打包好的 jar，怎么在部署时改环境而不用重新打包？
5. 生产环境把 `active: prod` 写死在配置文件里有什么风险？

## 1. 多环境配置的三种做法

### 做法一：一个文件，切换时改注释（不推荐）

```yaml
# 开发环境
server:
  port: 8002
# 生产环境
# server:
#   port: 80
```

**问题**：切环境要改代码、容易漏、改错了没人发现、无法在 CI 中自动化。**只适合临时调试。**

### 做法二：多个文件，主配置里切 `active`（本项目采用）

```
application.yml          主配置：所有环境共用
application-dev.yml      开发环境差异
application-prod.yml     生产环境差异
```

```yaml
# application.yml
spring:
  profiles:
    active: dev        # 改这一行就切环境
```

### 做法三：主配置只留最小信息，环境由外部注入（生产推荐）

```yaml
# application.yml —— 不写 active
spring:
  application:
    name: 02-配置管理
```

```bash
# 部署时指定
java -jar app.jar --spring.profiles.active=prod
```

**为什么生产推荐做法三**：见 [6.3 为什么生产不该在配置文件里写死 `active`](/backend/02-config/profile#_6-3-为什么生产不该在配置文件里写死-active)。

## 2. 配置划分原则

| 文件 | 写什么 | 不写什么 |
|---|---|---|
| `application.yml` | 所有环境**共用**的：应用名、业务配置、默认值 | 环境特有的值 |
| `application-dev.yml` | 开发差异：日志宽松、本地数据库地址、调试开关 | 重复主配置已有的 key |
| `application-prod.yml` | 生产差异：日志收紧、生产数据库、性能参数 | 调试开关（生产不该有） |

### 判断标准

**这个值在不同环境会不一样吗？**

```
不一样  → 放 application-{profile}.yml
一样    → 放 application.yml
```

拿本项目举例：

| 配置项 | 放哪 | 为什么 |
|---|---|---|
| `spring.application.name` | 主配置 | 三个环境都叫这个名，不会变 |
| `server.port` | 主配置 | 本地开发就是 8002，生产如果不同再覆盖 |
| `student.*` | 主配置 | 演示用的业务配置，不随环境变 |
| `logging.level.*` | 环境文件 | 开发要 debug，生产要 warn —— **典型的环境差异** |
| `env.name` / `env.description` | 环境文件 | 就是为了演示差异化而存在的 |
| 数据源 url / 密码 | 环境文件 | 开发连本地库，生产连线上库 |

## 3. 本项目的具体实现

### 3.1 主配置

```yaml [application.yml]
server:
  port: 8002

spring:
  application:
    name: 02-配置管理
  profiles:
    active: dev          # 默认激活 dev，可用命令行覆盖

mxu:
  name: a1788
  job: 学习者

student:
  name: 张三
  age: 18
  # ... 四种绑定形态

app:
  name: 配置管理模块
  author: ${mxu.name}
  port: 8002
  max-count: 100
```

### 3.2 开发环境

```yaml [application-dev.yml]
logging:
  level:
    top.a1788.config: debug        # 自己包的日志开 debug，便于排查
    org.springframework.web: info

env:
  name: 开发环境
  description: 用于开发调试，日志级别较低
```

### 3.3 生产环境

```yaml [application-prod.yml]
logging:
  level:
    root: warn                     # 全局收紧到 warn
    top.a1788.config: info          # 自己的包留 info

env:
  name: 生产环境
  description: 面向真实用户，日志级别收紧
```

::: tip 生产日志为什么这样配
```yaml
root: warn                  # 全局只记警告和错误，过滤掉海量 info
top.a1788.config: info      # 但自己的业务包保留 info
```

**原则**：第三方框架的 info 日志在生产是噪音（Spring 启动、连接池、Hibernate 每次都刷一堆），自己的业务日志才是有用信息。

`root: warn` 把噪音压下去，再单独给自己包放开。这是生产日志配置的标准套路。
:::

### 3.4 验证 profile 真的生效

**启动日志会打印实际激活的 profile**：

```
The following 1 profile is active: "dev"
```

**没打印这一行**说明没有 profile 被激活，配置可能没生效。

也可以写个接口读出来（本项目的方式）：

```java [ConfigController.java]
@Value("${env.name}")
private String envName;

@Value("${env.description}")
private String envDescription;

@GetMapping("/env")
public String getEnv() {
    return "当前环境：" + envName + "，" + envDescription
            + "；Profile Bean：" + envService.envInfo();
}
```

```bash
curl http://localhost:8002/config/env
```

真实输出（`active: dev` 时）：

```
当前环境：开发环境，用于开发调试，日志级别较低；Profile Bean：我是 dev 环境专属的 Bean
```

换成生产环境：

```bash
java -jar 02-config/target/02-config-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
curl http://localhost:8002/config/env
```

```
当前环境：生产环境，面向真实用户，日志级别收紧；Profile Bean：我是 prod 环境专属的 Bean
```

**注意两次输出的 `env.name` 和 Profile Bean 都变了，但代码一行没改。**

## 4. `@Profile`：按环境注册 Bean

配置文件能切**值**，`@Profile` 能切**Bean** —— 让某个实现类只在特定环境下被注册进容器。

### 4.1 接口 + 两个实现

```java [EnvService.java]
public interface EnvService {
    String envInfo();
}
```

```java [DevEnvService.java]
@Service
@Profile("dev")
public class DevEnvService implements EnvService {
    @Override
    public String envInfo() {
        return "我是 dev 环境专属的 Bean";
    }
}
```

```java [ProdEnvService.java]
@Service
@Profile("prod")
public class ProdEnvService implements EnvService {
    @Override
    public String envInfo() {
        return "我是 prod 环境专属的 Bean";
    }
}
```

```java [ConfigController.java]
private final EnvService envService;   // 构造器注入接口，不关心是哪个实现
```

**运行时只会有其中一个实现被注册**，`active=dev` 时注入的就是 `DevEnvService`。

### 4.2 不重叠是硬性要求

::: danger 两个实现同时生效会怎样
```
NoUniqueBeanDefinitionException:
expected single matching bean but found 2: devEnvService, prodEnvService
```

**前提是激活了**同时匹配两个 `@Profile` 的环境，比如：

```java
@Profile({"dev", "test"})   // DevEnvService
@Profile({"prod", "test"})  // ProdEnvService
```

激活 `test` 时两个都注册，注入就炸。

**规则**：同类型的多个 `@Profile` 实现，profile 集合**必须两两互斥**。
:::

### 4.3 `@Profile` 的几种写法

```java
@Profile("dev")                      // 只在 dev 激活
@Profile({"dev", "test"})            // dev 或 test 都激活
@Profile("!prod")                    // 不是 prod 时激活（取反）
@Profile("!dev & !test")             // 复杂表达式
```

### 4.4 什么时候该用 `@Profile`

| 场景 | 适合 | 说明 |
|---|---|---|
| 开发用 Mock 实现，生产用真实实现 | ✅ | 比如支付：dev 环境用假的支付网关，prod 用真的 |
| 开发环境自动灌测试数据 | ✅ | `@Profile("dev")` 的 `CommandLineRunner` |
| 生产环境才需要的监控组件 | ✅ | |
| 只是配置值不同 | ❌ | 用 yml 环境文件就够，别写两个 Bean |

**判断标准**：差异在**代码行为**上（不只是参数值），才用 `@Profile`。差异只是配置值，用环境配置文件。

## 5. 配置优先级实战

完整优先级链见 [模块总览](/backend/02-config/#_1-2-同一个-key-在多个来源都有-听谁的)。这里看实际效果。

### 5.1 环境文件覆盖主配置

```yaml
# application.yml
env:
  name: 默认环境          # ← 主配置定义了

# application-dev.yml
env:
  name: 开发环境          # ← 环境文件重新定义
```

激活 dev 时，`${env.name}` 得到 **`开发环境`**。同名 key，**更具体的（profile 文件）赢**。

### 5.2 命令行覆盖一切

```bash
java -jar app.jar --env.name=命令行环境
```

即使 `active=dev`、`application-dev.yml` 里写了 `开发环境`，最终也是 **`命令行环境`**。

**这条对运维很重要**：线上改个参数不用重新打包，重启时带上参数就行。

### 5.3 环境变量（容器部署常用）

```bash
# Spring Boot 3 的松散绑定支持环境变量名映射
export ENV_NAME=环境变量环境
java -jar app.jar
```

配置项 `env.name` → 环境变量名 `ENV_NAME`（大写 + 点换下划线）。

::: tip Docker / K8s 里改配置的标准做法
容器里不方便改文件、也不方便传一长串命令行参数，环境变量是最顺手的：

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ENV_NAME=生产环境
```
:::

## 6. 踩过的坑

### 6.1 忘写 `active` 导致配置不生效

**现象**：`application-dev.yml` 里的日志级别没生效，`${env.name}` 报占位符解析失败，启动直接崩。

```
IllegalArgumentException: Could not resolve placeholder 'env.name'
```

**原因**：`env.name` 只定义在 `application-dev.yml` 里，但没有任何 profile 被激活，那个文件**根本没被加载**。

**排查**：看启动日志有没有 `The following 1 profile is active: "dev"`。没有就说明没激活。

**三种激活方式**：

```yaml
# ① 配置文件里
spring:
  profiles:
    active: dev
```

```bash
# ② 命令行参数
java -jar app.jar --spring.profiles.active=dev

# ③ 环境变量
export SPRING_PROFILES_ACTIVE=dev
```

### 6.2 `active` 和 `include` 的区别

```yaml
# active：完全替换，只激活 dev
spring:
  profiles:
    active: dev

# include：追加，永远额外加载 common
spring:
  profiles:
    include: common
```

`include` 出来的文件**优先级低于** `active` 指定的文件。这个特性用得少，知道有这回事就行。

### 6.3 为什么生产不该在配置文件里写死 `active`

假设 `application.yml` 里写：

```yaml
spring:
  profiles:
    active: prod        # ← 写死在仓库里
```

**三个问题**：

| 问题 | 后果 |
|---|---|
| **密钥泄露** | 为了让 prod profile 生效，`application-prod.yml` 得跟着进仓库。里面有数据库密码、第三方 key，全在 Git 历史里 |
| **无法一套包多环境** | 想用同一个 jar 在测试和生产跑，得改代码重新打包 |
| **改环境要重新构建** | 生产出故障想紧急切配置，得走一遍构建流程 |

**正确做法**：

```yaml
# application.yml —— 不写 active，或写成本地开发默认值
spring:
  application:
    name: 02-配置管理
```

```bash
# 部署时指定；prod 配置不进仓库（放服务器本地或配置中心）
java -jar app.jar --spring.profiles.active=prod
```

### 6.4 yml 缩进错误导致整块失效

**现象**：`logging` 配置改了没效果，或者启动报 `Could not resolve placeholder`。

**原因**：yml 用缩进表达层级，缩进错了 key 的路径就变了。

```yaml
# 正确的缩进（2 空格一层）
logging:
  level:
    root: warn

# 错误示例：level 和 logging 同级了
logging:
level:
  root: warn
```

**排查工具**：IDEA 打开 yml 文件，左侧会有**缩进辅助线**，一眼能看出层级对不对。或者右键 yml 文件 → **Diagrams → Show Diagram** 看结构树。

## 7. 小结与自检

### 核心结论

1. 多环境 = 主配置写共用的 + 环境文件写差异的 + `spring.profiles.active` 切
2. 划分标准：**这个值在不同环境会变吗**？会变就放环境文件
3. `@Profile` 按环境注册 Bean，适合「代码行为有差异」的场景，不适合「只是参数不同」
4. 优先级：命令行 > 系统属性 > 环境变量 > 环境文件 > 主配置
5. 生产用 `--spring.profiles.active=prod` 外部注入，不要写死在仓库里

### 自检清单

- [ ] `application.yml` 和 `application-dev.yml` 都有 `env.name`，激活 dev 时取哪个？
- [ ] 怎么在不重新打包的前提下，把已部署的 jar 切到 prod 环境？
- [ ] `@Profile("dev")` 和 `@Profile("!prod")` 有什么区别？
- [ ] 两个同类型的 `@Service` 都加了 `@Profile`，什么情况下会报 `NoUniqueBeanDefinitionException`？
- [ ] 生产环境日志为什么配成 `root: warn` + `自己包: info`？
- [ ] 为什么 `active: prod` 不该写在提交到仓库的配置文件里？

### 下一步

[配置校验与最佳实践](/backend/02-config/validation) —— 让配置写错在启动阶段就暴露，而不是等到线上。
