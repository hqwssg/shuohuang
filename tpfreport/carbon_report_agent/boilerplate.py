from __future__ import annotations

import json
import re
from pathlib import Path

from pydantic import BaseModel, Field

from carbon_report_agent.audit import append_audit_event
from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.factors import FactorLibrary
from carbon_report_agent.models import ReportInput

DEFAULT_BOILERPLATE_PATH = Path(__file__).resolve().parent / "data" / "boilerplate_suning_ghg_v1.json"

_SLOT_RE = re.compile(r"\{([a-zA-Z0-9_]+)\}")


class BoilerplateEntry(BaseModel):
    id: str
    source: str
    text: str
    requires: list[str] = Field(default_factory=list)


class BoilerplateLibrary(BaseModel):
    version: str
    entries: list[BoilerplateEntry]

    @property
    def by_id(self) -> dict[str, BoilerplateEntry]:
        return {entry.id: entry for entry in self.entries}


def load_boilerplate_library(path: Path | None = None) -> BoilerplateLibrary:
    target = Path(path) if path is not None else DEFAULT_BOILERPLATE_PATH
    payload = json.loads(target.read_text(encoding="utf-8"))
    return BoilerplateLibrary.model_validate(payload)


def build_slots(
    report: ReportInput,
    factors: FactorLibrary,
    calculations: CalculationResult | None = None,
) -> dict[str, str]:
    slots = {
        "short_name": report.entity_profile.short_name,
        "entity_name": report.metadata.entity_name,
        "report_year": str(report.metadata.report_year),
        "report_period": report.metadata.report_period,
        "electricity_factor_tco2_per_mwh": f"{factors.electricity_factor_tco2_per_mwh}",
        "heat_factor_tco2_per_gj": f"{factors.heat_factor_tco2_per_gj}",
    }
    for fuel_type, ff in factors.fuel_factors.items():
        slots[f"carbon_content_{fuel_type}"] = f"{ff.carbon_content}"
        slots[f"oxidation_rate_{fuel_type}"] = f"{ff.oxidation_rate}"
    if calculations is not None:
        slots["total_emissions_tco2"] = f"{calculations.total_emissions_tco2}"
        slots["fuel_combustion_tco2"] = f"{calculations.fuel_combustion_tco2}"
        slots["electricity_emissions_tco2"] = f"{calculations.electricity_emissions_tco2}"
        slots["heat_emissions_tco2"] = f"{calculations.heat_emissions_tco2}"
    return slots


def requirements_met(entry: BoilerplateEntry, report: ReportInput) -> bool:
    for req in entry.requires:
        if req.startswith("fuel:"):
            fuel_type = req.split(":", 1)[1]
            if not any(r.fuel_type == fuel_type for r in report.fuel_activity):
                return False
        elif req == "electricity:any":
            if not report.electricity_activity:
                return False
        elif req == "heat:any":
            if not report.heat_activity:
                return False
        else:
            return False
    return True


def render_entry(entry: BoilerplateEntry, slots: dict[str, str]) -> str:
    def _replace(match: re.Match[str]) -> str:
        key = match.group(1)
        return slots.get(key, "")

    return _SLOT_RE.sub(_replace, entry.text)


def collect_paragraphs(
    library: BoilerplateLibrary,
    ids: list[str],
    report: ReportInput,
    factors: FactorLibrary,
    calculations: CalculationResult | None = None,
) -> list[str]:
    slots = build_slots(report, factors, calculations)
    paragraphs: list[str] = []
    for entry_id in ids:
        entry = library.by_id.get(entry_id)
        if entry is None:
            append_audit_event("missing_boilerplate_id", {"id": entry_id})
            continue
        if not requirements_met(entry, report):
            continue
        paragraphs.append(render_entry(entry, slots))
    return paragraphs
