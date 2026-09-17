# 12 单元测试

<NoteStatus level="wip" />

::: info 模块坐标
源码：`E:\houduan\12-test`（**目录已建，仅有 `.gitkeep` 占位**）
已有实践：`houduan/02-config/src/test/java/top/a1788/config/properties/StudentPropertiesTest.java`
:::

> 代码状态 <NoteStatus level="wip" />：`02-config` 里已有一个跑通的测试作为起点，但 12 模块本身还没开始。
> 这一篇先给出**完整规划**和**已有实例的拆解**。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 怎么证明代码是对的，而不是「我点了一遍没问题」？
2. `@SpringBootTest`、`@WebMvcTest`、纯单元测试，启动成本差多少？怎么选？
3. 一个 Service 依赖了三个外部组件，测试时怎么隔离？
4. 接口测试怎么做？要真起一个 Tomcat 吗？
5. 什么时候**不该**写 `@SpringBootTest`？

## 1. 模块定位

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 06 MyBatis：数据层能用了 | **12：证明数据层/业务层真的对** | 所有后续改动的**安全网** |

**核心价值**：测试是**重构的前提**。没有测试，每次改动都在赌；有测试，改完跑一遍就知道有没有破坏原有功能。

## 2. 已有实例拆解

先看已经在运行的测试，理解基本结构：

```java [02-config/src/test/java/top/a1788/config/properties/StudentPropertiesTest.java]
@SpringBootTest           // ① 启动完整的 Spring 容器
@Slf4j                    // ② Lombok 提供 log 对象
public class StudentPropertiesTest {

    @Resource             // ③ 注入真实 Bean（从容器里拿）
    private StudentProperties studentProperties;

    @Test
    public void printStudentInfo() {
        log.info("学生信息：{}", studentProperties);
        log.info("学生姓名: {}", studentProperties.getName());
        log.info("学生语文成绩: {}", studentProperties.getScores().get("chinese"));

        assertEquals("张三", studentProperties.getName());
        assertEquals(90, studentProperties.getScores().get("chinese"));
    }
}
```

### 2.1 运行结果（真实输出）

```
2026-09-16T10:12:25.674+08:00  INFO 42648 --- [02-config] [main] t.a.c.properties.StudentPropertiesTest : Started StudentPropertiesTest in 0.958 seconds (process running for 1.653)
2026-09-16T10:12:26.098+08:00  INFO 42648 --- [02-config] [main] t.a.c.properties.StudentPropertiesTest : 学生信息：StudentProperties(name=张三, age=18, hobbies=[篮球, 编程, 阅读], scores={chinese=90, math=95, english=88}, address=StudentProperties.Address(province=江苏省, city=南京市), courses=[StudentProperties.Course(name=高等数学, credit=4), StudentProperties.Course(name=大学英语, credit=3)])
2026-09-16T10:12:26.098+08:00  INFO 42648 --- [02-config] [main] t.a.c.properties.StudentPropertiesTest : 学生姓名: 张三
2026-09-16T10:12:26.099+08:00  INFO 42648 --- [02-config] [main] t.a.c.properties.StudentPropertiesTest : 学生语文成绩: 90
```

**关键行**：`Started StudentPropertiesTest in 0.958 seconds` —— 一个 `@SpringBootTest` 用了**近 1 秒**启动容器。测试多了以后这个成本会累加。

### 2.2 解读这段日志里的三个「吓人但无害」的输出

| 输出 | 是什么 | 要处理吗 |
|---|---|---|
| `Could not detect default configuration classes ... does not declare @Configuration` | **INFO 级**提示。它在找测试类内部的静态配置类，没找到就向上找启动类 | 不用 |
| `Found @SpringBootConfiguration ... ConfigApplication` | **下一行就告诉你找到了** —— 这是我们想要的结果 | 不用 |
| `Mockito is currently self-attaching ...` / `WARNING: A Java agent has been loaded dynamically` | Spring Boot Test 内置 Mockito 在 JDK 21 上动态挂 agent 的提示，JDK 官方给未来版本留的提醒 | 不用 |
| `Sharing is only supported for boot loader classes` | JVM 的 CDS 提示，加了 agent 后必然出现 | 不用 |

::: tip 判断测试是否成功的唯一标准
**看最后一行「退出代码」**：

| 输出 | 含义 |
|---|---|
| `进程已结束，退出代码为 0` + 测试树绿勾 | ✅ 成功 |
| `退出代码为 1` + `AssertionFailedError` | ❌ 断言失败 |
| `退出代码为 1` + `NoSuchBeanDefinitionException` | ❌ Bean 找不到 |

