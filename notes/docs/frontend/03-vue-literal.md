# 03 CDN 字面量入门

<NoteStatus level="done" />

::: info 素材坐标
源码：`E:\qianduan\01-vue-literal\index.html`（**单文件，双击浏览器就能跑，不需要任何构建工具**）
:::

## 0. 本篇要解决的问题

1. 不装 Node、不装 Vite，怎么最快看到 Vue 的响应式效果？
2. `createApp()` 和 `.mount('#app')` 分别在做什么？
3. 选项式 API 的 `data` / `computed` / `methods` 各自放什么？
4. `:class` 和 `class` 有什么区别？为什么一个要冒号？
5. `v-if` 和 `v-for` 一起用有什么问题？

## 1. 为什么先用 CDN 方式学

大多数人学 Vue 的第一步是 `npm create vue@latest`，然后被一堆配置文件淹没：`vite.config.js`、`package.json`、`tsconfig.json`……还没写一行业务代码，先花两小时配环境。

**这个文件的做法相反**：一个 HTML 文件，一行 `<script>` 引入 Vue，**打开浏览器就能看到响应式**。

| 维度 | CDN 方式（本篇） | 工程化方式（[05](/frontend/05-vite-project)） |
|---|---|---|
| 依赖安装 | 无 | `npm install` |
| 构建 | 无 | Vite 编译打包 |
| `.vue` 单文件组件 | ❌ 不支持 | ✅ |
| 起步时间 | 0 秒 | 几分钟 |
| 适合 | **理解概念** | 真实项目 |

**先理解响应式是什么，再学工程化怎么组织代码。** 顺序反了容易「会敲命令但不懂原理」。

## 2. 完整代码结构

```html [01-vue-literal/index.html]
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>01 · Vue 字面量入门（CDN）</title>
  <!-- CDN 引入 Vue3 全局构建版本，无需打包工具即可使用 -->
  <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
  <style> /* 样式省略 */ </style>
</head>
<body>
  <!-- Vue 挂载点：模板里所有指令都作用在这个元素内 -->
  <div id="app">
    <!-- ... 四个演示区块 ... -->
  </div>

  <script>
    const { createApp } = Vue       // CDN 引入后挂在 window.Vue 上

    createApp({
      data() { return { /* 响应式数据 */ } },
      computed: { /* 计算属性 */ }
    }).mount('#app')
  </script>
</body>
</html>
```

::: tip `vue.global.js` 是什么意思
Vue 3 提供多种构建版本：

| 文件 | 格式 | 用途 |
|---|---|---|
| `vue.global.js` | IIFE 全局变量 | **CDN 直接用**，挂到 `window.Vue`。本篇用这个 |
| `vue.esm-browser.js` | 原生 ESM | 浏览器 `<script type="module">` 里 `import` |
| `vue.runtime.esm-bundler.js` | ESM（不含编译器） | 打包工具用，模板在构建时已编译 |

`global` 版本**包含模板编译器**（所以能直接解析 HTML 里的 <code v-pre>{{ }}</code> 和指令），体积大一些。打包项目里用 `runtime` 版本，模板在构建时编译掉，运行时更小。

**这就是为什么打包项目里的 Vue 更小** —— 编译器不需要进产物。
:::

## 3. `createApp` 与挂载

```js
const { createApp } = Vue

createApp({
  // 选项
}).mount('#app')
```

| 步骤 | 做什么 |
|---|---|
| `createApp({...})` | 创建一个**应用实例**，传入根组件的选项对象 |
| `.mount('#app')` | 把应用挂载到 `#app` 元素上，**Vue 接管这块 DOM** |

**挂载后发生的事**：

1. Vue 编译 `#app` 内部的模板（解析 <code v-pre>{{ }}</code> 和指令）
2. 建立响应式数据与 DOM 的关联
3. 之后数据变 → DOM 自动更新（不用手动操作 DOM）

::: warning 挂载点内部的内容会被当作「模板」
```html
<div id="app">
  <p>{{ count }}</p>       <!-- 这是模板，会被 Vue 编译并接管 -->
</div>
```

**挂载点内部原有的内容不会保留原样**，而是被当成模板解析。所以：

