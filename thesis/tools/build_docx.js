#!/usr/bin/env node
/**
 * Build thesis/论文.docx from thesis/src/*.md (controlled markdown subset).
 *
 * Conventions (see src/*.md):
 *   # title        -> heading 1 (new page)
 *   ## title       -> heading 2
 *   ![caption](p)  -> centered figure + centered caption below
 *   表 X-Y caption -> centered caption paragraph above table
 *   | ... |        -> table (row 2 must be the separator)
 *   **bold** / `code` inline runs
 *   ``` fence      -> monospace code block (appendix)
 *
 * Layout: A4, margins 2.54cm top/bottom & 3.17cm left/right,
 * body 宋体/Times New Roman 小四 1.5 line spacing, first-line indent 2 chars,
 * headings 黑体, captions 五号 centered.
 */
"use strict";

const fs = require("fs");
const path = require("path");
const {
  AlignmentType,
  BorderStyle,
  Document,
  Footer,
  HeadingLevel,
  ImageRun,
  PageNumber,
  Paragraph,
  ShadingType,
  Table,
  TableCell,
  TableRow,
  TextRun,
  VerticalAlign,
  WidthType,
} = require("docx");

const ROOT = path.resolve(__dirname, "..");
const SRC = path.join(ROOT, "src");

const PAGE_W = 11906; // A4 twips
const MARGIN_LR = 1800; // 3.17cm
const CONTENT_W = PAGE_W - MARGIN_LR * 2; // 8306 twips
const MARGIN_TB = 1440; // 2.54cm
const CONTENT_H = 16838 - MARGIN_TB * 2; // 13958 twips
const PX_PER_TWIP = 96 / 1440; // twips -> px @96dpi (1 inch = 1440 twips = 96 px)
const MAX_IMG_W = Math.floor(CONTENT_W * PX_PER_TWIP); // ~554 px
const MAX_IMG_H = 780; // image + caption + 2-line note must share one page

const BODY_FONT = { ascii: "Times New Roman", hAnsi: "Times New Roman", eastAsia: "宋体" };
const HEAD_FONT = { ascii: "Arial", hAnsi: "Arial", eastAsia: "黑体" };
const CODE_FONT = { ascii: "Consolas", hAnsi: "Consolas", eastAsia: "宋体" };

const CHILD_FILES = [
  "front.md",
  "ch1.md",
  "ch2.md",
  "ch3.md",
  "ch4.md",
  "ch5.md",
  "ch6.md",
  "ch7.md",
  "refs.md",
  "thanks.md",
  "appendix.md",
];

/* ---------------------------------------------------------------- inline */

/** Split a text into runs honoring **bold** and `code`. */
function inlineRuns(text, opts = {}) {
  const runs = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let last = 0;
  let m;
  const push = (t, over) => {
    if (!t) return;
    runs.push(new TextRun({ text: t, ...over }));
  };
  while ((m = re.exec(text)) !== null) {
    push(text.slice(last, m.index), opts);
    const tok = m[0];
    if (tok.startsWith("**")) {
      push(tok.slice(2, -2), { ...opts, bold: true });
    } else {
      push(tok.slice(1, -1), { ...opts, font: CODE_FONT });
    }
    last = m.index + tok.length;
  }
  push(text.slice(last), opts);
  return runs.length ? runs : [new TextRun({ text: "", ...opts })];
}

/** Strip markdown formatting (for measuring / plain uses). */
function plain(text) {
  return text
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1");
}

/** Display width units: CJK/full-width = 2, others = 1. */
function widthUnits(text) {
  let w = 0;
  for (const ch of plain(text)) {
    const c = ch.codePointAt(0);
    w +=
      (c >= 0x1100 && c <= 0x115f) ||
      (c >= 0x2e80 && c <= 0xa4cf) ||
      (c >= 0xac00 && c <= 0xd7a3) ||
      (c >= 0xf900 && c <= 0xfaff) ||
      (c >= 0xfe30 && c <= 0xfe6f) ||
      (c >= 0xff00 && c <= 0xff60) ||
      (c >= 0xffe0 && c <= 0xffe6)
        ? 2
        : 1;
  }
  return w;
}

