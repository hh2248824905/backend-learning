# 踩坑与排错记录

<NoteStatus level="done" text="6 个真实问题" />

::: info 这部分怎么用
下面是**真实卡住过的问题**，每条都带原始报错、原因、解法。按「现象」检索，不要按顺序读。

排查通用三步：
1. **看最后一行**——`退出代码为 0` 就是成功，前面红色 `WARNING` 未必是问题
2. **命令行复现一次**——IDEA 报错但 `mvn` 命令行能跑，说明是 IDE 缓存问题，不是代码问题
3. **看时间戳**——报的错是这次改的，还是三天前的旧现象？
:::

## 1. Mockito / ByteBuddy 警告被当成报错

**现象**：IDEA 跑单元测试，日志一大片红字，看着像崩了：

```
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer
work in future releases of the JDK. Please add Mockito as an agent to your build as
described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/...

WARNING: A Java agent has been loaded dynamically
         (D:\tools\maven_jar\net\bytebuddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading
         to hide this warning
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes
         because bootstrap classpath has been appended
```

**这是报错吗**：不是。**退出代码 0，测试通过**。

| 输出 | 级别 | 含义 |
|---|---|---|
| `Could not detect default configuration classes ... does not declare @Configuration` | INFO | 它先找测试类内部的静态配置类，没找到就向上找 `@SpringBootConfiguration`（下一行会打印 `Found @SpringBootConfiguration top.a1788.config.ConfigApplication`）——**这正是我们要的结果** |
| `Mockito is currently self-attaching` | 提示 | Spring Boot Test 内置 Mockito 在 JDK 21+ 上改用动态 agent 挂载，JDK 官方给未来版本留的兼容提醒 |
| `A Java agent has been loaded dynamically (byte-buddy-agent...)` | WARNING | 同上，ByteBuddy 动态挂载 agent；JDK 21 开始会警告 |
| `Sharing is only supported for boot loader classes` | WARNING | JVM 的 CDS 类共享机制提示，加了 agent 后必然出现 |

**判断成功的唯一标准**：

```
✅ 退出代码为 0  + 测试树上的绿色勾 + 日志里能看到你的业务输出
❌ 退出代码为 1  + AssertionFailedError / NoSuchBeanDefinitionException 之类的堆栈
```

**想消掉 ByteBuddy 那条警告**：运行配置的 `VM options` 加

```
-XX:+EnableDynamicAgentLoading
```

Mockito 那条是它自己打的，消不掉，也无害。

**教训**：日志的**颜色不代表严重程度**，`WARNING` 和 `ERROR` 是两个级别。别看到红字就慌，先找 `退出代码`。

---

## 2. 双启动类导致打包失败

**现象**：`mvn package` 直接 BUILD FAILURE：

```
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.5.16:repackage
        (default) on project 02-config: Unable to find a single main class from the following
        candidates [com.itheima.Application, top.a1788.config.ConfigApplication]
```

**原因**：`spring-boot-maven-plugin:repackage` 要把普通 jar 重打成可执行 fat jar，它需要知道「从哪个类启动」。模块里有两个 `@SpringBootApplication`，它无法决定。

**为什么会有两个**：项目从旧的 `com.itheima` 包结构迁到 `top.a1788`，旧代码按要求「保留不删」，于是并存了两个启动类。

**解法（保留两个类）**：pom 里显式指定打包用的主类：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>top.a1788.config.ConfigApplication</mainClass>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

这样 `<mainClass>` 只影响 **jar 打包**，IDEA 里两个类照样都能单独运行（点绿三角选入口）。

**但隐患还在**：

```
com.itheima.Application        → 扫 com.itheima.**
top.a1788.config.ConfigApplication → 扫 top.a1788.config.**
```

两个扫包范围不重叠。用 `com.itheima.Application` 启动时，`top.a1788` 的 `StudentProperties` **注入不进来**，会直接 `NoSuchBeanDefinitionException`。

**根本解法**：删掉多余的那套。迁移类项目就该一次迁干净，留一半只会让人踩两种坑。

---

## 3. IDEA 报 ClassNotFoundException

**现象**：IDEA 里点运行，直接挂：

```
错误: 找不到或无法加载主类 com.itheima.Application
原因: java.lang.ClassNotFoundException: com.itheima.Application
```

**先确认是不是真的没编译**：

```bash
ls /e/houduan/02-config/target/classes/com/itheima/     # 类文件在不在？
```

类文件**在**，说明代码和编译都没问题——**问题在 IDEA 的工程模型**。

**根因（两个叠加）**：

1. `.idea/modules.xml` 里只登记了一个**空模块** `houduan.iml`，`content url` 指向根目录但**没有任何源码根**；`workspace.xml` 里连一条 Maven 工程记录都没有
   → **IDEA 根本没把 `E:\houduan` 当 Maven 工程导入**，编译时没把 `com.itheima.Application` 放进 classpath
