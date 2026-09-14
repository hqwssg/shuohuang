from __future__ import annotations

import os
import shutil
import subprocess
import tempfile
from pathlib import Path


class PdfConversionUnavailableError(RuntimeError):
    """Raised when no LibreOffice/soffice executable can be found."""


class PdfConversionError(RuntimeError):
    """Raised when LibreOffice is found but the conversion fails."""


def find_soffice_executable() -> str:
    configured = os.environ.get("CARBON_REPORT_SOFFICE_PATH", "").strip()
    if configured:
        configured_path = Path(configured)
        if configured_path.is_file():
            return str(configured_path)
        configured_command = shutil.which(configured)
        if configured_command:
            return configured_command
        raise PdfConversionUnavailableError(
            "Configured CARBON_REPORT_SOFFICE_PATH does not point to a usable executable: "
            f"{configured}. Install LibreOffice or set CARBON_REPORT_SOFFICE_PATH to soffice/libreoffice."
        )

    executable = shutil.which("soffice") or shutil.which("libreoffice")
    if executable:
        return executable

    raise PdfConversionUnavailableError(
        "LibreOffice/soffice not found. Install LibreOffice or set CARBON_REPORT_SOFFICE_PATH "
        "to the soffice/libreoffice executable."
    )


def _user_installation_uri(profile_dir: Path) -> str:
    return "file:///" + profile_dir.resolve().as_posix().lstrip("/")


def convert_docx_to_pdf(
    docx_path: Path | str,
    pdf_path: Path | str,
    *,
    timeout_seconds: float = 120.0,
    job_id: str | None = None,
) -> Path:
    docx_path = Path(docx_path)
    pdf_path = Path(pdf_path)
    if not docx_path.exists():
        raise FileNotFoundError(f"DOCX input does not exist: {docx_path}")

    soffice = find_soffice_executable()
    pdf_path.parent.mkdir(parents=True, exist_ok=True)
    prefix = f"carbon-report-pdf-{job_id or 'job'}-"

    with tempfile.TemporaryDirectory(prefix=prefix) as tmp:
        tmp_dir = Path(tmp)
        outdir = tmp_dir / "out"
        profile_dir = tmp_dir / "lo-profile"
        outdir.mkdir()
        profile_dir.mkdir()
        command = [
            soffice,
            "--headless",
            "--nologo",
            "--nofirststartwizard",
            "--nodefault",
            "--norestore",
            f"-env:UserInstallation={_user_installation_uri(profile_dir)}",
            "--convert-to",
            "pdf",
            "--outdir",
            str(outdir),
            str(docx_path),
        ]
        try:
            result = subprocess.run(
                command,
                capture_output=True,
                text=True,
                timeout=timeout_seconds,
                check=False,
            )
        except subprocess.TimeoutExpired as exc:
            raise PdfConversionError(
                f"LibreOffice PDF conversion timed out after {timeout_seconds:g} seconds."
            ) from exc

        if result.returncode != 0:
            details = "\n".join(part for part in [result.stderr.strip(), result.stdout.strip()] if part)
            raise PdfConversionError(
                "LibreOffice PDF conversion failed"
                + (f": {details}" if details else ".")
            )

        converted = outdir / f"{docx_path.stem}.pdf"
        if not converted.exists():
            available = ", ".join(path.name for path in outdir.glob("*")) or "none"
            raise PdfConversionError(
                "LibreOffice PDF conversion did not produce the expected PDF "
                f"({converted.name}); output files: {available}."
            )

        if pdf_path.exists():
            pdf_path.unlink()
        shutil.move(str(converted), str(pdf_path))

    return pdf_path


__all__ = [
    "PdfConversionError",
    "PdfConversionUnavailableError",
    "convert_docx_to_pdf",
    "find_soffice_executable",
]
