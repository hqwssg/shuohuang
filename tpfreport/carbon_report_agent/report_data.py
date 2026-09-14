from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from pathlib import Path
from typing import Literal, Self
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

from pydantic import BaseModel, model_validator

from carbon_report_agent.models import ReportInput

WORKSPACE_ROOT = Path(__file__).resolve().parent.parent
PACKAGE_DIR = Path(__file__).resolve().parent
FIXTURE_PATH = WORKSPACE_ROOT / "tests" / "fixtures" / "normalized_suning_2024.json"
SUNING_ASSETS_DIR = PACKAGE_DIR / "assets" / "suning"

_ASSET_FILES = {
    "cover_logo": "cover_logo.png",
    "org_chart": "org_chart.png",
    "office_location": "office_location.jpeg",
}
_MIME_BY_SUFFIX = {
    ".png": "image/png",
    ".jpeg": "image/jpeg",
    ".jpg": "image/jpeg",
}
_TEMPLATE_PLACEHOLDERS = ("{entity}", "{year}", "{grain}", "{month}")
_DEFAULT_MONITORING_NOTE = (
    "\u6c7d\u6cb9\u548c\u67f4\u6cb9\u5747\u901a\u8fc7\u793e\u4f1a\u52a0\u6cb9\u7ad9\u52a0\u6cb9\uff1b"
    "\u70ed\u529b\u6309\u7167\u4f9b\u6696\u9762\u79ef\u548c\u7ed3\u7b97\u6570\u636e\u3002"
)
_DEFAULT_AI_PROSE_CONTEXT = {
    "emission_boundary_notes": {
        "fuel_boundary_exclusion": (
            "\u7ebf\u8def\u8fd0\u8f93\u673a\u8f66\u5f52\u5c5e\u673a\u8f86\u5206\u516c\u53f8\uff0c"
            "\u4e0d\u5728\u8083\u5b81\u5206\u516c\u53f8\u8fd0\u8425\u8fb9\u754c\u5185\uff0c"
            "\u4e0d\u8ba1\u5165\u8083\u5b81\u5206\u516c\u53f8\u78b3\u6392\u653e\u6838\u7b97\u8fb9\u754c\u3002"
        ),
    },
    "activity_prose": {
        "natural_gas": {
            "usage": "\u5929\u7136\u6c14\u7528\u4e8e\u98df\u5802\u7076\u5177",
            "metering_method": "\u7531\u6d41\u91cf\u8ba1\u8fde\u7eed\u8ba1\u91cf",
            "source_documents": ["\u6cbf\u7ebf\u5404\u7ad9\u98df\u5802\u5929\u7136\u6c14\u7edf\u8ba1\u8868"],
            "unit_note": "\u5355\u4f4d\u4e3am3",
            "standard_condition_note": (
                "\u7531\u4e8e\u672a\u8ba1\u91cf\u6e29\u5ea6\u548c\u538b\u529b\u7b49\u53c2\u6570\uff0c"
                "\u5929\u7136\u6c14\u4f7f\u7528\u6761\u4ef6\u63a5\u8fd1\u6807\u51b5\uff0c"
                "\u56e0\u6b64\u6309\u7167\u6807\u51b5\u8ba1\u7b97"
            ),
            "change_note": "2024\u5e747\u6708\u8d77\uff0c\u5168\u90e8\u66f4\u6362\u4e3a\u7535\u708a\u5177\uff0c\u4e0d\u518d\u4f7f\u7528\u5929\u7136\u6c14",
        },
        "liquid_fuel": {
            "usage": "\u516c\u52a1\u8f66\u4f7f\u7528\u6c7d\u6cb9\uff0c\u751f\u4ea7\u8f66\u8f86\u548c\u8f68\u9053\u4f5c\u4e1a\u8f66\u4f7f\u7528\u67f4\u6cb9",
            "metering_method": "\u7531\u52a0\u6cb9\u7ad9\u6216\u52a0\u6cb9\u8f66\u7684\u6d41\u91cf\u8ba1\u6bcf\u6b21\u8ba1\u91cf",
            "source_documents": [
                "\u6c7d\u8f66\u6c7d\u6cb9\u548c\u67f4\u6cb9\u6d88\u8017\u7edf\u8ba1\u8868",
                "\u8f68\u9053\u8f66\u7528\u67f4\u6cb91-12\u6708\u6c47\u603b",
            ],
        },
        "electricity": {
            "non_fossil_power_note": "\u4e0d\u5b58\u5728\u4f7f\u7528\u975e\u5316\u77f3\u80fd\u6e90\u7535\u91cf\u7684\u60c5\u51b5",
            "grid_source": "\u4ece\u534e\u5317\u7535\u7f51\u8d2d\u5165\u7535\u529b",
            "source_documents": ["2024\u5e74\u7535\u8d39\u62a5\u8868-\u5185\u8f6c\u8868"],
            "settlement_note": (
                "\u7531\u4e8e\u6714\u9ec4\u516c\u53f8\u53ca\u5b50\u5206\u516c\u53f8\u5916\u8d2d\u7535\u91cf\u5168\u90e8"
                "\u7531\u8083\u5b81\u5206\u516c\u53f8\u7ed3\u7b97\uff0c\u7ed3\u7b97\u53d1\u7968\u4e3a\u6240\u6709\u4f01\u4e1a"
                "\u7684\u5408\u8ba1\u5916\u8d2d\u7535\u91cf\uff0c\u65e0\u6cd5\u63d0\u4f9b\u8083\u5206\u516c\u53f8"
                "\u5355\u72ec\u7684\u7ed3\u7b97\u53d1\u7968"
            ),
        },
        "heat": {
            "contract_note": "\u548c\u5f53\u5730\u70ed\u529b\u516c\u53f8\u7b7e\u8ba2\u534f\u8bae",
            "payment_method": "\u6309\u9762\u79ef\u6536\u8d39\u6216\u6309\u70ed\u91cf\u6536\u8d39\u65b9\u5f0f\u7f34\u7eb3\u70ed\u8d39",
            "estimation_method": "\u65e0\u8ba1\u91cf\u70ed\u91cf\u65f6\u6309\u7167\u4f9b\u6696\u9762\u79ef\u6298\u7b97\u70ed\u529b\u8fdb\u884c\u4f30\u7b97",
            "no_resale_note": "\u6ca1\u6709\u8f6c\u4f9b\u70ed\u529b\u7684\u60c5\u51b5",
            "source_documents": ["\u70ed\u529b\u7ed3\u7b97\u5355"],
        },
    },
    "chapter6": {
        "emission_intensity": {
            "applicable": False,
            "not_applicable_note": "\u6e29\u5ba4\u6c14\u4f53\u6392\u653e\u5f3a\u5ea6\u4e0d\u6d89\u53ca\u3002",
        }
    },
}


