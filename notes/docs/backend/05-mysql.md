# 05 MySQL

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\05-mysql`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8005`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 建库建表时字符集怎么定？为什么必须是 `utf8mb4` 而不是 `utf8`？
2. 不用任何 ORM，最原始的 Java 访问数据库是怎么写的（JDBC 六步）？
3. `DataSource` 和 `Connection` 是什么关系？连接池为什么必须要有？
4. Spring Boot 里配一个数据源，最少需要写哪些配置？
5. 为什么拼 SQL 字符串会导致注入？`PreparedStatement` 是怎么防住的？

## 1. 模块定位

05 是**从「内存里的对象」到「能持久化的系统」的转折点**。

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 04 Web：能收请求、能返回数据 | **05：数据能存下来** | 06 ORM、07 MQ（消息落库）、12 测试（数据层测试） |

**为什么要先学原生 JDBC 再学 ORM**：ORM 把 JDBC 封装了七八层，出问题（连接泄漏、事务失效、SQL 慢）时你必须能穿透到 JDBC 那一层理解。**跳过 JDBC 直接学 MyBatis，遇到问题会完全无从下手。**

## 2. 计划覆盖的知识点

### 2.1 建库建表

```sql
-- 库
CREATE DATABASE backend_learning
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 表
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
  `age`        INT          DEFAULT NULL COMMENT '年龄',
  `birthday`   DATE         DEFAULT NULL COMMENT '生日',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';
```

::: danger `utf8` 和 `utf8mb4` 的区别必须记住
MySQL 的 `utf8` 是个**历史遗留的残缺实现** —— 每个字符最多 3 字节，**存不了 emoji 和部分生僻字**（这些字符需要 4 字节）。

```sql
-- 用 utf8 的表插入 emoji
INSERT INTO t (content) VALUES ('😀');
-- 报错：Incorrect string value: '\xF0\x9F\x98\x80'
```

**建库建表一律 `utf8mb4`**，这是唯一正确的选择。`utf8mb4_unicode_ci` 或 `utf8mb4_general_ci` 作为排序规则都可以，前者更准确（支持多语言排序），后者略快。
:::

### 2.2 JDBC 六步

```java
// 1. 注册驱动（JDBC 4.0 后通常可省略，由 SPI 自动加载）
Class.forName("com.mysql.cj.jdbc.Driver");

// 2. 获取连接
Connection conn = DriverManager.getConnection(url, username, password);

// 3. 创建 Statement（防注入必须用 PreparedStatement）
PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE username = ?");
ps.setString(1, "张三");

// 4. 执行 SQL
ResultSet rs = ps.executeQuery();

// 5. 处理结果集
while (rs.next()) {
    Long id = rs.getLong("id");
    String username = rs.getString("username");
}

// 6. 释放资源（顺序：ResultSet → Statement → Connection）
rs.close();
ps.close();
conn.close();
```

**每一步都要能说清为什么**：

| 步骤 | 关键点 |
|---|---|
| 注册驱动 | JDBC 4.0 起通过 SPI（`META-INF/services/java.sql.Driver`）自动加载，手写 `Class.forName` 可以不写 |
| 获取连接 | `DriverManager` 每次新建物理连接，**开销极大** → 所以需要连接池 |
| 创建 Statement | 必须用 `PreparedStatement` 防注入 |
| 执行 | `executeQuery` 返回 `ResultSet`；`executeUpdate` 返回影响行数 |
| 处理结果 | 列名或列索引都可以，**建议用列索引**（列名改动时不敏感，且略快） |
| 释放 | **必须用 try-with-resources**，否则异常路径下连接泄漏 |

### 2.3 SQL 注入与 `PreparedStatement`

```java
// ❌ 拼接 SQL —— 可被注入
String sql = "SELECT * FROM user WHERE username = '" + input + "'";
// 输入：' OR '1'='1
// 实际执行：SELECT * FROM user WHERE username = '' OR '1'='1'
// 结果：全表数据被查出来

// ✅ 预编译占位符
String sql = "SELECT * FROM user WHERE username = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, input);
```

**为什么 `?` 能防住**：SQL 的**结构**在预编译阶段就固定了，参数只被当作**数据**填入，不会被解析成 SQL 语法。输入 `' OR '1'='1` 会被当成一个普通字符串去找，查不到。

::: warning 有三个地方容易被绕过
| 场景 | 问题 |
|---|---|
| `ORDER BY ?` | **不能用占位符**！排序字段是 SQL 结构的一部分，只能用白名单校验后拼接 |
| `LIKE ?` | 占位符位置对，但 `%` 得拼在参数里：`ps.setString(1, "%" + kw + "%")` |
| `IN (?)` | 占位符只代表一个值，要动态生成 `IN (?, ?, ?)` 并按数量设置参数 |
:::

### 2.4 连接池

**为什么必须有连接池**：一次数据库操作里，**建立 TCP 连接 + 认证握手占绝大部分耗时**。每次都新建连接，接口响应会被连接开销拖垮。

| 做法 | 单次操作耗时（量级） | 问题 |
|---|---|---|
| 每次 `DriverManager.getConnection` | 几十毫秒 | 无法承受并发 |
| 连接池复用 | 亚毫秒 | 需要正确配置池大小 |

