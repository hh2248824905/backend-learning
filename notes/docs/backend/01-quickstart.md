# 01 快速入门

<NoteStatus level="done" />

::: info 模块坐标
源码：`E:\houduan\01-quickstart`　·　端口：`8080`　·　包名：`com.itheima`（历史遗留，见 [规范约定](/guide/conventions#_1-1-包名)）
:::

## 0. 本篇要解决的问题

1. 一个 Spring Boot 项目最少需要哪些东西才能跑起来？
2. 为什么没有 `web.xml`、没有外置 Tomcat，`main` 方法一跑就能访问 HTTP 接口？
3. `@SpringBootApplication` 这一个注解到底做了什么？
4. 为什么依赖不用写版本号？
5. 怎么在 3 分钟内新建一个返回 JSON 的接口？

## 1. 核心概念

### 1.1 起步依赖（Starter）

传统 Spring 项目里，光是配一个 Web 功能就要引一堆 jar 并保证版本互相兼容。Spring Boot 的做法是把**一组功能相关的依赖打包成一个 starter**：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 注意：没有 <version> -->
</dependency>
```

`spring-boot-starter-web` 一个坐标，实际带进来的是一整棵依赖树：

```
spring-boot-starter-web
├── spring-boot-starter            ← 核心：自动配置、日志、YAML 支持
├── spring-boot-starter-json       ← Jackson：对象 ↔ JSON
├── spring-boot-starter-tomcat     ← 内嵌 Tomcat
└── spring-webmvc                  ← Spring MVC
```

**没有 `<version>` 是因为父工程统一管了**：

```
spring-boot-starter-parent:3.5.16
   └── spring-boot-dependencies:3.5.16   ← 这里锁定了上千个依赖的版本
```

这是 Spring Boot 解决「版本地狱」的核心手段：**版本在继承链里定一次，所有子模块和 starter 都不再写版本号**。

### 1.2 自动配置：为什么不用写 web.xml

Spring Boot 启动时会做一件事：**扫描所有依赖 jar 里的自动配置类，按条件决定装不装**。

以「内嵌 Tomcat 自动启动」为例，大致逻辑是：

| 步骤 | 做什么 | 关键条件 |
|---|---|---|
| 1 | 扫描 `META-INF/spring/…AutoConfiguration.imports` | 这是 Spring Boot 3 的自动配置注册文件 |
| 2 | 找到 `ServletWebServerFactoryAutoConfiguration` | `@ConditionalOnClass`：classpath 里有 Servlet 相关类才生效 |
| 3 | 发现有 `tomcat-embed-core` | 于是创建 Tomcat 容器工厂 |
| 4 | 读 `server.port` | 没配就用默认 8080 |
| 5 | 启动 Tomcat，注册 DispatcherServlet | 应用就能收 HTTP 请求了 |

::: tip 一句话理解自动配置
**「你加了什么依赖，我就替你配什么」**。加了 Web starter 就自动配 Tomcat + Spring MVC；加了 MySQL 驱动就自动配数据源。

因为它按 `@ConditionalOnClass`（classpath 里有某个类才生效）来判断，所以你没加的依赖不会瞎配。
:::

### 1.3 `@SpringBootApplication` 拆开是什么

这个注解不是「一个」注解，是三个注解的合成：

```java
@SpringBootApplication
// 等价于下面三个

@SpringBootConfiguration    // 本质是 @Configuration，声明这是个配置类
@EnableAutoConfiguration    // 开启自动配置（上面那套机制的总开关）
@ComponentScan              // 扫描当前包及子包下的 @Component/@Service/@Controller
```

**`@ComponentScan` 的作用范围很关键**：默认只扫**启动类所在包及其子包**。

这解释了一个常见问题：把 Controller 写到启动类的**兄弟包**或**上级包**，注解扫不到，接口 404。

```
com.itheima
├── QuickStartApplication        ← 启动类在 com.itheima
├── controller/UserController    ← ✅ 会被扫到
└── entity/User                  ← ✅ 会被扫到

xxx.other
└── OtherController              ← ❌ 扫不到，接口 404
```

## 2. 代码实现

### 2.1 启动类

```java [QuickStartApplication.java]
package com.itheima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuickStartApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuickStartApplication.class, args);
    }
}
```

`main` 方法里没写任何"启动服务器"的代码 —— 那是自动配置干的。这一行 `SpringApplication.run()` 就完成：创建 Spring 容器 → 触发自动配置 → 启动内嵌 Tomcat → 注册 DispatcherServlet。

### 2.2 实体类

```java [User.java]
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /** 用户 ID（包装类型 Long） */
    private Long id;
    /** 用户名 */
    private String username;
    /** 年龄（包装类型 Integer） */
    private Integer age;
    /** 生日（JDK 8 时间 API） */
    private LocalDate birthday;
}
```

三个 Lombok 注解的效果：

| 注解 | 生成什么 |
|---|---|
| `@Data` | getter / setter / `toString` / `equals` / `hashCode` |
| `@NoArgsConstructor` | 无参构造（Jackson 反序列化需要） |
| `@AllArgsConstructor` | 全参构造（方便 `new User(1L, "张三", 25, ...)`） |

::: tip 为什么字段用包装类型 `Long` / `Integer` 而不是 `long` / `int`
基本类型有默认值（`0`），无法表达「这个字段没值」。用包装类型，`null` 就能明确表示"未设置"。

在实体类、返回体、可选参数里一律用包装类型，这是 Java 后端的通行做法。
:::

### 2.3 控制器

```java [UserController.java]
@RestController
public class UserController {

