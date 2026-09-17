import DefaultTheme from 'vitepress/theme'
import type { Theme } from 'vitepress'
import NoteStatus from './components/NoteStatus.vue'
import './style.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    // 全局组件：在任意 .md 里直接写 <NoteStatus level="done" /> 标记笔记进度
    app.component('NoteStatus', NoteStatus)
  }
} satisfies Theme
