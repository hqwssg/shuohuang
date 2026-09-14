from __future__ import annotations

import re

from pydantic import BaseModel

from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.models import ReportInput
from carbon_report_agent.planner import ReportPlan

class ReviewFinding(BaseModel):
    code: str
    message: str
    severity: str


def review_report_input(report: ReportInput, calculations: CalculationResult) -> list[ReviewFinding]:
    findings: list[ReviewFinding] = []
    if not report.metadata.entity_name.strip():
        findings.append(
            ReviewFinding(
                code="empty_entity_name",
                message="Entity name is required.",
                severity="error",
            )
        )
    year = report.metadata.report_year
    if year < 2000 or year > 2100:
        findings.append(
            ReviewFinding(
                code="invalid_report_year",
                message="Report year must be between 2000 and 2100.",
                severity="error",
            )
        )
    if not report.fuel_activity and not report.electricity_activity and not report.heat_activity:
        findings.append(
            ReviewFinding(
                code="missing_activity_data",
                message="At least one activity record is required.",
                severity="error",
            )
        )
    if calculations.total_emissions_tco2 < 0:
        findings.append(
            ReviewFinding(
                code="negative_total_emissions",
                message="Total emissions must be non-negative.",
                severity="error",
            )
        )
    expected_total = round(
        calculations.fuel_combustion_tco2
        + calculations.electricity_emissions_tco2
        + calculations.heat_emissions_tco2,
        2,
    )
    if expected_total != calculations.total_emissions_tco2:
        findings.append(
            ReviewFinding(
                code="total_emissions_mismatch",
                message="Total emissions do not match component sums.",
                severity="error",
            )
        )
    if report.prior_year is not None and report.prior_year.report_year >= report.metadata.report_year:
        findings.append(
            ReviewFinding(
                code="invalid_prior_year",
                message="prior_year.report_year must be earlier than metadata.report_year.",
                severity="error",
            )
        )
    return findings


def _allowed_numeric_tokens(report: ReportInput, calculations: CalculationResult) -> set[str]:
    tokens: set[str] = {
        f"{calculations.total_emissions_tco2:.2f}",
        f"{calculations.fuel_combustion_tco2:.2f}",
        f"{calculations.electricity_emissions_tco2:.2f}",
        f"{calculations.heat_emissions_tco2:.2f}",
        str(report.metadata.report_year),
    }
    for share in calculations.shares.values():
        tokens.add(f"{share}")
        tokens.add(f"{share:.1f}")
        tokens.add(f"{share:.2f}")
    if report.prior_year is not None:
        prior = report.prior_year
        tokens.add(str(prior.report_year))
        tokens.add(f"{prior.total_emissions_tco2:.2f}")
        delta = round(calculations.total_emissions_tco2 - prior.total_emissions_tco2, 2)
        tokens.add(f"{delta:.2f}")
        tokens.add(f"{abs(delta):.2f}")
        if prior.total_emissions_tco2 > 0:
            pct = round(delta / prior.total_emissions_tco2 * 100, 1)
            tokens.add(f"{pct}")
            tokens.add(f"{pct:+.1f}".lstrip("+"))
            tokens.add(f"{abs(pct)}")
    for record in report.fuel_activity:
        tokens.add(f"{record.quantity}")
        tokens.add(f"{record.quantity:.2f}" if isinstance(record.quantity, float) else str(record.quantity))
    for record in report.electricity_activity:
        tokens.add(f"{record.quantity_mwh}")
        tokens.add(f"{record.quantity_mwh:.2f}")
        tokens.add(f"{record.quantity_mwh * 1000:.2f}")  # kWh display
    for record in report.heat_activity:
        tokens.add(f"{record.quantity_gj}")
        tokens.add(f"{record.quantity_gj:.2f}")
    return {t for t in tokens if t}


def review_narratives(
    narratives: dict[str, str],
    report: ReportInput,
    calculations: CalculationResult,
) -> list[ReviewFinding]:
    """Advisory compliance review: flag empty chapters and unsupported tCO2 claims."""
    findings: list[ReviewFinding] = []
    required = [
        "chapter1_basic",
        "chapter2_boundary",
        "chapter3_activity",
        "chapter4_factors",
        "chapter5_calculation",
        "chapter6_conclusion",
        "chapter7_monitoring",
    ]
    for key in required:
        text = (narratives.get(key) or "").strip()
        if not text:
            findings.append(
                ReviewFinding(
                    code="empty_chapter",
                    message=f"Narrative section {key} is empty.",
                    severity="warning",
                )
            )

    allowed = _allowed_numeric_tokens(report, calculations)
    # Match emission totals, not factor units like tCO2/MWh or tCO2/GJ.
    claim_re = re.compile(
        r"(?P<num>\d+(?:\.\d+)?)\s*(?:tCO2|tCO₂|吨二氧化碳|吨CO2)(?!\s*/)",
        re.IGNORECASE,
    )
    for key, text in narratives.items():
        for match in claim_re.finditer(text or ""):
            num = match.group("num")
            normalized = f"{float(num):.2f}" if "." in num else num
            alt = f"{float(num):.2f}"
            if num not in allowed and normalized not in allowed and alt not in allowed:
                findings.append(
                    ReviewFinding(
                        code="unsupported_emission_claim",
                        message=f"{key} contains unsupported emission figure {num}.",
                        severity="warning",
                    )
                )

    total = f"{calculations.total_emissions_tco2:.2f}"
    body = " ".join(narratives.get(k, "") for k in ("chapter5_calculation", "chapter6_conclusion"))
    if total not in body:
        findings.append(
            ReviewFinding(
                code="missing_total_in_narrative",
                message="Calculated total emissions do not appear in chapters 5/6.",
                severity="warning",
            )
        )
    return findings


def review_report_plan(plan: ReportPlan, report: ReportInput, calculations: CalculationResult) -> list[ReviewFinding]:
    findings: list[ReviewFinding] = []
    if plan.entity_name != report.metadata.entity_name:
        findings.append(
            ReviewFinding(
                code="entity_name_mismatch",
                message="Plan entity name does not match report metadata.",
                severity="error",
            )
        )
    if plan.report_year != report.metadata.report_year:
        findings.append(
            ReviewFinding(
                code="report_year_mismatch",
                message="Plan report year does not match report metadata.",
                severity="error",
            )
        )
    if plan.total_emissions_tco2 != calculations.total_emissions_tco2:
        findings.append(
            ReviewFinding(
                code="total_emissions_mismatch",
                message="Plan total emissions does not match calculations.",
                severity="error",
            )
        )
    if not plan.sections:
        findings.append(
            ReviewFinding(
                code="missing_sections",
                message="Plan has no template-derived sections.",
                severity="error",
            )
        )
    return findings
