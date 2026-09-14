from __future__ import annotations

import os
import json
import hashlib
from dataclasses import dataclass
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, JSONResponse, Response
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from pydantic import ValidationError

from carbon_report_agent.audit import read_recent_audit
from carbon_report_agent.env_loader import load_env
from carbon_report_agent.models import ReportInput
from carbon_report_agent.pipeline import (
    GenerateBlockedError,
    PdfConversionError,
    PdfConversionUnavailableError,
    convert_report_docx_to_pdf,
    generate_report_docx,
)
from carbon_report_agent.report_data import (
    ReportSelectors,
    list_entities,
    load_fixture_dict,
    load_report_input,
)
from carbon_report_agent.style_pack import load_style_pack

load_env()

PACKAGE_DIR = Path(__file__).resolve().parent
TEMPLATES_DIR = PACKAGE_DIR / "templates"
STATIC_DIR = PACKAGE_DIR / "static"
ASSETS_SUNING_DIR = PACKAGE_DIR / "assets" / "suning"
GENERATED_DIR = Path(
    os.environ.get(
        "CARBON_REPORT_GENERATED_DIR",
        str(PACKAGE_DIR.parent / "output" / "generated_reports"),
    )
)


@dataclass(frozen=True)
class GeneratedReport:
    report_id: str
    docx_path: Path
    pdf_path: Path
    year: int


_GENERATED_REPORTS: dict[str, GeneratedReport] = {}

app = FastAPI(title="Carbon Report Agent")
templates = Jinja2Templates(directory=str(TEMPLATES_DIR))
STATIC_DIR.mkdir(parents=True, exist_ok=True)
ASSETS_SUNING_DIR.mkdir(parents=True, exist_ok=True)
GENERATED_DIR.mkdir(parents=True, exist_ok=True)
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
app.mount(
    "/assets/suning",
    StaticFiles(directory=str(ASSETS_SUNING_DIR)),
    name="assets_suning",
)


@app.get("/", response_class=HTMLResponse)
async def get_home(request: Request) -> HTMLResponse:
    return templates.TemplateResponse(request, "form.html", {"error": None})


@app.get("/mock-backend/report-input")
async def mock_backend_report_input() -> JSONResponse:
    """Simulated external business API for local upstream integration tests."""
    return JSONResponse(load_fixture_dict())


@app.get("/api/entities")
async def api_entities() -> JSONResponse:
    return JSONResponse({"entities": list_entities()})


@app.get("/api/report-input")
async def api_report_input(
    entity: str | None = None,
    year: int | None = None,
    grain: str = "year",
    month: int | None = None,
) -> JSONResponse:
    """
    Return report input from upstream backend when CARBON_REPORT_INPUT_URL is set;
    otherwise return fixed fixture data that simulates the backend contract.

    Optional query params entity/year/grain/month select which report to load.
    """
    selectors: ReportSelectors | None = None
    if entity is not None and year is not None:
        try:
            selectors = ReportSelectors(entity=entity, year=year, grain=grain, month=month)
        except ValidationError as exc:
            return JSONResponse({"ok": False, "error": str(exc)}, status_code=400)
    try:
        return JSONResponse(load_report_input(selectors))
    except ValueError as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=400)
    except RuntimeError as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=502)


@app.get("/api/audit")
async def api_audit(limit: int = 20) -> JSONResponse:
    return JSONResponse({"ok": True, "events": read_recent_audit(limit=max(1, min(limit, 200)))})


