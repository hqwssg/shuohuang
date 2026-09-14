from __future__ import annotations

from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


FuelType = Literal["natural_gas", "gasoline", "diesel"]
FuelUnit = Literal["10k_m3", "t"]
ReportTableRole = Literal[
    "workload_monthly",
    "electricity_internal_detail",
    "electricity_balance_detail",
    "appendix_emission_summary",
    "appendix_fuel",
    "appendix_electricity",
    "appendix_heat",
]


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class ReportMetadata(StrictModel):
    entity_name: str
    report_year: int = Field(ge=2000, le=2100)
    report_period: str
    compiler: str
    compile_date: date
    reviewer: str = ""
    approver: str = ""
    validator: str = ""
    checker: str = ""
    authors: str = ""
    report_number: str = ""
    header_text: str = ""


class EntityProfile(StrictModel):
    short_name: str
    business_description: str
    carbon_department: str
    company_overview: str = ""
    process_description: str = ""
    monitoring_note: str = ""


class EmissionSourceRow(StrictModel):
    category: str = ""
    facility: str = ""
    location: str = ""
    source: str = ""


class OrganizationBoundary(StrictModel):
    boundary_description: str = ""
    emission_sources: list[str] = []
    emission_source_rows: list[EmissionSourceRow] = []
    accounting_method: str = ""


class EmissionBoundaryNotes(StrictModel):
    fuel_boundary_exclusion: str = ""


class ActivityNaturalGasProse(StrictModel):
    usage: str = ""
    metering_method: str = ""
    source_documents: list[str] = []
    unit_note: str = ""
    standard_condition_note: str = ""
    change_note: str = ""


class ActivityLiquidFuelProse(StrictModel):
    usage: str = ""
    metering_method: str = ""
    source_documents: list[str] = []


class ActivityElectricityProse(StrictModel):
    non_fossil_power_note: str = ""
    grid_source: str = ""
    source_documents: list[str] = []
    settlement_note: str = ""


class ActivityHeatProse(StrictModel):
    contract_note: str = ""
    payment_method: str = ""
    estimation_method: str = ""
    no_resale_note: str = ""
    source_documents: list[str] = []


class ActivityProseContext(StrictModel):
    natural_gas: ActivityNaturalGasProse = Field(default_factory=ActivityNaturalGasProse)
    liquid_fuel: ActivityLiquidFuelProse = Field(default_factory=ActivityLiquidFuelProse)
    electricity: ActivityElectricityProse = Field(default_factory=ActivityElectricityProse)
    heat: ActivityHeatProse = Field(default_factory=ActivityHeatProse)


class Chapter6EmissionIntensity(StrictModel):
    applicable: bool | None = None
    unit: str = ""
    current_year_value: float | None = None
    prior_year_value: float | None = None
    change_percent: float | None = None
    analysis: str = ""
    not_applicable_note: str = ""


class Chapter6TrendYear(StrictModel):
    year: int = Field(ge=2000, le=2100)
    total_emissions_tco2: float = Field(ge=0)
    fuel_combustion_tco2: float | None = Field(default=None, ge=0)
    electricity_emissions_tco2: float | None = Field(default=None, ge=0)
    heat_emissions_tco2: float | None = Field(default=None, ge=0)
    workload: float | None = Field(default=None, ge=0)
    workload_unit: str = ""


class Chapter6ProseContext(StrictModel):
    emission_intensity: Chapter6EmissionIntensity = Field(default_factory=Chapter6EmissionIntensity)
    trend_years: list[Chapter6TrendYear] = []


class EquipmentItem(StrictModel):
    name: str
    category: str = ""
    quantity: float = 0
    unit: str = ""
    model: str = ""
    energy_type: str = ""
    remark: str = ""


class WorkloadRecord(StrictModel):
    name: str
    quantity: float = 0
    unit: str = ""


class FuelActivityRecord(StrictModel):
    fuel_type: FuelType
    facility: str
    quantity: float = Field(ge=0)
    unit: FuelUnit
    month: int | None = Field(default=None, ge=1, le=12)
    source_name: str


class ElectricityActivityRecord(StrictModel):
    scope: str
    quantity_mwh: float = Field(ge=0)
    month: int | None = Field(default=None, ge=1, le=12)
    source_name: str


class HeatActivityRecord(StrictModel):
    site: str
    quantity_gj: float = Field(ge=0)
    source_name: str
    month: int | None = Field(default=None, ge=1, le=12)
    heating_area_m2: float | None = Field(default=None, ge=0)
    heat_coefficient: float | None = Field(default=None, ge=0)


class ReportImage(StrictModel):
    role: str
    caption: str = ""
    url: str | None = None
    mime_type: str = "image/png"


class ReportTablePayload(StrictModel):
    role: ReportTableRole
    caption: str = ""
    rows: list[list[str | int | float | None]]


class MonitoringDevice(StrictModel):
    name: str
    model: str = ""
    accuracy: str = ""
    measurement_range: str = ""
    location: str = ""
    calibration_frequency: str = ""
    actual_calibration_frequency: str = ""
    calibration_time: str = ""
    guide_compliant: str = ""


class PriorYearComparison(StrictModel):
    report_year: int = Field(ge=2000, le=2100)
    total_emissions_tco2: float = Field(ge=0)
    fuel_combustion_tco2: float | None = Field(default=None, ge=0)
    electricity_emissions_tco2: float | None = Field(default=None, ge=0)
    heat_emissions_tco2: float | None = Field(default=None, ge=0)


class SnapshotFuelFactor(StrictModel):
    carbon_content: float | None = None
    oxidation_rate: float | None = None
    expected_unit: FuelUnit
    ncv: float = 0.0
    emission_factor: float | None = None
    source: str = ""
    name: str = ""
    fuel_type: FuelType | None = None


class SnapshotFactors(StrictModel):
    version: str = ""
    source: str = ""
    electricity_factor_tco2_per_mwh: float | None = None
    heat_factor_tco2_per_gj: float | None = None
    fuel_factors: dict[str, SnapshotFuelFactor] = Field(default_factory=dict)


class ReportInput(StrictModel):
    entity_code: str = "suning"
    metadata: ReportMetadata
    entity_profile: EntityProfile
    period_grain: Literal["year", "month"] = "year"
    organization_boundary: OrganizationBoundary | None = None
    emission_boundary_notes: EmissionBoundaryNotes = Field(default_factory=EmissionBoundaryNotes)
    activity_prose: ActivityProseContext = Field(default_factory=ActivityProseContext)
    equipment: list[EquipmentItem] = []
    workload: list[WorkloadRecord] = []
    prior_year: PriorYearComparison | None = None
    fuel_activity: list[FuelActivityRecord]
    electricity_activity: list[ElectricityActivityRecord]
    heat_activity: list[HeatActivityRecord]
    monitoring_devices: list[MonitoringDevice]
    report_tables: list[ReportTablePayload] = []
    images: list[ReportImage] = []
    chapter6: Chapter6ProseContext = Field(default_factory=Chapter6ProseContext)
    factors: SnapshotFactors | None = None
