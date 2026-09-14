from __future__ import annotations

import io
from typing import Literal

from carbon_report_agent.audit import append_audit_event

ImageFormat = Literal["png", "jpeg", "gif", "bmp", "webp", "unknown"]

_WORD_SAFE_FORMATS = frozenset({"png", "jpeg", "gif", "bmp"})


def detect_image_format(data: bytes) -> ImageFormat:
    if len(data) >= 8 and data[:8] == b"\x89PNG\r\n\x1a\n":
        return "png"
    if len(data) >= 3 and data[:3] == b"\xff\xd8\xff":
        return "jpeg"
    if len(data) >= 6 and data[:6] in (b"GIF87a", b"GIF89a"):
        return "gif"
    if len(data) >= 2 and data[:2] == b"BM":
        return "bmp"
    if len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "webp"
    return "unknown"


def is_probably_html(data: bytes) -> bool:
    if not data:
        return False
    head = data[:256].lstrip().lower()
    return head.startswith(b"<!doctype") or head.startswith(b"<html") or head.startswith(b"<head")


def _webp_to_png(data: bytes) -> bytes | None:
    try:
        from PIL import Image
    except ImportError:
        return None
    try:
        with Image.open(io.BytesIO(data)) as image:
            rgb = image.convert("RGB")
            out = io.BytesIO()
            rgb.save(out, format="PNG")
            return out.getvalue()
    except OSError:
        return None


def prepare_word_image_bytes(raw: bytes, *, role: str = "") -> bytes | None:
    if not raw:
        append_audit_event(
            "image_embed_rejected",
            {"role": role, "severity": "warning", "reason": "empty_payload"},
        )
        return None
    if is_probably_html(raw):
        append_audit_event(
            "image_embed_rejected",
            {"role": role, "severity": "warning", "reason": "html_response"},
        )
        return None

    fmt = detect_image_format(raw)
    if fmt in _WORD_SAFE_FORMATS:
        return raw
    if fmt == "webp":
        converted = _webp_to_png(raw)
        if converted is not None:
            return converted
        append_audit_event(
            "image_embed_rejected",
            {"role": role, "severity": "warning", "reason": "webp_convert_failed"},
        )
        return None

    append_audit_event(
        "image_embed_rejected",
        {
            "role": role,
            "severity": "warning",
            "reason": "unsupported_format",
            "detected": fmt,
        },
    )
    return None
