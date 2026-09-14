from __future__ import annotations

from pathlib import Path
from typing import Literal

from pydantic import BaseModel, Field

BlueprintBlockKind = Literal[
    "heading",
    "paragraph_key",
    "table_role",
    "image_slot",
    "chart_slot",
    "fixed_text",
    "cover_meta",
    "cover_chrome",
    "toc_field",
    "page_break",
    "section_break",
]


class BlueprintBlock(BaseModel):
    kind: BlueprintBlockKind
    level: int | None = None
    text: str | None = None
    key: str | None = None
    role: str | None = None
    caption: str | None = None
    page_number_format: str | None = None
    page_number_start: int | None = None
    section_orientation: Literal["portrait", "landscape"] | None = None


class ReportBlueprint(BaseModel):
    version: str
    wording_snippets: dict[str, str] = Field(default_factory=dict)
    blocks: list[BlueprintBlock]


DEFAULT_BLUEPRINT_PATH = (
    Path(__file__).resolve().parent.parent / "tests" / "fixtures" / "blueprint_suning_ghg_v1.json"
)


def load_blueprint(path: str | Path | None = None) -> ReportBlueprint:
    p = Path(path) if path else DEFAULT_BLUEPRINT_PATH
    if not p.exists():
        return _bundled_blueprint()
    return ReportBlueprint.model_validate_json(p.read_text(encoding="utf-8"))