**Spring Boot 默认用 HikariCP**（引入 `spring-boot-starter-jdbc` 后自动配置），因为它在主流连接池里性能最好。

| 参数 | 默认值 | 说明 |
|---|---|---|
| `maximum-pool-size` | 10 | 最大连接数 |
| `minimum-idle` | 同 maximum | 最小空闲连接 |
| `connection-timeout` | 30s | 拿不到连接等待多久报错 |
| `max-lifetime` | 30min | 连接最长存活时间（**要小于数据库的 `wait_timeout`**，否则拿到已被服务端关闭的连接） |
| `idle-timeout` | 10min | 空闲连接多久被回收 |

::: tip 池大小不是越大越好
`maximum-pool-size = 10` 对绝大多数场景够用。设成 100 反而可能因为数据库端连接数上限、上下文切换开销导致整体变慢。

**经验值**：`CPU 核数 * 2 + 磁盘数`。对单机 MySQL 来说，10~20 通常足够。
:::

### 2.5 Spring Boot 数据源配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/backend_learning?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD}          # ← 走占位符，不写明文
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      max-lifetime: 1800000
```

**URL 参数逐个说明**：

| 参数 | 作用 | 不写会怎样 |
|---|---|---|
| `useUnicode=true` + `characterEncoding=utf8mb4` | 字符编码 | 中文乱码 |
| `serverTimezone=Asia/Shanghai` | 时区 | 时间差 8 小时（或直接报错） |
| `useSSL=false` | 关闭 SSL | 本地开发会刷警告日志 |
| `allowPublicKeyRetrieval=true` | 允许获取公钥 | MySQL 8 用 `caching_sha2_password` 时会连接失败 |

::: danger 密码不要写明文提交到仓库
```yaml
password: ${DB_PASSWORD:}       # 从环境变量读，不写默认值
```

配合部署时注入环境变量。**仓库是会被 clone 的，明文密码一旦提交，Git 历史里就永久留下了。**
（这也是 02 模块 [配置校验](/backend/02-config/validation#_5-哪些配置该校验-哪些不用) 里强调「敏感配置不给默认值」的原因。）
:::

### 2.6 与 02 模块的衔接

数据源配置本身就是**典型的多环境差异配置**：

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/backend_learning_dev

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://10.0.0.10:3306/backend_learning
```

**这正是 02 模块「主配置写共用、环境文件写差异」原则的直接应用。**

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| SQL 脚本 | `05-mysql/sql/schema.sql`：建库建表 + 初始化数据 |
| 代码 | `JdbcDemo`：演示 JDBC 六步（不用 Spring，纯 `main` 方法） |
| 代码 | `UserDao`：用 `JdbcTemplate` 或原生 JDBC 做 CRUD |
| 代码 | `DataSourceController`：暴露连接池状态（活跃连接数等） |
| 配置 | `application.yml` 数据源 + Hikari 参数；dev/prod 分别指向不同库 |
| 笔记 | **注入攻击的实测对比**（拼接 vs 预编译，同一输入两种结果） |

## 4. 待办清单

- [ ] 本地装 MySQL 或用已有实例，建库 `backend_learning`（**必须 `utf8mb4`**）
- [ ] 写 `schema.sql` 建 `user` 表，插入测试数据
- [ ] 写一个不用 Spring 的 `JdbcDemo`，完整走完 JDBC 六步
- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8005 端口
- [ ] 配数据源（密码用环境变量占位符）
- [ ] 写 `UserDao` 做 CRUD，**用 try-with-resources 管理资源**
- [ ] 实测 SQL 注入：同一段恶意输入，拼接写法被绕过、预编译写法防住
- [ ] 起服务，验证接口能从数据库读到数据
- [ ] 贴真实输出：连接池启动日志、查询结果、注入对比结果
- [ ] 写踩坑：中文乱码、时区差 8 小时、`Public Key Retrieval is not allowed`、连接泄漏
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| 直连 `JdbcTemplate` 还是先上 ORM？ | 本模块**必须用原生方式**，不然学不到 JDBC 层的东西 |
| 逻辑删除还是物理删除？ | 逻辑删除（加 `deleted` 字段）更符合生产实践 |
| 时间字段类型 | `DATETIME`（不带时区）/ `TIMESTAMP`（带时区，2038 问题）/ `BIGINT` 存毫秒 |
| 主键类型 | `BIGINT AUTO_INCREMENT` / UUID / 雪花算法（分布式） |
| 密码怎么管理 | 环境变量 / 配置中心 / 启动参数 —— 绝不写进仓库 |

## 6. 预习要点

1. **InnoDB vs MyISAM**：InnoDB 支持事务和行锁，是默认选择；MyISAM 早已过时
2. **索引基础**：主键索引（聚簇索引）、唯一索引、普通索引。索引加速查询但拖慢写入
3. **`EXPLAIN`**：看一个 SQL 有没有走索引，`type` 列从好到差是 `const > eq_ref > ref > range > index > ALL`
4. **事务 ACID**：原子性、一致性、隔离性、持久性
5. **`Connector/J` 版本**：MySQL 8 用 `com.mysql.cj.jdbc.Driver`，老的 `com.mysql.jdbc.Driver` 已废弃

## 下一步

[06 MyBatis](/backend/06-mybatis) —— 手写 JDBC 太啰嗦，ORM 来解决。
