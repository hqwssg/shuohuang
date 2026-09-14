from __future__ import annotations

import json
import logging
import os
import re
import urllib.error
import urllib.request
import warnings
from contextlib import contextmanager
from contextvars import ContextVar

from carbon_report_agent.calculations import CalculationResult
from carbon_report_agent.factors import FactorLibrary
from carbon_report_agent.models import ReportInput

DEFAULT_LLM_BASE_URL = "https://api.deepseek.com"
DEFAULT_LLM_MODEL = "deepseek-reasoner"
DEFAULT_LLM_TIMEOUT_S = 120
DEFAULT_LLM_TEMPERATURE = 0
LOGGER = logging.getLogger("carbon_report_agent.llm")
_LLM_LOGGER_CONFIGURED = False
_LLM_OVERRIDE: ContextVar[bool | None] = ContextVar("carbon_report_llm_override", default=None)


def llm_enabled() -> bool:
    override = _LLM_OVERRIDE.get()
    if override is not None:
        return override
    flag = os.environ.get("CARBON_REPORT_LLM_ENABLED", "").strip().lower()
    return flag in {"1", "true", "yes", "on"}


@contextmanager
def llm_enabled_override(enabled: bool | None):
    token = _LLM_OVERRIDE.set(enabled)
    try:
        yield
    finally:
        _LLM_OVERRIDE.reset(token)


def llm_debug_enabled() -> bool:
    flag = os.environ.get("CARBON_REPORT_LLM_DEBUG", "").strip().lower()
    return flag in {"1", "true", "yes", "on"}


def default_llm_base_url() -> str:
    return os.environ.get("CARBON_REPORT_LLM_BASE_URL", DEFAULT_LLM_BASE_URL).rstrip("/")


def default_llm_model() -> str:
    return os.environ.get("CARBON_REPORT_LLM_MODEL", DEFAULT_LLM_MODEL)


def _chat_completions_url(base_url: str | None = None) -> str:
    base = (base_url or default_llm_base_url()).rstrip("/")
    if base.endswith("/chat/completions"):
        return base
    if base.endswith("/v1"):
        return f"{base}/chat/completions"
    return f"{base}/v1/chat/completions"


def _extract_json_object(text: str) -> dict:
    text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
    return json.loads(text)


def _message_final_content(message: dict) -> str:
    """Use final answer content only; ignore reasoning fields if present."""
    content = message.get("content")
    if content is None:
        return ""
    return str(content).strip()


def _log_llm_debug(event: str, **fields) -> None:
    if not llm_debug_enabled():
        return
    global _LLM_LOGGER_CONFIGURED
    if not _LLM_LOGGER_CONFIGURED:
        LOGGER.setLevel(logging.INFO)
        if not logging.getLogger().handlers and not LOGGER.handlers:
            handler = logging.StreamHandler()
            handler.setFormatter(logging.Formatter("%(levelname)s:%(name)s:%(message)s"))
            LOGGER.addHandler(handler)
        _LLM_LOGGER_CONFIGURED = True
    safe_fields = {
        key: value
        for key, value in fields.items()
        if key.lower() not in {"api_key", "authorization"}
    }
    details = " ".join(f"{key}={value!r}" for key, value in safe_fields.items())
    LOGGER.info("%s %s", event, details)


def _unique_nonempty(values: list[str]) -> list[str]:
    seen = set()
    result = []
    for value in values:
        text = (value or "").strip()
        if text and text not in seen:
            seen.add(text)
            result.append(text)
    return result


def _derived_report_facts(report_dump: dict) -> dict:
    return {
        "heat_sites": _unique_nonempty(
            [
                str(item.get("site") or "")
                for item in report_dump.get("heat_activity", [])
                if isinstance(item, dict)
            ]
        ),
        "heat_sites_source": "heat_activity[].site",
    }