def _report_cache_key(report: ReportInput) -> str:
    style_version = str(load_style_pack().get("version", "unknown"))
    canonical = json.dumps(
        {
            "cache_schema": "docx-render-v2",
            "style_pack_version": style_version,
            "report": report.model_dump(mode="json"),
        },
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def _artifact_for_cache_key(cache_key: str, year: int) -> GeneratedReport:
    artifact_dir = GENERATED_DIR / cache_key
    return GeneratedReport(
        report_id=cache_key,
        docx_path=artifact_dir / f"carbon-report-{year}.docx",
        pdf_path=artifact_dir / f"carbon-report-{year}.pdf",
        year=year,
    )


def _artifact_for_report(report: ReportInput) -> GeneratedReport:
    cache_key = _report_cache_key(report)
    artifact = _artifact_for_cache_key(cache_key, report.metadata.report_year)
    _GENERATED_REPORTS[cache_key] = artifact
    return artifact


def _ensure_docx_artifact(report: ReportInput) -> tuple[GeneratedReport, bool]:
    artifact = _artifact_for_report(report)
    if artifact.docx_path.is_file():
        _GENERATED_REPORTS[artifact.report_id] = artifact
        return artifact, True
    artifact.docx_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = artifact.docx_path.with_suffix(".docx.tmp")
    if tmp_path.exists():
        tmp_path.unlink()
    try:
        generate_report_docx(report, tmp_path)
        tmp_path.replace(artifact.docx_path)
    finally:
        if tmp_path.exists():
            tmp_path.unlink()
    _GENERATED_REPORTS[artifact.report_id] = artifact
    return artifact, False


def _get_docx_artifact(report_id: str) -> GeneratedReport:
    artifact = _GENERATED_REPORTS.get(report_id)
    if artifact is None:
        matches = list(GENERATED_DIR.glob(f"{report_id}/carbon-report-*.docx"))
        if not matches:
            raise KeyError(report_id)
        docx_path = matches[0]
        year_text = docx_path.stem.removeprefix("carbon-report-")
        try:
            year = int(year_text)
        except ValueError as exc:
            raise KeyError(report_id) from exc
        artifact = _artifact_for_cache_key(report_id, year)
        _GENERATED_REPORTS[report_id] = artifact
    if not artifact.docx_path.is_file():
        raise KeyError(report_id)
    return artifact


def _docx_response(artifact: GeneratedReport, *, cache_hit: bool) -> Response:
    filename = f"carbon-report-{artifact.year}.docx"
    headers = {
        "Content-Disposition": f'attachment; filename="{filename}"',
        "X-Report-Id": artifact.report_id,
        "X-Cache": "HIT" if cache_hit else "MISS",
    }
    return Response(
        content=artifact.docx_path.read_bytes(),
        media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        headers=headers,
    )


def _pdf_response_from_docx(artifact: GeneratedReport, *, cache_hit: bool) -> Response:
    if artifact.pdf_path.is_file():
        data = artifact.pdf_path.read_bytes()
        cache_hit = True
    else:
        artifact.pdf_path.parent.mkdir(parents=True, exist_ok=True)
        tmp_path = artifact.pdf_path.with_suffix(".pdf.tmp")
        if tmp_path.exists():
            tmp_path.unlink()
        try:
            convert_report_docx_to_pdf(artifact.docx_path, tmp_path)
            tmp_path.replace(artifact.pdf_path)
        finally:
            if tmp_path.exists():
                tmp_path.unlink()
        data = artifact.pdf_path.read_bytes()
    filename = f"carbon-report-{artifact.year}.pdf"
    headers = {
        "Content-Disposition": f'attachment; filename="{filename}"',
        "X-Report-Id": artifact.report_id,
        "X-Cache": "HIT" if cache_hit else "MISS",
    }
    return Response(content=data, media_type="application/pdf", headers=headers)


def _report_id_from_payload(payload: object) -> str | None:
    if not isinstance(payload, dict):
        return None
    report_id = payload.get("report_id")
    if not isinstance(report_id, str):
        return None
    report_id = report_id.strip()
    return report_id or None


def _payload_looks_like_selectors(payload: object) -> bool:
    return (
        isinstance(payload, dict)
        and "entity" in payload
        and "year" in payload
        and "metadata" not in payload
    )


def _report_from_payload(payload: object) -> ReportInput:
    if _payload_looks_like_selectors(payload):
        selectors = ReportSelectors.model_validate(payload)
        data = load_report_input(selectors)
        return ReportInput.model_validate(data)
    return ReportInput.model_validate(payload)


def _require_renderer_token(request: Request) -> JSONResponse | None:
    expected = os.environ.get("CARBON_REPORT_RENDERER_TOKEN", "")
    incoming = request.headers.get("X-Renderer-Token", "")
    if not expected or incoming != expected:
        return JSONResponse({"ok": False, "error": "unauthorized renderer"}, status_code=401)
    return None


@app.post("/internal/render")
async def internal_render(request: Request):
    denied = _require_renderer_token(request)
    if denied is not None:
        return denied
    try:
        payload = await request.json()
        report = ReportInput.model_validate(payload)
    except (ValidationError, ValueError, TypeError) as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=400)
    try:
        artifact, cache_hit = _ensure_docx_artifact(report)
        return _docx_response(artifact, cache_hit=cache_hit)
    except GenerateBlockedError as exc:
        error = "; ".join(f.message for f in exc.findings)
        return JSONResponse({"ok": False, "error": error}, status_code=422)
    except Exception as exc:  # noqa: BLE001
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=500)