**中间的 INFO/WARNING 一律不影响结果。** 想消掉 ByteBuddy 那条警告，在运行配置的 VM options 加 `-XX:+EnableDynamicAgentLoading`。
:::

## 3. 三种测试的取舍

这是本模块最核心的知识点 —— **不是所有测试都该起 Spring 容器**。

| 类型 | 注解 | 启动内容 | 耗时 | 适用 |
|---|---|---|---|---|
| **纯单元测试** | 无（`new` 对象） | 什么都不启动 | ~毫秒 | Service 里的纯计算逻辑、工具类 |
| **Slice 测试** | `@WebMvcTest` / `@DataJpaTest` | 只启动相关的**一片** | ~几百毫秒 | 只测 Controller 层 / 只测数据层 |
| **集成测试** | `@SpringBootTest` | **完整** Spring 容器 | **1~3 秒** | 需要验证多层协作、配置绑定 |

### 选型判断

```
被测逻辑有外部依赖（DB、HTTP、Redis）吗？
├─ 没有 → 纯单元测试（最快）
└─ 有
   ├─ 只关心 HTTP 层（参数绑定、状态码、JSON 格式）吗？
   │   └─ 是 → @WebMvcTest + Mock Service
   └─ 需要真实验证「配置 → Bean → 行为」的完整链路吗？
       └─ 是 → @SpringBootTest
```

### 什么时候**不该**用 `@SpringBootTest`

| 场景 | 为什么不该用 |
|---|---|
| **测一个纯计算方法**（比如金额计算） | 起容器纯属浪费，直接 `new` 被测对象 |
| **测工具类**（字符串、日期处理） | 同上 |
| **测一个分支逻辑** | 用 Mock 隔离依赖更快更稳 |
| 测试数量会很多 | 每个都 1 秒，100 个测试就是 100 秒，CI 会很慢 |

::: tip 一个实用建议：按「测试金字塔」分配
```
        /\
       /  \      少量 @SpringBootTest（端到端关键路径）
      /____\
     /      \    中等 @WebMvcTest（每个 Controller 一两个）
    /________\
   /          \  大量纯单元测试（Service 逻辑、工具类）
  /____________\
```

**底层多、顶层少**。反过来（什么都用 `@SpringBootTest`）会导致测试慢到没人愿意跑。
:::

## 4. 计划覆盖的知识点

### 4.1 断言

优先用 **AssertJ**（Spring Boot Test 已带），链式调用可读性更好：

```java
// JUnit 原生断言
assertEquals("张三", user.getName());

// AssertJ（推荐）
assertThat(user.getName()).isEqualTo("张三");
assertThat(user.getAge()).isGreaterThan(0);
assertThat(userList).hasSize(3).extracting("name").containsExactly("A", "B", "C");
assertThatThrownBy(() -> service.delete(-1))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("ID 不能为负");
```

