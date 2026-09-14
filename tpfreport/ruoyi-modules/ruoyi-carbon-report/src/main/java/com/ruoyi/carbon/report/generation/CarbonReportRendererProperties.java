package com.ruoyi.carbon.report.generation;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CarbonReportRendererProperties
{
    @Value("${carbon.report.renderer.python-executable:python}")
    private String pythonExecutable;

    @Value("${carbon.report.renderer.module-root:}")
    private String moduleRoot;

    @Value("${carbon.report.renderer.work-dir:${java.io.tmpdir}/carbon-report-work}")
    private String workDir;

    @Value("${carbon.report.renderer.artifact-dir:${java.io.tmpdir}/carbon-report-artifacts}")
    private String artifactDir;

    @Value("${carbon.report.renderer.soffice-path:}")
    private String sofficePath;

    @Value("${carbon.report.renderer.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${carbon.report.renderer.max-log-bytes:1048576}")
    private int maxLogBytes;

    public String getPythonExecutable() { return pythonExecutable; }
    public void setPythonExecutable(String pythonExecutable) { this.pythonExecutable = pythonExecutable; }
    public String getModuleRoot() { return moduleRoot; }
    public void setModuleRoot(String moduleRoot) { this.moduleRoot = moduleRoot; }
    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }
    public String getArtifactDir() { return artifactDir; }
    public void setArtifactDir(String artifactDir) { this.artifactDir = artifactDir; }
    public String getSofficePath() { return sofficePath; }
    public void setSofficePath(String sofficePath) { this.sofficePath = sofficePath; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxLogBytes() { return maxLogBytes; }
    public void setMaxLogBytes(int maxLogBytes) { this.maxLogBytes = maxLogBytes; }

    public Path workDirPath()
    {
        return Path.of(workDir).toAbsolutePath().normalize();
    }

    public Path artifactDirPath()
    {
        return Path.of(artifactDir).toAbsolutePath().normalize();
    }

    public Path moduleRootPath()
    {
        if (moduleRoot == null || moduleRoot.isBlank())
        {
            return Path.of(".").toAbsolutePath().normalize();
        }
        return Path.of(moduleRoot).toAbsolutePath().normalize();
    }
}
