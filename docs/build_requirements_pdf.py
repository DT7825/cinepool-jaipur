"""Build the submission PDFs from the editable PRD and SRS Markdown files.

Requires reportlab. Run from anywhere with:
    python docs/build_requirements_pdf.py
"""

from __future__ import annotations

import html
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "pdf"
OUT.mkdir(parents=True, exist_ok=True)

FONT_DIR = Path("C:/Windows/Fonts")
pdfmetrics.registerFont(TTFont("ArialDoc", str(FONT_DIR / "arial.ttf")))
pdfmetrics.registerFont(TTFont("ArialDoc-Bold", str(FONT_DIR / "arialbd.ttf")))
pdfmetrics.registerFontFamily("ArialDoc", normal="ArialDoc", bold="ArialDoc-Bold")
pdfmetrics.registerFont(TTFont("ConsolasDoc", str(FONT_DIR / "consola.ttf")))

INK = colors.HexColor("#14242b")
MUTED = colors.HexColor("#54636a")
ACCENT = colors.HexColor("#e56541")
PALE = colors.HexColor("#f4f7f6")
LINE = colors.HexColor("#d9e1e1")


def inline(value: str) -> str:
    value = html.escape(value.strip())
    value = re.sub(r"`([^`]+)`", r'<font name="ConsolasDoc" color="#315f60">\1</font>', value)
    value = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", value)
    return value


def styles():
    base = getSampleStyleSheet()
    body = ParagraphStyle(
        "BodyDoc", parent=base["Normal"], fontName="ArialDoc", fontSize=9.25,
        leading=14.1, textColor=INK, spaceAfter=7.4, splitLongWords=1,
        allowWidows=0, allowOrphans=0,
    )
    h1 = ParagraphStyle(
        "HeadingOneDoc", parent=body, fontName="ArialDoc-Bold", fontSize=15,
        leading=19, textColor=INK, spaceBefore=18, spaceAfter=8,
        keepWithNext=1,
    )
    h2 = ParagraphStyle(
        "HeadingTwoDoc", parent=body, fontName="ArialDoc-Bold", fontSize=11.5,
        leading=15, spaceBefore=12, spaceAfter=5, keepWithNext=1,
    )
    h3 = ParagraphStyle(
        "HeadingThreeDoc", parent=body, fontName="ArialDoc-Bold", fontSize=10,
        leading=13, spaceBefore=9, spaceAfter=4, keepWithNext=1,
    )
    bullet = ParagraphStyle(
        "BulletDoc", parent=body, leftIndent=13, firstLineIndent=-9,
        spaceAfter=4.5,
    )
    cell = ParagraphStyle(
        "CellDoc", parent=body, fontSize=8.3, leading=11.2,
        spaceAfter=0, splitLongWords=1,
    )
    cell_header = ParagraphStyle(
        "CellHeadDoc", parent=cell, fontName="ArialDoc-Bold",
        textColor=colors.white,
    )
    title = ParagraphStyle(
        "CoverTitle", parent=body, fontName="ArialDoc-Bold", fontSize=31,
        leading=38, textColor=INK, spaceAfter=15,
    )
    kicker = ParagraphStyle(
        "CoverKicker", parent=body, fontName="ArialDoc-Bold", fontSize=10,
        leading=14, textColor=ACCENT, spaceAfter=12,
    )
    subtitle = ParagraphStyle(
        "CoverSubtitle", parent=body, fontSize=13, leading=19, textColor=MUTED,
    )
    cover_note = ParagraphStyle(
        "CoverNote", parent=body, fontSize=9, leading=14, textColor=MUTED,
    )
    return body, h1, h2, h3, bullet, cell, cell_header, title, kicker, subtitle, cover_note


class RequirementsDoc(BaseDocTemplate):
    def __init__(self, filename: Path, short_title: str):
        super().__init__(
            str(filename), pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
            topMargin=21 * mm, bottomMargin=19 * mm,
            title=f"Cinepool Jaipur - {short_title}", author="Cinepool Jaipur project",
        )
        self.short_title = short_title
        frame = Frame(
            self.leftMargin, self.bottomMargin, self.width, self.height,
            id="normal", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0,
        )
        self.addPageTemplates(PageTemplate(id="document", frames=frame, onPage=self.decorate))

    def decorate(self, canvas, doc):
        canvas.saveState()
        w, h = A4
        canvas.setStrokeColor(LINE)
        canvas.setLineWidth(0.5)
        canvas.line(20 * mm, h - 16 * mm, w - 20 * mm, h - 16 * mm)
        canvas.setFont("ArialDoc-Bold", 7.8)
        canvas.setFillColor(MUTED)
        canvas.drawString(20 * mm, h - 13 * mm, "CINEPOOL JAIPUR")
        canvas.setFont("ArialDoc", 7.8)
        canvas.drawRightString(w - 20 * mm, h - 13 * mm, self.short_title.upper())
        canvas.line(20 * mm, 15 * mm, w - 20 * mm, 15 * mm)
        canvas.drawString(20 * mm, 10.5 * mm, "Version 1.0  |  28 October 2026  |  Implementation baseline")
        canvas.drawRightString(w - 20 * mm, 10.5 * mm, str(doc.page))
        canvas.restoreState()


