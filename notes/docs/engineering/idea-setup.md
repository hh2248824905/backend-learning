# IDEA 工程配置

<NoteStatus level="done" />

::: info 关联资产
工程根：`E:\houduan`　·　IDE：IntelliJ IDEA 2025.3.1（`E:\IntelliJ IDEA 2025.3.1`）　·　插件：Apifox Helper
:::

## 0. IDEA 的两种工程模型

这是理解所有「IDEA 报错但命令行能跑」问题的钥匙：

| | IDEA 模块模型 | 命令行 |
|---|---|---|
| 谁决定编译什么 | `.idea/` 里的模块配置 + 外部存储 | `pom.xml` 的 `<modules>` |
| 源码根在哪 | `.iml` / `modules.xml` 登记 | Maven 约定（`src/main/java`） |
| 依赖从哪来 | IDEA 的依赖缓存（由 Maven 导入生成） | Maven 解析 pom |

**关键结论**：`pom.xml` 写对了 ≠ IDEA 认识。IDEA 需要执行一次「**导入（Import / Reload）**」把 pom 翻译成它自己的模型。**改完 pom 不 reload，IDEA 就是瞎的**——这是 ClassNotFoundException 系列的根源。

```
pom.xml ──[Maven 导入]──> IDEA 模块模型 ──> 编译 classpath ──> 运行
           ↑
      这一步不做，后面全是报错
```

## 1. 导入多模块工程

**首次打开**：

1. `File → Open` → 选**工程根目录** `E:\houduan`（不是选子模块目录）
2. IDEA 检测到根目录有 `pom.xml`，右下角弹提示 → 点 **Load / 导入**
3. 等索引（1~2 分钟）

**没弹提示时手动触发**：

- 右键根 `pom.xml` → **Add as Maven Project**
- 或右侧 `Maven` 工具窗口 → 左上角 `+` → 选根 `pom.xml`
- 或 `Maven` 工具窗口 → 左上角刷新按钮（🔄）→ **Reload All Maven Projects**

**验证导入成功**：

```
Maven 工具窗口应显示：
backend-learning
├── 01-quickstart
└── 02-config
```

`.idea/misc.xml` 里能找到 `MavenProjectsManager` → 说明托管的 pom 路径已登记：

```xml
<component name="MavenProjectsManager">
  <option name="originalFiles">
    <list><option value="$PROJECT_DIR$/pom.xml" /></list>
  </option>
</component>
```

### 1.1 新版 IDEA 没有 `.iml` 文件是正常的

2025.x 版本把模块配置挪到了**外部存储**（`%APPDATA%\JetBrains\IntelliJIdea2025.3\`），项目里只剩 `.idea/`，没有 `*.iml`。**不要因为找不到 `.iml` 就以为导入失败**——看 `.idea/misc.xml` 里有没有 `MavenProjectsManager` 才是准确判断。

## 2. 缓存坏了怎么办

**症状**：

- `错误: 找不到或无法加载主类 com.itheima.Application`
- 明明命令行 `mvn package` 全绿，IDEA 里就是一堆红线
- 改了 pom 里的模块名，IDEA 还显示旧名字

**第一步：先 reload Maven**（90% 的问题到此结束）

```
Maven 工具窗口 → 🔄 Reload All Maven Projects
```

不行再往下：

**第二步：清 IDEA 缓存**

```
File → Invalidate Caches... → 勾选 Clear file system cache and Local History → Invalidate and Restart
```

**第三步：核对该模块有没有被纳入聚合**

```bash
grep -A5 "<modules>" E:/houduan/pom.xml        # 子模块是否在列表里
ls E:/houduan/.idea/                            # 有没有 modules.xml / misc.xml
```

**第四步（最彻底）：重建 `.idea`**

⚠️ **必须先把 IDEA 完全退出**（`File → Exit`，不是关窗口）。否则 IDEA 会在运行中把 `workspace.xml` 写回磁盘，改完的目录里又冒出一个残缺的 `.idea`，局面更乱。

```bash
# 1. 确认进程已退出
tasklist | grep -i idea64

