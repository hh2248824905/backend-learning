# @ConfigurationProperties

<NoteStatus level="done" />

## 0. 本篇要解决的问题

1. 十几个配置项要注入，一个个写 `@Value` 太啰嗦，怎么一次性绑？
2. yml 里的 List、Map、嵌套对象、对象列表怎么绑到 Java 结构上？
3. `max-count` 是短横线，Java 字段是 `maxCount` 驼峰，为什么能匹配上？
4. 配置类怎么注册成 Bean？`@Component` 和 `@EnableConfigurationProperties` 有什么区别？
5. 为什么视频里测试报 `No bean of type 'StudentProperties'`？

## 1. 核心用法

### 1.1 最小可运行的三件套

```java [StudentProperties.java]
@Data                                    // ① Lombok 生成 getter/setter（绑定必须要有 setter）
@Component                               // ② 注册成 Spring Bean
@ConfigurationProperties(prefix = "student")   // ③ 声明绑定 yml 里的 student 前缀
public class StudentProperties {
    private String name;
    private Integer age;
}
```

```yaml [application.yml]
student:
  name: 张三
  age: 18
```

三者缺一不可：

| 元素 | 缺了会怎样 |
|---|---|
| `@Data`（或手写 setter） | 绑定静默失败，字段全是 `null` |
| `@Component` | Bean 不存在，注入时 `NoSuchBeanDefinitionException` |
| `@ConfigurationProperties(prefix)` | 不知道绑哪一块，字段全是 `null` |

::: danger 视频里的报错就是这个原因
```
No bean of type 'StudentProperties'
```

配置属性类**不会自动成为 Bean**，必须显式注册。两种方式：

**方式一（本项目用的）**：在类上加 `@Component`

```java
@Component
@ConfigurationProperties(prefix = "student")
public class StudentProperties { }
```

**方式二**：在启动类或配置类上加 `@EnableConfigurationProperties`

```java
@SpringBootApplication
@EnableConfigurationProperties(StudentProperties.class)
public class ConfigApplication { }
```

**区别**：方式一要求这个类能被 `@ComponentScan` 扫到（在启动类的包或子包下）；方式二不受扫包范围限制，适合配置类在第三方包里的场景。

方式一更常见，因为代码更少。
:::

### 1.2 绑定靠的是 setter

这一点容易忽略：`@ConfigurationProperties` 的绑定机制是**调 setter 方法**。

```java
// 相当于 Spring 内部做了这件事
StudentProperties bean = new StudentProperties();
bean.setName(env.getProperty("student.name"));
bean.setAge(...);
```

所以：

- 必须有**无参构造**（Lombok `@Data` 不生成构造器时，Java 默认有；但如果加了 `@AllArgsConstructor` 就必须补 `@NoArgsConstructor`）
- 必须有**setter**（`@Data` 提供了）

**例外（Spring Boot 3 支持的一种更优雅的写法）**：构造器绑定。用 `@ConstructorBinding` 或直接写全参构造，字段可以是 `final`：

```java
@ConfigurationProperties(prefix = "student")
public class StudentProperties {
    private final String name;
    private final Integer age;

    public StudentProperties(String name, Integer age) {   // 单构造器时可省略 @ConstructorBinding
        this.name = name;
        this.age = age;
    }
}
```

好处是对象**创建后不可变**。代价是不能用 `@Component` 注册（因为构造器绑定要求由 Spring 通过 `@EnableConfigurationProperties` 实例化）。

## 2. 四种数据形态的绑定

这是本篇的重点。yml 里的结构五花八门，Java 侧怎么写才对得上。

### 2.1 完整配置

```yaml [application.yml]
student:
  name: 张三
  age: 18

  # 形态二：List —— 用 "- 值" 列表语法
  hobbies:
    - 篮球
    - 编程
    - 阅读

  # 形态三：Map —— 用 "key: value" 语法
  scores:
    chinese: 90
    math: 95
    english: 88

  # 形态四a：嵌套对象
  address:
    province: 江苏省
    city: 南京市

  # 形态四b：对象列表 —— 列表每项都是完整对象
  courses:
    - name: 高等数学
      credit: 4
    - name: 大学英语
      credit: 3
```

### 2.2 对应 Java 类

```java [StudentProperties.java]
@Data
@Component
@ConfigurationProperties(prefix = "student")
public class StudentProperties {

    /** 形态一：基本属性 */
    private String name;
    private Integer age;

    /** 形态二：List 集合 */
    private List<String> hobbies;

    /** 形态三：Map 集合 */
    private Map<String, Integer> scores;

    /** 形态四a：嵌套对象（初始化防止空指针） */
    private Address address = new Address();

    /** 形态四b：对象列表 */
    private List<Course> courses;

    @Data
    public static class Address {
        private String province;
        private String city;
    }

    @Data
    public static class Course {
        private String name;
        private Integer credit;
    }
}
```

### 2.3 形态对照表

