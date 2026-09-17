# 工程化总览

<NoteStatus level="done" />

::: info 这一部分解决什么
前 13 个模块解决的是**「Spring Boot 怎么写」**，工程化解决的是**「一个项目怎么管起来」**。

代码写得再对，如果依赖版本一团乱、IDEA 天天报 ClassNotFoundException、改完不知道提交了什么、接手的人跑不起来——那这个项目就是不成立的。
:::

## 为什么单独拆出「工程化」

对比一下两种状态：

| | 没有工程化 | 有工程化 |
|---|---|---|
| 加一个模块 | 复制粘贴旧项目，改包名，漏改一处就报错 | 父 pom 加一行 `<module>` |
| 依赖版本 | 每个模块自己写，同事之间版本不一致 | 父工程统一声明，子模块不写版本号 |
| 别人拿到代码 | 「我这能跑啊」 | `mvn clean package` 就能起来 |
| 出故障 | 翻聊天记录找上次改了什么 | `git log` / `git bisect` 定位 |
| IDEA 配置 | 每人一套，新人来了配一天 | 提交 `.gitattributes` / 约定，缓存目录别提交 |

## 本部分的四篇

| 篇章 | 解决的具体问题 | 关联的真实资产 |
|---|---|---|
| [Maven 多模块工程](/engineering/maven-multimodule) | 13 个模块怎么组织在一个仓库里，父 pom 到底管什么 | `E:\houduan\pom.xml` |
| [Git 工作流](/engineering/git-workflow) | 提交粒度、信息规范、空目录、忽略规则 | 两个仓库的真实提交历史 |
| [IDEA 工程配置](/engineering/idea-setup) | Maven 导入、运行配置、`.idea` 该不该提交 | `E:\houduan\.idea` |
| [踩坑与排错记录](/engineering/troubleshooting) | 实际卡住过的 6 个问题，含错误原文与解法 | 全部来自真实复现 |

## 两个仓库的分工

```
E:\houduan   → github.com/hh2248824905/backend-learning   Java 多模块后端
E:\qianduan  → github.com/hh2248824905/frontend-learning  Vue 前端
E:\notes     → VitePress 笔记站点（本文档）
```

**为什么笔记不塞进代码仓库**：`E:\houduan` 的目录结构是对照课程严格还原的（01~13 模块），塞进 `node_modules` 200MB 的 VitePress 工程会彻底破坏这个结构。**工具链的东西独立成仓**，这是更通用的原则。

## 一句话原则清单

- 父 pom **只声明**依赖版本（`dependencyManagement`），**不引入**依赖
- 子模块靠 `parent` 继承，不重复写版本号
- `target/`、`node_modules/`、`.idea/` **永不提交**
- 空目录用 `.gitkeep` 占位，否则 git 存不下来
- 每完成一个可验证的小功能就提交一次，别攒成「一个巨大 commit」
- 提交信息写**为什么**改，不写「修改代码」
