from __future__ import annotations

import re

from carbon_report_agent.audit import append_audit_event
from carbon_report_agent.boilerplate import collect_paragraphs, load_boilerplate_library
from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.factors import FactorLibrary
from carbon_report_agent.llm_narrative import (
    draft_chapter6_segments_with_llm,
    draft_chapter3_activity_intro_segments,
    draft_chapter6_with_llm,
    draft_emission_source_intro_segments,
    llm_enabled,
)
from carbon_report_agent.models import ReportInput

_CH2_LIBRARY_IDS = [
    "ch2.guide_boundary",
]

_FUEL_SOURCE_LABELS = {
    "natural_gas": "\u5929\u7136\u6c14",
    "gasoline": "\u6c7d\u6cb9",
    "diesel": "\u67f4\u6cb9",
}


def _unique_nonempty(values: list[str]) -> list[str]:
    seen = set()
    result = []
    for value in values:
        text = (value or "").strip()
        if text and text not in seen:
            seen.add(text)
            result.append(text)
    return result


def _heat_activity_sites(report: ReportInput) -> list[str]:
    return _unique_nonempty([record.site for record in report.heat_activity])


def _extract_station_names(text: str) -> list[str]:
    return _unique_nonempty(
        [
            match
            for match in re.findall(r"[\u4e00-\u9fffA-Za-z0-9\uff08\uff09()\u00b7\-]+\u7ad9", text or "")
            if match not in {"\u7ad9", "\u7ad9\u70b9", "\u7b49\u7ad9"}
        ]
    )


def _conflicting_heat_sites_in_text(report: ReportInput, text: str) -> list[str]:
    authoritative = set(_heat_activity_sites(report))
    if not authoritative:
        return []
    return [site for site in _extract_station_names(text) if site not in authoritative]


def _heat_segment_conflicts_with_activity(report: ReportInput, text: str) -> bool:
    return bool(_conflicting_heat_sites_in_text(report, text))


def _row_blob(row) -> str:
    return "\u3001".join(
        part
        for part in (row.category, row.facility, row.location, row.source)
        if part
    )


def _emission_rows_by_kind(report: ReportInput, keywords: tuple[str, ...]):
    rows = []
    boundary = report.organization_boundary
    if not boundary:
        return rows
    for row in boundary.emission_source_rows:
        blob = _row_blob(row)
        if any(keyword in blob for keyword in keywords):
            rows.append(row)
    return rows


def _format_facility_source_pairs(rows) -> list[str]:
    pairs = []
    for row in rows:
        facility = (row.facility or "").strip()
        source = (row.source or "").strip()
        if facility and source and source not in facility:
            pairs.append(f"{facility}\u4f7f\u7528{source}")
        else:
            pairs.append(facility or source)
    return _unique_nonempty(pairs)


def _fallback_emission_source_segments(report: ReportInput) -> dict[str, str]:
    short_name = report.entity_profile.short_name or report.metadata.entity_name
    joiner = "\u3001"
    fuel_rows = _emission_rows_by_kind(
        report,
        ("\u5316\u77f3", "\u71c3\u6599", "\u5929\u7136\u6c14", "\u6c7d\u6cb9", "\u67f4\u6cb9"),
    )
    fuel_parts = _format_facility_source_pairs(fuel_rows)
    if not fuel_parts:
        fuel_parts = _unique_nonempty(
            [_FUEL_SOURCE_LABELS.get(record.fuel_type, record.fuel_type) for record in report.fuel_activity]
        )
    if fuel_parts:
        fuel = (
            f"{short_name}\u6240\u6d89\u53ca\u7684\u71c3\u6599\u71c3\u70e7\u6392\u653e\u5305\u62ec\uff1a"
            f"{joiner.join(fuel_parts)}\u7b49\u5316\u77f3\u71c3\u6599\u8fc7\u7a0b\u4ea7\u751f\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"
        )
    else:
        fuel = f"{short_name}\u4e0d\u6d89\u53ca\u71c3\u6599\u71c3\u70e7\u4ea7\u751f\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"

    electricity_rows = _emission_rows_by_kind(report, ("\u7535\u529b", "\u7528\u7535", "\u5916\u8d2d\u7535"))
    electricity_facilities = _format_facility_source_pairs(electricity_rows)
    if electricity_facilities:
        electricity = (
            f"{short_name}{joiner.join(electricity_facilities)}\u6d88\u8017\u7535\u529b\uff0c"
            "\u6d89\u53ca\u5916\u8d2d\u7535\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"
        )
    elif report.electricity_activity:
        electricity = f"{short_name}\u6d88\u8017\u5916\u8d2d\u7535\u529b\uff0c\u6d89\u53ca\u5916\u8d2d\u7535\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"
    else:
        electricity = f"{short_name}\u4e0d\u6d89\u53ca\u51c0\u8d2d\u5165\u7535\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"

    heat_sites = _heat_activity_sites(report)
    if not heat_sites:
        heat_rows = _emission_rows_by_kind(report, ("\u70ed\u529b", "\u4f9b\u6696", "\u5e02\u653f\u70ed"))
        heat_sites = _unique_nonempty([row.facility or row.location or row.source for row in heat_rows])
    if not heat_sites:
        heat_sites = _unique_nonempty([record.site for record in report.heat_activity])
    if heat_sites:
        heat = (
            f"{short_name}{joiner.join(heat_sites)}\u63a5\u5165\u5e02\u653f\u70ed\u529b\uff0c"
            "\u9700\u8981\u8ba1\u7b97\u51c0\u8d2d\u5165\u70ed\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"
        )
    else:
        heat = f"{short_name}\u4e0d\u6d89\u53ca\u51c0\u8d2d\u5165\u70ed\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u3002"
    return {"fuel": fuel, "electricity": electricity, "heat": heat}