class ReportSelectors(BaseModel):
    entity: str
    year: int
    grain: Literal["year", "month"] = "year"
    month: int | None = None

    @model_validator(mode="after")
    def _require_month_when_grain_month(self) -> Self:
        if self.grain == "month" and self.month is None:
            raise ValueError("month is required when grain is 'month'")
        return self


def list_entities() -> list[dict]:
    fixture = load_fixture_report()
    entity_name = fixture["metadata"]["entity_name"]
    return [{"code": "suning", "name": entity_name}]


def _is_placeholder_image(item: dict) -> bool:
    url = (item.get("url") or "").strip()
    return not bool(url)


def _asset_url(role: str) -> str | None:
    filename = _ASSET_FILES.get(role)
    if not filename:
        return None
    return f"/assets/suning/{filename}"


def _load_asset_image(role: str) -> dict | None:
    filename = _ASSET_FILES.get(role)
    url = _asset_url(role)
    if not filename or not url:
        return None
    path = SUNING_ASSETS_DIR / filename
    if not path.is_file():
        return None
    return {
        "role": role,
        "caption": "",
        "url": url,
        "mime_type": _MIME_BY_SUFFIX.get(path.suffix.lower(), "application/octet-stream"),
    }


def enrich_images_from_assets(payload: dict, *, prefer_asset_urls: bool = False) -> dict:
    """Fill missing/placeholder Suning image roles from package assets."""
    images = list(payload.get("images") or [])
    by_role = {item.get("role"): item for item in images if isinstance(item, dict)}
    changed = False
    for role in _ASSET_FILES:
        current = by_role.get(role)
        should_replace = current is None or _is_placeholder_image(current)
        if (
            prefer_asset_urls
            and current is not None
            and not (current.get("url") or "").strip()
        ):
            should_replace = True
        if not should_replace:
            continue
        asset = _load_asset_image(role)
        if asset is None:
            continue
        if current is not None:
            asset["caption"] = current.get("caption") or asset["caption"]
            by_role[role] = asset
        else:
            by_role[role] = asset
        changed = True
    if not changed:
        return payload
    order = {role: index for index, role in enumerate(_ASSET_FILES)}
    payload = dict(payload)
    payload["images"] = sorted(by_role.values(), key=lambda item: order.get(item.get("role"), 99))
    return payload


