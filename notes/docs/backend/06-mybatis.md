# 06 MyBatis

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\06-mybatis`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8006`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 上一模块手写 JDBC 六步太啰嗦，ORM 具体省掉了什么？
2. MyBatis 和 MyBatis-Plus 是什么关系？该用哪个？
3. 不写一行 XML，能不能完成条件查询 + 分页？
4. `BaseMapper` 的 `insert` / `selectById` / `updateById` 是从哪来的？
5. 什么时候必须回到写 XML？

## 1. 模块定位

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 05 MySQL：知道数据怎么存 | **06：把 JDBC 模板代码消掉** | 07 MQ、08 定时任务、09 安全（用户表）、12 测试（数据层测试） |

**核心价值**：做完这个模块，一个标准 CRUD 接口的代码量应该从上模块的几十行降到**几行**，而且分页查询不用自己拼 `LIMIT`。

## 2. 计划覆盖的知识点

### 2.1 MyBatis vs MyBatis-Plus

| 维度 | 原生 MyBatis | MyBatis-Plus |
|---|---|---|
| 定位 | 半自动 ORM 框架 | 在 MyBatis 上**做增强**，不改变 MyBatis |
| 单表 CRUD | 每个方法都要写 SQL/映射 | 继承 `BaseMapper<T>` 直接有 17 个方法 |
| 条件查询 | 手写 XML `<where>` / `<if>` | `LambdaQueryWrapper` 链式调用 |
| 分页 | 手写 `LIMIT` 或引第三方插件 | 内置分页插件 |
| 联表/复杂 SQL | 强项 | 仍要写 SQL（Plus 不替代 MyBatis） |

**结论**：**用 MyBatis-Plus，但保留写原生 SQL 的能力**。单表走 `BaseMapper`，复杂查询写 XML。

::: tip 为什么不是「全用 ORM 自动生成」
JPA/Hibernate 那种「全自动 ORM」在复杂查询场景下会生成难以优化的 SQL。MyBatis 系列的核心优势是 **SQL 完全可控** —— 你能看到并调整每一条 SQL。

这是国内后端项目普遍选 MyBatis 而不是 JPA 的主要原因。
:::

### 2.2 起步配置

```yaml
mybatis-plus:
  # XML 映射文件位置（如果写了 XML）
  mapper-locations: classpath*:/mapper/**/*.xml
  # 实体包，用于生成 SQL 别名
  type-aliases-package: top.a1788.mybatis.entity
  configuration:
    # 数据库下划线 → Java 驼峰自动映射
    map-underscore-to-camel-case: true
    # 控制台打印 SQL（开发环境开，生产必须关）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键策略：ASSIGN_ID(雪花) / AUTO(数据库自增)
      id-type: auto
      # 逻辑删除配置
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

```java
@SpringBootApplication
@MapperScan("top.a1788.mybatis.mapper")     // ① 开启 Mapper 扫描
public class MybatisApplication { }
```

::: warning 少了 `@MapperScan` 的报错
```
Description: A component required a bean of type '...UserMapper' that could not be found.
```

两种解法：
- 启动类加 `@MapperScan("包路径")` ← 推荐，一处配置管所有
- 每个 Mapper 接口上加 `@Mapper` ← 接口多了要加一堆

**`@MapperScan` 的包路径要精确**，写成过宽的路径会把非 Mapper 接口也当 Mapper 注册，启动时报诡异错误。
:::

### 2.3 `BaseMapper` 通用 CRUD

```java
public interface UserMapper extends BaseMapper<User> {
    // 什么都不用写，就有 17 个方法
}
```

| 方法 | 说明 |
|---|---|
| `insert(T entity)` | 新增，插入后主键回填到实体 |
| `deleteById(Serializable id)` | 按主键删除（**逻辑删除配置下是更新 `deleted` 字段**） |
| `updateById(T entity)` | 按主键更新，**只更新非 null 字段** |
| `selectById(Serializable id)` | 按主键查询 |
| `selectList(Wrapper<T>)` | 条件查询列表 |
| `selectPage(IPage<T>, Wrapper<T>)` | 分页查询 |
| `selectCount(Wrapper<T>)` | 条件计数 |

::: tip `updateById` 的「只更新非 null 字段」是把双刃剑
**好处**：只想改一个字段时，不用查完整对象再改。

**坑**：如果你想**把某个字段置为 null**，`updateById` 做不到（null 被忽略）。这种情况要：
- 用 `UpdateWrapper` 显式 `.set(User::getPhone, null)`
- 或在实体字段上加 `@TableField(updateStrategy = FieldStrategy.IGNORED)`
:::

### 2.4 条件构造器

```java
// 链式构造，类型安全（LambdaQueryWrapper 用方法引用，字段名重构不会漏改）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

wrapper.like(StringUtils.hasText(keyword), User::getUsername, keyword)   // 条件成立才拼这一段
       .ge(minAge != null, User::getAge, minAge)
       .orderByDesc(User::getCreatedAt);