@app.post("/internal/render-pdf")
async def internal_render_pdf(request: Request):
    denied = _require_renderer_token(request)
    if denied is not None:
        return denied
    try:
        payload = await request.json()
        report = ReportInput.model_validate(payload)
    except (ValidationError, ValueError, TypeError) as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=400)
    try:
        artifact, _ = _ensure_docx_artifact(report)
        pdf_cache_hit = artifact.pdf_path.is_file()
        return _pdf_response_from_docx(artifact, cache_hit=pdf_cache_hit)
    except GenerateBlockedError as exc:
        error = "; ".join(f.message for f in exc.findings)
        return JSONResponse({"ok": False, "error": error}, status_code=422)
    except PdfConversionUnavailableError as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=503)
    except PdfConversionError as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=500)
    except Exception as exc:  # noqa: BLE001
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=500)


@app.post("/api/generate")
async def api_generate(request: Request):
    try:
        payload = await request.json()
        report = _report_from_payload(payload)
    except (ValidationError, ValueError, TypeError) as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=400)

    try:
        artifact, cache_hit = _ensure_docx_artifact(report)
        return _docx_response(artifact, cache_hit=cache_hit)
    except GenerateBlockedError as exc:
        error = "; ".join(f.message for f in exc.findings)
        return JSONResponse({"ok": False, "error": error}, status_code=422)
    except Exception as exc:  # noqa: BLE001
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=500)


@app.post("/api/generate-pdf")
async def api_generate_pdf(request: Request):
    artifact: GeneratedReport | None = None
    report: ReportInput | None = None
    try:
        payload = await request.json()
        report_id = _report_id_from_payload(payload)
        if report_id is not None:
            try:
                artifact = _get_docx_artifact(report_id)
            except KeyError:
                return JSONResponse(
                    {"ok": False, "error": "report_id not found or expired; generate Word again."},
                    status_code=404,
                )
        else:
            report = _report_from_payload(payload)
    except (ValidationError, ValueError, TypeError) as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=400)

    try:
        if artifact is None:
            if report is None:
                return JSONResponse({"ok": False, "error": "missing report payload"}, status_code=400)
            artifact, _ = _ensure_docx_artifact(report)
        pdf_cache_hit = artifact.pdf_path.is_file()
        return _pdf_response_from_docx(artifact, cache_hit=pdf_cache_hit)
    except GenerateBlockedError as exc:
        error = "; ".join(f.message for f in exc.findings)
        return JSONResponse({"ok": False, "error": error}, status_code=422)
    except PdfConversionUnavailableError as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=503)
    except PdfConversionError as exc:
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=500)
    except Exception as exc:  # noqa: BLE001
        return JSONResponse({"ok": False, "error": str(exc)}, status_code=500)