### 4.2 Mock 与打桩

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;          // 假的 Mapper

    @InjectMocks
    private UserServiceImpl userService;     // 把 mock 注入进去

    @Test
    void shouldReturnNullWhenNotFound() {
        // given：打桩，规定 mock 的行为
        when(userMapper.selectById(999L)).thenReturn(null);

        // when：执行
        User result = userService.getById(999L);

        // then：验证
        assertThat(result).isNull();
        verify(userMapper, times(1)).selectById(999L);   // 验证被调用了 1 次
    }
}
```

| 常用方法 | 作用 |
|---|---|
| `when(...).thenReturn(...)` | 规定返回值 |
| `when(...).thenThrow(...)` | 规定抛异常 |
| `doNothing().when(...)` | void 方法打桩 |
| `verify(mock, times(n))` | 验证调用次数 |
| `ArgumentCaptor` | 捕获传入 mock 的参数做断言 |

### 4.3 测 Controller

```java
@WebMvcTest(UserController.class)        // 只启动 Web 层，不启动 Service/DB
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;              // 不起真实 Tomcat，模拟 HTTP

    @MockBean
    private UserService userService;      // Service 用 mock 替换

    @Test
    void shouldReturnUserWhenExists() throws Exception {
        when(userService.getById(1L)).thenReturn(new UserVO(1L, "张三"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("张三"));
    }
}
```

**`MockMvc` 的价值**：不用起真实服务器和端口，但能验证**完整的 Spring MVC 链路**（参数绑定、校验、序列化、状态码）。

### 4.4 测试命名与结构

```java
@Test
void shouldThrowExceptionWhenUsernameIsBlank() { }
```

**命名模式**：`should<期望结果>When<条件>`，读起来就是一句需求描述。

**结构**：`given / when / then` 三段，用空行或注释分隔：

```java
@Test
void shouldReturnZeroWhenListIsEmpty() {
    // given
    List<Order> orders = List.of();

    // when
    BigDecimal total = calculator.sum(orders);

    // then
    assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
}
```

### 4.5 测试数据与事务

```java
@SpringBootTest
@Transactional          // 每个测试方法结束后自动回滚，不污染数据库
class UserMapperTest { }
```

::: warning `@Transactional` 在测试里的两个陷阱
| 陷阱 | 说明 |
|---|---|
| **掩盖了事务问题** | 测试方法本身在一个事务里，被测代码的 `@Transactional` 行为会不同（传播行为、事务边界都可能被影响） |
| **异步代码看不到数据** | 异步线程在另一个事务里，读不到测试事务中未提交的数据 |

**需要真实事务行为时**，用 `@Sql` 或 `@BeforeEach` 手动插入 + `@AfterEach` 手动清理，不要用 `@Transactional`。
:::

### 4.6 测试覆盖率

IDEA 自带覆盖率统计（Run with Coverage）。也可以用 JaCoCo：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

::: tip 覆盖率是参考指标，不是目标
**不要为了 80% 覆盖率去写没意义的测试**（比如只 assert 不抛异常）。

| 该追求 | 不该追求 |
|---|---|
| **核心业务逻辑**有测试 | 让每个 getter/setter 都被覆盖 |
| **边界条件**有测试（空、null、最大值） | 数字好看 |
| **历史 bug** 补了回归测试 | 覆盖率 100% |

**判断标准**：这段代码如果改错了，有没有测试能发现它？
:::

## 5. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | 一个 Service 的纯单元测试（Mock 依赖，不起容器） |
| 代码 | 一个 Controller 的 `@WebMvcTest`（验证参数绑定 + 校验 + 返回结构） |
| 代码 | 一个 Mapper 的集成测试（真连数据库，验证 SQL） |
| 代码 | 一个异常路径的测试（`assertThatThrownBy`） |
| 配置 | JaCoCo 插件 + 覆盖率报告 |
| 笔记 | **三种测试的耗时实测对比**（贴真实的时间数字） |

## 6. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`（可选：12 也可以不建独立模块，测试就放在各模块里）
- [ ] 写一个纯单元测试（Service 逻辑 + Mock），记录耗时
- [ ] 写一个 `@WebMvcTest`，记录耗时
- [ ] 写一个 `@SpringBootTest`，记录耗时
- [ ] **贴三种测试的真实耗时对比表**（这是本篇最有说服力的内容）
- [ ] 写异常路径测试（参数非法、资源不存在）
- [ ] 配置 JaCoCo，跑出覆盖率报告
- [ ] 补一个 02-config 已有的测试到「正常 + 边界 + 异常」三类
- [ ] 写踩坑：`@SpringBootTest` 太慢、Mockito 警告、测试污染数据库
- [ ] 更新内容规划表与进度快照

## 7. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| 测试放哪 | 各模块自己的 `src/test/`（推荐）/ 集中到一个 12-test 模块 |
| 用 Mockito 还是集成测试 | 按「测试金字塔」：单元多、集成少 |
| 测数据库用真库还是内存库 | H2（快，但 SQL 方言有差异）/ Testcontainers（真实但慢）/ 真测试库（最准） |
| 测试数据怎么准备 | `@Sql` 脚本 / `@BeforeEach` 代码插入 / 共享 fixture |
| 覆盖率门槛 | 不设硬门槛（容易催生垃圾测试）/ 核心模块设 60~70% |

## 8. 预习要点

1. **测试的价值是「允许你重构」**，不是为了证明代码现在没 bug
2. **一个测试只验证一件事** —— 断言太多，失败了不知道是哪出了问题
3. **测试要能独立运行**，不依赖执行顺序，不依赖上一个测试留下的数据
4. **`@MockBean` vs `@Mock`**：前者替换 Spring 容器里的 Bean（配合 `@SpringBootTest`/`@WebMvcTest`），后者是纯 Mockito 对象

## 下一步

[13 监控运维](/backend/13-actuator) —— 测试保证功能对，监控保证线上活着。
