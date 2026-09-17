"""校验 VitePress 站内锚点链接是否有效。

规则：VitePress（markdown-it-anchor + slugify）把标题转成锚点
- 去掉标点符号（. ： ` 「」等）
- 空格 -> -
- 纯中文保留
- 以数字开头时前面补 _
"""
import re
import sys
import unicodedata
from pathlib import Path

DOCS = Path(r"E:\notes\docs")


# VitePress（@mdit-vue/shared slugify）的真实规则
R_COMBINING = re.compile(r"[\u0300-\u036f]")
R_CONTROL = re.compile(r"[\u0000-\u001f]")
R_SPECIAL = re.compile(r"""[\s~`!@#$%^&*()\-_+=\[\]{}|\\;:"'“”‘’<>,.?/]+""")
R_DUP_DASH = re.compile(r"-{2,}")
R_EDGE_DASH = re.compile(r"^-+|-+$")


def slugify(text: str) -> str:
    s = text
    # 去掉 markdown 标题末尾的自定义锚点 {#xxx}
    s = re.sub(r"\{#[^}]*\}\s*$", "", s)
    # 去掉 markdown 链接语法，保留文字
    s = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", s)
    # NFKD 归一化：全角标点（，：（））会被分解成 ASCII 标点
    s = unicodedata.normalize("NFKD", s)
    s = R_COMBINING.sub("", s)
    s = R_CONTROL.sub("", s)
    s = R_SPECIAL.sub("-", s)
    s = R_DUP_DASH.sub("-", s)
    s = R_EDGE_DASH.sub("", s)
    s = s.lower()
    # 以数字开头时补前导下划线
    if s and s[0].isdigit():
        s = "_" + s
    return s


def collect_anchors(md_path: Path) -> set[str]:
    anchors = set()
    for line in md_path.read_text(encoding="utf-8").splitlines():
        m = re.match(r"^(#{1,6})\s+(.*)$", line)
        if not m:
            continue
        raw = m.group(2)
        custom = re.search(r"\{#([^}]+)\}", raw)
        if custom:
            anchors.add(custom.group(1))
        anchors.add(slugify(raw))
    return anchors


def md_to_path(link: str, cur: Path) -> Path | None:
    rel = link.strip("/")
    if not rel:
        return None
    for cand in (
        DOCS / (rel + ".md"),
        DOCS / rel / "index.md",
        DOCS / rel,
    ):
        if cand.is_file():
            return cand
    return None


LINK_RE = re.compile(r"\]\((/[^)\s]*|#[^)\s]*)\)")
problems = []
checked = 0

for md in sorted(DOCS.rglob("*.md")):
    text = md.read_text(encoding="utf-8")
    for i, line in enumerate(text.splitlines(), 1):
        for link in LINK_RE.findall(line):
            if "#" not in link:
                continue
            path_part, anchor = link.split("#", 1)
            target = md if not path_part else md_to_path(path_part, md)
            if target is None:
                problems.append((md, i, link, "目标文件不存在"))
                continue
            checked += 1
            if anchor not in collect_anchors(target):
                problems.append((md, i, link, f"锚点不存在于 {target.name}"))

print(f"校验带锚点链接 {checked} 条")
if problems:
    print(f"\n发现 {len(problems)} 处问题：")
    for md, i, link, why in problems:
        print(f"  {md.relative_to(DOCS)}:{i}  {link}  -> {why}")
    sys.exit(1)
print("全部有效 ✅")

# ---------- 附加告警：标题里含不会被 NFKD 归一化的中日韩标点 ----------
# 这类标题生成的锚点会保留原字符，链接时若写成 "-" 会静默 404
HARD_PUNCT = "、。「」『』《》【】—…·"
warns = []
for md in sorted(DOCS.rglob("*.md")):
    for i, line in enumerate(md.read_text(encoding="utf-8").splitlines(), 1):
        m = re.match(r"^(#{1,6})\s+(.*)$", line)
        if not m or re.search(r"\{#[^}]+\}", line):
            continue
        title = m.group(2)
        # 行内代码块里的符号不影响锚点之外的语义，但仍会进锚点
        hit = [c for c in HARD_PUNCT if c in title]
        if hit:
            warns.append((md, i, title, slugify(title)))

if warns:
    print(f"\n提示：{len(warns)} 个标题含不会归一化的标点，锚点里会原样保留：")
    for md, i, title, slug in warns[:12]:
        print(f"  {md.relative_to(DOCS)}:{i}  {title}  ->  #{slug}")
    if len(warns) > 12:
        print(f"  ... 另有 {len(warns) - 12} 处")
    print("  （引用这些标题时，锚点必须照抄上面输出的内容）")
