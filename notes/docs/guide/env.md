# 开发环境与工具链

本页记录本机实际在用的环境。换机器时照着配一遍即可，不用重新摸索。

## 1. 环境清单

| 组件 | 版本 | 安装位置 | 备注 |
|---|---|---|---|
| JDK | 21.0.11 | `C:\Program Files\Java\latest\jdk-21` | 编译目标设为 17，21 兼容编译 |
| Maven | 3.9.16 | `F:\heima\apache-maven-3.9.16` | 本地仓库在 `D:\tools\maven_jar` |
| Node.js | 22.22.2 | 托管运行时 | Vite 8 要求 Node ≥ 20 |
| Git | 2.55.0 | 系统安装 | 终端用 Git Bash |
| IDEA | 2025.3.1 | `E:\IntelliJ IDEA 2025.3.1` | 主力 IDE |
| VS Code | — | `E:\Microsoft VS Code` | 前端顺手的编辑器 |

**代码位置**

| 仓库 | 路径 | 远程 |
|---|---|---|
| 后端 | `E:\houduan` | `git@ssh.github.com:443/hh2248824905/backend-learning.git` |
| 前端 | `E:\qianduan` | `git@ssh.github.com:443/hh2248824905/frontend-learning.git` |
| 笔记站点 | `E:\notes` | 待发布 |

## 2. 后端环境

### 2.1 JDK 与编译目标

本机装的是 JDK 21，但父 pom 里把编译目标设成了 17：

```xml
<properties>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

::: tip 为什么不直接用 21
编译目标（`java.version`）决定**字节码版本**，运行环境（JDK）决定**运行时能力**。

设成 17 意味着产物能在 JDK 17 的服务器上跑，扩展了部署范围。如果服务器确定也是 21，改这个值即可，不影响本地开发。
:::

### 2.2 Maven 本地仓库

```
D:\tools\maven_jar
```

这个路径不是默认值（默认是 `~/.m2/repository`），所以 Maven 的 `settings.xml` 里做了配置。**换机器时容易漏掉这一步**，导致依赖重新下载几十 MB。

### 2.3 阿里云镜像

`settings.xml` 里配了阿里云镜像加速。配置大致形如：

```xml
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

::: warning 本仓库里曾经有一份 `.tools/settings-aliyun.xml`
为了「目录结构与教学仓库严格一致」，那份文件连同 `mvnw` 包装器已从 `backend-learning` 仓库移除。所以现在构建**依赖本机全局 Maven 配置**。

如果换机器后依赖下载很慢，检查 `C:\Users\<用户名>\.m2\settings.xml` 里的镜像配置，不要指望仓库里还有。
:::

### 2.4 Git Bash 下 `mvn` 脚本的问题

**现象**：在 Git Bash 里直接执行 `mvn` 命令，报错找不到主类。

```
错误: 找不到或无法加载主类 org.codehaus.plexus.classworlds.launcher.Launcher
```

**原因**：`mvn` 是个 shell 脚本，需要把安装目录转成 Windows 路径并拼出 classpath。在 Git Bash（MSYS2）环境下，路径转换逻辑会把 `F:\...` 转成 `/f/...` 的形式，导致 classpath 拼错。

**解法一（推荐）**：不用 shell 脚本，直接用 `java` 启动 Maven 内核。

```bash
java -classpath "F:\heima\apache-maven-3.9.16\boot\plexus-classworlds-2.11.0.jar" \
     "-Dmaven.multiModuleProjectDirectory=E:\houduan" \
     -Dclassworlds.conf="F:\heima\apache-maven-3.9.16\bin\m2.conf" \
     -Dmaven.home="F:\heima\apache-maven-3.9.16" \
     org.codehaus.plexus.classworlds.launcher.Launcher \
     -q test
```

参数说明：

| 参数 | 作用 |
|---|---|
| `-classpath` | 指向 Maven 的 classworlds 引导 jar |
| `-Dmaven.multiModuleProjectDirectory` | **必填**，指向项目根（含 `.mvn` 或 `pom.xml` 的目录）。缺了它 Maven 会直接拒绝启动 |
| `-Dclassworlds.conf` | Maven 的类世界配置文件 |
| `-Dmaven.home` | Maven 安装目录，用于定位插件与本地仓库配置 |

**解法二**：改用 `mvn.cmd`（Windows 原生批处理），或用 PowerShell 执行。

### 2.5 IDEA 里不受影响