def draft_chapter6_segments_with_llm(
    report: ReportInput,
    calculations: CalculationResult,
    factors: FactorLibrary,
) -> dict[str, str] | None:
    api_key = os.environ.get("CARBON_REPORT_LLM_API_KEY") or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        return None

    base_url = default_llm_base_url()
    model = default_llm_model()
    total = f"{calculations.total_emissions_tco2:.2f}"

    report_dump = report.model_dump(mode="json")
    report_dump["images"] = []
    fact_pack = {
        "report": report_dump,
        "derived": _derived_report_facts(report_dump),
        "calculations": calculations.model_dump(mode="json"),
        "factor_version": factors.version,
        "electricity_factor_tco2_per_mwh": factors.electricity_factor_tco2_per_mwh,
        "heat_factor_tco2_per_gj": factors.heat_factor_tco2_per_gj,
        "fuel_factors": {key: value.model_dump() for key, value in factors.fuel_factors.items()},
    }
    system = (
        "You draft Chinese greenhouse-gas report chapter 6 prose around a distribution chart. "
        "Return JSON only with keys before_chart and after_chart. "
        "Use ONLY facts from the JSON fact pack. Never invent numbers. "
        "Write in the style of the 2024 sample reports, with concise formal Chinese prose. "
        "Use tCO2 as the emissions unit; avoid phrases like tonnes of carbon-dioxide equivalent. "
        "The complete chapter-6 prose should normally be 180-350 Chinese characters; it may be longer "
        "when emission-intensity or multi-year trend facts are supplied. "
        "Copy the emission total exactly from calculations.total_emissions_tco2. "
        "before_chart must contain at least two paragraphs separated by a newline: "
        "paragraph 1 states the current total, prior-year comparison when supplied, and the year-on-year "
        "percentage change when it can be calculated from supplied facts; "
        "paragraph 2 analyzes change reasons, preferring supplied fuel/electricity/heat prior-year "
        "category data when available, and otherwise using only the available total-level comparison. "
        "The final before_chart paragraph must end with \u6e29\u5ba4\u6c14\u4f53\u6392\u653e\u91cf\u4e2d\u7684\u5206\u5e03\u60c5\u51b5\u89c1\u4e0b\u56fe\uff1a. "
        "after_chart must contain at least two paragraphs separated by a newline: "
        "paragraph 1 must start like \u4e0a\u56fe\u53ef\u4ee5\u770b\u51fa and analyze emission shares from largest to smallest; "
        "paragraph 2 must address emission intensity. If report.chapter6.emission_intensity.not_applicable_note "
        "is supplied, you must copy it verbatim. If emission-intensity values are supplied, write the comparison "
        "and cause analysis from those facts. "
        "Do not invent freight turnover, emission intensity, category year-on-year changes, trend years, "
        "source categories, or causes that are not present in the fact pack. "
        "Do not include headings, chart captions, markdown, or tables."
    )
    body = {
        "model": model,
        "temperature": DEFAULT_LLM_TEMPERATURE,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": json.dumps(fact_pack, ensure_ascii=True)},
        ],
    }
    url = _chat_completions_url(base_url)
    _log_llm_debug(
        "llm_call_start",
        section="chapter6_conclusion",
        url=url,
        model=model,
        fact_keys=sorted(fact_pack.keys()),
        request_chars=len(json.dumps(body, ensure_ascii=True)),
    )
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=DEFAULT_LLM_TIMEOUT_S) as response:
            raw = response.read().decode("utf-8")
            _log_llm_debug(
                "llm_call_response",
                section="chapter6_conclusion",
                status=getattr(response, "status", None),
                response_chars=len(raw),
            )
            payload = json.loads(raw)
        message = payload["choices"][0]["message"]
        content = _message_final_content(message)
        if not content:
            _log_llm_debug(
                "llm_call_fallback",
                section="chapter6_conclusion",
                reason="empty_content",
            )
            return None
        parsed = _extract_json_object(content)
    except (
        urllib.error.URLError,
        TimeoutError,
        OSError,
        KeyError,
        IndexError,
        TypeError,
        json.JSONDecodeError,
        ValueError,
    ) as exc:
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter6_conclusion",
            reason=type(exc).__name__,
            error=str(exc)[:300],
        )
        return None

    if not isinstance(parsed, dict):
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter6_conclusion",
            reason="non_dict_response",
        )
        return None
    segments = {
        key: str(parsed.get(key, "")).strip()
        for key in ("before_chart", "after_chart")
    }
    if not all(segments.values()):
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter6_conclusion",
            reason="missing_segment",
            segment_keys=[key for key, value in segments.items() if bool(value)],
        )
        return None

    content = segments["before_chart"] + segments["after_chart"]
    if total not in content:
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter6_conclusion",
            reason="missing_required_total",
            required_total=total,
            content_chars=len(content),
        )
        return None
    _log_llm_debug(
        "llm_call_success",
        section="chapter6_conclusion",
        segment_keys=sorted(segments.keys()),
        content_chars=len(content),
    )
    return segments


