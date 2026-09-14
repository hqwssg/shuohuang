from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt
from docx.table import Table
from docx.text.paragraph import Paragraph

from carbon_report_agent.audit import append_audit_event

DEFAULT_STYLE_PACK_PATH = (
    Path(__file__).resolve().parent / "data" / "style_pack_suning_ghg_v1.json"
)

_ALIGN = {
    "left": WD_ALIGN_PARAGRAPH.LEFT,
    "center": WD_ALIGN_PARAGRAPH.CENTER,
    "right": WD_ALIGN_PARAGRAPH.RIGHT,
    "justify": WD_ALIGN_PARAGRAPH.JUSTIFY,
}


@lru_cache(maxsize=4)
def load_style_pack(path: str | None = None) -> dict[str, Any]:
    p = Path(path) if path else DEFAULT_STYLE_PACK_PATH
    return json.loads(p.read_text(encoding="utf-8"))


def apply_section_page(document: Document, pack: dict[str, Any] | None = None) -> None:
    pack = pack or load_style_pack()
    page = pack.get("page") or {}
    section = document.sections[0]
    if "top_in" in page:
        section.top_margin = Inches(float(page["top_in"]))
    if "bottom_in" in page:
        section.bottom_margin = Inches(float(page["bottom_in"]))
    if "left_in" in page:
        section.left_margin = Inches(float(page["left_in"]))
    if "right_in" in page:
        section.right_margin = Inches(float(page["right_in"]))
    normal = pack.get("normal")
    if isinstance(normal, dict):
        _apply_token_to_paragraph_style(document.styles["Normal"], normal)
    for style_name, token_name in (
        ("Heading 1", "heading_1"),
        ("Heading 2", "heading_2"),
        ("Heading 3", "heading_3"),
    ):
        token = pack.get(token_name)
        if isinstance(token, dict):
            _apply_token_to_paragraph_style(document.styles[style_name], token)


def _apply_token_to_paragraph_style(style, token: dict[str, Any]) -> None:
    align = token.get("align")
    if align in _ALIGN:
        style.paragraph_format.alignment = _ALIGN[align]
    if "line_spacing" in token:
        style.paragraph_format.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
        style.paragraph_format.line_spacing = float(token["line_spacing"])
    size_pt = token.get("size_pt")
    if size_pt is not None:
        style.font.size = Pt(float(size_pt))
    if token.get("font_ascii"):
        style.font.name = str(token["font_ascii"])
    rpr = style.element.get_or_add_rPr()
    r_fonts = rpr.find(qn("w:rFonts"))
    if r_fonts is None:
        r_fonts = OxmlElement("w:rFonts")
        rpr.insert(0, r_fonts)
    if token.get("font_ascii"):
        r_fonts.set(qn("w:ascii"), str(token["font_ascii"]))
        r_fonts.set(qn("w:hAnsi"), str(token["font_ascii"]))
    if token.get("font_east_asia"):
        r_fonts.set(qn("w:eastAsia"), str(token["font_east_asia"]))
    _set_rpr_color(rpr, token)


def _set_run_fonts(run, token: dict[str, Any]) -> None:
    ascii_name = token.get("font_ascii")
    east = token.get("font_east_asia")
    if not ascii_name and not east:
        return
    r = run._r
    rpr = r.get_or_add_rPr()
    r_fonts = rpr.find(qn("w:rFonts"))
    if r_fonts is None:
        r_fonts = OxmlElement("w:rFonts")
        rpr.insert(0, r_fonts)
    if ascii_name:
        r_fonts.set(qn("w:ascii"), str(ascii_name))
        r_fonts.set(qn("w:hAnsi"), str(ascii_name))
    if east:
        r_fonts.set(qn("w:eastAsia"), str(east))


def _set_rpr_color(rpr, token: dict[str, Any]) -> None:
    color = token.get("color")
    if color is None:
        return
    value = str(color).lstrip("#")
    if not value:
        return
    color_el = rpr.find(qn("w:color"))
    if color_el is None:
        color_el = OxmlElement("w:color")
        rpr.append(color_el)
    color_el.set(qn("w:val"), value)


