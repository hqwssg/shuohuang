from __future__ import annotations

import os
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import unquote, urlparse

from carbon_report_agent.audit import append_audit_event
from carbon_report_agent.image_bytes import detect_image_format, prepare_word_image_bytes

PACKAGE_DIR = Path(__file__).resolve().parent
SUNING_ASSETS_DIR = PACKAGE_DIR / "assets" / "suning"

DEFAULT_TIMEOUT_SECONDS = 15.0
DEFAULT_MAX_BYTES = 5 * 1024 * 1024
_ALLOWED_SCHEMES = {"http", "https"}
_LOCAL_SUNING_ASSET_PREFIX = "/assets/suning/"
_JOB_ASSET_PREFIX = "/job-assets/"
_JOB_ASSETS_ENV = "CARBON_REPORT_JOB_ASSETS_DIR"
_IMAGE_CONTENT_PREFIXES = ("image/",)
_USER_AGENT = "carbon-report-agent/1.0"

_CONTENT_TYPE_TO_FORMAT = {
    "image/png": "png",
    "image/jpeg": "jpeg",
    "image/jpg": "jpeg",
    "image/gif": "gif",
    "image/bmp": "bmp",
    "image/webp": "webp",
}


def _fetch_local_suning_asset(
    url: str,
    *,
    role: str,
    max_bytes: int,
) -> bytes | None:
    parsed = urlparse(url)
    path = unquote(parsed.path)
    if parsed.scheme or parsed.netloc or not path.startswith(_LOCAL_SUNING_ASSET_PREFIX):
        return None

    filename = path.removeprefix(_LOCAL_SUNING_ASSET_PREFIX)
    if not filename or "/" in filename or "\\" in filename:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "invalid_local_asset_path",
            },
        )
        return None

    asset_path = SUNING_ASSETS_DIR / filename
    if not asset_path.is_file():
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "local_asset_not_found",
            },
        )
        return None

    data = asset_path.read_bytes()
    if len(data) > max_bytes:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "too_large",
                "max_bytes": max_bytes,
            },
        )
        return None
    return prepare_word_image_bytes(data, role=role)


def _fetch_job_asset(
    url: str,
    *,
    role: str,
    max_bytes: int,
) -> bytes | None:
    parsed = urlparse(url)
    path = unquote(parsed.path)
    if parsed.scheme or parsed.netloc or not path.startswith(_JOB_ASSET_PREFIX):
        return None
    root = os.environ.get(_JOB_ASSETS_ENV, "").strip()
    if not root:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "job_assets_dir_missing",
            },
        )
        return None
    filename = path.removeprefix(_JOB_ASSET_PREFIX)
    if not filename or "/" in filename or "\\" in filename:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "invalid_job_asset_path",
            },
        )
        return None
    asset_root = Path(root).resolve()
    asset_path = (asset_root / filename).resolve()
    if asset_root not in asset_path.parents and asset_path != asset_root:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "job_asset_outside_root",
            },
        )
        return None
    if not asset_path.is_file():
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "job_asset_not_found",
            },
        )
        return None
    data = asset_path.read_bytes()
    if len(data) > max_bytes:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "too_large",
                "max_bytes": max_bytes,
            },
        )
        return None
    return prepare_word_image_bytes(data, role=role)


def fetch_image_bytes(
    url: str,
    *,
    role: str = "",
    timeout_seconds: float = DEFAULT_TIMEOUT_SECONDS,
    max_bytes: int = DEFAULT_MAX_BYTES,
) -> bytes | None:
    parsed = urlparse(url)
    local = _fetch_local_suning_asset(url, role=role, max_bytes=max_bytes)
    if local is not None:
        return local
    job_asset = _fetch_job_asset(url, role=role, max_bytes=max_bytes)
    if job_asset is not None:
        return job_asset
    if parsed.scheme.lower() not in _ALLOWED_SCHEMES:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "unsupported_scheme",
            },
        )
        return None
    try:
        request = urllib.request.Request(
            url,
            method="GET",
            headers={"User-Agent": _USER_AGENT},
        )
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            content_type = (response.headers.get("Content-Type") or "").split(";")[0].strip().lower()
            data = response.read(max_bytes + 1)
    except (urllib.error.URLError, TimeoutError, OSError, ValueError) as exc:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "error": str(exc),
            },
        )
        return None
    if len(data) > max_bytes:
        append_audit_event(
            "image_fetch_failed",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "reason": "too_large",
                "max_bytes": max_bytes,
            },
        )
        return None

    detected = detect_image_format(data)
    expected = _CONTENT_TYPE_TO_FORMAT.get(content_type)
    if (
        content_type
        and expected
        and detected != "unknown"
        and expected != detected
    ):
        append_audit_event(
            "image_fetch_content_type_mismatch",
            {
                "role": role,
                "url": url,
                "severity": "warning",
                "content_type": content_type,
                "detected": detected,
            },
        )
    elif content_type and not any(content_type.startswith(p) for p in _IMAGE_CONTENT_PREFIXES):
        if detected == "unknown":
            append_audit_event(
                "image_fetch_content_type_unexpected",
                {
                    "role": role,
                    "url": url,
                    "severity": "warning",
                    "content_type": content_type,
                },
            )

    return prepare_word_image_bytes(data, role=role)
