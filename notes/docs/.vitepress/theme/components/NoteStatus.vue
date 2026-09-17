<script setup lang="ts">
/**
 * 笔记进度标记组件
 * 用法：<NoteStatus level="done" /> / <NoteStatus level="wip" /> / <NoteStatus level="todo" />
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    level?: 'done' | 'wip' | 'todo'
    text?: string
  }>(),
  { level: 'todo' }
)

const config = {
  done: { label: '已完成', cls: 'is-done' },
  wip: { label: '整理中', cls: 'is-wip' },
  todo: { label: '待补充', cls: 'is-todo' }
}

const current = computed(() => config[props.level])
</script>

<template>
  <span class="note-status" :class="current.cls">
    <span class="dot" />
    {{ text || current.label }}
  </span>
</template>

<style scoped>
.note-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 5px;
  border-radius: 2px;
  font-size: 12px;
  line-height: 18px;
  vertical-align: middle;
  white-space: nowrap;
  border: 1px solid currentColor;
}
.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
.is-done {
  color: var(--vp-c-green-1);
}
.is-wip {
  color: var(--vp-c-yellow-1);
}
.is-todo {
  color: var(--vp-c-text-3);
}
</style>