/* -------------------------------------------------------------- png size */

function pngSize(buf) {
  // bytes 16..23: width/height big-endian for valid PNG signature
  if (buf.readUInt32BE(0) !== 0x89504e47) return { w: 0, h: 0 };
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}

function figureRun(relPath) {
  const abs = path.join(ROOT, relPath.replace(/\//g, path.sep));
  const buf = fs.readFileSync(abs);
  const { w, h } = pngSize(buf);
  const scale = Math.min(MAX_IMG_W / w, MAX_IMG_H / h, 1);
  return new ImageRun({
    type: "png",
    data: buf,
    transformation: {
      width: Math.max(1, Math.round(w * scale)),
      height: Math.max(1, Math.round(h * scale)),
    },
  });
}

/* ------------------------------------------------------------ paragraphs */

function bodyParagraph(text, extra = {}) {
  return new Paragraph({
    alignment: AlignmentType.JUSTIFIED,
    indent: { firstLine: 480 },
    spacing: { line: 360, after: 0 },
    children: inlineRuns(text),
    ...extra,
  });
}

function heading(text, level) {
  const styleId = level === 1 ? "heading1" : "heading2";
  return new Paragraph({
    heading: level === 1 ? HeadingLevel.HEADING_1 : HeadingLevel.HEADING_2,
    style: styleId,
    children: [new TextRun({ text })],
  });
}

function caption(text, position) {
  // position: "below" (figure) or "above" (table)
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    indent: { firstLine: 0 },
    keepNext: position === "above",
    spacing:
      position === "below"
        ? { before: 60, after: 180, line: 300 }
        : { before: 180, after: 80, line: 300 },
    children: [new TextRun({ text, size: 21, font: BODY_FONT })],
  });
}

function figureParagraph(relPath) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    indent: { firstLine: 0 },
    keepNext: true, // keep figure together with its caption
    spacing: { before: 120, after: 40 },
    children: [figureRun(relPath)],
  });
}

/* ---------------------------------------------------------------- tables */

function parseTable(lines, start) {
  // returns { rows: string[][], end } — rows[0] is header; separator skipped
  const rows = [];
  let i = start;
  for (; i < lines.length; i++) {
    const l = lines[i].trim();
    if (!l.startsWith("|")) break;
    const cells = l.replace(/^\|/, "").replace(/\|$/, "").split("|").map((s) => s.trim());
    if (i === start + 1 && cells.every((c) => /^:?-{2,}:?$/.test(c) || c === "")) continue;
    rows.push(cells);
  }
  return { rows, end: i };
}

function colWidths(rows, aligns) {
  const ncols = Math.max(...rows.map((r) => r.length));
  const weights = [];
  for (let c = 0; c < ncols; c++) {
    let wmax = 4;
    for (const r of rows) wmax = Math.max(wmax, widthUnits(r[c] || ""));
    weights.push(wmax);
  }
  const total = weights.reduce((a, b) => a + b, 0);
  const MIN = 700;
  const widths = weights.map((w) => Math.max(MIN, Math.round((CONTENT_W * w) / total)));

  // Reclaim/expand the min-clamp overflow proportionally on flexible columns.
  let sum = widths.reduce((a, b) => a + b, 0);
  const diff = CONTENT_W - sum;
  if (diff !== 0) {
    const eligible = widths.map((w, i) => i).filter((i) => widths[i] > MIN);
    if (eligible.length) {
      const eSum = eligible.reduce((a, i) => a + widths[i], 0);
      let assigned = 0;
      eligible.forEach((i, idx) => {
        const take =
          idx === eligible.length - 1 ? diff - assigned : Math.round((diff * widths[i]) / eSum);
        widths[i] += take;
        assigned += take;
      });
    }
  }
  if (widths.some((w) => w <= 0)) {
    // pathological fallback: equal split
    const eq = Math.floor(CONTENT_W / ncols);
    for (let c = 0; c < ncols; c++) widths[c] = eq;
    widths[ncols - 1] += CONTENT_W - eq * ncols;
  }
  void aligns;
  return widths;
}

