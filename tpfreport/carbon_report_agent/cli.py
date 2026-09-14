from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from carbon_report_agent.env_loader import load_env
from carbon_report_agent.models import ReportInput
from carbon_report_agent.report_data import enrich_images_from_assets
from carbon_report_agent.pipeline import (
    PdfConversionError,
    PdfConversionUnavailableError,
    convert_report_docx_to_pdf,
    generate_report_docx,
    generate_report_pdf,
)

load_env()


def _parse_bool(value: str) -> bool:
    normalized = str(value).strip().lower()
    if normalized in {"true", "1", "yes"}:
        return True
    if normalized in {"false", "0", "no"}:
        return False
    raise argparse.ArgumentTypeError(f"expected true or false, got {value!r}")


def _print_ok(output: Path, *, llm_enabled: bool | None = None) -> None:
    payload = {"ok": True, "output": str(output)}
    if llm_enabled is not None:
        payload["llm_enabled"] = llm_enabled
    print(json.dumps(payload, ensure_ascii=False), flush=True)


def _fail(message: str, code: int = 1) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(code)


def _load_report(input_path: Path) -> ReportInput:
    payload = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("report input must be a JSON object")
    payload = enrich_images_from_assets(payload)
    return ReportInput.model_validate(payload)


def run_generate(input_path: Path, output_path: Path, *, llm_enabled: bool) -> Path:
    report = _load_report(input_path)
    return generate_report_docx(report, output_path, llm_enabled=llm_enabled)


def run_generate_pdf(input_path: Path, output_path: Path) -> Path:
    report = _load_report(input_path)
    return generate_report_pdf(report, output_path)


def run_convert_pdf(input_docx: Path, output_path: Path) -> Path:
    return convert_report_docx_to_pdf(input_docx, output_path)


def run_generate_batch(input_dir: Path, output_dir: Path) -> list[Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []
    for input_path in sorted(input_dir.glob("*.json")):
        report = ReportInput.model_validate_json(input_path.read_text(encoding="utf-8"))
        out = output_dir / f"{input_path.stem}.docx"
        paths.append(generate_report_docx(report, out))
    return paths


def main() -> None:
    parser = argparse.ArgumentParser(prog="carbon-report-agent")
    subparsers = parser.add_subparsers(dest="command", required=True)

    generate = subparsers.add_parser("generate")
    generate.add_argument("--input", required=True)
    generate.add_argument("--output", required=True)
    generate.add_argument("--llm-enabled", required=True, type=_parse_bool)

    generate_pdf = subparsers.add_parser("generate-pdf")
    generate_pdf.add_argument("--input", required=True)
    generate_pdf.add_argument("--output", required=True)

    convert_pdf = subparsers.add_parser("convert-pdf")
    convert_pdf.add_argument("--input-docx", required=True)
    convert_pdf.add_argument("--output", required=True)

    batch = subparsers.add_parser("generate-batch")
    batch.add_argument("--input-dir", required=True)
    batch.add_argument("--output-dir", required=True)

    args = parser.parse_args()
    try:
        if args.command == "generate":
            path = run_generate(Path(args.input), Path(args.output), llm_enabled=args.llm_enabled)
            _print_ok(path, llm_enabled=args.llm_enabled)
        elif args.command == "generate-pdf":
            path = run_generate_pdf(Path(args.input), Path(args.output))
            _print_ok(path)
        elif args.command == "convert-pdf":
            path = run_convert_pdf(Path(args.input_docx), Path(args.output))
            _print_ok(path)
        elif args.command == "generate-batch":
            paths = run_generate_batch(Path(args.input_dir), Path(args.output_dir))
            print(json.dumps([str(p) for p in paths], ensure_ascii=False))
    except (PdfConversionUnavailableError, PdfConversionError, ValueError, OSError) as exc:
        _fail(str(exc))
    except Exception as exc:  # noqa: BLE001
        _fail(str(exc))


if __name__ == "__main__":
    main()
