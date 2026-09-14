from __future__ import annotations

import os
import tempfile
from dataclasses import dataclass
from pathlib import Path

from carbon_report_agent.audit import append_audit_event
from carbon_report_agent.calculations import CalculationResult, calculate_report
from carbon_report_agent.charts import ChartRenderError, render_emissions_distribution_png
from carbon_report_agent.docx_pdf import (
    PdfConversionError,
    PdfConversionUnavailableError,
    convert_docx_to_pdf,
)
from carbon_report_agent.factors import FactorLibrary
from carbon_report_agent.models import ReportInput
from carbon_report_agent.narrative import build_narrative_sections
from carbon_report_agent.publisher import publish_docx
from carbon_report_agent.reviewer import ReviewFinding, review_narratives, review_report_input
from carbon_report_agent.template_distiller import load_template_package


class GenerateBlockedError(Exception):
    def __init__(self, findings: list[ReviewFinding]):
        self.findings = findings
        super().__init__("; ".join(f.message for f in findings))


@dataclass
class GenerateResult:
    path: Path
    calculations: CalculationResult
    narratives: dict[str, str]
    input_findings: list[ReviewFinding]
    narrative_findings: list[ReviewFinding]


__all__ = [
    "GenerateBlockedError",
    "GenerateResult",
    "PdfConversionError",
    "PdfConversionUnavailableError",
    "convert_report_docx_to_pdf",
    "generate_report_docx",
    "generate_report_pdf",
]


def _block_on_advisory() -> bool:
    flag = os.environ.get("CARBON_REPORT_BLOCK_ON_ADVISORY", "").strip().lower()
    return flag in {"1", "true", "yes", "on"}


def _run_pipeline(
    report: ReportInput,
    *,
    factors: FactorLibrary | None = None,
    llm_enabled: bool | None = None,
) -> tuple[CalculationResult, dict[str, str], list[ReviewFinding], list[ReviewFinding]]:
    if factors is not None:
        library = factors
    elif report.factors is not None:
        library = FactorLibrary.from_snapshot(report.factors)
    else:
        library = FactorLibrary.default_2024()
    calculations = calculate_report(report, library)
    input_findings = review_report_input(report, calculations)
    if any(f.severity == "error" for f in input_findings):
        raise GenerateBlockedError(input_findings)
    narratives = build_narrative_sections(
        report, calculations, library, llm_enabled=llm_enabled
    )
    narrative_findings = review_narratives(narratives, report, calculations)
    if _block_on_advisory() and any(f.severity == "warning" for f in narrative_findings):
        raise GenerateBlockedError(narrative_findings)
    return calculations, narratives, input_findings, narrative_findings


def _render_chart_png(report: ReportInput, calculations: CalculationResult) -> bytes | None:
    try:
        return render_emissions_distribution_png(report, calculations)
    except ChartRenderError as exc:
        append_audit_event(
            "chart_render_error",
            {
                "entity": report.metadata.entity_name,
                "year": report.metadata.report_year,
                "error": str(exc),
            },
        )
        return None


def generate_report_docx(
    report: ReportInput,
    output_path: Path,
    *,
    factors: FactorLibrary | None = None,
    llm_enabled: bool | None = None,
) -> Path:
    calculations, narratives, input_findings, narrative_findings = _run_pipeline(
        report, factors=factors, llm_enabled=llm_enabled
    )
    template = load_template_package()
    chart_png = _render_chart_png(report, calculations)
    path = publish_docx(
        report, calculations, narratives, output_path, template=template, chart_png=chart_png
    )
    append_audit_event(
        "generate_docx",
        {
            "entity": report.metadata.entity_name,
            "year": report.metadata.report_year,
            "total_tco2": calculations.total_emissions_tco2,
            "output": str(path),
            "chart_included": chart_png is not None,
            "input_findings": [f.model_dump() for f in input_findings],
            "narrative_findings": [f.model_dump() for f in narrative_findings],
        },
    )
    return path


def convert_report_docx_to_pdf(docx_path: Path, output_path: Path) -> Path:
    path = convert_docx_to_pdf(docx_path, output_path)
    append_audit_event(
        "convert_docx_pdf",
        {
            "input": str(docx_path),
            "output": str(path),
            "pdf_conversion": "docx_to_pdf",
        },
    )
    return path


def generate_report_pdf(
    report: ReportInput,
    output_path: Path,
    *,
    factors: FactorLibrary | None = None,
    llm_enabled: bool | None = None,
) -> Path:
    calculations, narratives, input_findings, narrative_findings = _run_pipeline(
        report, factors=factors, llm_enabled=llm_enabled
    )
    template = load_template_package()
    chart_png = _render_chart_png(report, calculations)
    with tempfile.TemporaryDirectory(prefix="carbon-report-docx-") as tmp:
        docx_path = Path(tmp) / f"carbon-report-{report.metadata.report_year}.docx"
        publish_docx(
            report,
            calculations,
            narratives,
            docx_path,
            template=template,
            chart_png=chart_png,
        )
        path = convert_report_docx_to_pdf(docx_path, output_path)
    append_audit_event(
        "generate_pdf",
        {
            "entity": report.metadata.entity_name,
            "year": report.metadata.report_year,
            "total_tco2": calculations.total_emissions_tco2,
            "output": str(path),
            "pdf_conversion": "docx_to_pdf",
            "chart_included": chart_png is not None,
            "input_findings": [f.model_dump() for f in input_findings],
            "narrative_findings": [f.model_dump() for f in narrative_findings],
        },
    )
    return path


def generate_report_bundle(
    report: ReportInput,
    output_path: Path,
    *,
    factors: FactorLibrary | None = None,
    llm_enabled: bool | None = None,
) -> GenerateResult:
    """Generate DOCX and return structured result (for tests / batch)."""
    calculations, narratives, input_findings, narrative_findings = _run_pipeline(
        report, factors=factors, llm_enabled=llm_enabled
    )
    template = load_template_package()
    chart_png = _render_chart_png(report, calculations)
    path = publish_docx(
        report, calculations, narratives, output_path, template=template, chart_png=chart_png
    )
    append_audit_event(
        "generate_docx",
        {
            "entity": report.metadata.entity_name,
            "year": report.metadata.report_year,
            "total_tco2": calculations.total_emissions_tco2,
            "output": str(path),
            "chart_included": chart_png is not None,
            "input_findings": [f.model_dump() for f in input_findings],
            "narrative_findings": [f.model_dump() for f in narrative_findings],
        },
    )
    return GenerateResult(
        path=path,
        calculations=calculations,
        narratives=narratives,
        input_findings=input_findings,
        narrative_findings=narrative_findings,
    )