def _valid_emission_source_segments(segments: dict[str, str] | None) -> bool:
    if not isinstance(segments, dict):
        return False
    return all(str(segments.get(key, "")).strip() for key in ("fuel", "electricity", "heat"))


def _valid_chapter3_activity_intro_segments(segments: dict[str, str] | None) -> bool:
    if not isinstance(segments, dict):
        return False
    return all(
        str(segments.get(key, "")).strip()
        for key in ("natural_gas", "liquid_fuel", "electricity", "heat")
    )


def _build_chapter2_emission_source_intro(report: ReportInput) -> str:
    segments = _fallback_emission_source_segments(report)
    if llm_enabled():
        drafted = draft_emission_source_intro_segments(report)
        if _valid_emission_source_segments(drafted):
            drafted_segments = {key: str(drafted[key]).strip() for key in ("fuel", "electricity", "heat")}
            if _heat_segment_conflicts_with_activity(report, drafted_segments["heat"]):
                append_audit_event(
                    "llm_heat_site_conflict",
                    {
                        "section": "chapter2_emission_source_intro",
                        "conflicting_sites": _conflicting_heat_sites_in_text(report, drafted_segments["heat"]),
                        "authoritative_heat_sites": _heat_activity_sites(report),
                    },
                )
                segments["fuel"] = drafted_segments["fuel"]
                segments["electricity"] = drafted_segments["electricity"]
            else:
                segments = drafted_segments
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    lines = [
        f"\u6839\u636e\u300a\u8fd0\u8f93\u4f01\u4e1a\u6838\u7b97\u6307\u5357\u300b\u7684\u89c4\u5b9a\uff0c\u5bf9{short_name}\u8bbe\u65bd\u8fb9\u754c\u5185\u6392\u653e\u6e90\u8fdb\u884c\u8bc6\u522b\uff1a",
        "1\u3001\u71c3\u6599\u71c3\u70e7\u4ea7\u751f\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e",
        segments["fuel"],
        "2\u3001\u51c0\u8d2d\u5165\u7684\u7535\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e",
        segments["electricity"],
        "3\u3001\u4f01\u4e1a\u51c0\u8d2d\u5165\u70ed\u529b\u9690\u542b\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e",
        segments["heat"],
    ]
    return "\n".join(lines)