def draft_chapter6_with_llm(
    report: ReportInput,
    calculations: CalculationResult,
    factors: FactorLibrary,
) -> str | None:
    api_key = os.environ.get("CARBON_REPORT_LLM_API_KEY") or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        return None

    base_url = default_llm_base_url()
    model = default_llm_model()
    total = f"{calculations.total_emissions_tco2:.2f}"

    report_dump = report.model_dump(mode="json")
    # Strip images so LLM requests stay small and image URLs are not interpreted as facts.
    report_dump["images"] = []
    fact_pack = {
        "report": report_dump,
        "derived": _derived_report_facts(report_dump),
        "calculations": calculations.model_dump(mode="json"),
        "factor_version": factors.version,
        "electricity_factor_tco2_per_mwh": factors.electricity_factor_tco2_per_mwh,
        "heat_factor_tco2_per_gj": factors.heat_factor_tco2_per_gj,
        "fuel_factors": {k: v.model_dump() for k, v in factors.fuel_factors.items()},
    }
    system = (
        "You draft Chinese greenhouse-gas report chapter 6 (结论与建议) prose only. "
        "Use ONLY facts from the JSON fact pack. Never invent numbers. "
        "Prefer report.chapter6.emission_intensity and report.chapter6.trend_years for "
        "emission-intensity and trend statements when they are supplied. "
        "Copy the emission total exactly from calculations.total_emissions_tco2. "
        "Return plain prose for chapter 6 only (not JSON for other chapters)."
    )
    body = {
        "model": model,
        "temperature": DEFAULT_LLM_TEMPERATURE,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": json.dumps(fact_pack, ensure_ascii=True)},
        ],
    }
    url = _chat_completions_url(base_url)
    _log_llm_debug(
        "llm_call_start",
        section="chapter6_conclusion",
        url=url,
        model=model,
        fact_keys=sorted(fact_pack.keys()),
        request_chars=len(json.dumps(body, ensure_ascii=True)),
    )
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=DEFAULT_LLM_TIMEOUT_S) as response:
            raw = response.read().decode("utf-8")
            _log_llm_debug(
                "llm_call_response",
                section="chapter6_conclusion",
                status=getattr(response, "status", None),
                response_chars=len(raw),
            )
            payload = json.loads(raw)
        message = payload["choices"][0]["message"]
        content = _message_final_content(message)
        if not content:
            _log_llm_debug(
                "llm_call_fallback",
                section="chapter6_conclusion",
                reason="empty_content",
            )
            return None
        # Allow optional JSON wrapper {"chapter6_conclusion": "..."}
        try:
            parsed = _extract_json_object(content)
            if isinstance(parsed, dict) and "chapter6_conclusion" in parsed:
                content = str(parsed["chapter6_conclusion"]).strip()
        except (json.JSONDecodeError, ValueError):
            pass
    except (
        urllib.error.URLError,
        TimeoutError,
        OSError,
        KeyError,
        IndexError,
        TypeError,
        json.JSONDecodeError,
        ValueError,
    ) as exc:
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter6_conclusion",
            reason=type(exc).__name__,
            error=str(exc)[:300],
        )
        return None

    if not content or total not in content:
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter6_conclusion",
            reason="missing_required_total",
            required_total=total,
            content_chars=len(content or ""),
        )
        return None
    _log_llm_debug(
        "llm_call_success",
        section="chapter6_conclusion",
        content_chars=len(content),
    )
    return content


