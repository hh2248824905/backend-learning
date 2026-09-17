"""扫描 markdown 中会被 Vue 当成插值的 {{ }} —— 只在 fenced 代码块内是安全的。

VitePress 行为（已实测验证）：
- ``` 围栏代码块 -> <pre v-pre>  -> 安全
- 行内代码 `xxx`  -> <code> 无 v-pre -> 里面的 {{ expr }} 会被 Vue 编译，expr 未定义时报
  TypeError: Cannot read properties of undefined
- 裸文本里的 {{ expr }} 同样会被编译

修法：把含 {{ }} 的行内代码换成 <code v-pre>...</code> 裸 HTML，或挪进围栏代码块。
"""
import re
import sys
from pathlib import Path

DOCS = Path(r"E:\notes\docs")
FENCE = re.compile(r"^\s*(```|~~~)")

hits = []
for md in sorted(DOCS.rglob("*.md")):
    in_fence = False
    for i, line in enumerate(md.read_text(encoding="utf-8").splitlines(), 1):
        if FENCE.match(line):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        if re.search(r"\{\{[^}]*\}\}", line):
            # 已经用 <code v-pre> / <span v-pre> 包起来的算安全
            stripped = re.sub(r"<(code|span|div)[^>]*v-pre[^>]*>.*?</\1>", "", line)
            if re.search(r"\{\{[^}]*\}\}", stripped):
                hits.append((md, i, line.strip()))

print(f"发现 {len(hits)} 处行内 {{ }} 插值风险：")
for md, i, line in hits:
    print(f"  {md.relative_to(DOCS)}:{i}")
    print(f"      {line[:150]}")
sys.exit(1 if hits else 0)
