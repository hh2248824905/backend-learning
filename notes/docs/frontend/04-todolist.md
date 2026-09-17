# 04 综合案例：TodoList

<NoteStatus level="done" />

::: info 素材坐标
源码：`E:\qianduan\vue-app\src\App.vue`
技术栈：Vue 3 组合式 API（`<script setup>`） + Vite

```bash
cd E:\qianduan\vue-app
npm install      # 首次
npm run dev      # http://localhost:5173
```
:::

## 0. 本篇要解决的问题

1. 上一篇的七个语法点，在一个真实需求里怎么配合？
2. `<script setup>` 相比选项式 API 好在哪？`ref` 为什么要 `.value`？
3. `computed` 的缓存机制在实际渲染中怎么体现？
4. `watch` 和 `computed` 都能「响应数据变化」，什么时候用哪个？
5. 刷新页面数据就没了，怎么持久化？

## 1. 案例需求

一个待办事项列表，包含：

| 功能 | 用到的语法点 |
|---|---|
| 输入框添加待办 | `v-model` + `@keyup.enter` + `@click` |
| 列表展示 | `v-for` + `:key` |
| 勾选完成 | `:checked` + `@change` + 动态 class |
| 删除单项 / 清除已完成 | `@click` |
| 筛选：全部 / 未完成 / 已完成 | `v-for` + 动态 class + `@click` 切换状态 |
| 空状态提示 | `v-if` / `v-else` |
| 统计未完成 / 已完成数量 | `computed` |
| **刷新后数据不丢** | `watch` + `localStorage` |

**八个语法点全部用上**，且都不是为了演示而演示 —— 每个都在解决真实需求。

## 2. 组合式 API 结构

```vue [App.vue]
<script setup>
import { ref, computed, watch } from 'vue'

/* ---------- 响应式数据 ---------- */
const newTodo = ref('')
const filter = ref('all')
const todos = ref([...])

/* ---------- 计算属性 ---------- */
const filteredTodos = computed(() => { ... })
const activeCount = computed(() => { ... })
const doneCount = computed(() => { ... })

/* ---------- 侦听器 ---------- */
watch(todos, (val) => { ... }, { deep: true })

/* ---------- 方法 ---------- */
function addTodo() { ... }
function toggleTodo(id) { ... }
function removeTodo(id) { ... }
function clearDone() { ... }
</script>

<template>
  <!-- 结构与上面一一对应 -->
</template>

<style scoped>
/* 样式 */
</style>
```

### 2.1 `<script setup>` 省掉了什么

对比上一篇的选项式 API：

| 选项式 | 组合式 |
|---|---|
| `data() { return { count: 0 } }` | `const count = ref(0)` |
| `computed: { double() { return this.count * 2 } }` | `const double = computed(() => count.value * 2)` |
| `methods: { add() { this.count++ } }` | `function add() { count.value++ }` |
| 模板里写 <code v-pre>{{ count }}</code> | 模板里也写 <code v-pre>{{ count }}</code> |

**`<script setup>` 的关键特性**：**顶层声明的变量、函数，自动暴露给模板**。不需要 `return` 出去，也不需要 `this`。

```vue
<script setup>
const msg = 'hello'      // 模板里可以直接用 {{ msg }}
function sayHi() {}       // 模板里可以直接 @click="sayHi"
</script>

<template>
  <p>{{ msg }}</p>
  <button @click="sayHi">打招呼</button>
</template>
```

**为什么没有 `this`**：组合式 API 里数据和方法都在同一个作用域，直接按名字引用就行，不用 `this` 中转。

::: tip 选哪个：选项式还是组合式
| 场景 | 建议 |
|---|---|
| 学概念、写小 demo | 选项式（结构清晰，一眼看到有什么数据） |
| **真实项目、逻辑复杂的组件** | **组合式**（相关逻辑可以放一起，不用在 `data`/`computed`/`methods` 之间跳） |
| 维护老项目 | 跟随项目现有风格 |