def draft_emission_source_intro_segments(report: ReportInput) -> dict[str, str] | None:
    api_key = os.environ.get("CARBON_REPORT_LLM_API_KEY") or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        return None

    base_url = default_llm_base_url()
    model = default_llm_model()
    report_dump = report.model_dump(mode="json")
    report_dump["images"] = []
    fact_pack = {
        "metadata": report_dump["metadata"],
        "entity_profile": report_dump["entity_profile"],
        "derived": _derived_report_facts(report_dump),
        "organization_boundary": report_dump.get("organization_boundary"),
        "emission_boundary_notes": report_dump.get("emission_boundary_notes", {}),
        "activity_prose": report_dump.get("activity_prose", {}),
        "fuel_activity": report_dump.get("fuel_activity", []),
        "electricity_activity": report_dump.get("electricity_activity", []),
        "heat_activity": report_dump.get("heat_activity", []),
    }
    system = (
        "You draft three Chinese prose segments for section 2.1.2 排放源识别. "
        "Return JSON only with keys fuel, electricity, heat. "
        "Use ONLY the supplied JSON facts. Do not invent facilities, sites, fuels, or sources. "
        "For heat station/site names, derived.heat_sites and heat_activity[].site are authoritative. "
        "Use emission_boundary_notes for boundary exclusions and category-specific notes. "
        "Use activity_prose only as supporting factual context, not as a source for new categories. "
        "Do not include the fixed numbering headings. Keep each value one sentence."
    )
    body = {
        "model": model,
        "temperature": DEFAULT_LLM_TEMPERATURE,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": json.dumps(fact_pack, ensure_ascii=True)},
        ],
    }
    url = _chat_completions_url(base_url)
    _log_llm_debug(
        "llm_call_start",
        section="chapter2_emission_source_intro",
        url=url,
        model=model,
        fact_keys=sorted(fact_pack.keys()),
        request_chars=len(json.dumps(body, ensure_ascii=True)),
    )
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=DEFAULT_LLM_TIMEOUT_S) as response:
            raw = response.read().decode("utf-8")
            _log_llm_debug(
                "llm_call_response",
                section="chapter2_emission_source_intro",
                status=getattr(response, "status", None),
                response_chars=len(raw),
            )
            payload = json.loads(raw)
        message = payload["choices"][0]["message"]
        content = _message_final_content(message)
        parsed = _extract_json_object(content)
    except (
        urllib.error.URLError,
        TimeoutError,
        OSError,
        KeyError,
        IndexError,
        TypeError,
        json.JSONDecodeError,
        ValueError,
    ) as exc:
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter2_emission_source_intro",
            reason=type(exc).__name__,
            error=str(exc)[:300],
        )
        return None

    if not isinstance(parsed, dict):
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter2_emission_source_intro",
            reason="non_dict_response",
        )
        return None
    segments = {
        key: str(parsed.get(key, "")).strip()
        for key in ("fuel", "electricity", "heat")
    }
    if not all(segments.values()):
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter2_emission_source_intro",
            reason="missing_segment",
            segment_keys=[key for key, value in segments.items() if bool(value)],
        )
        return None
    _log_llm_debug(
        "llm_call_success",
        section="chapter2_emission_source_intro",
        segment_keys=sorted(segments.keys()),
        content_chars=sum(len(value) for value in segments.values()),
    )
    return segments


