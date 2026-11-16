# -*- coding: utf-8 -*-
"""Minimal SVG helpers for thesis diagrams.

All coordinates are absolute in the SVG user space. Diagrams are rendered to
PNG later with headless Chrome (see render_all.sh).
"""

FONT = "'Microsoft YaHei','PingFang SC','Segoe UI',sans-serif"

INK = "#1a202c"
LINE = "#4a5568"

BLUE_FILL = "#e8f4ff"
BLUE_STROKE = "#2b6cb0"
GREEN_FILL = "#eaf7ef"
GREEN_STROKE = "#2f855a"
ORANGE_FILL = "#fff4e5"
ORANGE_STROKE = "#c05621"
PURPLE_FILL = "#f3eaff"
PURPLE_STROKE = "#6b46c1"
GRAY_FILL = "#f7fafc"
GRAY_STROKE = "#718096"
RED_FILL = "#fff0f0"
RED_STROKE = "#c53030"
YELLOW_FILL = "#fffbea"
YELLOW_STROKE = "#b7791f"


def esc(s):
    return (str(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def _defs():
    return (
        "<defs>"
        '<marker id="ar" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">'
        f'<path d="M0,0 L9,3.5 L0,7 z" fill="{LINE}"/></marker>'
        '<marker id="arr" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">'
        f'<path d="M0,0 L9,3.5 L0,7 z" fill="{RED_STROKE}"/></marker>'
        '<marker id="arg" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">'
        f'<path d="M0,0 L9,3.5 L0,7 z" fill="{GREEN_STROKE}"/></marker>'
        "</defs>"
    )


class Canvas:
    def __init__(self, w, h, bg="#ffffff"):
        self.w, self.h = w, h
        self.parts = [f'<rect width="{w}" height="{h}" fill="{bg}"/>']

    def raw(self, s):
        self.parts.append(s)

    def text(self, x, y, s, fs=16, anchor="middle", fill=INK, bold=False,
             base="central", italic=False, family=None):
        weight = ' font-weight="bold"' if bold else ""
        style = ' font-style="italic"' if italic else ""
        fam = f' font-family="{family}"' if family else f' font-family="{FONT}"'
        self.parts.append(
            f'<text x="{x}" y="{y}" font-size="{fs}" text-anchor="{anchor}" '
            f'dominant-baseline="{base}"{fam}{weight}{style} fill="{fill}">{esc(s)}</text>'
        )

    def multiline(self, x, y, lines, fs=15, lh=1.35, **kw):
        """Vertically centered block of lines around y."""
        n = len(lines)
        start = y - (n - 1) * fs * lh / 2
        for i, ln in enumerate(lines):
            self.text(x, start + i * fs * lh, ln, fs=fs, **kw)

    def box(self, x, y, w, h, lines, fill=BLUE_FILL, stroke=BLUE_STROKE, fs=15,
            rx=6, sw=1.6, bold=False, fill_text=INK):
        self.parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>'
        )
        if isinstance(lines, str):
            lines = [lines]
        self.multiline(x + w / 2, y + h / 2, lines, fs=fs, bold=bold, fill=fill_text)

    def diamond(self, cx, cy, w, h, lines, fill=YELLOW_FILL, stroke=YELLOW_STROKE, fs=14):
        pts = f"{cx},{cy - h / 2} {cx + w / 2},{cy} {cx},{cy + h / 2} {cx - w / 2},{cy}"
        self.parts.append(
            f'<polygon points="{pts}" fill="{fill}" stroke="{stroke}" stroke-width="1.6"/>'
        )
        if isinstance(lines, str):
            lines = [lines]
        self.multiline(cx, cy, lines, fs=fs)

    def ellipse(self, cx, cy, w, h, lines, fill="#ffffff", stroke=BLUE_STROKE, fs=14):
        self.parts.append(
            f'<ellipse cx="{cx}" cy="{cy}" rx="{w / 2}" ry="{h / 2}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="1.6"/>'
        )
        if isinstance(lines, str):
            lines = [lines]
        self.multiline(cx, cy, lines, fs=fs)

    def stadium(self, x, y, w, h, lines, fill=GRAY_FILL, stroke=GRAY_STROKE, fs=14, sw=1.6):
        self.parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{h / 2}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>'
        )
        if isinstance(lines, str):
            lines = [lines]
        self.multiline(x + w / 2, y + h / 2, lines, fs=fs)

    def line(self, x1, y1, x2, y2, marker="ar", color=LINE, dash=None, sw=1.6):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        m = f' marker-end="url(#{marker})"' if marker else ""
        self.parts.append(
            f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" '
            f'stroke-width="{sw}"{d}{m}/>'
        )

    def poly(self, pts, marker="ar", color=LINE, dash=None, sw=1.6):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        m = f' marker-end="url(#{marker})"' if marker else ""
        p = " ".join(f"{x},{y}" for x, y in pts)
        self.parts.append(
            f'<polyline points="{p}" fill="none" stroke="{color}" stroke-width="{sw}"{d}{m}/>'
        )

    def arrow(self, x1, y1, x2, y2, **kw):
        self.line(x1, y1, x2, y2, **kw)

    def label(self, x, y, s, fs=13, fill="#2d3748", bg="#ffffff", pad=3, bold=False):
        """Small label with white background, e.g. on a transition edge."""
        w = len(s) * fs * 0.62 + pad * 2
        self.parts.append(
            f'<rect x="{x - w / 2}" y="{y - fs * 0.72}" width="{w}" height="{fs * 1.44}" '
            f'fill="{bg}"/>'
        )
        self.text(x, y, s, fs=fs, fill=fill, bold=bold)

    def actor(self, cx, top, label, fs=15, scale=1.0):
        """Stick-figure actor; `top` is the top of the head."""
        r = 12 * scale
        head_cy = top + r
        neck = head_cy + r
        body_end = neck + 34 * scale
        arm_y = neck + 14 * scale
        leg_end = body_end + 26 * scale
        self.parts.append(
            f'<circle cx="{cx}" cy="{head_cy}" r="{r}" fill="#ffffff" stroke="{LINE}" stroke-width="1.8"/>'
        )
        self.line(cx, neck, cx, body_end, marker=None)
        self.line(cx - 18 * scale, arm_y, cx + 18 * scale, arm_y, marker=None)
        self.line(cx, body_end, cx - 14 * scale, leg_end, marker=None)
        self.line(cx, body_end, cx + 14 * scale, leg_end, marker=None)
        self.text(cx, leg_end + fs * 0.9, label, fs=fs, bold=True)

    def save(self, path):
        svg = (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" '
            f'viewBox="0 0 {self.w} {self.h}">'
            + _defs()
            + "".join(self.parts)
            + "</svg>"
        )
        with open(path, "w", encoding="utf-8") as f:
            f.write(svg)
        return path
