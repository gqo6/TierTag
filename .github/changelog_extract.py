import os
import re
import sys

ver = os.environ.get("GITHUB_REF_NAME", "")
text = open("CHANGELOG.md", encoding="utf-8-sig").read().replace("\r\n", "\n")
m = re.search(
    r"^## " + re.escape(ver) + r"[^\n]*\n(.*?)(?=\n## v|\Z)",
    text,
    re.S | re.M,
)
if not m:
    sys.exit(0)
sys.stdout.write(m.group(0).strip())