# 2. 备份（不要直接删，里面有你的运行配置和插件配置）
cp -r /e/houduan/.idea /e/houduan/.idea.bak

# 3. 移出项目目录（留在项目里会污染 git status）
mkdir -p /e/houduan-idea-backup && mv /e/houduan/.idea* /e/houduan-idea-backup/

# 4. 重新打开 IDEA → Open → E:\houduan，它会重新识别 pom.xml 并导入
```

**重建会丢什么、怎么补**：

| 内容 | 会丢吗 | 恢复方式 |
|---|---|---|
| Maven 模块结构 | 会，自动重建 | 重新导入即可 |
| 运行配置（Run Configuration） | 会 | 重新点 `main` 方法旁的绿三角，IDEA 自动新建 |
| 代码风格配置 | 会 | 手动重配，或用 `.editorconfig`（推荐） |
| **插件配置**（如 `ApifoxUploaderProjectSetting.xml`） | 会 | **从备份目录拷回来** |

插件配置的恢复：

```bash
cp /e/houduan-idea-backup/.idea.bak/ApifoxUploaderProjectSetting.xml /e/houduan/.idea/
```

## 3. 常用配置

### 3.1 编码：UTF-8（必做）

```
Settings → Editor → File Encodings
├── Global Encoding:        UTF-8
├── Project Encoding:       UTF-8
└── Default encoding for properties files: UTF-8
    └── ✅ Transparent native-to-ascii conversion（properties 里的中文才正常）
```

**不设的后果**：Windows 默认 GBK，控制台中文日志乱码、`application.properties` 中文读成问号。`application.yml` 因为 YAML 规范默认 UTF-8 稍好，但别赌。

### 3.2 Lombok：必须开注解处理器

IDEA 自带 Lombok 插件（2025.x 已内置），但**注解处理必须手动开**，否则 `@Data` 生成的 getter 全红：

```
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
└── ✅ Enable annotation processing
```

**验证**：`User` 类上用 `@Data`，`Ctrl+点击` `user.getName()` 能跳到 Lombok 生成的代码（会显示 decompiled）。

**常见误区**：Lombok 是 `optional=true` 的编译期依赖，**只在编译/IDE 里需要，运行期不需要**。所以 fat jar 里要排除它（见父 pom 配置）。

### 3.3 自动导包与格式化

```
Settings → Editor → General → Auto Import
├── ✅ Add unambiguous imports on the fly      （唯一匹配的包自动导入）
└── ✅ Optimize imports on the fly             （删除没用的 import）

Settings → Tools → Actions on Save
├── ✅ Reformat code
├── ✅ Optimize imports
└── ✅ Run code cleanup
```

## 4. 运行配置

### 4.1 跑 Spring Boot 应用

点 `main` 方法左侧**绿三角** → `Run 'XxxApplication'`。IDEA 自动生成运行配置：类路径、工作目录、JDK 全自动。

**改端口/加参数**：`Run → Edit Configurations...`

| 字段 | 用途 | 例子 |
|---|---|---|
| `VM options` | JVM 参数 | `-XX:+EnableDynamicAgentLoading`（消 ByteBuddy 警告） |
| `Program arguments` | 应用参数（等价于 `--key=value`） | `--spring.profiles.active=prod` |
| `Environment variables` | 环境变量 | `MXU_NAME=李四` |
| `Active profiles` | 等价于 `--spring.profiles.active` | `prod` |

**优先级**：`Program arguments` > `VM options`（`-D`）> 环境变量 > yml。

### 4.2 跑单个测试

测试方法左侧绿三角 → `Run 'printStudentInfo()'`。只跑这一个方法，比跑整个测试类快得多。

⚠️ 测试类上必须有 `@SpringBootTest`（或 `@ContextConfiguration`），否则 `@Resource` 注入的 Bean 是 `null`，报 `NullPointerException`。

### 4.3 双启动类的处理

一个模块里有两个 `@SpringBootApplication` 时：

- **IDEA 里**：两处都能点绿三角，各自独立启动（用哪个就跑哪个的扫包范围）
- **命令行 `mvn package`**：会失败，必须在 pom 里指定主类：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>top.a1788.config.ConfigApplication</mainClass>
    </configuration>
</plugin>
```

