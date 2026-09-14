from __future__ import annotations

from pathlib import Path

from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.models import ReportInput


def publish_pdf_summary(
    report: ReportInput,
    calculations: CalculationResult,
    narratives: dict[str, str],
    output_path: Path,
) -> Path:
    """
    Best-effort PDF export without Microsoft Word.
    Renders a structured text summary of the seven chapters via fpdf2 when available;
    otherwise writes a minimal PDF using a tiny pure-Python writer.
    """
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    title = f"{report.metadata.entity_name} {report.metadata.report_year} GHG Report"
    lines = [
        title,
        "",
        f"Total emissions: {calculations.total_emissions_tco2:.2f} tCO2",
        f"Fuel: {calculations.fuel_combustion_tco2:.2f} tCO2",
        f"Electricity: {calculations.electricity_emissions_tco2:.2f} tCO2",
        f"Heat: {calculations.heat_emissions_tco2:.2f} tCO2",
        "",
    ]
    chapter_titles = [
        ("chapter1_basic", "1 Basic information"),
        ("chapter2_boundary", "2 Boundary"),
        ("chapter3_activity", "3 Activity data"),
        ("chapter4_factors", "4 Emission factors"),
        ("chapter5_calculation", "5 Calculation"),
        ("chapter6_conclusion", "6 Conclusion"),
        ("chapter7_monitoring", "7 Monitoring"),
    ]
    for key, heading in chapter_titles:
        lines.append(heading)
        lines.append(narratives.get(key, ""))
        lines.append("")

    try:
        from fpdf import FPDF  # type: ignore

        pdf = FPDF()
        pdf.set_auto_page_break(auto=True, margin=15)
        pdf.add_page()
        pdf.set_font("Helvetica", size=11)
        for line in lines:
            safe = line.encode("latin-1", errors="replace").decode("latin-1")
            pdf.multi_cell(0, 6, safe)
        pdf.output(str(output_path))
        return output_path
    except Exception:
        _write_minimal_pdf(output_path, "\n".join(lines))
        return output_path


def _write_minimal_pdf(path: Path, text: str) -> None:
    """Write a single-page PDF with Helvetica text (ASCII-safe)."""
    safe = text.encode("latin-1", errors="replace").decode("latin-1")
    # Escape PDF special chars
    safe = safe.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
    # Limit length to keep stream small
    content_lines = safe.splitlines()[:80]
    y = 750
    stream_parts = ["BT", "/F1 10 Tf", "14 TL"]
    for i, line in enumerate(content_lines):
        if i == 0:
            stream_parts.append(f"50 {y} Td")
        else:
            stream_parts.append("T*")
        stream_parts.append(f"({line[:110]}) Tj")
    stream_parts.append("ET")
    stream = "\n".join(stream_parts).encode("latin-1")

    objects: list[bytes] = []
    objects.append(b"1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n")
    objects.append(b"2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n")
    objects.append(
        b"3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
        b"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>endobj\n"
    )
    objects.append(
        f"4 0 obj<< /Length {len(stream)} >>stream\n".encode("latin-1")
        + stream
        + b"\nendstream\nendobj\n"
    )
    objects.append(b"5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n")

    out = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for obj in objects:
        offsets.append(len(out))
        out.extend(obj)
    xref_pos = len(out)
    out.extend(f"xref\n0 {len(objects) + 1}\n".encode("latin-1"))
    out.extend(b"0000000000 65535 f \n")
    for off in offsets[1:]:
        out.extend(f"{off:010d} 00000 n \n".encode("latin-1"))
    out.extend(
        f"trailer<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_pos}\n%%EOF\n".encode(
            "latin-1"
        )
    )
    path.write_bytes(bytes(out))