function cellAlign(alignSpec) {
  if (!alignSpec) return undefined;
  if (alignSpec.startsWith(":") && alignSpec.endsWith(":")) return AlignmentType.CENTER;
  if (alignSpec.endsWith(":")) return AlignmentType.RIGHT;
  if (alignSpec.startsWith(":")) return AlignmentType.LEFT;
  return undefined;
}

function buildTable(rows, sepCells) {
  const widths = colWidths(rows, sepCells);
  const headerTr = new TableRow({
    tableHeader: true,
    children: rows[0].map(
      (cell, c) =>
        new TableCell({
          width: { size: widths[c], type: WidthType.DXA },
          shading: { type: ShadingType.CLEAR, fill: "F2F2F2", color: "auto" },
          verticalAlign: VerticalAlign.CENTER,
          margins: { top: 40, bottom: 40, left: 80, right: 80 },
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              indent: { firstLine: 0 },
              spacing: { line: 260 },
              children: inlineRuns(cell, { size: 21, font: BODY_FONT, bold: true }),
            }),
          ],
        })
    ),
  });
  const bodyTrs = rows.slice(1).map(
    (row) =>
      new TableRow({
        children: row.map((cell, c) => {
          const spec = sepCells && sepCells[c];
          const align = cellAlign(spec) || (spec === undefined && widthUnits(cell) <= 8 ? AlignmentType.CENTER : undefined);
          return new TableCell({
            width: { size: widths[c], type: WidthType.DXA },
            verticalAlign: VerticalAlign.CENTER,
            margins: { top: 40, bottom: 40, left: 80, right: 80 },
            children: [
              new Paragraph({
                alignment: align || AlignmentType.LEFT,
                indent: { firstLine: 0 },
                spacing: { line: 260 },
                children: inlineRuns(cell, { size: 21, font: BODY_FONT }),
              }),
            ],
          });
        }),
      })
  );
  const border = { style: BorderStyle.SINGLE, size: 4, color: "808080" };
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: widths,
    margins: { top: 40, bottom: 40, left: 80, right: 80 },
    borders: {
      top: border,
      bottom: border,
      left: border,
      right: border,
      insideHorizontal: border,
      insideVertical: border,
    },
    rows: [headerTr, ...bodyTrs],
  });
}

/* --------------------------------------------------------------- parser */

