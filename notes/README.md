# 前后端工程化学习笔记

基于 [VitePress](https://vitepress.dev/zh/) 构建的学习笔记站点，用来有序管理「前后端工程化」课程的学习记录。

配套代码仓库：

| 仓库 | 内容 |
|---|---|
| [backend-learning](https://github.com/hh2248824905/backend-learning) | `E:\houduan` —— Spring Boot 多模块后端（01~13 模块） |
| [frontend-learning](https://github.com/hh2248824905/frontend-learning) | `E:\qianduan` —— Vue 3 + Vite 前端 |

## 为什么单独建一个仓库

不把笔记塞进代码仓库的原因：`E:\houduan` 的目录结构是对照课程严格还原的（01~13 模块），而本站点带 `node_modules`（200MB+）和构建产物，混在一起会破坏代码仓库的结构与可读性。**工具链产物与业务代码分开管理**。

## 目录结构

```
E:\notes\
├── docs\
│   ├── index.md                    首页
│   ├── guide\                      开始：总览 / 路线 / 内容规划 / 环境 / 规范
│   ├── backend\                    后端笔记：01~13 模块
│   │   └── 02-config\              拆成 4 篇的示例（总览 / @Value / 绑定 / Profile / 校验）
│   ├── frontend\                   前端笔记：HTML/CSS → JS → Vue → Vite
│   ├── engineering\                工程化：Maven 多模块 / Git / IDEA / 排错记录
│   ├── public\                     静态资源（logo 等）
│   └── .vitepress\
│       ├── config.mts              站点配置（nav / sidebar / 搜索）
│       └── theme\                  自定义主题与组件（NoteStatus 进度标记）
└── scripts\                        笔记质量校验脚本
```

## 本地运行

```bash
npm install          # 首次
npm run dev          # 开发预览，默认 http://localhost:5173
npm run build        # 构建到 docs/.vitepress/dist
npm run preview      # 预览构建产物
```

> ⚠️ 本机 Node 是 WorkBuddy 托管版本，`npm` 不在 PATH 里。若 `npm` 命令不存在，用完整路径：
>
> ```bash
> "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node.exe" \
>   "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node_modules/npm/bin/npm-cli.js" run dev
> ```

## 笔记质量校验

站点规模上来后，最容易出现两类静默错误，两个脚本用于在写完后立刻自查：

```bash
npm run check:anchors        # 校验站内锚点链接是否有效
python scripts/check-interpolation.py   # 扫描会被 Vue 当插值的 {{ }}
```

| 脚本 | 检查什么 | 为什么需要 |
|---|---|---|
| `check-anchors.py` | 站内链接的 `#锚点` 是否真实存在 | 锚点失效不会报错，读者点进去只是停在页首，很难发现 |
| `check-interpolation.py` | 行内代码里的 `{{ expression }}` | VitePress **只给围栏代码块加 `v-pre`**，行内代码里的 `{{ }}` 会被 Vue 当插值编译：轻则显示为空，重则构建直接崩 |

两条已有的固定约定：

- 正文要展示插值语法时写 `<code v-pre>{{ item.title }}</code>`，不要写 `` `{{ item.title }}` ``
- 标题里不写 `{{ }}`（会破坏锚点与显示），改用文字描述

## 写作约定

单篇笔记的固定结构（详见 [笔记内容规划](/guide/outline)）：

1. **解决什么问题** —— 开头就讲清
2. **核心知识点** —— 概念、原理、对比
3. **运行验证** —— 命令 + 真实输出，不写「应该会输出」
4. **与相邻知识点的对比** —— 和上一个模块、和替代方案的区别
5. **踩过的坑** —— 报错原文 + 原因 + 解法

状态标记用组件 `<NoteStatus level="done|wip|todo" />`。**标记 `done` 的前提是里面的运行输出都真实复现过**，写完一篇要同步更新「学习路线 → 进度快照」。

## 部署到 GitHub Pages

仓库已按 `base` 可通过环境变量覆盖的方式配置：

```bash
DOCS_BASE=/engineering-notes/ npm run build
```

配合 GitHub Actions（推 `main` 时自动构建并发布 `docs/.vitepress/dist`），即可获得可访问链接。Actions 工作流尚未创建——需要时补 `.github/workflows/deploy.yml`。