- 里面写 <code v-pre>{{ count }}</code> 才会渲染成数据
- 想要「Vue 不管这段」，加 `v-pre` 指令

**挂载点本身（`<div id="app">`）不在模板内**，所以它的属性不会被 Vue 处理。
:::

## 4. 七个核心指令

这是本篇的核心内容。四个演示区块覆盖了七个语法点。

### 4.1 插值（双大括号）

```html
<span class="num" :class="count % 2 === 0 ? 'even' : 'odd'">{{ count }}</span>
```

<code v-pre>{{ 表达式 }}</code> 把 JS 表达式的值渲染成文本。**能写表达式，不能写语句**：

```html
{{ count + 1 }}              <!-- ✅ 表达式 -->
{{ count % 2 === 0 ? '偶' : '奇' }}  <!-- ✅ 三元 -->
{{ let x = 1 }}              <!-- ❌ 语句，报错 -->
{{ if (count > 0) {} }}      <!-- ❌ 语句，报错 -->
```

**插值会转义 HTML**（防 XSS）：

```html
{{ '<b>粗体</b>' }}          <!-- 显示成文本 <b>粗体</b> -->
```

真要渲染 HTML 得用 `v-html`（**有 XSS 风险，只用于可信内容**）。

### 4.2 事件绑定 `v-on` / `@`

```html
<button @click="count++">+1</button>
<button @click="count--">-1</button>
<button @click="count = 0">重置</button>
```

| 写法 | 说明 |
|---|---|
| `v-on:click` | 完整写法 |
| `@click` | **简写**，实际都用这个 |

**三种绑定形式**：

```html
<button @click="count++">内联表达式</button>
<button @click="handleClick">方法引用（不加括号，自动传事件对象）</button>
<button @click="handleClick(1, $event)">调用方法（显式传参 + 事件对象）</button>
```

**修饰符**：

| 修饰符 | 作用 |
|---|---|
| `@click.stop` | 阻止冒泡（`event.stopPropagation()`） |
| `@click.prevent` | 阻止默认行为（`event.preventDefault()`） |
| `@keyup.enter` | 只在回车键时触发 |
| `@click.once` | 只触发一次 |

### 4.3 动态绑定 `v-bind` / `:`

```html
<!-- 静态：值是字符串 "even"，永远不变 -->
<span class="even">

<!-- 动态：值是 JS 表达式的结果，会随数据变 -->
<span :class="count % 2 === 0 ? 'even' : 'odd'">
```

**核心区别**：

| 写法 | 值被当作什么 |
|---|---|
| `class="even"` | **字符串字面量** |
| `:class="xxx"` | **JS 表达式**，在组件作用域里求值 |

**`v-bind` 的简写是 `:`**，所以 `:class` 就是 `v-bind:class`。

**动态 class 的三种语法**：

```html
<!-- ① 字符串 -->
<div :class="'box active'">

<!-- ② 对象（条件成立才加这个类） -->
<div :class="{ active: isActive, done: item.done }">

<!-- ③ 数组 -->
<div :class="[baseClass, isActive ? 'active' : '']">
```

本项目用的对象语法：`:class="{ active: filter === f.value }"` —— 条件成立才加 `active` 类。

### 4.4 双向绑定 `v-model`

```html
<input v-model="name" placeholder="输入你的名字..." />
<p v-if="name" class="hello">你好，{{ name }}！👋</p>
```

**`v-model` 是语法糖**，等价于：

```html
<input
  :value="name"
  @input="name = $event.target.value"
/>
```

**手动实现这四行 vs 写一个 `v-model`** —— 这就是它存在的意义。

**不同表单元素的 `v-model` 行为**：

| 元素 | 绑定的值 |
|---|---|
| `<input type="text">` | `value` 字符串 |
| `<input type="checkbox">` 单个 | `boolean`（是否勾选） |
| `<input type="checkbox">` 多个 | **数组**（勾选的值集合） |
| `<input type="radio">` | 选中项的值 |
| `<select>` | 选中项的 value |

**修饰符**：