    @GetMapping("/user/info")
    public User getUserInfo() {
        return new User(1L, "张三", 25, LocalDate.of(2001, 5, 20));
    }
}
```

两个注解搞定一个接口：

| 注解 | 作用 |
|---|---|
| `@RestController` | `@Controller` + `@ResponseBody` 的合成。方法返回值**直接序列化成 JSON 写进响应体**，而不是当成视图名去找 HTML 模板 |
| `@GetMapping("/user/info")` | 把 HTTP GET 请求映射到该方法。等价于 `@RequestMapping(value="/user/info", method=RequestMethod.GET)` |

**返回值 `User` 对象是怎么变成 JSON 的**：`spring-boot-starter-web` 带进来了 Jackson，`@ResponseBody` 触发 `HttpMessageConverter`，Jackson 把对象序列化。所以只要返回一个 Java 对象，框架自动处理 JSON 转换，不用手写 `JSON.toJSONString()`。

### 2.4 配置文件

```yaml [application.yml]
server:
  port: 8080

spring:
  application:
    name: 01-quickstart

app:
  name: backend-learning
  version: 0.0.1-SNAPSHOT
  description: Spring Boot 多模块项目演示
```

`spring.application.name` 这个名字会出现在**日志的方括号里**：

```
INFO 47328 --- [01-quickstart] [main] ...
              ^^^^^^^^^^^^^^ 这里
```

多模块项目里这个值很有用 —— 同时起了几个服务时，从日志前缀就能分辨是哪个应用打的。

### 2.5 模块 pom

```xml [pom.xml]
<parent>
    <groupId>top.a1788</groupId>
    <artifactId>backend-learning</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>01-quickstart</artifactId>
