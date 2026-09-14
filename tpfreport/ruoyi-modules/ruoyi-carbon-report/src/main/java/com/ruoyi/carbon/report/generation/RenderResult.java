package com.ruoyi.carbon.report.generation;

import java.nio.file.Path;

public class RenderResult
{
    private final Path outputPath;
    private final boolean llmEnabled;

    public RenderResult(Path outputPath, boolean llmEnabled)
    {
        this.outputPath = outputPath;
        this.llmEnabled = llmEnabled;
    }

    public Path getOutputPath() { return outputPath; }
    public boolean isLlmEnabled() { return llmEnabled; }
}