| 修饰符 | 作用 |
|---|---|
| `v-model.trim` | 自动去掉首尾空格 |
| `v-model.number` | 转成数字（`type="number"` 时有用） |
| `v-model.lazy` | 在 `change` 时同步（不是每次输入） |

### 4.5 条件渲染 `v-if` / `v-else`

```html
<p v-if="name" class="hello">你好，{{ name }}！👋</p>
<p v-else class="tip">↑ 输入名字后，这里会实时显示问候语</p>
```

| 指令 | 行为 | 切换成本 |
|---|---|---|
| `v-if` | **销毁/重建** DOM 元素 | 高（首次渲染快，切换慢） |
| `v-show` | 切换 `display: none` | 低（首次渲染慢，切换快） |

**选型判断**：
- 频繁切换（比如 tab）→ `v-show`
- 条件很少变（比如权限控制）→ `v-if`

**完整的分支**：

```html
<div v-if="status === 'loading'">加载中...</div>
<div v-else-if="status === 'error'">出错了</div>
<div v-else-if="status === 'empty'">暂无数据</div>
<div v-else>正常内容</div>
```

::: tip `v-if` 和 `v-for` 不要用在同一个元素上
```html
<!-- ❌ 不推荐 -->
<li v-for="item in list" v-if="item.visible">

<!-- ✅ 推荐：用 computed 先过滤 -->
<li v-for="item in visibleList">
```

**原因**：在 Vue 3 里 `v-if` 的优先级**高于** `v-for`，意味着 `v-if` 里访问不到 `item`（`item` 还不存在），直接报错。

正确做法是用 `computed` 先过滤出列表 —— 这正好是 [04 TodoList](/frontend/04-todolist) 里的做法。
:::

### 4.6 列表渲染 `v-for`

```html
<ul>
  <li v-for="item in skills" :key="item.id">
    <b>{{ item.name }}</b> —— {{ item.desc }}
  </li>
</ul>
```

```js
skills: [
  { id: 1, name: 'HTML', desc: '页面结构' },
  { id: 2, name: 'CSS', desc: '页面样式' },
  { id: 3, name: 'JavaScript', desc: '页面行为' },
  { id: 4, name: 'Vue', desc: '响应式框架' }
]
```

**两种遍历语法**：

```html
<li v-for="item in list">                  <!-- 值 -->
<li v-for="(item, index) in list">          <!-- 值 + 索引 -->
<li v-for="(value, key, index) in obj">     <!-- 对象：值 + 键 + 索引 -->
<li v-for="n in 10">                        <!-- 数字：1 到 10 -->
```

::: danger `:key` 不是可选的
```html
<li v-for="item in list" :key="item.id">    <!-- ✅ -->
<li v-for="item in list">                    <!-- ⚠️ 报 ESLint 警告，且有过渲染 bug 风险 -->
```

**`:key` 的作用**：给 Vue 一个**稳定且唯一**的标识，用于 diff 算法判断「哪些元素是同一个」。

**为什么不能用 `index` 当 key**：

```
初始列表：[A, B, C]        key = 0, 1, 2
在头部插入 D：[D, A, B, C]  key = 0, 1, 2, 3

用 index 当 key → Vue 认为"第0个元素还是 A，只是内容变了"
                → 实际是"D 是新插入的，A/B/C 都往后挪了"
                → 结果：所有元素都被当作"更新"而不是"插入"，复用错位
```

**表现**：列表里有输入框时，删除一项后输入框的内容会串位。

**结论**：key 用**数据的唯一 ID**，没有 ID 时用 `item.id` 或后端给的主键，**绝不用 index**。
:::

## 5. 选项式 API 的三个部分

```js
createApp({
  data() {
    return {
      count: 0,
      name: '',
      skills: [...]
    }
  },
  computed: {
    evenOrOdd() {
      return this.count % 2 === 0 ? '偶数' : '奇数'
    }
  },
  methods: {
    // 事件处理函数
  }
}).mount('#app')
```

| 选项 | 放什么 | 特点 |
|---|---|---|
| `data()` | 响应式数据 | 必须是个**函数**，返回对象 |
| `computed` | 计算属性 | 有缓存，依赖不变不重算 |
| `methods` | 方法 | 每次调用都执行 |

