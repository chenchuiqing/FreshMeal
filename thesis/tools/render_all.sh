#!/bin/bash
# Rasterize all figures/*.svg to PNG with headless Chrome (2x scale).
# Uses a private user-data-dir so a running desktop Chrome (or another
# headless instance) cannot lock the profile and silently skip writing.
CHROME="C:/Program Files/Google/Chrome/Application/chrome.exe"
cd "$(dirname "$0")/.." || exit 1
PROFILE="$(mktemp -d)"
WINP="$(pwd -W 2>/dev/null || pwd)"
for svg in figures/*.svg; do
  base="${svg%.svg}"
  size=$(grep -o 'width="[0-9]*" height="[0-9]*"' "$svg" | head -1 | grep -o '[0-9]*' | tr '\n' ',' | sed 's/,$//')
  w=$(echo "$size" | cut -d, -f1)
  h=$(echo "$size" | cut -d, -f2)
  out="$(pwd -W 2>/dev/null || pwd)/$base.png"
  rm -f "$out"
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars \
    --user-data-dir="$PROFILE" \
    --force-device-scale-factor=2 --window-size="$w,$h" \
    --screenshot="$out" "file:///$WINP/$svg" >/dev/null 2>&1
  if [ -f "$out" ]; then echo "rendered $base.png (${w}x${h})"; else echo "FAILED $base"; fi
done
rm -rf "$PROFILE"