List<User> list = userMapper.selectList(wrapper);
```

**关键点：第一个布尔参数是「条件开关」**。

```java
wrapper.like(keyword != null, User::getUsername, keyword);
```

等价于手写 XML 里的 `<if test="keyword != null">`。**这个特性是消灭 XML 的核心** —— 动态 SQL 用 Lambda 就能表达。

**`LambdaQueryWrapper` vs `QueryWrapper`**：

| 类型 | 写法 | 推荐度 |
|---|---|---|
| `QueryWrapper` | `wrapper.eq("user_name", name)` 字符串字段名 | ⚠️ 字段改名时不会报错，运行时才炸 |
| `LambdaQueryWrapper` | `wrapper.eq(User::getUserName, name)` 方法引用 | ✅ 字段改名 IDE 能重构到 |

### 2.5 分页

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));   // ← 必须注册
        return interceptor;
    }
}
```

```java
Page<User> page = new Page<>(pageNum, pageSize);
userMapper.selectPage(page, wrapper);
// page.getRecords()  当前页数据
// page.getTotal()    总条数
```

::: danger 不注册分页插件，`selectPage` 会静默返回全量数据
**这是 MyBatis-Plus 最坑的一个点**：没有 `PaginationInnerInterceptor` 时，`selectPage` **不报错**，但 SQL 里**不会加 `LIMIT`**，直接把全表查出来。

数据量小的时候完全看不出来，上生产后一次查询拖垮数据库。

**验证方法**：把 `log-impl` 配成 `StdOutImpl`，看控制台打印的 SQL 里有没有 `LIMIT`。
:::

### 2.6 什么时候必须写 XML

| 场景 | 为什么 Lambda 不够 |
|---|---|
| **多表联查** | `BaseMapper` 只管单表，join 要自己写 |
| **聚合统计** | `GROUP BY` + `HAVING` + 多列聚合，Wrapper 表达力不足 |
| **复杂子查询** | `EXISTS`、相关子查询 |
| **需要精细控制 SQL** | 要强制走某个索引（`FORCE INDEX`）、要用特定的 SQL 写法 |
| **批量操作优化** | 自定义 `INSERT ... ON DUPLICATE KEY UPDATE` |

**XML 的取舍**：写 XML 意味着 SQL 和 Java 分离在两类文件里，维护时要在两边跳。所以**只有复杂 SQL 才写 XML**，简单单表一律走 Lambda。

### 2.7 与 02/05 模块的衔接

- **02 配置**：`mybatis-plus.*` 的配置项正好可以按环境区分 —— 开发环境开 SQL 日志，生产关闭
- **05 MySQL**：数据源配置直接复用；`DbType.MYSQL` 指的就是 05 里建的库

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `MybatisApplication`（带 `@MapperScan`） |
| 代码 | `entity/User`（带 `@TableName` / `@TableId` / `@TableLogic`） |
| 代码 | `mapper/UserMapper extends BaseMapper<User>` |
| 代码 | `config/MybatisPlusConfig`（分页插件） |
| 代码 | `UserService` / `UserController`：CRUD + 条件查询 + 分页 |
| 代码 | 一段用 XML 实现的**多表联查**（对比 Lambda 的边界） |
| 笔记 | **分页插件前后的 SQL 对比**（有 `LIMIT` vs 没有） |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8006 端口
- [ ] 引 `mybatis-plus-spring-boot3-starter` 依赖（注意是 **boot3** 版本）
- [ ] 写 `User` 实体，配 `@TableName` / `@TableId(type=AUTO)` / `@TableLogic`
- [ ] 写 `UserMapper`，验证 `BaseMapper` 的 17 个方法可直接用
- [ ] 注册分页插件，实测 `selectPage` 的 SQL 里**有** `LIMIT`
- [ ] **故意注释掉分页插件**，实测 SQL 里**没有** `LIMIT`（这是踩坑素材）
- [ ] 用 `LambdaQueryWrapper` 写一个多条件动态查询接口
- [ ] 写一段 XML 多表联查，对比「什么时候 Lambda 不够」
- [ ] 实测 `updateById` 无法把字段置 null，写出解法
- [ ] 贴真实输出：控制台打印的 SQL、分页结果、联查结果
- [ ] 写踩坑：分页插件未注册、`@MapperScan` 路径错、驼峰映射失效
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| 用原生 MyBatis 还是 MyBatis-Plus？ | Plus（单表省事，复杂 SQL 仍可写） |
| 主键策略 | `AUTO`（数据库自增，简单）/ `ASSIGN_ID`（雪花，分布式） |
| 逻辑删除？ | 要（生产实践），配 `logic-delete-field` |
| 实体直接当接口返回体？ | 不建议（暴露表结构），用 VO 转换 |
| Service 层用 `ServiceImpl<M,T>` 还是自己写？ | Plus 的 `IService` 提供了更多批量方法，可用可不用 |
| SQL 日志开发开吗？ | 开（`StdOutImpl`），但**生产必须关**（性能 + 信息泄露） |

## 6. 预习要点

1. **ORM 的核心矛盾**：抽象越强，SQL 越不可控；抽象越弱，代码越啰嗦。MyBatis 选了中间路线
2. **`#{}` 和 `${}` 的区别**（MyBatis 层）：
   - `#{}` → 预编译占位符 `?`，**防注入**，用于参数值
   - `${}` → 字符串直接拼接，**有注入风险**，只用于表名/字段名这类 SQL 结构（且必须白名单校验）
3. **`@TableField`**：Java 字段名和数据库列名对不上时用（`@TableField("user_name")`）
4. **`@TableId`**：标记主键字段，可指定 `type`（主键生成策略）

## 下一步

[07 消息队列](/backend/07-mq) —— 数据能存了，接下来处理「不该同步做的事」。