```

**子模块不写 `<groupId>` 和 `<version>`**，继承父工程的。只声明自己的 `artifactId`。

打包插件部分有个值得注意的细节：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

**为什么要把 Lombok 排除出可执行 jar**：Lombok 只在**编译期**工作（生成 getter/setter 字节码），运行时不需要它。打进去会让 jar 白胖一圈。这是 Spring Initializr 生成项目的默认做法。

## 3. 运行验证

### 3.1 启动

```bash
cd E:\houduan
mvn clean package -DskipTests
java -jar 01-quickstart/target/01-quickstart-0.0.1-SNAPSHOT.jar
```

::: warning Git Bash 下 `mvn` 命令有问题
见 [开发环境 · Maven 问题](/guide/env#_2-4-git-bash-下-mvn-脚本的问题)。IDEA 里用 Maven 面板点 `package` 不受影响。
:::

### 3.2 启动日志（真实输出）

```
2026-09-16T11:35:14.733+08:00  INFO 47328 --- [01-quickstart] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2026-09-16T11:35:14.744+08:00  INFO 47328 --- [01-quickstart] [           main] com.itheima.QuickStartApplication        : Started QuickStartApplication in 1.622 seconds (process running for 1.957)
```

这条日志把启动过程说清楚了：

| 日志片段 | 含义 |
|---|---|
| `Tomcat started on port 8080 (http)` | 内嵌 Tomcat 起来了，监听 8080 |
| `with context path '/'` | 根路径上下文，所以接口是 `/user/info` 而不是 `/app/user/info` |
| `[01-quickstart]` | `spring.application.name` 的值 |
| `Started ... in 1.622 seconds` | 整个应用启动耗时 1.6 秒 |

**1.6 秒启动**是 Spring Boot 相对传统 Spring 的直观优势 —— 改一行代码重启，等两秒就行。

### 3.3 接口返回（真实输出）

```bash
curl http://localhost:8080/user/info
```

```json
{"id":1,"username":"张三","age":25,"birthday":"2001-05-20"}
```

**三个细节值得注意**：

1. **字段顺序**：`id → username → age → birthday`，和类里声明顺序一致（Jackson 默认按字段声明顺序输出）
2. **`LocalDate` 序列化成了字符串** `"2001-05-20"`，不是时间戳数字 —— 这是 `jackson-datatype-jsr310` 模块（被 `starter-web` 间接引入）的默认行为
3. **中文没乱码**：`张三` 正常显示。如果乱码，是响应头 `Content-Type` 少了 `charset=UTF-8`，Spring Boot 3 默认已经处理好

## 4. 与其他方式的对比

### 4.1 传统 Spring vs Spring Boot

| 维度 | 传统 Spring MVC | Spring Boot |
|---|---|---|
| 依赖管理 | 手动引 jar，自己解决版本冲突 | starter 一键引入，父工程锁版本 |
| 配置 | 大量 XML / `@Configuration` 手写 Bean | 自动配置，按需覆盖 |
| 服务器 | 外置 Tomcat，部署 war 包 | 内嵌 Tomcat，`java -jar` 直接跑 |
| 起步成本 | 半天配环境 | 3 分钟建项目 |

**代价**：自动配置是「约定优于配置」，出了问题要看懂它的判断条件（`@ConditionalOn*`）才能定位。这也是后面要学"自定义配置覆盖自动配置"的原因。

### 4.2 `@Controller` vs `@RestController`

| 注解 | 方法返回值含义 | 用于 |
|---|---|---|
| `@Controller` | 视图名，去找模板渲染 HTML | 传统前后端不分离（JSP/Thymeleaf） |
| `@RestController` | 数据对象，直接序列化成 JSON/XML | 前后端分离，提供 REST API |

**本项目的所有模块都用 `@RestController`** —— 前端是 Vue 单页应用，后端只提供 JSON 接口，不渲染页面。

## 5. 踩过的坑

### 5.1 常见的 404：Controller 不在扫描范围内

**现象**：接口写好了，启动正常，访问报 404。

**原因**：`@ComponentScan` 只扫启动类所在包及子包。

**排查**：看 Controller 的包名是不是启动类包名或其子包。比如启动类在 `top.a1788.config`，Controller 写在 `top.a1788.web` 就是兄弟包，扫不到。

**解法**：把 Controller 移到启动类子包下，或显式指定扫描范围：

```java
@SpringBootApplication(scanBasePackages = "top.a1788")
```

### 5.2 端口被占用

**现象**：

```
Web server failed to start. Port 8080 was already in use.
```

**原因**：上一个应用没关干净。开发时很常见 —— IDE 里点了停止但进程没退。

**排查**：

```bash
netstat -ano | findstr 8080
```

拿到最后一列的 PID，然后 `taskkill /F /PID <PID>`。

**预防**：用命令行参数临时换端口，不动配置文件：

```bash
java -jar app.jar --server.port=9080
```

### 5.3 模块间依赖不清导致的重构

这个项目早期 `01-quickstart` 依赖过 `02-config` 里的 `AppConfig`，后来为了「一个模块自包含」把那条依赖删了，`/app/info` 端点也随之删除。

**教训**：**模块之间尽量零依赖**。跨模块引用会让单模块无法独立启动和测试，改一个模块要连带测另一个。确实需要共用代码时，抽出独立的 `common` 模块，而不是让 A 依赖 B。

## 6. 小结与自检

### 核心结论

1. Spring Boot 最小可运行项目 = 一个启动类 + 一个 Web starter
2. starter 解决依赖引入，父工程解决版本管理，自动配置解决装配
3. `@SpringBootApplication` = 配置类 + 自动配置开关 + 组件扫描
4. 内嵌 Tomcat 让 `java -jar` 就能起服务
5. `@RestController` 的返回值直接变 JSON，不用手动序列化

### 自检清单

能回答下面这些，本模块就算过关：

- [ ] `@SpringBootApplication` 拆开是哪三个注解？各自作用是什么？
- [ ] 为什么 `spring-boot-starter-web` 不用写 `<version>`？
- [ ] Controller 写在启动类的兄弟包下会发生什么？为什么？
- [ ] 返回 `User` 对象时，是谁把它变成 JSON 的？
- [ ] 怎么在不改配置文件的前提下换端口？
- [ ] 实体类字段为什么推荐用 `Integer` 而不是 `int`？

### 下一步

[02 配置管理](/backend/02-config/) —— 把 `application.yml` 从「会写」提升到「会用」：注入方式、多环境、配置校验。