def parse_table(lines: list[str], width: float, cell, cell_header):
    rows = []
    for line in lines:
        fields = [field.strip() for field in line.strip().strip("|").split("|")]
        if fields and all(re.fullmatch(r":?-{3,}:?", field) for field in fields):
            continue
        rows.append(fields)
    if not rows:
        return None
    columns = max(len(row) for row in rows)
    if columns == 2:
        widths = [width * 0.34, width * 0.66]
    elif columns == 3:
        widths = [width * 0.30, width * 0.19, width * 0.51] if "Route" in rows[0] else [width * 0.25, width * 0.36, width * 0.39]
    else:
        widths = [width / columns] * columns
    data = []
    for idx, row in enumerate(rows):
        row += [""] * (columns - len(row))
        data.append([Paragraph(inline(value), cell_header if idx == 0 else cell) for value in row])
    table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT", spaceBefore=4, spaceAfter=10)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), INK),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]),
        ("LINEBELOW", (0, 0), (-1, 0), 1.1, ACCENT),
        ("LINEBELOW", (0, 1), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return table


def build_document(name: str, short_title: str, description: str):
    source = ROOT / "docs" / f"Cinepool_Jaipur_{name}.md"
    output = OUT / f"Cinepool_Jaipur_{name}.pdf"
    lines = source.read_text(encoding="utf-8").splitlines()
    body, h1, h2, h3, bullet, cell, cell_header, title, kicker, subtitle, cover_note = styles()
    doc = RequirementsDoc(output, short_title)
    story = [
        Spacer(1, 34 * mm),
        Paragraph(short_title.upper(), kicker),
        Paragraph("Cinepool Jaipur", title),
        Paragraph(description, subtitle),
        Spacer(1, 10 * mm),
        Table([[""]], colWidths=[65 * mm], rowHeights=[1.2 * mm], style=TableStyle([("BACKGROUND", (0, 0), (-1, -1), ACCENT)])),
        Spacer(1, 11 * mm),
        Paragraph("Version 1.0  |  28 October 2026", cover_note),
        Paragraph("Source baseline: master at bea5ca0", cover_note),
        Paragraph("Current interface name: Picturehouse Jaipur", cover_note),
        Spacer(1, 19 * mm),
        Paragraph("Prepared from the implementation and the documented local MySQL inspection. Current behavior and proposed work are identified separately.", cover_note),
        PageBreak(),
    ]
    width = A4[0] - 40 * mm
    idx = 0
    while idx < len(lines) and not lines[idx].startswith("## "):
        idx += 1
    while idx < len(lines):
        raw = lines[idx].rstrip()
        if not raw:
            idx += 1
            continue
        if raw.startswith("| "):
            table_lines = []
            while idx < len(lines) and lines[idx].startswith("|"):
                table_lines.append(lines[idx])
                idx += 1
            table = parse_table(table_lines, width, cell, cell_header)
            if table:
                story.append(table)
            continue
        if raw.startswith("### "):
            story.append(Paragraph(inline(raw[4:]), h2))
        elif raw.startswith("## "):
            story.append(Paragraph(inline(raw[3:]), h1))
        elif raw.startswith("#### "):
            story.append(Paragraph(inline(raw[5:]), h3))
        elif raw.startswith("- "):
            story.append(Paragraph("&#8226; " + inline(raw[2:]), bullet))
        elif re.match(r"^\d+\. ", raw):
            number, content = raw.split(". ", 1)
            story.append(Paragraph(f"<b>{number}.</b> " + inline(content), bullet))
        else:
            paragraph = raw
            while idx + 1 < len(lines) and lines[idx + 1].strip() and not re.match(r"^(#|\||- |\d+\. )", lines[idx + 1]):
                idx += 1
                paragraph += " " + lines[idx].strip()
            story.append(Paragraph(inline(paragraph), body))
        idx += 1
    doc.build(story)
    print(output)


if __name__ == "__main__":
    build_document("PRD", "Product requirements document", "Product scope, roles, journeys, acceptance criteria and a clearly separated roadmap")
    build_document("SRS", "Software requirements specification", "Implemented system requirements, interfaces, data relationships, controls and verification checks")
