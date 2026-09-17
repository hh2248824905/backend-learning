# 01 HTML 与 CSS

<NoteStatus level="todo" text="未开始练习" />

::: info 本篇定位
前端三件套的第一件：**页面结构与样式**。这一篇目前是知识地图，`E:\qianduan` 里还没有对应练习代码（当前前端仓库从 `03-vue-literal` 起步）。
:::

## 0. 为什么 Vue 学习者要回头补 HTML/CSS

写完 Vue 组件你会发现：**Vue 只管「数据怎么变成 DOM」，不管「DOM 长什么样」**。

```vue
<template>
  <div class="todo-app">      <!-- 这里的 class 是 CSS 的事 -->
    <ul v-if="list.length">   <!-- v-if 是 Vue 的事 -->
      <li v-for="item in list" :key="item.id">{{ item.title }}</li>
    </ul>
  </div>
</template>
```

- `v-if` / `v-for` 不懂 → 模板写不出来
- Flex 布局不懂 → 写出来是歪的
- 两者都缺 → TodoList 这种最简单的案例也做不完整

## 1. HTML：结构层

### 1.1 语义化标签

不要所有东西都用 `div`。标签名本身就是文档大纲的一部分：

| 标签 | 语义 | 什么时候用 |
|---|---|---|
| `<header>` | 页头 | 站点头部、卡片标题区 |
| `<nav>` | 导航 | 一组链接（菜单、面包屑） |
| `<main>` | 主内容 | 一个页面只能有一个 |
| `<section>` | 区块 | 有独立小标题的内容块 |
| `<article>` | 独立内容 | 一篇帖子、一条评论 |
| `<aside>` | 侧边 | 侧栏、补充说明 |
| `<footer>` | 页脚 | 版权、备案号 |

**好处**：SEO 友好、屏幕阅读器可读、代码可维护。**代价**：多打几个字符。

### 1.2 常用表单元素

```html
<input v-model="title" type="text" placeholder="输入待办事项" />
<button type="button" @click="add">添加</button>
<a href="/detail" target="_blank" rel="noopener">详情</a>
```

⚠️ `<button>` 在 `<form>` 内默认是 `type="submit"`，会触发表单提交并刷新页面。不想要这个行为就显式写 `type="button"`。这是 Vue 新手最常见的「点一下页面就刷新」的原因。

## 2. CSS：表现层

### 2.1 盒模型（理解一切的起点）

```
┌───────── margin ─────────┐
│ ┌─────── border ───────┐ │
│ │ ┌──── padding ────┐  │ │
│ │ │   content 内容  │  │ │
│ │ └─────────────────┘  │ │
│ └──────────────────────┘ │
└──────────────────────────┘
```

- **标准盒模型**（默认）：`width` 只算 content，加 padding/border 后实际变宽
- **IE 盒模型**：`box-sizing: border-box`，`width` 包含 padding + border

**结论：永远全局设 `border-box`**，否则算宽度算到崩溃。

```css
* {
  box-sizing: border-box;
}
```

### 2.2 选择器与优先级

| 选择器 | 权重 | 例子 |
|---|---|---|
| 行内样式 | 1000 | `style="color:red"` |
| id | 100 | `#app` |
| class / 属性 / 伪类 | 10 | `.todo-app`、`[type=text]`、`:hover` |
| 标签 / 伪元素 | 1 | `ul`、`li`、`::before` |

权重高者生效；相同权重**后写的赢**。`!important` 是权重 10000，能不用就不用——它会让后续维护变成解谜游戏。

### 2.3 Flex：现代布局主力

TodoList 里「输入框占满剩余宽度 + 按钮固定宽度」一行搞定：

```css
.input-row {
  display: flex;
  gap: 10px;              /* 子元素间距，比 margin 干净 */
}
.input-row input {
  flex: 1;                /* 占满剩余空间 */
}
```

高频属性速查：

| 属性 | 作用 | 常用值 |
|---|---|---|
| `justify-content` | 主轴对齐 | `flex-start` / `center` / `space-between` |
| `align-items` | 交叉轴对齐 | `flex-start` / `center` / `stretch` |
| `flex-direction` | 主轴方向 | `row`（默认）/ `column` |
| `flex-wrap` | 换行 | `wrap` |
| `flex: 1` | 分配剩余空间 | 子元素上 |

### 2.4 为什么项目里到处是 CSS 变量

看 `E:\qianduan\vue-app\src\style.css` 的写法：

```css
:root {
  --primary: #42b883;
  --danger: #f56c6c;
  --text: #303133;
}
button.add { background: var(--primary); }
button.del { color: var(--danger); }
```

**一个变量改全站换色**。`:root` 就是 `<html>`，所有后代都能继承。这套机制在 Vue 组件里同样能用（`scoped` 样式里 `var(--primary)` 正常取值），是比 Sass 变量更轻的方案。

## 3. 本篇待办

- [ ] 写一个纯 HTML+CSS 的静态简历页（练语义化标签）
- [ ] 写一个 Flex 卡片布局 demo（练 `flex` / `gap` / `justify-content`）
- [ ] 对比 `content-box` 与 `border-box` 的实际宽度差异，截图记录
- [ ] 用 CSS 变量做一套深色/浅色主题切换

## 4. 参考

- [MDN CSS 参考](https://developer.mozilla.org/zh-CN/docs/Web/CSS)
- [Flex 布局教程：语法篇（阮一峰）](https://www.ruanyifeng.com/blog/2015/07/flex-grammar.html)
