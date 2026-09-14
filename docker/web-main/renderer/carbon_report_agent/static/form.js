let reportPayload = null;
let generatedReportId = null;

function showError(message) {
  const el = document.getElementById("error");
  if (!message) {
    el.hidden = true;
    el.textContent = "";
    return;
  }
  el.hidden = false;
  el.textContent = message;
}

function setStatus(message) {
  document.getElementById("load-status").textContent = message;
}

function setGenerateEnabled(enabled) {
  document.getElementById("btn-generate").disabled = !enabled;
  document.getElementById("btn-generate-pdf").disabled = !enabled;
}

async function loadReportInput() {
  showError("");
  setGenerateEnabled(false);
  setStatus("正在加载传入 JSON...");
  try {
    const response = await fetch("/api/report-input");
    if (!response.ok) {
      throw new Error("加载失败: HTTP " + response.status);
    }
    reportPayload = await response.json();
    generatedReportId = null;
    document.getElementById("raw-json").textContent = JSON.stringify(reportPayload, null, 2);
    setStatus("已加载传入 JSON。");
    setGenerateEnabled(true);
  } catch (err) {
    reportPayload = null;
    generatedReportId = null;
    document.getElementById("raw-json").textContent = "";
    setStatus("加载失败。");
    showError(String(err.message || err));
  }
}

async function downloadReport(format) {
  if (!reportPayload) {
    showError("请先等待 JSON 加载完成。");
    return;
  }
  showError("");
  setGenerateEnabled(false);
  const endpoint = format === "pdf" ? "/api/generate-pdf" : "/api/generate";
  const requestPayload = format === "pdf" && generatedReportId
    ? { report_id: generatedReportId }
    : reportPayload;
  const fallbackFilename = format === "pdf" ? "carbon-report.pdf" : "carbon-report.docx";
  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestPayload),
    });
    const contentType = response.headers.get("content-type") || "";
    if (!response.ok) {
      let message = "生成失败: HTTP " + response.status;
      if (contentType.includes("application/json")) {
        const errBody = await response.json();
        message = errBody.error || message;
      }
      throw new Error(message);
    }
    const reportId = response.headers.get("x-report-id");
    if (reportId) {
      generatedReportId = reportId;
    }
    const blob = await response.blob();
    const disposition = response.headers.get("content-disposition") || "";
    const match = /filename="?([^";]+)"?/.exec(disposition);
    const filename = match ? match[1] : fallbackFilename;
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  } catch (err) {
    showError(String(err.message || err));
  } finally {
    setGenerateEnabled(Boolean(reportPayload));
  }
}

document.getElementById("btn-generate").addEventListener("click", () => downloadReport("docx"));
document.getElementById("btn-generate-pdf").addEventListener("click", () => downloadReport("pdf"));
loadReportInput();
