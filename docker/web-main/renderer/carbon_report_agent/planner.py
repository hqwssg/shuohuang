from __future__ import annotations

from pydantic import BaseModel

from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.models import ReportInput
from carbon_report_agent.template_distiller import TemplatePackage


class ReportPlan(BaseModel):
    entity_name: str
    report_year: int
    sections: list[str]
    required_tables: list[str]
    total_emissions_tco2: float


def build_report_plan(report: ReportInput, calculations: CalculationResult, template: TemplatePackage) -> ReportPlan:
    required_tables = ["emission-source-summary", "activity-data", "emission-summary", "monitoring-devices"]
    return ReportPlan(
        entity_name=report.metadata.entity_name,
        report_year=report.metadata.report_year,
        sections=template.section_titles,
        required_tables=required_tables,
        total_emissions_tco2=calculations.total_emissions_tco2,
    )
