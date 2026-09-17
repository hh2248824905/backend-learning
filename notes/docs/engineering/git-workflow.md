# Git 工作流

<NoteStatus level="done" />

::: info 关联资产
后端：`E:\houduan` → `ssh://git@ssh.github.com:443/hh2248824905/backend-learning.git`（`main`）
前端：`E:\qianduan` → `ssh://git@ssh.github.com:443/hh2248824905/frontend-learning.git`（`main`）
:::

## 0. 一件事：git 只保存「你能看见的文件状态」

理解这一点，下面 80% 的困惑都能自己解释：

- **空目录存不下来** → 目录里没有文件，git 眼里没有东西可存
- **`.idea/` 提交了会打架** → 它是 IDE 的本地状态，每个人的都不一样
- **`target/` 提交了会污染** → 编译产物，每次构建都变

## 1. 提交粒度：小而完整

看 `E:\houduan` 的真实历史（节选）：

```
d153fa2 feat(02-config): @Value 全案例（基础/占位符/默认值/随机值/SpEL）+ EnvService 多环境 @Profile + StudentController
2a74779 feat(02-config): 配置对齐教学视频（端口 8002、应用名、profiles.active=dev、dev/prod 环境文件、groupId 改 top.a1788）
9e13607 fix(02-config): 指定打包主类，修复双启动类导致 spring-boot-maven-plugin 打包失败
9a98377 feat: 02-config 新增 top.a1788.config 配置绑定案例（StudentProperties/AppProperties + @SpringBootTest 测试）
fc7c061 chore: 空模块目录添加 .gitkeep 占位，使目录结构在 GitHub 可见
f2d3db6 refactor: 目录结构对齐 backend-learning（模块上移到根目录，01-quick-start 更名 01-quickstart）
ff07b37 chore: 重组前保存当前改动            ← 关键：危险操作前先存一次
```

观察到的三条实际做法：

1. **一个功能一个 commit**。`9a98377` 只做「配置绑定案例」，`2a74779` 只做「配置对齐」，没有把两件事塞一个提交里
2. **危险操作前先落一个「保险 commit」**。`ff07b37` 内容是空的（"重组前保存当前改动"），作用是给后面的目录大挪移留一个可回滚点：
   ```bash
   git add -A && git commit -m "chore: 重组前保存当前改动"
   # 然后放开手 mv / rm，出问题直接 git reset --hard ff07b37
   ```
3. **类型前缀**：`feat` 新功能 / `fix` 修 bug / `refactor` 重构（不改行为）/ `chore` 杂活 / `docs` 文档 / `test` 测试

## 2. 提交信息怎么写

**格式**：`<类型>(<范围>): <做了什么，为什么>`

| 差 | 好 |
|---|---|
| `修改代码` | `fix(02-config): 指定打包主类，修复双启动类导致打包失败` |
| `update` | `feat(02-config): 新增 @Value 全案例与多环境 @Profile 支持` |
| `改了点东西` | `refactor: 目录结构对齐 backend-learning，模块上移到根目录` |

**判断标准**：三个月后的你，只看这一行，能不能知道当时为什么动这段代码。写的不是给别人看的，是给未来的自己看的。

## 3. 提交前先看清单

```bash
git status --short        # 哪些文件被改了（?? = 未追踪）
git diff                  # 具体改了什么内容
git diff --staged         # 已经 add 但还没 commit 的部分
git lg                    # 已配置的彩色单行日志别名
```

`git status --short` 输出速查：

| 标记 | 含义 |
|---|---|
| ` M` | 已修改，未暂存 |
| `M ` | 已修改，已暂存 |
| `A ` | 新增，已暂存 |
| `??` | 未追踪（新文件，还没 add） |
| `D ` | 已删除，已暂存 |
| `R ` | 重命名（git 自动识别内容相同） |

## 4. `.gitignore`：什么绝对不能提交

`E:\houduan\.gitignore` 的六类：

```text
# 依赖
node_modules/          ← npm 依赖，几百 MB，靠 package.json 还原
.venv/  venv/  env/    ← Python 虚拟环境

# 构建产物
dist/  build/  out/    ← 前端构建输出
target/                ← Maven 编译输出，每次构建都变

# 环境与密钥
.env  .env.local  *.pem  *.key  secrets/
                       ← ⚠️ 密钥提交 = 泄露，改也删不掉（历史里有记录）

# 日志
*.log  logs/

# 编辑器与系统
.idea/  .vscode/  *.swp  .DS_Store  Thumbs.db
                       ← IDE 本地状态，各人不同，提交了必冲突

# 测试与覆盖率
coverage/  *.lcov

# 数据库
*.db  *.sqlite  *.sqlite3   ← 本地测试库

# 临时文件
*.tmp  *.bak
```

**判断某个文件该不该提交，问三个问题**：
1. 它能从别的文件**自动生成**吗？（能 → 不提交，如 `target/`、`dist/`、`node_modules/`）
2. 它是**我这台机器独有**的吗？（是 → 不提交，如 `.idea/`、`.vscode/`）
3. 它含**密钥/IP/账号**吗？（含 → 不提交，且要确认历史里从没出现过）

### 4.1 `.idea/` 该不该提交