def enrich_default_prose_fields(payload: dict) -> dict:
    payload = dict(payload)
    profile = dict(payload.get("entity_profile") or {})
    if not (profile.get("monitoring_note") or "").strip():
        profile["monitoring_note"] = _DEFAULT_MONITORING_NOTE
    payload["entity_profile"] = profile
    payload = _deep_fill_missing(payload, _DEFAULT_AI_PROSE_CONTEXT)
    return payload


def _deep_fill_missing(payload: dict, defaults: dict) -> dict:
    result = dict(payload)
    for key, value in defaults.items():
        current = result.get(key)
        if isinstance(value, dict):
            if not isinstance(current, dict):
                current = {}
            result[key] = _deep_fill_missing(current, value)
        elif current in (None, "", []):
            result[key] = value
    return result


def _compact_json(value):
    if isinstance(value, dict):
        result = {}
        for key, item in value.items():
            compacted = _compact_json(item)
            if compacted in ("", None, [], {}):
                continue
            result[key] = compacted
        return result
    if isinstance(value, list):
        return [
            compacted
            for item in value
            if (compacted := _compact_json(item)) not in ("", None, [], {})
        ]
    return value


def load_fixture_report() -> dict:
    payload = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))
    payload = enrich_default_prose_fields(payload)
    return enrich_images_from_assets(payload, prefer_asset_urls=True)


def load_fixture_dict() -> dict:
    """Alias used by mock-backend and tests."""
    return load_fixture_report()


def fetch_upstream_report(url: str, timeout_seconds: float = 15.0) -> dict:
    request = urllib.request.Request(url, method="GET", headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
        payload = json.loads(response.read().decode("utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("upstream report-input must be a JSON object")
    return payload


def _resolve_upstream_url(base_url: str, selectors: ReportSelectors) -> str:
    if any(placeholder in base_url for placeholder in _TEMPLATE_PLACEHOLDERS):
        return base_url.format(
            entity=selectors.entity,
            year=selectors.year,
            grain=selectors.grain,
            month="" if selectors.month is None else selectors.month,
        )

    params = {
        "entity": selectors.entity,
        "year": selectors.year,
        "grain": selectors.grain,
    }
    if selectors.month is not None:
        params["month"] = selectors.month

    parsed = urlparse(base_url)
    existing = dict(parse_qsl(parsed.query, keep_blank_values=True))
    existing.update({key: str(value) for key, value in params.items()})
    query = urlencode(existing)
    return urlunparse(parsed._replace(query=query))


def _load_local_fixture_for_selectors(selectors: ReportSelectors) -> dict:
    if selectors.entity == "suning" and selectors.year == 2024:
        return load_fixture_report()
    raise ValueError(
        f"no local fixture for entity={selectors.entity!r} year={selectors.year} "
        f"(phase 1 supports entity='suning', year=2024 only)"
    )


def load_report_input(selectors: ReportSelectors | None = None) -> dict:
    """
    Load report input from upstream backend when CARBON_REPORT_INPUT_URL is set;
    otherwise return the fixed local fixture (simulated backend data).

    When selectors is None, upstream URL is used as-is (legacy behavior).
    When selectors is provided, upstream URL is formatted or receives query params.
    """
    upstream = os.environ.get("CARBON_REPORT_INPUT_URL", "").strip()
    if upstream:
        url = _resolve_upstream_url(upstream, selectors) if selectors is not None else upstream
        try:
            payload = fetch_upstream_report(url)
        except (urllib.error.URLError, TimeoutError, ValueError, json.JSONDecodeError) as exc:
            raise RuntimeError(f"failed to load report input from upstream: {exc}") from exc
    elif selectors is not None:
        payload = _load_local_fixture_for_selectors(selectors)
    else:
        payload = load_fixture_report()
    payload = enrich_images_from_assets(payload)
    # Validate shape early so API and generate share the same contract.
    report = ReportInput.model_validate(payload)
    return _compact_json(report.model_dump(mode="json"))