def _apply_token_to_paragraph(paragraph: Paragraph, token: dict[str, Any]) -> None:
    align = token.get("align")
    if align in _ALIGN:
        paragraph.alignment = _ALIGN[align]
    pf = paragraph.paragraph_format
    if "space_before_pt" in token:
        pf.space_before = Pt(float(token["space_before_pt"]))
    if "space_after_pt" in token:
        pf.space_after = Pt(float(token["space_after_pt"]))
    if "first_line_indent_pt" in token:
        pf.first_line_indent = Pt(float(token["first_line_indent_pt"]))
    if "line_spacing" in token:
        pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
        pf.line_spacing = float(token["line_spacing"])
    if "left_indent_pt" in token:
        pf.left_indent = Pt(float(token["left_indent_pt"]))

    size_pt = token.get("size_pt")
    size_cs_pt = token.get("size_cs_pt")
    bold = token.get("bold")
    runs = list(paragraph.runs)
    if not runs:
        # Ensure at least one run exists for font application on empty paras
        return
    for run in runs:
        if size_pt is not None:
            run.font.size = Pt(float(size_pt))
        if size_cs_pt is not None:
            rpr = run._r.get_or_add_rPr()
            sz_cs = rpr.find(qn("w:szCs"))
            if sz_cs is None:
                sz_cs = OxmlElement("w:szCs")
                rpr.append(sz_cs)
            sz_cs.set(qn("w:val"), str(int(float(size_cs_pt) * 2)))
        if bold is not None:
            run.font.bold = bool(bold)
        _set_run_fonts(run, token)
        _set_rpr_color(run._r.get_or_add_rPr(), token)


def apply_paragraph_style(
    paragraph: Paragraph,
    token_name: str,
    pack: dict[str, Any] | None = None,
) -> None:
    pack = pack or load_style_pack()
    token = pack.get(token_name)
    if not isinstance(token, dict):
        append_audit_event(
            "style_token_missing",
            {"token": token_name, "severity": "warning"},
        )
        return
    _apply_token_to_paragraph(paragraph, token)


def apply_heading_style(
    paragraph: Paragraph,
    level: int,
    pack: dict[str, Any] | None = None,
) -> None:
    pack = pack or load_style_pack()
    token_name = f"heading_{max(1, min(3, int(level)))}"
    apply_paragraph_style(paragraph, token_name, pack=pack)


def style_approval_table(table: Table, pack: dict[str, Any] | None = None) -> None:
    pack = pack or load_style_pack()
    token = pack.get("approval_table") or {}
    _set_table_width(
        table,
        str(token.get("table_width", 0)),
        str(token.get("table_width_type", "auto")),
    )
    _set_table_alignment(table, str(token.get("align", "center")))
    _set_table_layout_fixed(table)
    _set_table_grid(table, [int(width) for width in token.get("column_widths", [])])
    _set_table_borders(table, enabled=bool(token.get("borders", True)))
    _style_approval_cells(table, token)


def approval_label_suffix(pack: dict[str, Any] | None = None) -> str:
    pack = pack or load_style_pack()
    token = pack.get("approval_table") or {}
    return str(token.get("label_suffix", "\uff1a"))


def image_slot_width_inches(role: str, pack: dict[str, Any] | None = None) -> float:
    pack = pack or load_style_pack()
    slots = pack.get("image_slots") or {}
    slot = slots.get(role) or {}
    return float(slot.get("width_inches", 5.5))


def image_slot_align(role: str, pack: dict[str, Any] | None = None) -> str:
    pack = pack or load_style_pack()
    slots = pack.get("image_slots") or {}
    slot = slots.get(role) or {}
    return str(slot.get("align", "center"))


def add_spacer_paragraphs(
    document: Document,
    count: int,
    *,
    pack: dict[str, Any] | None = None,
) -> None:
    """Insert empty Normal paragraphs with cover_spacer line spacing."""
    pack = pack or load_style_pack()
    spacer = pack.get("cover_spacer") or {}
    line_spacing = float(spacer.get("line_spacing", 3.0))
    for _ in range(max(0, int(count))):
        paragraph = document.add_paragraph("")
        paragraph.paragraph_format.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
        paragraph.paragraph_format.line_spacing = line_spacing


def cover_spacer_count(key: str, pack: dict[str, Any] | None = None) -> int:
    pack = pack or load_style_pack()
    spacer = pack.get("cover_spacer") or {}
    return int(spacer.get(key, 0))


def apply_toc_entry_style(
    paragraph: Paragraph,
    level: int,
    pack: dict[str, Any] | None = None,
) -> None:
    pack = pack or load_style_pack()
    level = max(1, min(3, int(level)))
    token_name = f"toc_entry_l{level}"
    token = pack.get(token_name) or pack.get("toc_entry") or {}
    if not isinstance(token, dict):
        return
    _apply_token_to_paragraph(paragraph, token)
    # Right-aligned tab with dot leader (sample TOC style)
    if token.get("tab_leader") == "dot":
        tab_pos_in = float(token.get("tab_position_in", 6.0))
        if token.get("tab_position_twips") is not None:
            tab_pos_in = float(token["tab_position_twips"]) / 1440
        _set_right_dot_tab(paragraph, tab_pos_in)


def _set_right_dot_tab(paragraph: Paragraph, position_inches: float) -> None:
    pPr = paragraph._p.get_or_add_pPr()
    tabs = pPr.find(qn("w:tabs"))
    if tabs is None:
        tabs = OxmlElement("w:tabs")
        pPr.append(tabs)
    # Clear existing tabs
    for child in list(tabs):
        tabs.remove(child)
    tab = OxmlElement("w:tab")
    tab.set(qn("w:val"), "right")
    tab.set(qn("w:leader"), "dot")
    # twips: 1 inch = 1440 twips
    tab.set(qn("w:pos"), str(int(position_inches * 1440)))
    tabs.append(tab)


