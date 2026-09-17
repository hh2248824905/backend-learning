"""把行内代码里含 {{ 表达式 }} 的写法替换为 <code v-pre>...</code>。

为什么：VitePress 只给围栏代码块加 v-pre，行内代码 <code> 没有，
里面的 {{ expr }} 会被 Vue 编译成插值 —— expr 里有属性访问/方法调用时直接构建崩溃。
"""
import re
import sys
from pathlib import Path

DOCS = Path(r"E:\notes\docs")
FENCE = re.compile(r"^\s*(```|~~~)")
HEADING = re.compile(r"^\s*#{1,6}\s")
# 行内代码：`...{{ ... }}...`
INLINE_WITH_INTERP = re.compile(r"`([^`\n]*\{\{[^}\n]*\}\}[^`\n]*)`")


def convert(content: str) -> str:
    return f"<code v-pre>{content}</code>"


changed = []
for md in sorted(DOCS.rglob("*.md")):
    lines = md.read_text(encoding="utf-8").splitlines()
    in_fence = False
    out = []
    hit_lines = []
    for i, line in enumerate(lines, 1):
        if FENCE.match(line):
            in_fence = not in_fence
            out.append(line)
            continue
        if not in_fence and not HEADING.match(line) and INLINE_WITH_INTERP.search(line):
            new_line = INLINE_WITH_INTERP.sub(lambda m: convert(m.group(1)), line)
            if new_line != line:
                hit_lines.append(i)
                line = new_line
        out.append(line)
    if hit_lines:
        md.write_text("\n".join(out) + "\n", encoding="utf-8")
        changed.append((md, hit_lines))

for md, hit_lines in changed:
    print(f"已修改 {md.relative_to(DOCS)}  行 {hit_lines}")
print(f"共 {len(changed)} 个文件")
print("\n注意：标题行已跳过，需手工处理（改标题文案，不要引入 HTML）")
