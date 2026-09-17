// 探测 VitePress 的 markdown 渲染规则：哪些写法里的 {{ }} 会被 Vue 当成插值
import { createMarkdownRenderer } from 'vitepress'

const md = await createMarkdownRenderer('E:/notes/docs', {}, '/')

const cases = {
  'inline 代码（段落内）': '`{{ item.title }}`',
  'inline 代码（表格单元格内）':
    '| 语法点 | 示例 |\n|---|---|\n| 插值 | `<span>{{ item.title }}</span>` |',
  'fenced 代码块': '```vue\n<span>{{ item.title }}</span>\n```',
  '普通文本内花括号': '模板里写 `:class="{ done: item.done }"` 是合法的'
}

for (const [name, src] of Object.entries(cases)) {
  console.log('---', name, '---')
  console.log(md.render(src).trim())
  console.log()
}