| 观点 | 理由 |
|---|---|
| 不提交（本仓库选择） | 各人 IDE 版本、插件、窗口布局不同，提交后天天冲突；`workspace.xml` 每次运行都会变 |
| 提交（部分团队） | 统一代码风格配置（`codeStyles/`）、运行配置（`runConfigurations/`），新人开箱即用 |

**折中做法**：忽略整个 `.idea/`，但用 `.editorconfig` 统一格式（那是 IDE 无关的标准）。本项目就是 `.idea/` 在 `.gitignore` 第 35 行被忽略。

## 5. 空目录与 `.gitkeep`

**问题**：`03-logging` ~ `13-actuator` 这些模块目录是空的（只有 `.gitkeep`），如果只建空目录，`git push` 后 GitHub 上一个都看不到。

**原因**：git 的存储模型是「快照 + 文件树」，**树节点必须有叶子（文件）才能存在**。空目录在 git 世界里不存在，这不是 bug，是设计。

**解法**：放一个占位文件。惯例名是 `.gitkeep`——注意这个名字**没有任何特殊含义**，git 不认识它，纯粹是社区约定俗成的「我是个占位符」的暗号。

```bash
for d in 03-logging 04-web 05-mysql ... 13-actuator; do touch "$d/.gitkeep"; done
git add -A && git commit -m "chore: 空模块目录添加 .gitkeep 占位，使目录结构在 GitHub 可见"
```

**代价与善后**：
- 每个空目录在 IDEA 项目树里会多一个 `.gitkeep`，看着碍眼
- **往模块里写代码时，把对应的 `.gitkeep` 删掉**（有真实文件了，不需要占位）

其他等价方案（择一即可，别混用）：

| 文件 | 说明 |
|---|---|
| `.gitkeep` | 最通用，无实际含义，纯约定 |
| `.keep` | 同上，另一个常见写法 |
| `README.md` | 每个模块放一个说明文件，顺带交代这个模块要做什么（**信息量最大**，适合模块较多时） |

本项目现在用的是 `.gitkeep`。等某个模块真正开工，建议把 `.gitkeep` 换成 `README.md`。

## 6. `.gitattributes`：跨平台换行符

```text
# Auto detect text files and perform LF normalization
* text=auto
```

**要解决什么**：Windows 用 `CRLF` 结尾，Linux/macOS 用 `LF`。同一个文件在不同系统提交，git 会认为「整文件都改了」，diff 一片红，review 无从看起。

`* text=auto` 的规则：提交时统一转 `LF` 存进仓库，检出时按当前系统转回（Windows 得到 `CRLF`）。

**只加这一行就够了**。想更细：

```text
*.sh text eol=lf          # shell 脚本必须 LF，否则 Linux 下报 bad interpreter
*.bat text eol=crlf       # Windows 批处理
*.png binary              # 二进制文件禁止做换行转换
```

## 7. SSH 走 443 端口

两个仓库的 remote 都不是标准形式：

```
ssh://git@ssh.github.com:443/hh2248824905/backend-learning.git
        ↑ 主机换成了 ssh.github.com      ↑ 端口换成了 443
```

**为什么这么配**：公司/学校网络常封 `22` 端口，导致 `git push` 挂在 `Connection timed out`。GitHub 提供 `ssh.github.com:443` 作为备用入口。缺点是走 SSH 协议跑在 HTTPS 端口上，速度略慢，但能用。

**验证连通性**：

```bash
ssh -T git@ssh.github.com -p 443
# → Hi hh2248824905! You've successfully authenticated...
```

## 8. 常用命令速查

::: code-group

```bash [日常]
git status --short --branch     # 状态 + 当前分支 + 领先/落后远程几个提交
git add -A                      # 暂存所有改动（含删除）
git commit -m "type(scope): msg"
git push origin main
```

```bash [回滚]
git restore <file>              # 丢弃工作区某个文件的改动
git restore --staged <file>     # 把它从暂存区撤下（改动保留）
git reset --hard <commit>       # ⚠️ 丢弃所有改动回到某提交，先确认真要这么做
git revert <commit>             # 生成一个「反向提交」来抵消，安全，适合已推送的
```

```bash [排查]
git log --oneline -10
git log -p <file>               # 某个文件的每次改动内容
git diff <commit1> <commit2> -- <file>
git blame <file>                # 每行是谁、哪次提交改的
git bisect start                # 二分查找哪个提交引入了 bug
```

:::

### 8.1 查「我的改动到底推上去了没」

```bash
git status --short --branch
# ## main...origin/main [ahead 2]      ← 本地领先远程 2 个提交，还没推
# ## main...origin/main                ← 完全同步
```

## 9. 本机踩过的 git 怪现象

::: danger commit 成功却打印 "nothing to commit"
本机 Git Bash 环境下偶发：`git commit` 实际执行成功，但紧接着的输出却显示 `nothing to commit, working tree clean`，看起来像失败了。

**不要相信那行输出**，用状态命令复核：

```bash
git log --oneline -1        # 看最新提交是不是你刚写的
git status --short --branch # 看是否还 ahead
```

如果 `git log` 里有新提交但 `status` 显示 ahead，直接补一次 `git push origin main` 即可。
:::

## 10. 相关篇章

- 构建产物为什么不能提交：[Maven 多模块工程](/engineering/maven-multimodule)
- `.idea` 被忽略后的善后：[IDEA 工程配置](/engineering/idea-setup)
- 目录重组的完整操作过程：[踩坑与排错记录](/engineering/troubleshooting)
