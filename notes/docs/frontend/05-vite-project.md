# 05 Vite 工程化项目

<NoteStatus level="done" />

::: info 模块坐标
源码：`E:\qianduan\vue-app`　·　仓库：[frontend-learning](https://github.com/hh2248824905/frontend-learning)　·　技术栈：Vue 3.5 + Vite
:::

## 0. 从 CDN 到工程化，差了什么

`01-vue-literal/index.html` 那种 CDN 写法能跑，但只能写单文件、约等于玩具：

| | CDN 字面量 | Vite 工程 |
|---|---|---|
| 引入方式 | `<script src="unpkg...">` | `npm install vue` |
| 依赖管理 | 无，靠手改 URL | `package.json` + lock 文件 |
| 组件拆分 | 做不到（一个 HTML 里塞所有） | `.vue` 单文件组件 |
| 语法支持 | 浏览器支持什么写什么 | ES6+ / TS / SCSS 全支持，构建时降级 |
| 热更新 | 无，改完手动刷 | HMR，保存即刷新且保留状态 |
| 打包产物 | 无 | `dist/` 静态文件，可部署 |

**结论**：超过一个页面的项目，必须上构建工具。

## 1. 项目结构

```
E:\qianduan\vue-app\
├── index.html              ← 入口 HTML（Vite 唯一的 HTML）
├── package.json            ← 依赖与脚本
├── vite.config.js          ← 构建配置
├── public\                 ← 不参与打包的静态资源，原样拷贝到 dist
│   ├── favicon.svg
│   └── icons.svg
├── src\
│   ├── main.js             ← JS 入口：createApp + mount
│   ├── App.vue             ← 根组件
│   ├── style.css           ← 全局样式
│   └── assets\             ← 参与打包的资源（会被加 hash 文件名）
└── dist\                   ← 构建产物（已被 .gitignore）
```

**`public/` 与 `src/assets/` 的区别**（高频困惑）：

| | 引用路径 | 打包后 | 适用 |
|---|---|---|---|
| `public/logo.svg` | `/logo.svg`（绝对路径） | 原样拷贝，文件名不变 | favicon、需固定 URL 的文件 |
| `src/assets/vue.svg` | `import url from './assets/vue.svg'` | 加内容 hash，如 `vue-a1b2c3.svg` | 组件内引用的图片 |

hash 文件名是为了**缓存失效**：内容一变文件名就变，浏览器不会用到旧缓存。

## 2. 三个配置文件的真实内容

### 2.1 `package.json`

```json
{
  "name": "qianduan",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": { "vue": "^3.5.41" },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.8",
    "vite": "^8.2.2"
  }
}
```

| 字段 | 作用 |
|---|---|
| `"type": "module"` | 让 `.js` 文件按 ESM 解析，能用 `import` 语法 |
| `private: true` | 防止误 `npm publish` 到 npm 仓库 |
| `dependencies` vs `devDependencies` | 前者是运行时依赖（vue），后者只构建时用（vite、插件） |
| `^3.5.41` | 允许升级到 `3.x` 最新，不允许升 `4.0.0` |

⚠️ 版本号规则（semver）：`^1.2.3` = `>=1.2.3 <2.0.0`；`~1.2.3` = `>=1.2.3 <1.3.0`；`1.2.3` = 锁死。

### 2.2 `vite.config.js`

```js
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [vue()],
})
```

`defineConfig` 的作用是给 IDE 提供类型提示——不写它也能跑，但没补全。

**加常用配置的样子**（本项目暂未需要，记在这里备用）：

```js
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 前端 5173 调后端 8002，绕开跨域
      '/api': {
        target: 'http://localhost:8002',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

`proxy` 是前后端联调的关键：浏览器只认自己的源，跨域请求被拦，靠开发服务器转发解决。

### 2.3 `index.html`

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Vue 基础语法练习</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

三个关键点：
1. **`<div id="app">`** 是挂载点，JS 里 `mount('#app')` 找的就是它
2. **`type="module"`** 表示按 ES Module 加载，Vite 靠它做按需编译
3. 路径写 `/src/main.js`，不是 `./src/main.js`——Vite 把它当项目根解析

## 3. 入口文件 `src/main.js`

```js
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

createApp(App).mount('#app')
```

三行代码对应三件事：
1. 从 vue 包取 `createApp` 工厂函数
2. 引入全局样式（构建时会被抽成单独 CSS）
3. 创建应用实例，挂到 `#app` 上

**常见错误**：`mount('#app')` 写成 `mount('app')`（少了 `#`），页面全白，控制台 `Failed to mount app: mount target selector "#app" returned null`。

## 4. 单文件组件（SFC）

Vue 的 `.vue` 文件把三件事塞在一个文件里，靠 `<script setup>` 提升写法：

```vue
<script setup>
import { ref, computed, watch } from 'vue'
const newTodo = ref('')
const activeCount = computed(() => todos.value.filter(t => !t.done).length)
</script>

<template>
  <input v-model="newTodo" @keyup.enter="addTodo" />
  <span>未完成 <b>{{ activeCount }}</b></span>
</template>

<style scoped>
.footer b { color: #42b883; }
</style>
```

| 块 | 对应层面的什么 | 注意 |
|---|---|---|
| `<script setup>` | 逻辑（数据、方法、生命周期） | 顶层声明的变量/函数**自动暴露给模板**，不用 `return` |
| `<template>` | 结构 | 可以有多个根节点（Vue 3 支持 Fragment） |
| `<style scoped>` | 样式 | 加 `scoped` 后自动加唯一属性选择器，只影响本组件 |

### 4.1 `scoped` 实现原理

编译后变成：

```css
.footer b[data-v-7ba5bd90] { color: #42b883; }
```

每个组件一个唯一 hash，天然隔离。想穿透到子组件用 `:deep(.child-class)`。

### 4.2 组合式 API vs 选项式 API

同一个 `evenOrOdd`，两种写法对比：

```js
// 选项式（01-vue-literal 用的）
export default {
  data() { return { count: 0 } },
  computed: { evenOrOdd() { return this.count % 2 === 0 ? '偶数' : '奇数' } }
}

// 组合式（vue-app 用的）
const count = ref(0)
const evenOrOdd = computed(() => count.value % 2 === 0 ? '偶数' : '奇数')
```

| | 选项式 API | 组合式 API |
|---|---|---|
| 代码组织 | 按选项类型分块（data / methods / computed） | 按业务逻辑分块，相关代码放一起 |
| `this` | 依赖，箭头函数会踩坑 | 不依赖，纯变量 |
| 逻辑复用 | mixins（来源不清晰、命名冲突） | 组合式函数 `useXxx()` |
| 大组件可读性 | 差（一个功能散落在 5 个块里） | 好 |
| 上手难度 | 低 | 中 |

**本项目结论**：练习 `01-vue-literal` 用选项式理解概念，`vue-app` 起用组合式——这是官方推荐的新项目默认写法。

### 4.3 忘记 `.value` 是最常见错误

```js
const count = ref(0)
count = 1          // ❌ 报错，count 是对象不是数字
count.value = 1    // ✅ 改的是 .value

const todos = ref([])
todos.filter(...)        // ❌ ref 对象上没有 filter
todos.value.filter(...)  // ✅
```

**为什么模板里不用写 `.value`**：Vue 编译模板时会自动解包 ref。所以 <code v-pre><span>{{ count }}</span></code> 能正确显示，但 `<script>` 里必须手写。

## 5. 命令与脚本

::: code-group

```bash [开发]
npm run dev        # 启动 dev server，默认 http://localhost:5173
```

```bash [构建]
npm run build      # 产出 dist/，含压缩混淆、代码分割
```

```bash [预览构建产物]
npm run preview    # 本地起一个静态服务器指向 dist/，验证生产构建是否正常
```

:::

**`dev` 和 `preview` 的区别**：`dev` 用源码 + 按需编译 + HMR，速度快但不代表生产行为；`preview` 跑的是打包后的真实产物，**上线前一定用它验证一次**——有些问题（如按路径分割的懒加载）只在构建后暴露。

> ⚠️ 本机环境坑：这台机器的 Node 是 WorkBuddy 托管版本，`npm` 不在 PATH 里。跑命令要用完整路径：
>
> ```bash
> "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node.exe" \
>   "C:/Users/a1788/.workbuddy/binaries/node/versions/22.22.2-2/node_modules/npm/bin/npm-cli.js" run dev
> ```
>
> 详见 [踩坑与排错记录 - Git Bash 下 mvn 命令失效](/engineering/troubleshooting#_4-git-bash-下-mvn-命令失效)。

## 6. TodoList 综合案例

`src/App.vue` 用一个案例覆盖 8 个核心语法点，是 03 篇章的实战检验。

### 8 个语法点与代码位置

| 语法点 | 代码位置 | 作用 |
|---|---|---|
| ① 插值 <code v-pre>{{ }}</code> | <code v-pre><span class="title">{{ item.title }}</span></code> | 数据显示到页面 |
| ② `v-bind`（简写 `:`） | `:class="{ done: item.done }"`、`:checked="item.done"` | 属性动态绑定 |
| ③ `v-on`（简写 `@`） | `@click="addTodo"`、`@keyup.enter="addTodo"` | 事件绑定 + 修饰符 |
| ④ `v-model` | `<input v-model="newTodo" />` | 双向绑定表单 |
| ⑤ `v-if` / `v-else` | `<ul v-if="filteredTodos.length">` ... `<p v-else class="empty">` | 条件渲染 |
| ⑥ `v-for` | `v-for="item in filteredTodos" :key="item.id"` | 列表渲染 |
| ⑦ `computed` | `filteredTodos` / `activeCount` / `doneCount` | 派生状态，自动缓存 |
| ⑧ `watch` | `watch(todos, ..., { deep: true })` | 副作用：持久化到 localStorage |

### 关键实现解析

**过滤逻辑用 computed 而不是 method**：

```js
const filteredTodos = computed(() => {
  if (filter.value === 'active') return todos.value.filter((t) => !t.done)
  if (filter.value === 'done') return todos.value.filter((t) => t.done)
  return todos.value
})
```

`computed` 有缓存：只有依赖（`filter`、`todos`）变化才重算，模板里多次引用不会重复执行。用普通函数则每次渲染都跑一遍。

**持久化用 watch + localStorage**：

```js
watch(todos, (val) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
}, { deep: true })     // ← 必须 deep，否则改 item.done 不触发
```

`deep: true` 的必要性：`todos` 是个数组，往里 `push` 会触发（数组本身变了），但只改 `item.done` 时**数组引用没变**，不加 deep 监听不到。这是 `watch` 最经典的坑。

**初始化时读回本地数据**：

```js
const saved = localStorage.getItem(STORAGE_KEY)
const todos = ref(saved ? JSON.parse(saved) : DEFAULT_TODOS)
```

`JSON.parse` 可能抛异常（存的不是合法 JSON），生产代码要 try/catch，练习里简单处理。

### 运行验证

```bash
npm run dev
# → VITE ready，访问 http://localhost:5173
```

验证清单：
- [x] 输入框回车 → 新增一项，输入框清空
- [x] 勾选复选框 → 标题划线置灰，未完成计数 -1
- [x] 切换「全部 / 未完成 / 已完成」→ 列表随之过滤，按钮高亮
- [x] 删除单项 → 该行消失
- [x] 「清除已完成」→ 仅在有已完成项时出现
- [x] 刷新页面 → 数据仍在（localStorage 生效）

## 7. 下一篇方向

- 组件拆分：把 TodoList 拆成 `TodoInput` / `TodoItem` / `TodoFooter`，练 `props` + `emit`
- 引入 Vue Router，把 TodoList 和配置演示页做成两个路由
- 接入后端 `POST /config/student`，用 `fetch` + `async/await` 走通前后端联调
- 上 Pinia 做状态管理，替代 `localStorage` 手写持久化

## 8. 参考

- [Vite 官方文档](https://cn.vitejs.dev/)
- [Vue 3 组合式 API 常见问答](https://cn.vuejs.org/guide/extras/composition-api-faq.html)
- [Vue 3 响应式基础](https://cn.vuejs.org/guide/essentials/reactivity-fundamentals.html)
