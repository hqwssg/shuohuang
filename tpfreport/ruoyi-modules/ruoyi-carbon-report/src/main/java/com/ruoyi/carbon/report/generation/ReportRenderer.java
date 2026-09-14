package com.ruoyi.carbon.report.generation;

import java.nio.file.Path;

public interface ReportRenderer
{
    RenderResult generateDocx(String snapshotJson, boolean aiEnabled, Path outputPath);

    RenderResult convertPdf(Path docxPath, Path outputPath);
}