**组合式最大的优势**：一个功能相关的代码可以写在一起。选项式里，一个「待办筛选」功能的状态在 `data`、逻辑在 `computed`、处理方法在 `methods` —— 三处分散。组合式里它们连着写。

Vue 官方已经把**组合式 API + `<script setup>` 作为推荐写法**。
:::

## 3. `ref` 与 `.value`

```js
const newTodo = ref('')       // 声明
newTodo.value = '买牛奶'       // 脚本里改：要 .value
```

```html
<input v-model="newTodo" />   <!-- 模板里用：不要 .value -->
```

::: warning 这是组合式 API 最容易踩的坑
| 位置 | 要不要 `.value` |
|---|---|
| `<script>` 里读写 | **要** |
| `<template>` 里 | **不要**（Vue 自动解包） |

**原因**：`ref('')` 返回的是一个**对象** `{ value: '' }`，`.value` 才是真正存值的地方。Vue 在编译模板时会自动帮你解包，脚本里得自己写。

**最常见的报错**：忘了 `.value`，数据没变但也不报错。

```js
todos.push(newItem)          // ❌ 直接对 ref 对象调 push，不生效（且可能静默失败）
todos.value.push(newItem)    // ✅
```

**另一个**：在 `computed` 里忘了 `.value`

```js
// ❌ 返回的是 ref 对象本身，不是它的值
const count = computed(() => todos.length)

// ✅
const count = computed(() => todos.value.length)
```
:::

## 4. 数据初始化与持久化

### 4.1 从 `localStorage` 读初始值

```js
const STORAGE_KEY = 'vue-todos'

const DEFAULT_TODOS = [
  { id: 1, title: '学习插值语法 {{ }}', done: false },
  { id: 2, title: '学习 v-bind 属性绑定', done: false },
  { id: 3, title: '学习 v-on 事件绑定', done: true }
]

// 优先从 localStorage 读取历史数据
const saved = localStorage.getItem(STORAGE_KEY)
const todos = ref(saved ? JSON.parse(saved) : DEFAULT_TODOS)
```

**逻辑**：有存过的数据就用它，没有就用默认的三条示例。

::: tip `localStorage` 只能存字符串
```js
localStorage.setItem('key', { a: 1 })          // ❌ 存入的是 "[object Object]"
localStorage.setItem('key', JSON.stringify(x)) // ✅ 先序列化
JSON.parse(localStorage.getItem('key'))         // 读的时候反序列化
```

`localStorage` 的存储单位是字符串，对象/数组必须自己序列化。
:::

### 4.2 用 `watch` 自动保存

```js
watch(
  todos,
  (val) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
  },
  { deep: true }        // ← 关键
)
```

**`{ deep: true }` 为什么必须加**：

```js
todos.value.push(...)                  // 改的是数组内部，数组引用没变
todos.value[0].done = true             // 改的是对象内部属性
```

Vue 的 `watch` **默认只比较引用**。`push` 之后 `todos.value` 还是同一个数组对象，引用没变 → **不触发**。

加 `deep: true` 后会**递归遍历内部属性**，任何深层的改动都能侦听到。

| 写法 | 触发时机 |
|---|---|
| `watch(todos, cb)` | 只有 `todos.value = 新数组` 时触发 |
| `watch(todos, cb, { deep: true })` | 数组内任何元素的任何属性变化都触发 |

## 5. `computed` 的三个用法

```js
// ① 根据筛选条件算出要展示的列表
const filteredTodos = computed(() => {
  if (filter.value === 'active') return todos.value.filter((t) => !t.done)
  if (filter.value === 'done') return todos.value.filter((t) => t.done)
  return todos.value
})

// ② 统计未完成数量
const activeCount = computed(() => todos.value.filter((t) => !t.done).length)

// ③ 统计已完成数量
const doneCount = computed(() => todos.value.filter((t) => t.done).length)
```

