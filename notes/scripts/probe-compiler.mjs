// 测试 Vue 模板编译器对几种插值写法的反应
import { compile } from '@vue/compiler-dom'

const cases = ['<div>{{ }}</div>', '<div>{{ 表达式 }}</div>', '<div>{{ count }}</div>', '<div>{{ item.title }}</div>']

for (const c of cases) {
  try {
    compile(c)
    console.log('OK    ', c)
  } catch (e) {
    console.log('FAIL  ', c, '->', e.message.split('\n')[0])
  }
}
