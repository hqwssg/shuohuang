from __future__ import annotations

import io

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei", "DejaVu Sans"]
plt.rcParams["axes.unicode_minus"] = False

from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.models import PriorYearComparison, ReportInput

CATEGORY_LABELS = ("化石燃料", "净购入电力", "净购入热力")


class ChartRenderError(RuntimeError):
    pass


def _current_category_values(calculations: CalculationResult) -> list[float]:
    return [
        calculations.fuel_combustion_tco2,
        calculations.electricity_emissions_tco2,
        calculations.heat_emissions_tco2,
    ]


def _prior_year_category_values(prior: PriorYearComparison) -> list[float]:
    return [
        prior.fuel_combustion_tco2 or 0.0,
        prior.electricity_emissions_tco2 or 0.0,
        prior.heat_emissions_tco2 or 0.0,
    ]


def _should_include_prior_year(prior: PriorYearComparison) -> bool:
    # Only include prior bars when category breakdown exists; total-only skips prior.
    return any(
        value is not None
        for value in (
            prior.fuel_combustion_tco2,
            prior.electricity_emissions_tco2,
            prior.heat_emissions_tco2,
        )
    )


def render_emissions_distribution_png(report: ReportInput, calculations: CalculationResult) -> bytes:
    try:
        current_year = report.metadata.report_year
        current_values = _current_category_values(calculations)
        prior = report.prior_year
        include_prior = prior is not None and _should_include_prior_year(prior)

        buf = io.BytesIO()
        fig, ax = plt.subplots(figsize=(7, 4))

        x_positions = range(len(CATEGORY_LABELS))
        bar_width = 0.35 if include_prior else 0.6

        if include_prior and prior is not None:
            prior_values = _prior_year_category_values(prior)
            prior_year = prior.report_year
            ax.bar(
                [x - bar_width / 2 for x in x_positions],
                prior_values,
                width=bar_width,
                label=str(prior_year),
                color="#94a3b8",
            )
            ax.bar(
                [x + bar_width / 2 for x in x_positions],
                current_values,
                width=bar_width,
                label=str(current_year),
                color="#2563eb",
            )
            ax.legend(title="年度")
        else:
            ax.bar(
                list(x_positions),
                current_values,
                width=bar_width,
                label=str(current_year),
                color="#2563eb",
            )
            ax.legend(title="年度")

        ax.set_xticks(list(x_positions), CATEGORY_LABELS)
        ax.set_ylabel("排放量 (tCO2)")
        ax.set_title("温室气体排放分布")
        ax.grid(axis="y", linestyle="--", alpha=0.35)

        fig.tight_layout()
        fig.savefig(buf, format="png", dpi=120)
        plt.close(fig)
        return buf.getvalue()
    except Exception as exc:  # noqa: BLE001
        raise ChartRenderError(str(exc)) from exc
