from __future__ import annotations

from pydantic import BaseModel

from carbon_report_agent.models import SnapshotFactors


class FuelFactor(BaseModel):
    fuel_type: str
    carbon_content: float
    oxidation_rate: float
    expected_unit: str
    source: str
    ncv: float = 0.0
    emission_factor: float | None = None


class FactorLibrary(BaseModel):
    version: str
    fuel_factors: dict[str, FuelFactor]
    electricity_factor_tco2_per_mwh: float
    heat_factor_tco2_per_gj: float

    @classmethod
    def default_2024(cls) -> "FactorLibrary":
        return cls(
            version="transport-default-2024",
            fuel_factors={
                "natural_gas": FuelFactor(
                    fuel_type="natural_gas",
                    carbon_content=5.9564,
                    oxidation_rate=0.99,
                    expected_unit="10k_m3",
                    source="transport-guide-default",
                    ncv=389.31,
                ),
                "gasoline": FuelFactor(
                    fuel_type="gasoline",
                    carbon_content=0.8467,
                    oxidation_rate=0.98,
                    expected_unit="t",
                    source="transport-guide-default",
                    ncv=44.80,
                ),
                "diesel": FuelFactor(
                    fuel_type="diesel",
                    carbon_content=0.8753,
                    oxidation_rate=0.98,
                    expected_unit="t",
                    source="transport-guide-default",
                    ncv=43.33,
                ),
            },
            electricity_factor_tco2_per_mwh=0.5366,
            heat_factor_tco2_per_gj=0.11,
        )

    @classmethod
    def from_snapshot(cls, snapshot: SnapshotFactors) -> "FactorLibrary":
        fuels: dict[str, FuelFactor] = {}
        for fuel_type, item in snapshot.fuel_factors.items():
            fuels[fuel_type] = FuelFactor(
                fuel_type=fuel_type,
                carbon_content=item.carbon_content or 0.0,
                oxidation_rate=item.oxidation_rate or 0.0,
                expected_unit=item.expected_unit,
                source=item.source or snapshot.source or "template",
                ncv=item.ncv,
                emission_factor=item.emission_factor,
            )
        return cls(
            version=snapshot.version or "snapshot",
            fuel_factors=fuels,
            electricity_factor_tco2_per_mwh=snapshot.electricity_factor_tco2_per_mwh or 0.0,
            heat_factor_tco2_per_gj=snapshot.heat_factor_tco2_per_gj or 0.0,
        )
