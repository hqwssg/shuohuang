from __future__ import annotations

from collections import defaultdict

from pydantic import BaseModel

from carbon_report_agent.factors import FactorLibrary
from carbon_report_agent.models import ReportInput

CO2_CARBON_MASS_RATIO = 44 / 12


class CalculationResult(BaseModel):
    factor_version: str
    fuel_emissions: dict[str, float]
    electricity_emissions_tco2: float
    heat_emissions_tco2: float
    total_emissions_tco2: float
    fuel_combustion_tco2: float
    shares: dict[str, float]
    activity_fuel_totals: dict[str, float]
    electricity_total_mwh: float
    heat_total_gj: float


def round_tco2(value: float) -> float:
    return round(value + 1e-9, 2)


def calculate_report(report: ReportInput, factors: FactorLibrary) -> CalculationResult:
    fuel_totals: dict[str, float] = defaultdict(float)
    activity_fuel_totals: dict[str, float] = defaultdict(float)
    for record in report.fuel_activity:
        factor = factors.fuel_factors[record.fuel_type]
        if record.unit != factor.expected_unit:
            raise ValueError(f"{record.fuel_type} expected unit {factor.expected_unit}, got {record.unit}")
        activity_fuel_totals[record.fuel_type] += record.quantity
        if factor.emission_factor is not None:
            fuel_totals[record.fuel_type] += record.quantity * factor.emission_factor
        else:
            fuel_totals[record.fuel_type] += (
                record.quantity * factor.carbon_content * factor.oxidation_rate * CO2_CARBON_MASS_RATIO
            )

    fuel_emissions = {fuel_type: round_tco2(value) for fuel_type, value in sorted(fuel_totals.items())}
    electricity = round_tco2(sum(item.quantity_mwh for item in report.electricity_activity) * factors.electricity_factor_tco2_per_mwh)
    heat = round_tco2(sum(item.quantity_gj for item in report.heat_activity) * factors.heat_factor_tco2_per_gj)
    fuel_combustion = round_tco2(sum(fuel_emissions.values()))
    total = round_tco2(fuel_combustion + electricity + heat)
    shares = {
        "fuel": round(100 * fuel_combustion / total, 2) if total else 0.0,
        "electricity": round(100 * electricity / total, 2) if total else 0.0,
        "heat": round(100 * heat / total, 2) if total else 0.0,
    }
    electricity_total_mwh = sum(item.quantity_mwh for item in report.electricity_activity)
    heat_total_gj = sum(item.quantity_gj for item in report.heat_activity)

    return CalculationResult(
        factor_version=factors.version,
        fuel_emissions=fuel_emissions,
        electricity_emissions_tco2=electricity,
        heat_emissions_tco2=heat,
        total_emissions_tco2=total,
        fuel_combustion_tco2=fuel_combustion,
        shares=shares,
        activity_fuel_totals=dict(sorted(activity_fuel_totals.items())),
        electricity_total_mwh=electricity_total_mwh,
        heat_total_gj=heat_total_gj,
    )
