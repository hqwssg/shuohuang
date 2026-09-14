from __future__ import annotations

import os
from pathlib import Path


def load_env(dotenv_path: str | Path | None = None, *, override: bool = False) -> Path | None:
    """
    Load KEY=VALUE pairs from a .env file into os.environ.
    By default does not override variables that are already set.
    """
    if dotenv_path is None:
        root = Path(__file__).resolve().parent.parent
        candidate = root / ".env"
    else:
        candidate = Path(dotenv_path)
    if not candidate.is_file():
        return None

    try:
        from dotenv import load_dotenv  # type: ignore

        load_dotenv(candidate, override=override)
        return candidate
    except ImportError:
        pass

    for raw in candidate.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if not key:
            continue
        if override or key not in os.environ or os.environ.get(key, "") == "":
            os.environ[key] = value
    return candidate