**缓存的实际意义**：模板里 <code v-pre>{{ activeCount }}</code> 只出现一次，但如果父组件重新渲染导致这个组件也重渲染，`computed` 会**直接返回上次的结果**（因为 `todos` 没变），不重新遍历数组。

**如果用 `methods` 会怎样**：

```js
function getActiveCount() {
  return todos.value.filter((t) => !t.done).length    // 每次渲染都完整遍历一遍
}
```

每次渲染都遍历整个数组 —— 列表大了以后是明显的性能浪费。

::: tip `computed` 里不要做副作用
```js
// ❌ 反面教材
const filteredTodos = computed(() => {
  localStorage.setItem('cache', JSON.stringify(todos.value))   // 写操作！
  return todos.value.filter(...)
})
```

**`computed` 应该是纯函数**：只根据依赖算出一个值，不改任何外部状态。

原因：`computed` 的执行时机由 Vue 决定（懒执行 + 缓存），可能执行 0 次也可能多次，副作用会导致不可预期的行为。

**要写副作用用 `watch`。**
:::

## 6. `computed` vs `watch` 怎么选

这是本篇最重要的一条判断。

| 需求 | 用哪个 | 为什么 |
|---|---|---|
| 「已完成的条数」 | `computed` | 这是个**派生值**，从 `todos` 算出来的 |
| 「数据变了就存到 localStorage」 | `watch` | 这是个**副作用**，要执行一个动作 |
| 「筛选后的列表」 | `computed` | 派生值 |
| 「输入框变化就搜索接口」 | `watch`（+ 防抖） | 副作用，要调外部接口 |
| 「用户 ID 变了就重新拉数据」 | `watch` | 副作用 |

**判断口诀**：

```
我要「得到一个值」  → computed
我要「做一件事」    → watch
```

::: warning 能用 `computed` 就别用 `watch`
新手常见的反模式：

```js
// ❌ 用 watch 算一个派生值（多余，还容易出 bug）
const filteredTodos = ref([])
watch([todos, filter], () => {
  filteredTodos.value = todos.value.filter(...)
}, { immediate: true })     // 还要 immediate 才能初始化

// ✅ 直接用 computed
const filteredTodos = computed(() => todos.value.filter(...))
```

`computed` 自动处理依赖追踪和初始化，代码更短、更不容易错。
:::

## 7. 方法实现

```js
// 生成新 ID：取现有最大 ID + 1
let nextId = todos.value.length ? Math.max(...todos.value.map((t) => t.id)) + 1 : 1

function addTodo() {
  const title = newTodo.value.trim()
  if (!title) return                          // 空白输入不添加
  todos.value.push({ id: nextId++, title, done: false })
  newTodo.value = ''                          // 清空输入框
}

function toggleTodo(id) {
  const t = todos.value.find((t) => t.id === id)
  if (t) t.done = !t.done
}

function removeTodo(id) {
  todos.value = todos.value.filter((t) => t.id !== id)    // 赋新数组
}

function clearDone() {
  todos.value = todos.value.filter((t) => !t.done)
}
```

**三个细节值得注意**：

| 细节 | 说明 |
|---|---|
| `newTodo.value.trim()` | 先去掉首尾空格，防止「只输入空格」也能添加 |
| `if (!title) return` | **边界处理**：空标题直接返回，不加空条目 |
| `todos.value = todos.value.filter(...)` | **重新赋值**而不是原地 `splice` |

::: tip 为什么用 `filter` 赋新值而不是 `splice`
两种写法都能工作：

```js
todos.value.splice(index, 1)                     // 原地修改
todos.value = todos.value.filter((t) => t.id !== id)   // 生成新数组
```

**第二种更好**，原因：
1. **不需要先找 index**，代码更短
2. 声明的语义是「留下满足条件的」，比「删掉第 N 个」更贴合业务意图
3. 不易出现「边遍历边删除导致索引错位」的 bug