function parseFile(file) {
  const text = fs.readFileSync(path.join(SRC, file), "utf8");
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    const t = line.trim();

    if (t === "") {
      i++;
      continue;
    }

    // fenced code block
    if (t.startsWith("```")) {
      i++;
      const code = [];
      while (i < lines.length && !lines[i].trim().startsWith("```")) {
        code.push(lines[i]);
        i++;
      }
      i++; // closing fence
      for (const cl of code) {
        out.push(
          new Paragraph({
            alignment: AlignmentType.LEFT,
            indent: { firstLine: 0, left: 240 },
            spacing: { line: 240, after: 0 },
            children: [new TextRun({ text: cl === "" ? " " : cl, size: 18, font: CODE_FONT })],
          })
        );
      }
      continue;
    }

    // headings
    if (t.startsWith("# ")) {
      out.push(heading(t.slice(2).trim(), 1));
      i++;
      continue;
    }
    if (t.startsWith("## ")) {
      out.push(heading(t.slice(3).trim(), 2));
      i++;
      continue;
    }

    // figure
    const fig = t.match(/^!\[([^\]]+)\]\(([^)]+)\)$/);
    if (fig) {
      out.push(figureParagraph(fig[2]));
      out.push(caption(fig[1], "below"));
      i++;
      continue;
    }

    // table caption line (表 X-Y ...) directly above a table
    if (/^表\s*[\d]/.test(t) && i + 1 < lines.length && lines[i + 1].trim().startsWith("|")) {
      out.push(caption(t, "above"));
      i++;
      continue;
    }

    // table block
    if (t.startsWith("|")) {
      const { rows, end } = parseTable(lines, i);
      // separator row: markdown second line of the raw block
      let sep = null;
      const raw2 = lines[i + 1] || "";
      if (rows.length >= 2 && /^\|[\s|:-]+$/.test(raw2.trim())) {
        sep = raw2
          .trim()
          .replace(/^\|/, "")
          .replace(/\|$/, "")
          .split("|")
          .map((s) => s.trim());
      }
      out.push(buildTable(rows, sep));
      i = end;
      // spacing paragraph after table
      out.push(
        new Paragraph({ indent: { firstLine: 0 }, spacing: { line: 120 }, children: [new TextRun({ text: "", size: 12 })] })
      );
      continue;
    }

    // normal paragraph
    const isKeyword = /^\*\*(关键词|Keywords)/.test(t);
    out.push(
      bodyParagraph(t, isKeyword ? { indent: { firstLine: 0 }, spacing: { before: 120, line: 360 } } : {})
    );
    i++;
  }
  return out;
}

/* ----------------------------------------------------------------- doc */

function build() {
  const missing = CHILD_FILES.filter((f) => !fs.existsSync(path.join(SRC, f)));
  if (missing.length) {
    console.error("missing sources:", missing.join(", "));
    process.exit(1);
  }

  const children = CHILD_FILES.flatMap((f) => parseFile(f));

  const doc = new Document({
    creator: "FreshMeal Thesis",
    title: "轻食预制菜线上订购配送管理系统的设计与实现",
    description: "本科毕业论文",
    features: { updateFields: true },
    styles: {
      default: {
        document: {
          run: { font: BODY_FONT, size: 24 },
          paragraph: {
            alignment: AlignmentType.JUSTIFIED,
            indent: { firstLine: 480 },
            spacing: { line: 360, before: 0, after: 0 },
          },
        },
        heading1: {
          run: { font: HEAD_FONT, size: 32, bold: true, color: "000000" },
          paragraph: {
            alignment: AlignmentType.CENTER,
            indent: { firstLine: 0, left: 0, right: 0 },
            spacing: { before: 240, after: 240 },
            pageBreakBefore: true,
            keepNext: true,
            outlineLevel: 0,
          },
        },
        heading2: {
          run: { font: HEAD_FONT, size: 28, bold: true, color: "000000" },
          paragraph: {
            alignment: AlignmentType.LEFT,
            indent: { firstLine: 0, left: 0, right: 0 },
            spacing: { before: 240, after: 120 },
            keepNext: true,
            outlineLevel: 1,
          },
        },
      },
    },
    sections: [
      {
        properties: {
          page: {
            size: { width: PAGE_W, height: 16838 },
            margin: { top: MARGIN_TB, bottom: MARGIN_TB, left: MARGIN_LR, right: MARGIN_LR },
          },
        },
        footers: {
          default: new Footer({
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [
                  new TextRun({ children: [PageNumber.CURRENT], size: 21, font: BODY_FONT }),
                ],
              }),
            ],
          }),
        },
        children,
      },
    ],
  });

  return doc;
}

async function main() {
  const { Packer } = require("docx");
  const doc = build();
  const buf = await Packer.toBuffer(doc);
  const out = path.join(ROOT, "论文.docx");
  fs.writeFileSync(out, buf);
  console.log(`wrote ${out} (${(buf.length / 1024).toFixed(0)} KB, ${CHILD_FILES.length} sources)`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