def _bundled_blueprint() -> ReportBlueprint:
    """Complete fallback used by the deployable package when test fixtures are absent."""
    def block(kind: BlueprintBlockKind, **kwargs) -> BlueprintBlock:
        return BlueprintBlock(kind=kind, **kwargs)

    blocks = [
        block("cover_chrome"),
        block("fixed_text", key="cover_line1"),
        block("fixed_text", key="cover_line2"),
        block("cover_meta"),
        block("page_break"),
        block("fixed_text", key="preface.opening_guide"),
        block("table_role", role="approval"),
        block("page_break"),
        block("fixed_text", key="toc_title", text="目录"),
        block("toc_field"),
        block("section_break", page_number_format="decimal", page_number_start=1),

        block("heading", level=1, text="第一章 报告主体基本情况"),
        block("heading", level=2, text="1.1 基本信息"),
        block("paragraph_key", key="chapter1_basic"),
        block("image_slot", role="office_location", caption="公司机关所在地"),
        block("heading", level=2, text="1.2 工艺信息"),
        block("paragraph_key", key="chapter1_process"),
        block("image_slot", role="org_chart", caption="组织机构图"),
        block("heading", level=2, text="1.3 设备信息"),
        block("paragraph_key", key="chapter1_equipment_intro"),
        block("table_role", role="equipment"),
        block("heading", level=2, text="1.4 工作量信息"),
        block("paragraph_key", key="chapter1_workload_intro"),
        block("table_role", role="workload_monthly"),

        block("heading", level=1, text="第二章 温室气体排放情况"),
        block("heading", level=2, text="2.1 排放边界和排放源识别"),
        block("heading", level=3, text="2.1.1 排放边界识别"),
        block("fixed_text", key="ch2.guide_boundary"),
        block("paragraph_key", key="chapter2_boundary"),
        block("heading", level=3, text="2.1.2 排放源识别"),
        block("paragraph_key", key="chapter2_emission_source_intro"),
        block("table_role", role="emission_sources"),
        block("heading", level=2, text="2.2 核算方法"),
        block("fixed_text", key="ch2.method_total"),
        block("heading", level=3, text="2.2.1 燃料燃烧CO2排放"),
        block("fixed_text", key="ch2.method_fuel"),
        block("heading", level=3, text="2.2.2 净购入电力隐含的CO2排放"),
        block("fixed_text", key="ch2.method_electricity"),
        block("heading", level=3, text="2.2.3 净购入热力隐含的CO2排放"),
        block("fixed_text", key="ch2.method_heat"),

        block("heading", level=1, text="第三章 活动水平数据及来源说明"),
        block("paragraph_key", key="chapter3_activity"),
        block("heading", level=2, text="3.1 燃料燃烧CO2排放的活动水平数据"),
        block("paragraph_key", key="chapter3_fuel_intro"),
        block("heading", level=3, text="3.1.1 天然气消耗量"),
        block("paragraph_key", key="chapter3_natural_gas_intro"),
        block("table_role", role="activity_natural_gas"),
        block("heading", level=3, text="3.1.2 汽油、柴油消耗量"),
        block("paragraph_key", key="chapter3_liquid_fuel_intro"),
        block("table_role", role="activity_fuel"),
        block("heading", level=2, text="3.2 净购入电力活动水平数据"),
        block("paragraph_key", key="chapter3_electricity_intro"),
        block("table_role", role="activity_electricity"),
        block("heading", level=2, text="3.3 净购入热力活动水平数据"),
        block("paragraph_key", key="chapter3_heat_intro"),
        block("table_role", role="activity_heat"),

        block("heading", level=1, text="第四章 排放因子数据及来源说明"),
        block("heading", level=2, text="4.1 燃料燃烧CO2排放的排放因子"),
        block("heading", level=3, text="4.1.1 天然气的含碳量"),
        block("heading", level=3, text="4.1.2 汽油的含碳量"),
        block("heading", level=3, text="4.1.3 柴油的含碳量"),
        block("heading", level=3, text="4.1.4 天然气的碳氧化率"),
        block("heading", level=3, text="4.1.5 汽油的碳氧化率"),
        block("heading", level=3, text="4.1.6 柴油的碳氧化率"),
        block("heading", level=2, text="4.2 电力和热力排放因子"),
        block("heading", level=3, text="4.2.1 电网排放因子"),
        block("heading", level=3, text="4.2.2 热力供应排放因子"),
        block("table_role", role="emission_factors"),

        block("heading", level=1, text="第五章 温室气体排放量的计算"),
        block("heading", level=2, text="5.1 燃料燃烧CO2排放"),
        block("paragraph_key", key="chapter5_fuel_intro"),
        block("table_role", role="calc_fuel_detail"),
        block("heading", level=2, text="5.2 净购入的电力和热力隐含的CO2排放"),
        block("paragraph_key", key="chapter5_power_heat_intro"),
        block("table_role", role="calc_power_heat_detail"),
        block("heading", level=2, text="5.3 温室气体排放汇总表"),
        block("paragraph_key", key="chapter5_summary_intro"),
        block("table_role", role="emission_summary"),

        block("heading", level=1, text="第六章 结论"),
        block("paragraph_key", key="chapter6_conclusion_before_chart"),
        block("chart_slot", role="emissions_distribution", caption="温室气体排放构成"),
        block("paragraph_key", key="chapter6_conclusion_after_chart"),

        block("heading", level=1, text="第七章 监测设备信息"),
        block("paragraph_key", key="chapter7_monitoring_intro"),
        block("table_role", role="monitoring"),

        block("page_break"),
        block("heading", level=1, text="附表 1  企业温室气体排放汇总表"),
        block("table_role", role="appendix_emission_summary"),
        block("heading", level=1, text="附表 2  化石燃料燃烧二氧化碳排放量数据表"),
        block("table_role", role="appendix_fuel"),
        block("heading", level=1, text="附表 3  净购入电力隐含的二氧化碳排放量数据表"),
        block("table_role", role="appendix_electricity"),
        block("heading", level=1, text="附表 4  净购入热力隐含的二氧化碳排放量数据表"),
        block("table_role", role="appendix_heat"),
    ]
    return ReportBlueprint(
        version="shuohuang-ghg-bundled-v1",
        wording_snippets={
            "cover_line1": "陆上交通运输企业",
            "cover_line2": "温室气体排放报告",
            "toc_title": "目录",
        },
        blocks=blocks,
    )