def _build_chapter1_basic(report: ReportInput) -> str:
    profile = report.entity_profile
    meta = report.metadata
    overview = (profile.company_overview or "").strip()
    process = (profile.process_description or "").strip()
    if not overview and not process:
        append_audit_event(
            "missing_chapter1_long_prose",
            {
                "entity_name": meta.entity_name,
                "report_year": meta.report_year,
            },
        )
    short_bits = (
        f"\u62a5\u544a\u4e3b\u4f53\u4e3a{meta.entity_name}\uff0c"
        f"\u7b80\u79f0{profile.short_name}\uff0c"
        f"{profile.business_description}"
        f"\u78b3\u6392\u653e\u4e3b\u7ba1\u90e8\u95e8\u4e3a{profile.carbon_department}\uff0c"
        f"\u62a5\u544a\u671f\u95f4\u4e3a{meta.report_period}\uff08{meta.report_year}\u5e74\uff09\uff0c"
        f"\u7f16\u5236\u5355\u4f4d\u4e3a{meta.compiler}\u3002"
    )
    parts = [p for p in (overview, process, short_bits) if p]
    return "".join(parts)


def _build_chapter1_equipment_intro(report: ReportInput) -> str:
    if not report.equipment:
        return "\u672c\u62a5\u544a\u671f\u672a\u63d0\u4f9b\u4e3b\u8981\u7528\u80fd\u8bbe\u5907\u53f0\u8d26\u4fe1\u606f\u3002"
    return (
        f"\u62a5\u544a\u4e3b\u4f53\u4e3b\u8981\u7528\u80fd\u8bbe\u5907\u5171{len(report.equipment)}\u9879\uff0c"
        "\u8be6\u89c1\u4e0b\u8868\u3002"
    )


def _build_chapter1_workload_intro(report: ReportInput) -> str:
    if not report.workload:
        return "\u672c\u62a5\u544a\u671f\u672a\u63d0\u4f9b\u4e1a\u52a1\u5de5\u4f5c\u91cf\u4fe1\u606f\u3002"
    return "\u62a5\u544a\u4e3b\u4f53\u4e3b\u8981\u4e1a\u52a1\u5de5\u4f5c\u91cf\u5982\u4e0b\u3002"


def _build_chapter2_boundary(report: ReportInput, factors: FactorLibrary) -> str:
    library = load_boilerplate_library()
    parts = collect_paragraphs(library, _CH2_LIBRARY_IDS, report, factors)
    boundary = report.organization_boundary
    if boundary and (
        boundary.boundary_description.strip()
        or boundary.emission_sources
        or boundary.accounting_method.strip()
    ):
        if boundary.boundary_description.strip():
            parts.append(boundary.boundary_description.strip())
        sources = "\u3001".join(boundary.emission_sources) if boundary.emission_sources else "\u65e0"
        parts.append(f"\u6392\u653e\u6e90\u8303\u56f4\uff1a{sources}\u3002")
        if boundary.accounting_method.strip():
            parts.append(f"\u6838\u7b97\u65b9\u6cd5\uff1a{boundary.accounting_method}\u3002")
    elif not parts:
        return (
            "\u672c\u62a5\u544a\u672a\u63d0\u4f9b\u7ec4\u7ec7\u8fb9\u754c\u3001"
            "\u6392\u653e\u6e90\u8303\u56f4\u53ca\u6838\u7b97\u65b9\u6cd5\u8bf4\u660e\u3002"
        )
    return "".join(parts)


def _join_zh_items(items: list[str]) -> str:
    if len(items) <= 1:
        return "".join(items)
    if len(items) == 2:
        return "\u548c".join(items)
    return "\uff0c".join(items[:-1]) + "\u548c" + items[-1]


def _chapter3_fuel_labels(report: ReportInput) -> list[str]:
    used_fuels = {record.fuel_type for record in report.fuel_activity}
    if "natural_gas" in used_fuels:
        preferred = ["natural_gas", "diesel", "gasoline"]
    else:
        preferred = ["gasoline", "diesel"]
    ordered = [fuel for fuel in preferred if fuel in used_fuels]
    ordered.extend(sorted(used_fuels - set(ordered)))
    return [_FUEL_SOURCE_LABELS.get(fuel, fuel) for fuel in ordered]


_FUEL_FACILITY_LABELS = {
    "canteen": "\u98df\u5802\u7076\u5177",
    "vehicle": "\u516c\u52a1\u8f66\u8f86",
    "rail-work-vehicle": "\u751f\u4ea7\u8f66\u8f86",
}


def _source_names(values: list[str]) -> str:
    names = sorted(_unique_nonempty(values))
    return "\u3001".join(names) if names else "\u6570\u636e\u6765\u6e90\u6587\u4ef6"