**Vue 3 用 Proxy 实现响应式**，`splice` 这种原地修改也能侦听到 —— 所以不是「必须」赋新值，而是**赋新值更清晰**。
:::

**ID 生成的小问题**（代码里已规避）：

```js
let nextId = todos.value.length ? Math.max(...todos.value.map((t) => t.id)) + 1 : 1
```

**为什么要取 max 而不是 `todos.length + 1`**：

如果初始数据是 `id=1,2,3`，删除 `id=2` 后 `length = 2`，用 `length + 1 = 3` 就**和已有的 `id=3` 冲突了**。

取最大 ID + 1 不会有这个问题。

> 这只是个前端演示级别的方案。真实项目里 ID 应该由后端生成，或者用 `crypto.randomUUID()`。

## 8. 模板部分

### 8.1 输入区

```vue
<div class="input-row">
  <input
    v-model="newTodo"
    type="text"
    placeholder="输入待办事项，回车或点「添加」"
    @keyup.enter="addTodo"
  />
  <button class="add" @click="addTodo">添加</button>
</div>
```

`@keyup.enter` 是**按键修饰符**，只在该键按下时触发。用户体验上「回车添加」是待办类应用的标配。

### 8.2 筛选标签

```vue
<div class="filters">
  <button
    v-for="f in filters"
    :key="f.value"
    :class="{ active: filter === f.value }"
    @click="filter = f.value"
  >
    {{ f.label }}
  </button>
</div>
```

```js
const filters = [
  { label: '全部', value: 'all' },
  { label: '未完成', value: 'active' },
  { label: '已完成', value: 'done' }
]
```

**三个语法点在这里配合**：

| 语法 | 作用 |
|---|---|
| `v-for` | 遍历渲染三个按钮 |
| `:class="{ active: xxx }"` | 当前选中的按钮加 `active` 类（高亮） |
| `@click="filter = f.value"` | 点击直接修改 `filter`，`computed` 自动重算列表 |

**注意 `@click` 里是「赋值表达式」而不是方法调用** —— 逻辑只有一行时这样写更简洁。

### 8.3 列表区

```vue
<ul v-if="filteredTodos.length" class="todo-list">
  <li v-for="item in filteredTodos" :key="item.id" :class="{ done: item.done }">
    <input type="checkbox" :checked="item.done" @change="toggleTodo(item.id)" />
    <span class="title">{{ item.title }}</span>
    <button class="del" @click="removeTodo(item.id)">删除</button>
  </li>
</ul>
<p v-else class="empty">暂无待办事项 🎉</p>
```

