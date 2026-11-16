# -*- coding: utf-8 -*-
"""Word-count checker for thesis chapters.

Counts CJK characters + Latin/digit word tokens (approximates Word's "字数"),
excluding fenced code blocks. Usage:
    python tools/count_words.py            # all chapters + total
    python tools/count_words.py 4          # only chapter 4
"""
import glob
import os
import re
import sys

SRC = os.path.join(os.path.dirname(__file__), "..", "src")

TARGETS = {
    "1": (1500, 2000),
    "2": (1500, 2000),
    "3": (2500, 3000),
    "4": (3500, 4500),
    "5": (3000, 4000),
    "6": (1500, 2000),
    "7": (800, 1200),
}
TOTAL_TARGET = (12000, 18000)


def count_text(text):
    """Approximates Word's 字数: each CJK char / full-width punctuation counts 1,
    each Latin word (incl. embedded digits/.-/%) counts 1. Code fences excluded."""
    text = re.sub(r"```.*?```", "", text, flags=re.S)
    cjk = len(re.findall(r"[一-鿿　-〿＀-￯]", text))
    latin = len(re.findall(r"[A-Za-z0-9]+(?:[./%'-][A-Za-z0-9]+)*", text))
    return cjk + latin, cjk, latin


def main():
    only = sys.argv[1] if len(sys.argv) > 1 else None
    total = 0
    for ch in sorted(TARGETS):
        if only and ch != only:
            continue
        path = os.path.join(SRC, f"ch{ch}.md")
        if not os.path.exists(path):
            print(f"ch{ch}: (missing {path})")
            continue
        with open(path, encoding="utf-8") as f:
            n, cjk, lat = count_text(f.read())
        lo, hi = TARGETS[ch]
        ok = "OK" if lo <= n <= hi else ("LOW" if n < lo else "HIGH")
        total += n
        print(f"ch{ch}: {n:>5} (CJK {cjk} + latin {lat})  target {lo}-{hi}  [{ok}]")
    if not only:
        lo, hi = TOTAL_TARGET
        ok = "OK" if lo <= total <= hi else ("LOW" if total < lo else "HIGH")
        print(f"TOTAL: {total}  target {lo}-{hi}  [{ok}]")
        # front/back matter info
        for name in ("front", "refs", "thanks"):
            p = os.path.join(SRC, f"{name}.md")
            if os.path.exists(p):
                with open(p, encoding="utf-8") as f:
                    n, _, _ = count_text(f.read())
                print(f"  ({name}: {n} chars, not counted in 正文)")


if __name__ == "__main__":
    main()