def _get_or_add(parent, tag: str):
    child = parent.find(qn(tag))
    if child is None:
        child = OxmlElement(tag)
        parent.append(child)
    return child


def _get_or_add_tbl_pr(table: Table):
    tbl = table._tbl
    tbl_pr = tbl.tblPr
    if tbl_pr is None:
        tbl_pr = OxmlElement("w:tblPr")
        tbl.insert(0, tbl_pr)
    return tbl_pr


def _set_table_width(table: Table, width: str, width_type: str) -> None:
    tbl_pr = _get_or_add_tbl_pr(table)
    tbl_w = _get_or_add(tbl_pr, "w:tblW")
    tbl_w.set(qn("w:w"), width)
    tbl_w.set(qn("w:type"), width_type)


def _set_table_alignment(table: Table, align: str) -> None:
    tbl_pr = _get_or_add_tbl_pr(table)
    jc = _get_or_add(tbl_pr, "w:jc")
    jc.set(qn("w:val"), align)


def _set_table_layout_fixed(table: Table) -> None:
    tbl_pr = _get_or_add_tbl_pr(table)
    layout = _get_or_add(tbl_pr, "w:tblLayout")
    layout.set(qn("w:type"), "fixed")


def _set_table_grid(table: Table, widths: list[int]) -> None:
    if not widths:
        return
    tbl = table._tbl
    grid = tbl.tblGrid
    if grid is None:
        grid = OxmlElement("w:tblGrid")
        tbl.insert(0, grid)
    for child in list(grid):
        grid.remove(child)
    for width in widths:
        col = OxmlElement("w:gridCol")
        col.set(qn("w:w"), str(width))
        grid.append(col)
    for row in table.rows:
        for index, cell in enumerate(row.cells):
            if index >= len(widths):
                continue
            tc_w = _get_or_add(cell._tc.get_or_add_tcPr(), "w:tcW")
            tc_w.set(qn("w:w"), str(widths[index]))
            tc_w.set(qn("w:type"), "dxa")


def _set_table_borders(table: Table, *, enabled: bool = True) -> None:
    tbl_pr = _get_or_add_tbl_pr(table)
    borders = _get_or_add(tbl_pr, "w:tblBorders")
    val = "single" if enabled else "none"
    size = "4" if enabled else "0"
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        element = _get_or_add(borders, f"w:{edge}")
        element.set(qn("w:val"), val)
        element.set(qn("w:sz"), size)
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), "000000")


def _set_paragraph_jc(paragraph: Paragraph, align: str) -> None:
    p_pr = paragraph._p.get_or_add_pPr()
    jc = _get_or_add(p_pr, "w:jc")
    jc.set(qn("w:val"), align)


def _style_approval_cells(table: Table, token: dict[str, Any]) -> None:
    row_heights = [int(height) for height in token.get("row_heights", [])]
    row_height_rule = str(token.get("row_height_rule", "atLeast"))
    vertical_align = str(token.get("vertical_align", "center"))
    label_align = str(token.get("label_align", "distribute"))
    value_align = str(token.get("value_align", "left"))
    size_half_points = str(int(float(token.get("size_pt", 14)) * 2))
    bold = bool(token.get("bold", True))

    for row_index, row in enumerate(table.rows):
        if row_index < len(row_heights):
            tr_pr = row._tr.get_or_add_trPr()
            tr_height = _get_or_add(tr_pr, "w:trHeight")
            tr_height.set(qn("w:val"), str(row_heights[row_index]))
            tr_height.set(qn("w:hRule"), row_height_rule)
        for cell_index, cell in enumerate(row.cells):
            tc_pr = cell._tc.get_or_add_tcPr()
            v_align = _get_or_add(tc_pr, "w:vAlign")
            v_align.set(qn("w:val"), vertical_align)
            align = label_align if cell_index == 0 else value_align
            for paragraph in cell.paragraphs:
                _set_paragraph_jc(paragraph, align)
                paragraph.paragraph_format.space_before = Pt(0)
                paragraph.paragraph_format.space_after = Pt(0)
                for run in paragraph.runs:
                    r_pr = run._r.get_or_add_rPr()
                    sz = _get_or_add(r_pr, "w:sz")
                    sz.set(qn("w:val"), size_half_points)
                    sz_cs = _get_or_add(r_pr, "w:szCs")
                    sz_cs.set(qn("w:val"), size_half_points)
                    if bold:
                        _get_or_add(r_pr, "w:b")
                        _get_or_add(r_pr, "w:bCs")
                    _set_run_fonts(run, token)
                    _set_rpr_color(r_pr, token)