**IDEA 内部自带 Maven 调用逻辑**，不经过 shell 脚本，所以：

- 在 IDEA 右侧 Maven 面板点 `test` / `package` → **正常**
- 在 IDEA 终端里敲 `mvn test` → **可能报错**（走的是同一个坏脚本）

结论：构建优先用 IDEA 的 Maven 面板或上面那条 `java -classpath` 命令。

## 3. 前端环境

### 3.1 Node 与包管理

本机 Node 由 WorkBuddy 托管，安装在隔离目录里：

| 项 | 值 |
|---|---|
| Node | `C:\Users\a1788\.workbuddy\binaries\node\versions\22.22.2-2\node.exe`（v22.22.2） |
| 备用 Node | `C:\Program Files\nodejs\node.exe`（v24.16.0，系统版） |
| npm 入口 | 同目录下 `node_modules\npm\bin\npm-cli.js` |

Vite 8 要求 Node ≥ 20，22 没问题。

::: warning `npm` 不在 PATH 里
托管 Node 没有把 `npm` 注册成全局命令。终端直接敲 `npm install` 会报 `command not found`。

绕过方式是用完整路径显式调用：

```bash
cd /e/notes && "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node.exe" \
  "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node_modules/npm/bin/npm-cli.js" install
```

VitePress 的 CLI 也可以直接调（比走 npm 快，省一层）：

```bash
cd /e/notes && "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node.exe" \
  node_modules/vitepress/bin/vitepress.js build docs
```

注意入口文件名是 **`vitepress.js`**（不是 `vitepress.mjs`），可以看 `node_modules/vitepress/package.json` 的 `bin` 字段确认。
:::

### 3.2 npm 全局目录与缓存

为了不给 C 盘增加负担，全局目录和缓存都挪到了 E 盘。`C:\Users\a1788\.npmrc` 内容：

```ini
prefix=E:\dev\npm-global
cache=E:\dev\npm-cache
registry=https://registry.npmmirror.com/
```

**效果**：`npm install -g` 装的包落在 `E:\dev\npm-global`，依赖下载走国内镜像。

::: warning 但项目依赖仍然装在项目目录
上面配的是**全局包**。项目级依赖（`npm install` 在项目里执行）仍然生成在项目自己的 `node_modules/`，这是 npm 的设计，无法改。

所以 `E:\notes\node_modules` 有几百 MB 是正常的，它已在 `.gitignore` 里，不会进版本库。
:::

### 3.3 前端项目启动

```bash
cd E:\qianduan\vue-app
npm install       # 首次
npm run dev       # 开发服务器，默认 5173
npm run build     # 产物输出到 dist/
```

（本机要用 3.1 里的完整路径写法替代 `npm`。）

## 4. 网络与代理

### 4.1 Git 走 SSH 443 端口

远程地址用的是 `ssh://git@ssh.github.com:443/...`，而不是默认的 `git@github.com:22`。

**原因**：22 端口常被网络环境封禁，GitHub 官方提供了 443 端口的 SSH 备用入口。这样在校园网等受限环境下也能正常推送。

**验证连通性**：

```bash
ssh -T -p 443 git@ssh.github.com
```

看到 `Hi xxx! You've successfully authenticated` 就正常。

### 4.2 npm 镜像

已在 `.npmrc` 里设成 `registry.npmmirror.com`，安装速度稳定。临时切回官方源：

```bash
npm install --registry=https://registry.npmjs.org/
```

## 5. 环境自检清单

换机器或环境异常时，按这个顺序查：

| # | 检查项 | 命令 | 期望结果 |
|---|---|---|---|
| 1 | JDK | `java -version` | `21.x` |
| 2 | Maven 本体 | 上面那条 `java -classpath ... -version` | `Apache Maven 3.9.16` |
| 3 | Maven 本地仓库 | `ls D:\tools\maven_jar` | 有内容（不是空的） |
| 4 | Maven 镜像 | 查看 `.m2/settings.xml` | 有 `aliyunmaven` 镜像 |
| 5 | Node | `node -v` | `v22.x` |
| 6 | npm 源 | `npm config get registry` | `https://registry.npmmirror.com/` |
| 7 | Git SSH | `ssh -T -p 443 git@ssh.github.com` | `Hi ... authenticated` |
| 8 | 端口占用 | `netstat -ano \| findstr 8002` | 无输出（说明端口空闲） |

<NoteStatus level="done" />