| 形态 | yml 写法 | Java 类型 | 要点 |
|---|---|---|---|
| 基本属性 | `name: 张三` | `String` / `Integer` | 类型自动转换 |
| List | `hobbies:` + `- 篮球` | `List<String>` | 短横线行必须是列表项，缩进对齐 |
| Map | `scores:` + `chinese: 90` | `Map<String, Integer>` | key 随意，value 类型由泛型决定 |
| 嵌套对象 | `address:` + `province: 江苏省` | 静态内部类 | 内部类要是 `static`，否则无法实例化 |
| 对象列表 | `courses:` + `- name: xx` + `credit: 4` | `List<内部类>` | `-` 后接多个键值对 |

::: warning 三个写 yml 容易错的地方

**① List 和 Map 的语法区别**

```yaml
# List：每一项前面有短横线
hobbies:
  - 篮球
  - 编程

# Map：是 key: value 结构，没有短横线
scores:
  chinese: 90
  math: 95
```

把 Map 写成 List 的格式，或者反过来，绑定会失败或得到空集合。

**② 嵌套类必须是 static**

```java
public static class Address { }   // ✅ Spring 能实例化

public class Address { }          // ❌ 非静态内部类需要外部类实例，绑定失败
```

**③ 嵌套对象建议给默认值**

```java
private Address address = new Address();   // ✅ 即使 yml 里没配 address，也不会 NPE
private Address address;                   // ⚠️ yml 里没配就是 null，getAddress().getCity() 直接 NPE
```

`@ConfigurationProperties` 在 yml 里**没有**对应配置块时，不会去 new 这个对象，字段就是 `null`。给个初始值更稳。
:::

### 2.4 运行验证（真实输出）

```java [StudentPropertiesTest.java]
@SpringBootTest
@Slf4j
public class StudentPropertiesTest {

    @Resource
    private StudentProperties studentProperties;

    @Test
    public void printStudentInfo() {
        log.info("学生信息：{}", studentProperties);
        log.info("学生姓名: {}", studentProperties.getName());
        log.info("学生语文成绩: {}", studentProperties.getScores().get("chinese"));
    }
}
```

测试输出：

```
学生信息：StudentProperties(name=张三, age=18, hobbies=[篮球, 编程, 阅读],
          scores={chinese=90, math=95, english=88},
          address=StudentProperties.Address(province=江苏省, city=南京市),
          courses=[StudentProperties.Course(name=高等数学, credit=4),
                   StudentProperties.Course(name=大学英语, credit=3)])
学生姓名: 张三
学生语文成绩: 90
```

**四种形态全部绑定成功**：

| 形态 | 输出片段 | 验证点 |
|---|---|---|
| 基本属性 | `name=张三, age=18` | 类型转换正常 |
| List | `hobbies=[篮球, 编程, 阅读]` | 顺序保持 |
| Map | `scores={chinese=90, math=95, english=88}` | key 和多类型 value 都对 |
| 嵌套对象 | `address=Address(province=江苏省, city=南京市)` | 内部类实例化成功 |
| 对象列表 | `courses=[Course(name=高等数学, credit=4), ...]` | 每项都是完整对象 |

> 输出里的 `StudentProperties.Address(...)` 是 Lombok `@Data` 生成的 `toString`，格式是 `简化类名(字段=值)`。嵌套类的简化名会带上外部类名。

### 2.5 顺带验证接口

```java [StudentController.java]
@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentProperties studentProperties;

    @GetMapping("/info")
    public StudentProperties getStudentInfo() {
        return studentProperties;
    }
}
```

```bash
curl http://localhost:8002/student/info
```

返回嵌套 JSON（截取）：

```json
{
  "name": "张三",
  "age": 18,
  "hobbies": ["篮球", "编程", "阅读"],
  "scores": { "chinese": 90, "math": 95, "english": 88 },
  "address": { "province": "江苏省", "city": "南京市" },
  "courses": [
    { "name": "高等数学", "credit": 4 },
    { "name": "大学英语", "credit": 3 }
  ]
}
```

**这里体现了一个实际价值**：配置类本身可以直接作为接口返回体，省掉「定义一个 DTO 再手动搬运字段」的活。当然对外的正式接口不建议直接暴露配置类（会把内部配置结构泄露出去），演示和联调够用。

## 3. 松散绑定（Relaxed Binding）

### 3.1 什么是松散绑定

同一个配置项，下面四种 key 写法**都能绑到** Java 的 `maxCount` 字段：

| yml 里的写法 | 能否绑定 `private Integer maxCount;` |
|---|---|
| `max-count`（kebab-case） | ✅ |
| `maxCount`（camelCase） | ✅ |
| `max_count`（snake_case） | ✅ |
| `MAX_COUNT`（大写下划线，常用于环境变量） | ✅ |