def _fuel_facility_labels(report: ReportInput, fuel_types: set[str]) -> list[str]:
    facilities = [
        _FUEL_FACILITY_LABELS.get(record.facility, record.facility)
        for record in report.fuel_activity
        if record.fuel_type in fuel_types
    ]
    return _unique_nonempty(facilities)


def _build_chapter3_fuel_intro(report: ReportInput) -> str:
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    if not report.fuel_activity:
        return f"{short_name}\u4e0d\u6d89\u53ca\u5316\u77f3\u71c3\u6599\u71c3\u70e7\u6d3b\u52a8\u6c34\u5e73\u6570\u636e\u3002"
    return f"{short_name}\u6d89\u53ca\u4ee5\u4e0b\u5316\u77f3\u71c3\u6599\u71c3\u70e7\u6d3b\u52a8\u6c34\u5e73\u6570\u636e\uff1a"


def _build_chapter3_natural_gas_intro(report: ReportInput) -> str:
    gas_records = [record for record in report.fuel_activity if record.fuel_type == "natural_gas"]
    if not gas_records:
        return ""
    facilities = _fuel_facility_labels(report, {"natural_gas"})
    source = _source_names([record.source_name for record in gas_records])
    facility_text = "\u3001".join(facilities) if facilities else "\u76f8\u5173\u8bbe\u65bd"
    return (
        f"\u5929\u7136\u6c14\u7528\u4e8e{facility_text}\uff0c"
        f"\u5929\u7136\u6c14\u6d88\u8017\u7531\u4f01\u4e1a\u7edf\u8ba1\u8bb0\u5f55\u5728\u300a{source}\u300b\u4e2d\u3002"
        "\u5929\u7136\u6c14\u6d88\u8017\u7edf\u8ba1\u7ed3\u679c\u548c\u6570\u636e\u6765\u6e90\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


def _build_chapter3_liquid_fuel_intro(report: ReportInput) -> str:
    liquid_records = [
        record
        for record in report.fuel_activity
        if record.fuel_type in {"gasoline", "diesel"}
    ]
    if not liquid_records:
        return ""
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    used_fuels = {record.fuel_type for record in liquid_records}
    source = _source_names([record.source_name for record in liquid_records])
    if used_fuels == {"gasoline", "diesel"}:
        use_sentence = "\u6c7d\u6cb9\u548c\u67f4\u6cb9\u5206\u522b\u7528\u4e8e\u516c\u52a1\u8f66\u8f86\u548c\u751f\u4ea7\u8f66\u8f86\u4f7f\u7528\u3002"
        fuel_text = "\u6c7d\u6cb9\u3001\u67f4\u6cb9"
    elif "gasoline" in used_fuels:
        facilities = _fuel_facility_labels(report, {"gasoline"})
        facilities_text = "\u3001".join(facilities) or "\u516c\u52a1\u8f66\u8f86"
        use_sentence = f"\u6c7d\u6cb9\u7528\u4e8e{facilities_text}\u4f7f\u7528\u3002"
        fuel_text = "\u6c7d\u6cb9"
    else:
        facilities = _fuel_facility_labels(report, {"diesel"})
        facilities_text = "\u3001".join(facilities) or "\u751f\u4ea7\u8f66\u8f86"
        use_sentence = f"\u67f4\u6cb9\u7528\u4e8e{facilities_text}\u4f7f\u7528\u3002"
        fuel_text = "\u67f4\u6cb9"
    return (
        use_sentence
        +
        f"\u6d88\u8017\u91cf\u7531{short_name}\u7edf\u8ba1\u8bb0\u5f55\u5728\u300a{source}\u300b\u4e2d\u3002"
        f"{short_name}{report.metadata.report_year}\u5e74\u6708\u5ea6{fuel_text}\u6d88\u8017\u7edf\u8ba1\u7ed3\u679c\u548c\u6570\u636e\u6765\u6e90\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


def _build_chapter3_electricity_intro(report: ReportInput) -> str:
    if not report.electricity_activity:
        return ""
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    source = _source_names([record.source_name for record in report.electricity_activity])
    return (
        "\u672c\u62a5\u544a\u4e2d\u7684\u51c0\u8d2d\u5165\u7535\u529b\u4e3a\u4f01\u4e1a\u5916\u8d2d\u7535\u529b\uff0c"
        f"\u7535\u529b\u6d88\u8017\u7531{short_name}\u7edf\u8ba1\u8bb0\u5f55\u5728\u300a{source}\u300b\u4e2d\u3002"
        "\u51c0\u8d2d\u5165\u7535\u529b\u7edf\u8ba1\u7ed3\u679c\u548c\u6570\u636e\u6765\u6e90\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


def _build_chapter3_heat_intro(report: ReportInput) -> str:
    if not report.heat_activity:
        return ""
    sites = _heat_activity_sites(report)
    site_text = f"\u6d89\u53ca\u7ad9\u70b9\u5305\u62ec{'\u3001'.join(sites)}\u3002" if sites else ""
    return (
        "\u672c\u62a5\u544a\u4e2d\u7684\u51c0\u8d2d\u5165\u70ed\u529b\u4e3a\u4f01\u4e1a\u5916\u8d2d\u70ed\u529b\uff0c"
        f"{site_text}"
        "\u51c0\u8d2d\u5165\u70ed\u529b\u7edf\u8ba1\u7ed3\u679c\u548c\u6570\u636e\u6765\u6e90\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


def _build_chapter3_activity(report: ReportInput, calculations: CalculationResult) -> str:
    del calculations
    items = []
    fuel_labels = _chapter3_fuel_labels(report)
    if fuel_labels:
        items.append(
            f"\u5316\u77f3\u71c3\u6599\uff08{'\u3001'.join(fuel_labels)}\uff09\u7684\u6d88\u8017\u91cf"
        )
    if report.electricity_activity:
        items.append("\u51c0\u8d2d\u5165\u7535\u529b\u4f7f\u7528\u91cf")
    if report.heat_activity:
        items.append("\u51c0\u8d2d\u5165\u70ed\u529b\u4f7f\u7528\u91cf")
    if not items:
        return "\u672c\u62a5\u544a\u672a\u63d0\u4f9b\u6d3b\u52a8\u6c34\u5e73\u6570\u636e\u3002"
    return f"\u672c\u62a5\u544a\u6d89\u53ca\u7684\u6d3b\u52a8\u6c34\u5e73\u6570\u636e\u5305\u62ec{_join_zh_items(items)}\u3002"


def _build_chapter4_factors(report: ReportInput, calculations: CalculationResult, factors: FactorLibrary) -> str:
    library = load_boilerplate_library()
    ch4_ids = [entry.id for entry in library.entries if entry.id.startswith("ch4.")]
    parts = collect_paragraphs(library, ch4_ids, report, factors, calculations)
    used_fuel_types = sorted({record.fuel_type for record in report.fuel_activity})
    fuel_factor_lines = []
    for fuel_type in used_fuel_types:
        factor = factors.fuel_factors[fuel_type]
        fuel_factor_lines.append(
            f"{fuel_type}(\u78b3\u542b\u91cf{factor.carbon_content}\u3001"
            f"\u6c27\u5316\u7387{factor.oxidation_rate}\u3001"
            f"\u6765\u6e90{factor.source})"
        )
    parts.append(
        f"\u672c\u62a5\u544a\u91c7\u7528\u6392\u653e\u56e0\u5b50\u7248\u672c{calculations.factor_version}\u3002"
    )
    if fuel_factor_lines:
        parts.append(f"\u71c3\u6599\u6392\u653e\u56e0\u5b50\uff1a{'\u3001'.join(fuel_factor_lines)}\u3002")
    if report.electricity_activity:
        parts.append(
            f"\u7535\u529b\u6392\u653e\u56e0\u5b50\u4e3a{factors.electricity_factor_tco2_per_mwh} tCO2/MWh"
            f"\uff08\u6765\u6e90transport-guide-default\uff09\u3002"
        )
    if report.heat_activity:
        parts.append(
            f"\u70ed\u529b\u6392\u653e\u56e0\u5b50\u4e3a{factors.heat_factor_tco2_per_gj} tCO2/GJ"
            f"\uff08\u6765\u6e90transport-guide-default\uff09\u3002"
        )
    return "".join(parts)


def _build_chapter5_calculation(calculations: CalculationResult) -> str:
    shares = calculations.shares
    return (
        f"\u5316\u77f3\u71c3\u6599\u71c3\u70e7\u6392\u653e\u91cf\u4e3a{calculations.fuel_combustion_tco2:.2f} tCO2\uff0c"
        f"\u51c0\u8d2d\u5165\u7535\u529b\u9690\u542b\u6392\u653e\u91cf\u4e3a{calculations.electricity_emissions_tco2:.2f} tCO2\uff0c"
        f"\u51c0\u8d2d\u5165\u70ed\u529b\u9690\u542b\u6392\u653e\u91cf\u4e3a{calculations.heat_emissions_tco2:.2f} tCO2\uff0c"
        f"\u6e29\u5ba4\u6c14\u4f53\u6392\u653e\u603b\u91cf\u4e3a{calculations.total_emissions_tco2:.2f} tCO2\u3002"
        f"\u5404\u5206\u7c7b\u5360\u6bd4\uff1a"
        f"\u71c3\u6599{shares['fuel']}%\u3001"
        f"\u7535\u529b{shares['electricity']}%\u3001"
        f"\u70ed\u529b{shares['heat']}%\u3002"
    )


def _chapter5_fuel_labels(report: ReportInput) -> list[str]:
    used_fuels = {record.fuel_type for record in report.fuel_activity}
    preferred = ["natural_gas", "gasoline", "diesel"]
    ordered = [fuel for fuel in preferred if fuel in used_fuels]
    ordered.extend(sorted(used_fuels - set(ordered)))
    return [_FUEL_SOURCE_LABELS.get(fuel, fuel) for fuel in ordered]


def _year_label(report: ReportInput) -> str:
    return f"{report.metadata.report_year}\u5e74"


def _build_chapter5_fuel_intro(report: ReportInput) -> str:
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    fuel_labels = _chapter5_fuel_labels(report)
    if not fuel_labels:
        return f"{short_name}\u4e0d\u6d89\u53ca\u5316\u77f3\u71c3\u6599\u71c3\u70e7CO2\u6392\u653e\u3002"
    fuel_text = "\u3001".join(fuel_labels)
    return (
        f"{short_name}\u5316\u77f3\u71c3\u6599\u71c3\u70e7\u5305\u62ec"
        f"{fuel_text}\u3002"
        f"{_year_label(report)}\u71c3\u6599\u71c3\u70e7\u6392\u653e\u91cf\u8ba1\u7b97\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


def _build_chapter5_power_heat_intro(report: ReportInput) -> str:
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    return (
        f"{short_name}{_year_label(report)}\u51c0\u8d2d\u5165\u7684\u7535\u529b\u548c\u70ed\u529b"
        "\u6d88\u8d39\u5f15\u8d77\u7684\u4e8c\u6c27\u5316\u78b3\u6392\u653e\u91cf\u8ba1\u7b97\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


def _build_chapter5_summary_intro(report: ReportInput) -> str:
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    return (
        f"{short_name}{report.metadata.report_year}\u5e74\u5ea6"
        "\u6e29\u5ba4\u6c14\u4f53\u6392\u653e\u91cf\u6c47\u603b\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )


_EMISSION_CATEGORY_LABELS = {
    "fuel": "\u71c3\u6599\u71c3\u70e7CO2\u6392\u653e",
    "electricity": "\u51c0\u8d2d\u5165\u7535\u529b\u9690\u542b\u7684CO2\u6392\u653e",
    "heat": "\u51c0\u8d2d\u5165\u70ed\u529b\u9690\u542b\u7684CO2\u6392\u653e",
}


def _chapter6_period_label(report: ReportInput) -> str:
    return f"{report.metadata.report_year}\u5e74"


def _build_chapter6_conclusion_before_chart(
    report: ReportInput,
    calculations: CalculationResult,
) -> str:
    profile = report.entity_profile
    text = (
        f"{profile.short_name}{report.metadata.report_period}"
        f"\u6e29\u5ba4\u6c14\u4f53\u6392\u653e\u603b\u91cf\u4e3a{calculations.total_emissions_tco2:.2f} tCO2\u3002"
    )
    if report.prior_year is not None:
        prior = report.prior_year
        delta = round(calculations.total_emissions_tco2 - prior.total_emissions_tco2, 2)
        if prior.total_emissions_tco2 > 0:
            pct = round(delta / prior.total_emissions_tco2 * 100, 1)
            direction = "\u4e0a\u5347" if delta > 0 else ("\u4e0b\u964d" if delta < 0 else "\u6301\u5e73")
            text += (
                f"\u4e0e{prior.report_year}\u5e74\u603b\u91cf{prior.total_emissions_tco2:.2f} tCO2\u76f8\u6bd4\uff0c"
                f"\u672c\u671f{direction}{abs(delta):.2f} tCO2\uff08{pct:+.1f}%\uff09\u3002"
            )
        else:
            text += (
                f"\u4e0e{prior.report_year}\u5e74\u603b\u91cf{prior.total_emissions_tco2:.2f} tCO2\u76f8\u6bd4\uff0c"
                f"\u672c\u671f\u53d8\u5316{delta:.2f} tCO2\u3002"
            )
    text += (
        f"{profile.short_name}{_chapter6_period_label(report)}\u5e74\u5ea6"
        "\u6e29\u5ba4\u6c14\u4f53\u6392\u653e\u91cf\u4e2d\u7684\u5206\u5e03\u60c5\u51b5\u89c1\u4e0b\u56fe\uff1a"
    )
    return text


def _build_chapter6_conclusion_after_chart(
    report: ReportInput,
    calculations: CalculationResult,
) -> str:
    ordered = sorted(
        (
            (key, float(calculations.shares.get(key, 0.0)))
            for key in ("fuel", "electricity", "heat")
        ),
        key=lambda item: item[1],
        reverse=True,
    )
    labels = [_EMISSION_CATEGORY_LABELS[key] for key, _ in ordered]
    shares = [f"{share:.2f}".rstrip("0").rstrip(".") for _, share in ordered]
    return (
        f"\u4e0a\u56fe\u53ef\u4ee5\u770b\u51fa\u4f01\u4e1a{report.metadata.report_year}\u5e74"
        "\u6392\u653e\u91cf\u5360\u6bd4\u7531\u5927\u5230\u5c0f\u5206\u522b\u4e3a"
        f"{_join_zh_items(labels)}\uff0c"
        f"\u5206\u522b\u5360\u603b\u6392\u653e\u91cf\u7684{_join_zh_items(shares)}%\u3002"
    )


def _build_chapter6_conclusion(report: ReportInput, calculations: CalculationResult) -> str:
    return (
        _build_chapter6_conclusion_before_chart(report, calculations)
        + _build_chapter6_conclusion_after_chart(report, calculations)
    )


def _build_chapter7_monitoring_intro(report: ReportInput) -> str:
    short_name = report.entity_profile.short_name or report.metadata.entity_name or "\u4f01\u4e1a"
    return f"{short_name}\u76d1\u6d4b\u8bbe\u5907\u4fe1\u606f\u5982\u4e0b\u8868\u6240\u793a\u3002"


def _fallback_chapter7_monitoring_note(report: ReportInput) -> str:
    fuel_types = {record.fuel_type for record in report.fuel_activity}
    parts = []
    if {"gasoline", "diesel"}.issubset(fuel_types):
        parts.append("\u6c7d\u6cb9\u548c\u67f4\u6cb9\u5747\u901a\u8fc7\u793e\u4f1a\u52a0\u6cb9\u7ad9\u52a0\u6cb9")
    elif "gasoline" in fuel_types:
        parts.append("\u6c7d\u6cb9\u901a\u8fc7\u793e\u4f1a\u52a0\u6cb9\u7ad9\u52a0\u6cb9")
    elif "diesel" in fuel_types:
        parts.append("\u67f4\u6cb9\u901a\u8fc7\u793e\u4f1a\u52a0\u6cb9\u7ad9\u52a0\u6cb9")
    if report.heat_activity:
        parts.append("\u70ed\u529b\u6309\u7167\u4f9b\u6696\u9762\u79ef\u548c\u7ed3\u7b97\u6570\u636e")
    if not parts:
        return "\u672a\u63d0\u4f9b\u71c3\u6599\u53ca\u70ed\u529b\u7ed3\u7b97\u65b9\u5f0f\u8bf4\u660e\u3002"
    return "\uff1b".join(parts) + "\u3002"


def _build_chapter7_monitoring(report: ReportInput) -> str:
    if not report.monitoring_devices:
        return "\u672a\u63d0\u4f9b\u76d1\u6d4b\u8bbe\u5907\u4fe1\u606f\u3002"
    note = (report.entity_profile.monitoring_note or "").strip()
    if note:
        return note
    return _fallback_chapter7_monitoring_note(report)


def _build_chapter1_process(report: ReportInput) -> str:
    text = (report.entity_profile.process_description or "").strip()
    if text:
        return text
    return (report.entity_profile.business_description or "").strip()


def build_narrative_sections(
    report: ReportInput,
    calculations: CalculationResult,
    factors: FactorLibrary | None = None,
    *,
    llm_enabled: bool | None = None,
) -> dict[str, str]:
    from carbon_report_agent.llm_narrative import llm_enabled_override

    if factors is None:
        factors = FactorLibrary.default_2024()
    with llm_enabled_override(llm_enabled):
        return _build_narrative_sections_inner(report, calculations, factors)


def _build_narrative_sections_inner(
    report: ReportInput,
    calculations: CalculationResult,
    factors: FactorLibrary,
) -> dict[str, str]:
    chapter3_intro_segments = {
        "chapter3_natural_gas_intro": _build_chapter3_natural_gas_intro(report),
        "chapter3_liquid_fuel_intro": _build_chapter3_liquid_fuel_intro(report),
        "chapter3_electricity_intro": _build_chapter3_electricity_intro(report),
        "chapter3_heat_intro": _build_chapter3_heat_intro(report),
    }
    if llm_enabled():
        drafted_ch3 = draft_chapter3_activity_intro_segments(report)
        if _valid_chapter3_activity_intro_segments(drafted_ch3):
            chapter3_intro_segments = {
                "chapter3_natural_gas_intro": str(drafted_ch3["natural_gas"]).strip(),
                "chapter3_liquid_fuel_intro": str(drafted_ch3["liquid_fuel"]).strip(),
                "chapter3_electricity_intro": str(drafted_ch3["electricity"]).strip(),
                "chapter3_heat_intro": str(drafted_ch3["heat"]).strip(),
            }
            if _heat_segment_conflicts_with_activity(report, chapter3_intro_segments["chapter3_heat_intro"]):
                append_audit_event(
                    "llm_heat_site_conflict",
                    {
                        "section": "chapter3_heat_intro",
                        "conflicting_sites": _conflicting_heat_sites_in_text(
                            report,
                            chapter3_intro_segments["chapter3_heat_intro"],
                        ),
                        "authoritative_heat_sites": _heat_activity_sites(report),
                    },
                )
                chapter3_intro_segments["chapter3_heat_intro"] = _build_chapter3_heat_intro(report)
    sections = {
        "chapter1_basic": _build_chapter1_basic(report),
        "chapter1_process": _build_chapter1_process(report),
        "chapter1_equipment_intro": _build_chapter1_equipment_intro(report),
        "chapter1_workload_intro": _build_chapter1_workload_intro(report),
        "chapter2_boundary": _build_chapter2_boundary(report, factors),
        "chapter2_emission_source_intro": _build_chapter2_emission_source_intro(report),
        "chapter3_activity": _build_chapter3_activity(report, calculations),
        "chapter3_fuel_intro": _build_chapter3_fuel_intro(report),
        **chapter3_intro_segments,
        "chapter4_factors": _build_chapter4_factors(report, calculations, factors),
        "chapter5_fuel_intro": _build_chapter5_fuel_intro(report),
        "chapter5_power_heat_intro": _build_chapter5_power_heat_intro(report),
        "chapter5_summary_intro": _build_chapter5_summary_intro(report),
        "chapter5_calculation": _build_chapter5_calculation(calculations),
        "chapter6_conclusion_before_chart": _build_chapter6_conclusion_before_chart(report, calculations),
        "chapter6_conclusion_after_chart": _build_chapter6_conclusion_after_chart(report, calculations),
        "chapter6_conclusion": _build_chapter6_conclusion(report, calculations),
        "chapter7_monitoring_intro": _build_chapter7_monitoring_intro(report),
        "chapter7_monitoring": _build_chapter7_monitoring(report),
    }
    if llm_enabled():
        drafted_ch6 = draft_chapter6_segments_with_llm(report, calculations, factors)
        if drafted_ch6 is not None:
            sections["chapter6_conclusion_before_chart"] = drafted_ch6["before_chart"]
            sections["chapter6_conclusion_after_chart"] = drafted_ch6["after_chart"]
            sections["chapter6_conclusion"] = drafted_ch6["before_chart"] + drafted_ch6["after_chart"]
    return sections