**`v-if` + `v-for` 的正确分工**（呼应 [03 篇](/frontend/03-vue-literal#_4-6-列表渲染-v-for) 的提醒）：

```vue
<!-- ❌ v-if 和 v-for 在同一元素上 -->
<li v-for="item in todos" v-if="item.done">
<!-- Vue 3 里 v-if 优先级更高，此时 item 还不存在，报错 -->

<!-- ✅ v-if 在外层容器，v-for 在内层 -->
<ul v-if="filteredTodos.length">
  <li v-for="item in filteredTodos">
```

**`v-if` 判断的是「过滤后的列表是否为空」**，用来切换「显示列表」和「显示空状态」两种 UI。

**勾选框的写法值得注意**：

```vue
<input type="checkbox" :checked="item.done" @change="toggleTodo(item.id)" />
```

用的是 `:checked` + `@change`，**而不是 `v-model="item.done"`**。

**两种写法都能工作**：

```vue
<!-- 写法一：v-model（更简洁） -->
<input type="checkbox" v-model="item.done" />

<!-- 写法二：:checked + @change（更显式） -->
<input type="checkbox" :checked="item.done" @change="toggleTodo(item.id)" />
```

**写法二的好处**：状态变更走 `toggleTodo` 方法，可以在里面加日志、加埋点、加校验。写法一直接改了数据，没有插入逻辑的位置。

**这是「显式优于隐式」的一个取舍。**

### 8.4 底部统计

```vue
<div class="footer">
  <span>未完成 <b>{{ activeCount }}</b> · 已完成 <b>{{ doneCount }}</b></span>
  <button v-if="doneCount" class="clear" @click="clearDone">清除已完成</button>
</div>
```

**`v-if="doneCount"` 的用意**：没有已完成项时，**「清除已完成」按钮根本不渲染** —— 而不是渲染了但点了没反应。

**这是比 `:disabled` 更好的处理** —— 不需要的功能就别显示出来。

## 9. `<style scoped>`

```vue
<style scoped>
.todo-app { ... }
</style>
```

**`scoped` 做了什么**：给这个组件的所有元素加一个**唯一的属性**（如 `data-v-7ba5bd90`），CSS 选择器自动变成：

```css
.todo-app[data-v-7ba5bd90] { ... }
```

**效果**：这个组件的样式**不会影响其他组件**，也不会被其他组件影响。

::: warning `scoped` 的两个边界情况
**① 子组件的根元素**

父组件的 `scoped` 样式**能影响子组件的根元素**（这是设计行为，为了让父组件能调整子组件的外边距等）。

**② 深度选择器**

想影响子组件内部元素，用 `:deep()`：

```css
.parent :deep(.child-inner) { color: red; }
```
:::

## 10. 小结与自检

### 八个语法点的最终映射

| # | 语法点 | 在项目里的位置 | 解决的需求 |
|---|---|---|---|
| ① | 插值 <code v-pre>{{ }}</code> | <code v-pre>{{ item.title }}</code>、<code v-pre>{{ activeCount }}</code> | 展示数据 |
| ② | `v-bind` / `:` | `:key`、`:checked`、`:class` | 动态属性 |
| ③ | `v-on` / `@` | `@click`、`@change`、`@keyup.enter` | 响应用户操作 |
| ④ | `v-model` | `v-model="newTodo"` | 输入框双向绑定 |
| ⑤ | `v-if` / `v-else` | 列表 vs 空状态、清除按钮 | 条件渲染 |
| ⑥ | `v-for` | 待办列表、筛选标签 | 列表渲染 |
| ⑦ | `computed` | `filteredTodos`、`activeCount`、`doneCount` | 派生数据 |
| ⑧ | `watch` | 监听 `todos` 存 `localStorage` | 副作用（持久化） |

### 核心结论

1. `<script setup>` 顶层声明自动暴露给模板，不需要 `return` 和 `this`
2. `ref` 在脚本里要 `.value`，在模板里不要
3. `watch` 侦听对象/数组内部变化必须加 `deep: true`
4. `computed` 求值，`watch` 做事 —— 能用 `computed` 就别用 `watch`
5. `computed` 必须是纯函数，不写副作用
6. `v-if` 和 `v-for` 不要同元素使用，用 `computed` 先过滤
7. `<style scoped>` 靠唯一属性实现样式隔离

### 自检清单

- [ ] 脚本里写 `count = 1` 和 `count.value = 1` 有什么区别？
- [ ] `watch(todos, cb)` 不加 `deep` 时，`todos.value.push(x)` 会触发吗？
- [ ] 「已完成数量」为什么用 `computed` 不用 `methods`？
- [ ] 「数据变化就存 localStorage」为什么不能用 `computed`？
- [ ] 为什么删除后要 `todos.value = ...` 而不是 `todos.value.splice(...)`？（两种都能跑，说清取舍）
- [ ] `v-if` 和 `v-for` 写在同一元素上，Vue 3 里会发生什么？
- [ ] `<style scoped>` 是怎么做到样式不串的？

### 下一步

[05 Vite 工程化项目](/frontend/05-vite-project) —— 这个 `.vue` 文件是怎么被浏览器认出来的？构建工具做了哪些事？