::: warning `data` 为什么必须是函数
```js
data() { return { count: 0 } }    // ✅ 函数
data: { count: 0 }                 // ❌ 组件复用时会共享同一个对象
```

**组件被复用多次时**，如果 `data` 是个对象，所有实例会共享同一份数据 —— 改一个全变。

用函数的话，每个实例调用一次得到**独立的对象**。

**这条规则在根组件上（只挂载一次）看不出来问题，但组件化之后就是致命 bug。** 所以养成一律写函数的习惯。
:::

## 6. `computed` 和 `methods` 的区别

```js
computed: {
  evenOrOdd() {
    console.log('computed 执行了')          // 只在 count 变时执行
    return this.count % 2 === 0 ? '偶数' : '奇数'
  }
},
methods: {
  getEvenOrOdd() {
    console.log('methods 执行了')           // 每次渲染都执行
    return this.count % 2 === 0 ? '偶数' : '奇数'
  }
}
```

| 维度 | `computed` | `methods` |
|---|---|---|
| **缓存** | ✅ 依赖不变则复用上次结果 | ❌ 每次访问都重新执行 |
| 依赖追踪 | 自动（读到哪些响应式数据就依赖哪些） | 不追踪 |
| 用于模板 | ✅ 适合 | ⚠️ 可行但性能差 |
| 用于事件处理 | ❌ 不适合 | ✅ 适合 |

**验证方式**：在 `computed` 和 `methods` 里各打一个 `console.log`，然后在模板里 <code v-pre>{{ evenOrOdd }}</code> 和 <code v-pre>{{ getEvenOrOdd() }}</code> 各引用两次。

**结果**：`computed` 只打印一次，`methods` 打印两次。

**判断标准**：

```
需要在模板里展示的「派生数据」 → computed
响应用户操作的动作           → methods
```

## 7. 页面效果说明

这个文件分四个区块，每块演示一组语法：

| 区块 | 演示的语法 | 交互效果 |
|---|---|---|
| ① 计数器 | <code v-pre>{{ }}</code> + `@click` + `:class` | 加减按钮改变数字，数字**偶数绿、奇数红** |
| ② 问候语 | `v-model` + `v-if` / `v-else` | 输入框实时绑定，有内容显示问候语，没内容显示提示 |
| ③ 技能列表 | `v-for` + `:key` | 渲染四项技能 |
| ④ 计数提示 | `computed` | 显示「当前计数是 偶数/奇数」 |

**动一下手验证**：

1. 点 `+1` 三次 —— 数字变 3，颜色从绿变红，提示语变「奇数」
2. 在输入框敲字 —— 问候语实时出现，清空后回到提示文案
3. 打开控制台改 `count` —— 数字和颜色同时更新（这就是响应式）

## 8. 小结与自检

### 核心结论

1. CDN 方式用 `vue.global.js`，`createApp().mount()` 两行就能跑
2. <code v-pre>{{ }}</code> 能写表达式不能写语句，且会转义 HTML
3. `:class` 的值是 JS 表达式，`class` 的值是字符串字面量
4. `v-model` 是 `:value` + `@input` 的语法糖
5. `v-if` 销毁重建 DOM，`v-show` 切 `display`，按切换频率选
6. `v-if` 和 `v-for` 不能同用在一个元素上（Vue 3 里 `v-if` 优先级更高）
7. `:key` 必须用唯一 ID，**不能用 index**
8. `data` 必须是函数；`computed` 有缓存，`methods` 没有

### 自检清单

- [ ] `createApp()` 和 `.mount()` 各自做了什么？
- [ ] `class="even"` 和 `:class="'even'"` 有什么区别？
- [ ] `v-model` 展开成哪两个绑定？
- [ ] `v-if` 和 `v-show` 在 DOM 层面分别做了什么？
- [ ] 为什么 `v-for` 里的 `:key` 不能用 index？举一个出 bug 的场景。
- [ ] `data` 为什么必须写成函数？
- [ ] `computed` 和 `methods` 在模板里各引用两次，哪个会执行两次？

### 下一步

[04 综合案例 TodoList](/frontend/04-todolist) —— 把这七个语法点串成一个真实可用的应用，并换成组合式 API。