2. `.idea/` 里残留一堆**失效路径**：
   - `encodings.xml`、`misc.xml` 指向已被删除的 `multi-module-project/`
   - `compiler.xml` 里模块名还是重组前的 `01-quick-start`

这是**目录重组之后没重新导入**导致的：文件移动了，IDEA 的缓存还记着旧地址。

**排查命令**：

```bash
cd /e/houduan
find . -name "*.iml" -not -path "*/target/*"     # 模块文件在哪
cat .idea/modules.xml                            # 登记了哪些模块
grep -o 'multi-module-project[^"<]*' .idea/*.xml # 是否残留旧路径
cat .idea/compiler.xml                           # 模块名是否过时
```

**解法（按代价从小到大）**：

| 步骤 | 操作 | 解决概率 |
|---|---|---|
| 1 | Maven 工具窗口 → 🔄 **Reload All Maven Projects** | ~60% |
| 2 | `File → Invalidate Caches... → Invalidate and Restart` | ~25% |
| 3 | **备份并重建 `.idea`** → 重新 `Open` 工程目录 | ~15%（彻底） |

**第 3 步的完整操作**（⚠️ **必须先完全退出 IDEA**，`File → Exit`，不是关窗口。否则 IDEA 运行中会把 `workspace.xml` 写回磁盘，磁盘上又冒出一个残缺的 `.idea`）：

```bash
# 1. 确认进程退出
tasklist | grep -i idea64

# 2. 备份（含你的运行配置和插件配置，别直接删）
cp -r /e/houduan/.idea /e/houduan/.idea.bak

# 3. 移出项目目录！留在项目里会污染 git status，也破坏"目录与课程一致"的约定
mkdir -p /e/houduan-idea-backup
mv /e/houduan/.idea* /e/houduan-idea-backup/

# 4. 重开 IDEA → Open → E:\houduan → 右下角点 Load 导入 Maven
```

**善后**：
- 老的运行配置会失效（其中一个本来就用着旧模块名 `01-quick-start`，重建更干净），重新点一次绿三角即可
- 插件配置从备份拷回：`cp /e/houduan-idea-backup/.idea.bak/ApifoxUploaderProjectSetting.xml /e/houduan/.idea/`

**教训**：**改完 `pom.xml` 的模块名/路径，一定要 reload Maven**。IDEA 不会自动发现你动了目录。

---

## 4. Git Bash 下 `mvn` 命令失效

**现象**：终端敲 `mvn -version`：

```
错误: 找不到或无法加载主类 org.codehaus.plexus.classworlds.launcher.Launcher
原因: java.lang.ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher
```

**原因**：本机 `F:\heima\apache-maven-3.9.16\bin\mvn` 这个 shell 脚本在 Git Bash（MinGW）环境下路径转换出错，拼不出正确的 `-classpath`，JVM 找不到 classworlds 引导类。**不是 Maven 装错了**，是包装脚本与环境不兼容。

**绕过方案**：跳过脚本，直接用 `java` 启动 Maven 内核：

```bash
cd /e/houduan && java -classpath "F:\heima\apache-maven-3.9.16\boot\plexus-classworlds-2.11.0.jar" \
  "-Dmaven.multiModuleProjectDirectory=E:\houduan" \
  -Dclassworlds.conf="F:\heima\apache-maven-3.9.16\bin\m2.conf" \
  -Dmaven.home="F:\heima\apache-maven-3.9.16" \
  -Dlibrary.jansi.path="F:\heima\apache-maven-3.9.16\lib\jansi-native" \
  org.codehaus.plexus.classworlds.launcher.Launcher -q -pl 02-config test
```

**四个参数的作用**：

| 参数 | 作用 | 不写会怎样 |
|---|---|---|
| `-classpath` | 指向 `plexus-classworlds-2.11.0.jar`（Maven 的类加载器引导） | 找不到主类 |
| `-Dmaven.multiModuleProjectDirectory` | 告诉 Maven **项目根在哪**（必须是绝对路径，用 Windows 反斜杠格式） | 多模块解析出问题 |
| `-Dclassworlds.conf` | Maven 的类世界配置 | 加载不到 Maven 本体 |
| `-Dmaven.home` | Maven 安装目录 | 找不到插件 |
| `-Dlibrary.jansi.path` | Jansi 本地库（彩色输出） | 仅影响颜色 |

**其他出路**：
- PowerShell / CMD 下用 `mvn.cmd`（不走那个坏脚本）
- **IDEA 完全不受影响**——它自己解析 Maven，不用 shell 脚本

---

## 5. 改了代码但跑的还是旧行为（404）

**现象**：新写了 `@GetMapping("/config/basic")` 等 6 个接口，启动后逐个 curl 全是 404。

**原因**：只跑了 `mvn test`（**不产出 jar**），然后 `java -jar target/02-config-0.0.1-SNAPSHOT.jar` 启动的是**上一次 `package` 留下的旧 jar**。代码改了，jar 没变。

