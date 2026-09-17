---
layout: home

hero:
  name: 前后端工程化
  text: 学习笔记
  tagline: 一个模块一篇笔记，代码跑通过才写进来
  actions:
    - theme: alt
      text: 学习路线
      link: /guide/roadmap
    - theme: alt
      text: 内容规划
      link: /guide/outline
    - theme: alt
      text: 后端模块地图
      link: /backend/
---

## 站点说明

这个站点用来放学习笔记，分三块：

- **后端**：Spring Boot 3，按课程的 01~13 模块推进，见 [后端模块地图](/backend/)
- **前端**：Vue 3 + Vite，从 CDN 字面量到工程化项目，见 [前端知识地图](/frontend/)
- **工程化**：Maven 多模块、Git 用法、IDEA 配置和踩坑记录，见 [工程化总览](/engineering/)

写笔记的原因是学习过程中最怕的不是难，是**散**：

- 视频看完就忘，代码写完就扔，过两周连自己写过什么都不记得
- 笔记散落在 README、IDE 注释里，找不回来
- 知识点之间没有关联，学了 `@Value` 不知道它和 `@ConfigurationProperties` 有什么区别

所以这里定了几条规矩：

| 做法 | 目的 |
|---|---|
| 先写「学习路线」和「内容规划表」 | 知道整体有多少东西，不至于盲人摸象 |
| 一篇笔记聚焦一个可验证的结论 | 读完能立刻动手复现 |
| 笔记与源码仓库路径一一对应 | 从笔记直接跳到项目里对应的模块目录 |
| 标注进度状态 | 明确哪些是学完的、哪些只是列了提纲 |
| 踩坑单独成篇 | 环境类问题可检索、可复用 |

## 两个仓库

笔记里的所有代码都来自实际练习，分前后端两个独立仓库：

| 仓库 | 本地路径 | 内容 |
|---|---|---|
| `backend-learning` | `E:\houduan` | Spring Boot 3 后端，01~13 模块化推进 |
| `frontend-learning` | `E:\qianduan` | Vue 3 + Vite 前端，从 CDN 字面量到工程化项目 |

## 进度标记怎么看

<NoteStatus level="done" /> 该模块已经写完并在本地跑通

<NoteStatus level="wip" /> 正在整理，内容可能还会调整

<NoteStatus level="todo" /> 只有提纲，还没开始动手 —— 看到这个标记说明那部分代码也还没写

## 阅读建议

新手从 [学习路线](/guide/roadmap) 看起；已经有基础、只想查某个知识点，用左上角的搜索，或者从 [后端模块地图](/backend/) 进入。

> 笔记基于 Spring Boot `3.5.16` / JDK `21`（编译目标 17）/ Vue `3.5` + Vite `8` 编写。版本升级后部分结论可能失效，遇到不一致以官方文档和实测结果为准。
