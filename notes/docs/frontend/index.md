# 前端知识地图

前端仓库：`E:\qianduan` → `github.com/hh2248824905/frontend-learning`

## 1. 学习路线

前端知识是**强线性**的 —— 跳过任何一层都会在后面卡住。

```
① HTML 结构          语义标签、表单、文档骨架
      ↓
② CSS 表现           选择器、盒模型、Flex / Grid、响应式
      ↓
③ JavaScript 语言    变量、类型、数组对象、函数、闭包
      ↓
④ DOM / BOM          querySelector、事件、事件委托、存储
      ↓
⑤ ES6+ / 异步        解构、展开、模块化、Promise、async/await
      ↓
⑥ Vue 3              响应式、指令、组件、计算属性
      ↓
⑦ 工程化             包管理、构建工具、环境变量、部署
```

**为什么这个顺序不能乱**：

| 跳过什么 | 会在哪卡住 |
|---|---|
| HTML / CSS | 写 Vue 时不知道 `<template>` 里的东西最终长什么样 |
| JavaScript 基础 | 看不懂 `<script setup>` 里的数组方法、解构、箭头函数 |
| DOM 基础 | 不理解 Vue 到底替你做了什么（为什么不用手写 `innerHTML`） |
| ES6+ | 看不懂 `import`、`?.`、`...` 这些到处都用的语法 |

## 2. 仓库结构

```
qianduan/
├── README.md                    总笔记（HTML/CSS/JS 速查，内容较全）
├── 01-vue-literal/              Vue CDN 入门
│   └── index.html               单文件，双击浏览器就能跑
└── vue-app/                     Vite 工程化项目
    ├── package.json
    ├── vite.config.js
    ├── index.html               入口 HTML（注意：在项目根，不在 src 里）
    ├── public/                  静态资源（不参与构建处理）
    └── src/
        ├── main.js              入口 JS：createApp + mount
        ├── App.vue              根组件（TodoList 案例）
        ├── style.css
        └── assets/
```

::: tip 为什么 `index.html` 在项目根而不是 `src/` 里
这是 **Vite 的设计**（和 Webpack 的传统结构不同）。

Vite 把 `index.html` 当作**构建入口**而非静态资源 —— 它是一个会被解析的模板，里面的 `<script type="module" src="/src/main.js">` 就是构建的起点。

Webpack 项目通常把 `index.html` 放在 `public/`，由 `HtmlWebpackPlugin` 处理。**两套结构的习惯不要互相套用。**
:::

## 3. 笔记清单

| # | 笔记 | 素材来源 | 状态 |
|---|---|---|---|
| 1 | [HTML 与 CSS](/frontend/01-html-css) | `README.md` 第二、三节 | <NoteStatus level="wip" /> |
| 2 | [JavaScript 与 ES6+](/frontend/02-js) | `README.md` 第四、五、七节 | <NoteStatus level="wip" /> |
| 3 | [CDN 字面量入门](/frontend/03-vue-literal) | `01-vue-literal/index.html` | <NoteStatus level="done" /> |
| 4 | [综合案例 TodoList](/frontend/04-todolist) | `vue-app/src/App.vue` | <NoteStatus level="done" /> |
| 5 | [Vite 工程化项目](/frontend/05-vite-project) | `vue-app/` 全套配置 | <NoteStatus level="wip" /> |

::: warning 状态说明
<NoteStatus level="done" /> 的两篇（03、04）是**基于真实运行的代码**写的，里面的代码片段和输出都能复现。

<NoteStatus level="wip" /> 的三篇是从 `README.md` 里已有笔记**重新组织**的 —— 内容是真的，但还没按「每篇一个可验证结论」的标准重构完。
:::

## 4. 前端与后端的会合点

两条线在**「前后端联调」**这里汇合：

```
后端（houduan）                     前端（qianduan）
04 Web  —— 提供 JSON 接口  ←────→  fetch / axios 调用
09 安全  —— 认证 token      ←────→  携带 Authorization 头
11 文档  —— OpenAPI 描述    ←────→  前端照着文档写请求
```

**联调时的关键认知**：

| 前端写什么 | 后端要配合什么 |
|---|---|
| `fetch('http://localhost:8013/users')` | 后端要开 **CORS**（[09 安全](/backend/09-security#_2-8-cors-与-csrf)） |
| 解析 `res.json()` | 后端返回**统一的 JSON 结构**（[04 Web](/backend/04-web#_2-4-统一返回体)） |
| 根据 `code` 判断成功失败 | 后端约定好 business code 的取值 |
| 带 `Authorization: Bearer xxx` | 后端用 JWT 过滤器解析 |

**目前两个仓库还没做过联调** —— 这是后面要做的事。联调时踩的坑会补充到本篇。

## 5. 和 Vue 官方文档的关系

本站的 Vue 部分**不替代官方文档**，定位是：

| 官方文档擅长的 | 本站擅长的 |
|---|---|
| API 完整参考 | 用真实跑通的案例说明**为什么这么用** |
| 概念解释 | 记录**踩过的坑**和排查路径 |
| 最佳实践建议 | 记录**实际项目的取舍决策** |

**遇到 API 细节查官方文档，遇到「为什么」和「怎么排查」查这里。**

## 6. 相关后端笔记

- [04 Web 开发](/backend/04-web) —— 前端要调的那些接口长什么样
- [09 安全认证](/backend/09-security) —— token 怎么带过去
- [11 接口文档](/backend/11-doc) —— 照着文档写请求，不用问后端
