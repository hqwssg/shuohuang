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
    return ReportBlueprint.model_validate_json(p.read_text(encoding="utf-8"))
