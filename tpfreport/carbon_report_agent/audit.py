from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


def _audit_path() -> Path:
    override = os.environ.get("CARBON_REPORT_AUDIT_PATH", "").strip()
    if override:
        return Path(override)
    return Path(__file__).resolve().parent.parent / "output" / "audit.jsonl"


def append_audit_event(event_type: str, payload: dict[str, Any]) -> Path:
    path = _audit_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    record = {
        "ts": datetime.now(timezone.utc).isoformat(),
        "event": event_type,
        **payload,
    }
    with path.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(record, ensure_ascii=False) + "\n")
    return path


def read_recent_audit(limit: int = 20) -> list[dict[str, Any]]:
    path = _audit_path()
    if not path.exists():
        return []
    lines = path.read_text(encoding="utf-8").splitlines()
    rows: list[dict[str, Any]] = []
    for line in lines[-limit:]:
        line = line.strip()
        if not line:
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            continue
    return rows