def draft_chapter3_activity_intro_segments(report: ReportInput) -> dict[str, str] | None:
    api_key = os.environ.get("CARBON_REPORT_LLM_API_KEY") or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        return None

    base_url = default_llm_base_url()
    model = default_llm_model()
    report_dump = report.model_dump(mode="json")
    report_dump["images"] = []
    fact_pack = {
        "metadata": report_dump["metadata"],
        "entity_profile": report_dump["entity_profile"],
        "derived": _derived_report_facts(report_dump),
        "activity_prose": report_dump.get("activity_prose", {}),
        "fuel_activity": report_dump.get("fuel_activity", []),
        "electricity_activity": report_dump.get("electricity_activity", []),
        "heat_activity": report_dump.get("heat_activity", []),
    }
    system = (
        "You draft four Chinese prose segments for chapter 3 activity data descriptions. "
        "Return JSON only with keys natural_gas, liquid_fuel, electricity, heat. "
        "Use ONLY the supplied JSON facts. Do not invent facilities, sites, fuels, sources, "
        "meters, calibration reports, or document names. "
        "For heat station/site names, derived.heat_sites and heat_activity[].site are authoritative. "
        "When activity_prose provides usage, metering, source documents, settlement, "
        "non-fossil power, heat-payment, estimate, or no-resale "
        "notes, weave those facts into the matching segment in the style of the 2024 samples. "
        "Do not include headings. Keep each value one paragraph and end with "
        "\u7edf\u8ba1\u7ed3\u679c\u548c\u6570\u636e\u6765\u6e90\u5982\u4e0b\u8868\u6240\u793a\u3002"
    )
    body = {
        "model": model,
        "temperature": DEFAULT_LLM_TEMPERATURE,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": json.dumps(fact_pack, ensure_ascii=True)},
        ],
    }
    url = _chat_completions_url(base_url)
    _log_llm_debug(
        "llm_call_start",
        section="chapter3_activity_intro",
        url=url,
        model=model,
        fact_keys=sorted(fact_pack.keys()),
        request_chars=len(json.dumps(body, ensure_ascii=True)),
    )
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=DEFAULT_LLM_TIMEOUT_S) as response:
            raw = response.read().decode("utf-8")
            _log_llm_debug(
                "llm_call_response",
                section="chapter3_activity_intro",
                status=getattr(response, "status", None),
                response_chars=len(raw),
            )
            payload = json.loads(raw)
        message = payload["choices"][0]["message"]
        content = _message_final_content(message)
        parsed = _extract_json_object(content)
    except (
        urllib.error.URLError,
        TimeoutError,
        OSError,
        KeyError,
        IndexError,
        TypeError,
        json.JSONDecodeError,
        ValueError,
    ) as exc:
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter3_activity_intro",
            reason=type(exc).__name__,
            error=str(exc)[:300],
        )
        return None

    if not isinstance(parsed, dict):
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter3_activity_intro",
            reason="non_dict_response",
        )
        return None
    segments = {
        key: str(parsed.get(key, "")).strip()
        for key in ("natural_gas", "liquid_fuel", "electricity", "heat")
    }
    if not all(segments.values()):
        _log_llm_debug(
            "llm_call_fallback",
            section="chapter3_activity_intro",
            reason="missing_segment",
            segment_keys=[key for key, value in segments.items() if bool(value)],
        )
        return None
    _log_llm_debug(
        "llm_call_success",
        section="chapter3_activity_intro",
        segment_keys=sorted(segments.keys()),
        content_chars=sum(len(value) for value in segments.values()),
    )
    return segments


def draft_narratives_with_llm(
    report: ReportInput,
    calculations: CalculationResult,
    factors: FactorLibrary,
) -> dict[str, str] | None:
    """Deprecated: chapter-6-only. Prefer draft_chapter6_with_llm."""
    warnings.warn(
        "draft_narratives_with_llm is deprecated; use draft_chapter6_with_llm",
        DeprecationWarning,
        stacklevel=2,
    )
    chapter6 = draft_chapter6_with_llm(report, calculations, factors)
    if chapter6 is None:
        return None
    return {"chapter6_conclusion": chapter6}