**根本解法**：删掉多余的那个。两个启动类扫包范围不重叠时，用 A 启动就注入不进 B 的 Bean——这是隐性炸弹，下一节细说。

### 4.4 扫包范围：注入不上 Bean 的头号原因

```
@SpringBootApplication 在 com.itheima.Application
   → 默认扫描 com.itheima.**           ← 只扫这个包及子包

top.a1788.config.properties.StudentProperties（@Component）
   → 在 top.a1788.** 下                 ← 扫不到！
```

**报错**：`No qualifying bean of type 'StudentProperties' available` 或 `NoSuchBeanDefinitionException`。

**判断方法**：把启动类所在包和 Bean 所在包都写出来，看前者是不是后者的前缀。

| 方案 | 做法 |
|---|---|
| 移动类（推荐） | 让所有 Bean 都在启动类的子包下 |
| 显式扩大扫描 | `@SpringBootApplication(scanBasePackages = {"com.itheima", "top.a1788"})` |
| 删掉多余启动类 | 本项目最终该走的路 |

## 5. Apifox Helper 插件

`E:\houduan\.idea\ApifoxUploaderProjectSetting.xml` 保存着插件配置（含 `apiAccessToken`）。**注意 `.idea/` 已被 gitignore，这个 token 不会推到 GitHub**——这是正确做法，密钥不该进版本库。

**上传接口**：

1. 启动应用（要在 Apifox 里点「运行」就必须开着）
2. 项目树右键 `controller` 包（或单个 Controller 类）→ 菜单里的 **Apifox** 子菜单 → **Upload to Apifox**（2025.3 也可能在 `Tools → Apifox` 下）
3. 弹窗里选目标 Apifox 项目 → 确认
4. Apifox 网页端刷新（F5）查看

**单向同步**：上传是「IDEA → Apifox」单向的。改了代码要**重新上传**才会同步；Apifox 里手改的内容会被下次上传覆盖（按路径 + 方法名匹配，智能合并）。

**常见卡点**：

| 现象 | 原因与解法 |
|---|---|
| 右键菜单没有 Upload to Apifox | `Settings → Plugins` 里禁用再启用 `ApifoxHelper`，重启 IDEA |
| 报令牌无效 | Apifox → 头像 → 账号设置 → **API 访问令牌** 新建一个，填回插件设置 |
| 上传成功但 Apifox 里看不到 | 选了错误的项目；或没刷新页面 |
| 中文 key 识别异常 | 返回类型用了 `Map<String, Object>`，建议定义明确的 DTO 类 |

**兜底**：插件实在不行时，Apifox 支持 `项目设置 → 导入数据 → OpenAPI/Swagger`，导入一份 `openapi.json` 也能拿到接口定义。

## 6. 快捷键速查（Windows）

| 操作 | 快捷键 |
|---|---|
| 全局搜索（任何东西） | `Shift` 连按两次 |
| 跳转到类 / 文件 / 符号 | `Ctrl+N` / `Ctrl+Shift+N` / `Ctrl+Alt+Shift+N` |
| 查看方法实现 | `Ctrl+Alt+B` |
| 重命名（含引用） | `Shift+F6` |
| 生成代码（getter/setter/构造） | `Alt+Insert` |
| 格式化当前文件 | `Ctrl+Alt+L` |
| 优化 import | `Ctrl+Alt+O` |
| 运行当前文件/上下文 | `Ctrl+Shift+F10` |
| 查看最近打开的文件 | `Ctrl+E` |
| 查看文件结构 | `Ctrl+F12` |
| 重构菜单 | `Ctrl+Alt+Shift+T` |

## 7. 相关篇章

- 工程结构怎么组织：[Maven 多模块工程](/engineering/maven-multimodule)
- IDEA 报错原文与完整处理过程：[踩坑与排错记录](/engineering/troubleshooting)
- `.idea/` 为什么不提交：[Git 工作流](/engineering/git-workflow#_4-gitignore-什么绝对不能提交)