**验证**：

```bash
ls -l 02-config/target/*.jar                 # 看 jar 的修改时间
unzip -l 02-config/target/*.jar | grep basic # 看新类在不在里面
```

**解法**：

```bash
mvn -DskipTests package       # 先打包，再启动
```

**教训**：`test` 和 `package` 是两个阶段，`test` 不产出可执行 jar。

| 命令 | 编译 | 跑测试 | 产出 jar |
|---|---|---|---|
| `mvn compile` | ✅ | ❌ | ❌ |
| `mvn test` | ✅ | ✅ | ❌ |
| `mvn package` | ✅ | ✅ | ✅ |
| `mvn install` | ✅ | ✅ | ✅（并装进本地仓库） |

**在 IDEA 里不会有这个问题**——直接跑 `main` 方法，IDEA 会先编译。

**顺带一个同类坑**：改了 `application.yml` 后行为没变，先确认 `target/classes/application.yml` 是不是新的（旧 jar 里的配置是打包时的那份）。

---

## 6. git 不存空目录

**现象**：本地建好了 `03-logging` ~ `13-actuator` 共 11 个模块目录，`git push` 后在 GitHub 上**一个都看不到**。GitHub 页面只有 `01-quickstart`、`02-config`、`pom.xml` 等。

**原因**：git 的存储模型是「快照 + 文件树」，**树节点必须有叶子文件才能存在**。空目录在 git 世界里不存在。**这是设计，不是 bug**。

同理 `.claude/` 也是空的，也不会显示。而 `.idea/` 看不到是因为 `.gitignore` 第 35 行忽略了它——属另一回事。

**解法**：放占位文件。惯例名 `.gitkeep`（**这个名字没有特殊含义**，git 不认它，纯社区约定）：

```bash
for d in 03-logging 04-web 05-mysql 06-mybatis 07-mq 08-schedule \
         09-security 10-file 11-doc 12-test 13-actuator .claude; do
  touch "$d/.gitkeep"
done
git add -A && git commit -m "chore: 空模块目录添加 .gitkeep 占位，使目录结构在 GitHub 可见"
git push origin main
```

**代价**：每个空目录在 IDEA 项目树里多一个 `.gitkeep` 文件，碍眼。**往模块写代码时把对应的 `.gitkeep` 删掉**（有真实文件了就不需要占位）。

**如果不想看到 `.gitkeep`**：让 IDEA 在项目树里隐藏它——`Settings → Editor → File Types → Ignore files and folders` 里加 `.gitkeep`。注意这只是本地视觉隐藏，不影响文件实际存在。

**更好的长期方案**：模块较多时，用 `README.md` 替代 `.gitkeep`——既是占位，又交代了「这个模块要做什么」，信息量最大。

---

## 7. 附：目录重组的完整安全流程

重组 `E:\houduan` 时用到的流程，通用：

```bash
# 1. 先落一个"保险 commit"，把当前所有改动固化
git add -A && git commit -m "chore: 重组前保存当前改动"
#   → 记下这个 commit hash，出问题 git reset --hard <hash> 就能回到现在

# 2. 执行 mv / rm / mkdir
mv multi-module-project/01-quick-start 01-quickstart
mv multi-module-project/pom.xml pom.xml
rmdir multi-module-project
mkdir -p 03-logging ...

# 3. 全仓搜索旧名称，别漏改
grep -rn "quick-start\|multi-module-project" \
  --include="*.java" --include="*.yml" --include="*.xml" --include="*.md" . \
  | grep -v target | grep -v ".git/"

# 4. 批量替换残留引用（sed -i 原地替换）
sed -i 's/01-quick-start/01-quickstart/g; s/multi-module-project/backend-learning/g' \
  mvnw 02-config/README.md 01-quickstart/src/main/java/com/itheima/controller/UserController.java

# 5. 构建验证，确保没改坏
mvn clean package -DskipTests

# 6. 提交（git 会自动识别为 rename，历史不丢）
git add -A && git commit -m "refactor: 目录结构对齐 backend-learning"
```

**要点**：
- 第 1 步不能省，这是**唯一的后悔药**
- `mv` 后 **必须全仓 grep 旧名称**——pom、yml、注释、README 里都可能藏着路径
- git 对「内容相同、路径变了」的文件自动识别为 `rename`，`git log --follow` 仍能追到改名前后的完整历史
- `.idea/` 里残留的旧路径**不用手动改**，IDEA 重新导入时会重建（见第 3 节）

---

## 相关篇章

- Maven 命令与多模块结构：[Maven 多模块工程](/engineering/maven-multimodule)
- git 忽略规则与空目录处理：[Git 工作流](/engineering/git-workflow)
- IDEA 导入与缓存重建：[IDEA 工程配置](/engineering/idea-setup)
