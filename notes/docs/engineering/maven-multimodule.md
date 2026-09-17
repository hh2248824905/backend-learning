# Maven 多模块工程

<NoteStatus level="done" />

::: info 关联资产
父 pom：`E:\houduan\pom.xml`　·　子模块：`01-quickstart/pom.xml`、`02-config/pom.xml`　·　仓库：[backend-learning](https://github.com/hh2248824905/backend-learning)
:::

## 0. 为什么要拆多模块

单模块项目的死法：所有代码堆在一个 `src` 里，`controller` 依赖 `service` 依赖 `mapper`，改一个实体类全项目重编译；想复用登录逻辑给另一个项目，只能复制粘贴。

多模块的核心价值是**边界**：模块之间谁能依赖谁，由 pom 显式声明，编译器帮你守规矩。

| | 单模块 | 多模块 |
|---|---|---|
| 编译范围 | 改一行全量重编 | 只重编受影响模块 |
| 依赖约束 | 靠自觉 | 不声明就用不了（编译报错） |
| 复用 | 复制粘贴 | 引用模块坐标 |
| 版本管理 | 每个依赖各写各的 | 父工程一处统一 |

## 1. 目录结构

```
E:\houduan\                    ← 工程根（聚合工程）
├── pom.xml                    ← 父 pom（packaging=pom，无 src）
├── .gitattributes
├── .gitignore
├── 01-quickstart\             ← 子模块（独立 Maven 工程）
│   ├── pom.xml
│   └── src\main\java\...
├── 02-config\
│   ├── pom.xml
│   └── src\main\java\...
├── 03-logging\ ... 13-actuator\   ← 空模块（仅 .gitkeep 占位）
└── README.md
```

**父工程没有 `src` 目录**——它不产出 jar，只负责聚合与统一配置。`packaging` 必须是 `pom`。

## 2. 父 pom 逐段解析

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
</parent>

<groupId>top.a1788</groupId>
<artifactId>backend-learning</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>pom</packaging>
<name>backend-learning</name>
```

| 配置 | 含义 |
|---|---|
| `<parent>` | 继承 **Spring Boot 官方父工程**，它内部管理了所有 Starter 的版本（这就是子模块依赖不写 `<version>` 的原因） |
| `<relativePath/>` | 空值 = **不从本地相对路径找父 pom**，强制从远程仓库解析。写 `<relativePath/>` 是为了避免「本地碰巧有同名 pom」导致的诡异问题 |
| `<packaging>pom</packaging>` | 声明这是个聚合/父工程，不打 jar |
| `<groupId>top.a1788</groupId>` | 本地工程自己的坐标，与 Boot 官方父工程无关——一个 pom 可以有自己的坐标，同时继承别人的 parent |

### 2.1 统一属性

```xml
<properties>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

`java.version` 是 Boot 父工程约定的属性名，写在这里会同时控制 `maven-compiler-plugin` 的 source/target。`UTF-8` 必须写——不写的话 Windows 默认 GBK，中文字符串在日志里全是乱码。

> 本机实际是 JDK 21 编译 target 17，属于「高版本 JDK 编译低版本字节码」，合法且常见。

### 2.2 子模块列表

```xml
<modules>
    <module>01-quickstart</module>
    <module>02-config</module>
</modules>
```

- 每个 `<module>` 是**目录名**（相对父 pom 所在目录），不是 artifactId
- 没列进 `<modules>` 的子模块会被 Maven 完全忽略——「改了代码没生效」的头号原因
- Maven 会自动做**依赖排序**（reactive：依赖者后构建），不用手排顺序

### 2.3 依赖版本统一管理

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.a1788</groupId>
            <artifactId>02-config</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

⚠️ **`dependencyManagement` 只声明版本，不引入依赖**。子模块要真用，还得在 `<dependencies>` 里写一遍（但可以不写 `<version>`）：

```xml
<!-- 子模块里：想用 02-config -->
<dependency>
    <groupId>top.a1788</groupId>
    <artifactId>02-config</artifactId>
    <!-- 版本由父工程的 dependencyManagement 决定，这里不写 -->
</dependency>
```

| 标签 | 行为 |
|---|---|
| `<dependencies>` | **真引入** jar 到 classpath |
| `<dependencyManagement>` | 只在版本冲突时生效：管理「写出来但没写版本的依赖」该用哪个版本 |
| `<pluginManagement>` | 同上，作用于插件 |

这是 Maven 最容易搞混的一对。记法：**`Management` = 只管版本，不管引入**。

## 3. 子模块 pom

```xml
<parent>
    <groupId>top.a1788</groupId>
    <artifactId>backend-learning</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>01-quickstart</artifactId>
<name>01-quickstart</name>
```

三个要点：

1. **`<parent>` 不带 `<relativePath/>`** —— 子模块默认就是「找上级目录的 pom」，空值反而会破坏解析
2. **子模块不写 `<groupId>` 和 `<version>`** —— 从 parent 继承，只在需要不一致时才写
3. **必须写 `<artifactId>`** —— 这是它自己唯一不能继承的坐标

### 3.1 可执行 jar 的打包插件

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

- `spring-boot-maven-plugin` 把普通 jar 重打成 **fat jar**（内嵌 Tomcat + 所有依赖），`java -jar` 才能直接跑
- 排除 Lombok：它只在编译期生成代码，运行期不需要，打进 fat jar 只会让它大一圈
- `<version>` 不写：Boot 父工程已在 `pluginManagement` 里管好

**双启动类的坑**：如果一个模块里有**两个** `@SpringBootApplication`，这个插件会报错终止：

```
Unable to find a single main class from the following candidates [com.itheima.Application, top.a1788.config.ConfigApplication]
```

解法见 [踩坑与排错记录](/engineering/troubleshooting#_2-双启动类导致打包失败)。

## 4. 构建命令

::: code-group

```bash [全量构建]
mvn clean install            # 清理 → 编译 → 测试 → 打包 → 装进本地仓库
```

```bash [只构建指定模块]
mvn -pl 02-config -am clean package     # -pl 指定模块，-am 连带它的依赖模块
```

```bash [跳过测试]
mvn clean package -DskipTests           # 打包但不跑测试
mvn clean package -Dmaven.test.skip=true # 连测试代码都不编译
```

```bash [只看某个模块的测试]
mvn -pl 02-config test -Dtest=StudentPropertiesTest
```

:::

| 参数 | 含义 |
|---|---|
| `-pl` / `--projects` | 只处理列出的模块（逗号分隔） |
| `-am` / `--also-make` | 同时构建所选模块**依赖的**模块 |
| `-amd` / `--also-make-dependents` | 同时构建**依赖所选模块的**模块 |
| `-DskipTests` | 跳过执行测试，但仍编译测试代码 |
| `-Dmaven.test.skip=true` | 完全跳过测试（编译也不做），更快但可能掩盖编译错误 |

**`install` 与 `package` 的区别**：`package` 只把 jar 放到本模块的 `target/`；`install` 还会把它装进本地仓库（`D:\tools\maven_jar`），这样其他模块（或别的工程）才能通过坐标引用到它。多模块本地联调时，**装过一次 `install` 才不会报「找不到符号」**。

## 5. 本机 Maven 环境

| 项 | 值 |
|---|---|
| Maven 安装目录 | `F:\heima\apache-maven-3.9.16` |
| 本地仓库 | `D:\tools\maven_jar` |
| JDK | 21（`E:\jdk`） |
| 目标字节码 | 17 |

::: warning Git Bash 下 `mvn` 命令是坏的
本机 `mvn` 脚本在 Git Bash 环境下拼不出正确 classpath，报 `找不到或无法加载主类 org.codehaus.plexus.classworlds.launcher.Launcher`。

**绕过方案**（直接调 Maven 内核，注意把 `maven.multiModuleProjectDirectory` 换成项目绝对路径）：

```bash
cd /e/houduan && java -classpath "F:\heima\apache-maven-3.9.16\boot\plexus-classworlds-2.11.0.jar" \
  "-Dmaven.multiModuleProjectDirectory=E:\houduan" \
  -Dclassworlds.conf="F:\heima\apache-maven-3.9.16\bin\m2.conf" \
  -Dmaven.home="F:\heima\apache-maven-3.9.16" \
  -Dlibrary.jansi.path="F:\heima\apache-maven-3.9.16\lib\jansi-native" \
  org.codehaus.plexus.classworlds.launcher.Launcher test
```

IDEA 不受影响（它自己解析 Maven），只有终端手敲 `mvn` 会踩。改用 `mvn.cmd`（PowerShell / CMD 下）也正常。
:::

## 6. 相关篇章

- 空目录怎么进 git：[Git 工作流 - 空目录](/engineering/git-workflow#_5-空目录与-gitkeep)
- IDEA 里怎么导入这个多模块工程：[IDEA 工程配置](/engineering/idea-setup)
- `mvn` 报错原文与排查：[踩坑与排错记录](/engineering/troubleshooting)