```yaml
app:
  max-count: 100      # 写短横线
```

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Integer maxCount;        // 驼峰字段，照样能绑上
}
```

### 3.2 为什么这个特性重要

三个理由：

| 理由 | 说明 |
|---|---|
| **yml 惯例是 kebab-case** | 官方配置全用短横线（`spring.jpa.hibernate.ddl-auto`）。松散绑定让你遵循惯例的同时，Java 侧仍用标准驼峰 |
| **环境变量只能大写下划线** | 环境变量名不允许短横线。`MAX_COUNT=100` 这种写法能自动映射到 `maxCount`，让「同一配置项通过环境变量注入」成为可能 |
| **`@Value` 没有这个能力** | 这是 `@Value` 和 `@ConfigurationProperties` 的关键差异之一 |

::: warning `@Value` 是严格匹配
```java
@Value("${app.maxCount}")     // ❌ 报 Could not resolve placeholder
private Integer maxCount;

@Value("${app.max-count}")    // ✅ 必须和 yml 里的 key 完全一致
private Integer maxCount;
```

**但只要这一条差异，就足够决定「一组配置该用哪个注解」了。**
:::

### 3.3 前缀匹配也是松散的

```yaml
my-app:
  name: xxx
```

```java
@ConfigurationProperties(prefix = "myApp")    // ✅ 也能匹配
```

`myApp` → `my-app` → `my_app` 互相等价。

## 4. IDE 元数据提示

配 `spring-boot-configuration-processor` 后，IDEA 里写 yml 会有**自动补全和类型提示**。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

**效果**：在 yml 里敲 `app.` 会自动弹出 `name` / `author` / `port` / `maxCount` 的候选列表，鼠标悬停能看到字段的 Javadoc 注释。

配了也不影响运行（`optional=true` 表示不传递依赖），属于「低成本高收益」的配置。

::: tip 这个依赖是可选的
本项目目前没配 —— 少了它只是没有 IDE 提示，功能完全不受影响。如果配置项变多（20+），建议补上，能省不少查字段名的时间。
:::

## 5. 踩过的坑

### 5.1 `No bean of type 'StudentProperties'`

**现象**：测试类里 `@Resource private StudentProperties studentProperties;` 注入后是 `null`，或者直接抛 `NoSuchBeanDefinitionException`。

**原因**：配置属性类没注册成 Bean。三种可能：

| 原因 | 检查 |
|---|---|
| 类上没加 `@Component` | 加上，或在启动类加 `@EnableConfigurationProperties(XxxProperties.class)` |
| 类不在 `@ComponentScan` 范围内 | 确认它的包是启动类包或其子包 |
| 测试类没加 `@SpringBootTest` | 没有这个注解，测试**不会启动 Spring 容器**，任何注入都是 `null` |

**第三种最隐蔽**：测试类上少了 `@SpringBootTest`，`@Resource` 不会被处理，字段永远是 `null`。

### 5.2 字段全是 null（绑定静默失败）

**现象**：Bean 能注入进来，但所有字段都是 `null`。

**排查清单**：

- [ ] `prefix` 写对了吗？（`student` 不是 `students`）
- [ ] yml 的缩进层级对吗？（缩进错了 key 路径就变了）
- [ ] 有 setter 吗？（只有 getter 绑不上）
- [ ] 有 `@Component` 或 `@EnableConfigurationProperties` 吗？
- [ ] 字段名和 yml 能松散匹配吗？（`userName` vs `user-name` 可以，`username` vs `user-name` **不可以** —— 松散绑定不拆分单词边界）

**最后一个坑很典型**：

```yaml
app:
  username: a1788
```

```java
private String userName;   // ❌ 绑不上！username ≠ user-name ≠ userName（这是不同的词）
private String username;   // ✅ 完全一致才能绑
```

### 5.3 嵌套对象空指针

**现象**：`studentProperties.getAddress().getCity()` 抛 NPE。

**原因**：yml 里没有 `student.address` 这一块，Spring 不会去 new 这个对象。

**解法**：

```java
private Address address = new Address();   // 给默认值
```

## 6. 小结与自检

### 核心结论

1. `@ConfigurationProperties` 把**同前缀的一组配置**一次性绑到一个 POJO 上
2. 三件套缺一不可：`@Data`（setter）+ `@Component`（Bean）+ `@ConfigurationProperties(prefix)`（绑哪块）
3. 支持 List / Map / 嵌套对象 / 对象列表四种复杂结构；嵌套类必须是 `static`
4. **松散绑定**是它相对 `@Value` 的核心优势：`max-count` ↔ `maxCount` ↔ `MAX_COUNT`
5. 配置类可以直接作为接口返回体，也可以配 `@Validated` 做校验（见下一篇）

### 自检清单

- [ ] `@Component` 和 `@EnableConfigurationProperties` 注册 Bean 有什么区别？
- [ ] yml 里 List 和 Map 的写法有什么区别？写错了会怎样？
- [ ] 为什么嵌套类必须是 `static`？
- [ ] yml 里写 `max-count`，Java 字段叫 `maxCount`，能绑上吗？为什么？
- [ ] 为什么 yml 里写 `username` 而 Java 字段叫 `userName` 就绑不上？
- [ ] 配置类绑定是靠 setter 还是字段反射？这个区别会导致什么实际问题？

### 下一步

[多环境与 Profile](/backend/02-config/profile) —— 同一份代码怎么跑出 dev / prod 两套配置。
