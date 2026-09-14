package com.ruoyi.carbon.report.generation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.mapper.ReportFileMapper;
import com.ruoyi.common.core.exception.ServiceException;

@Component
public class LocalProcessReportRenderer implements ReportRenderer
{
    private final CarbonReportRendererProperties properties;
    private final ProcessRunner processRunner;

    @Autowired(required = false)
    private ReportFileMapper fileMapper;

    public LocalProcessReportRenderer(CarbonReportRendererProperties properties, ProcessRunner processRunner)
    {
        this.properties = properties;
        this.processRunner = processRunner;
    }

    @Override
    public RenderResult generateDocx(String snapshotJson, boolean aiEnabled, Path outputPath)
    {
        Path jobDir = createJobDir();
        Path input = jobDir.resolve("input.json");
        Path output = outputPath == null ? jobDir.resolve("report.docx") : outputPath.toAbsolutePath().normalize();
        ensureInsideWorkRoot(output.getParent());
        try
        {
            Files.createDirectories(output.getParent());
            Path jobAssets = jobDir.resolve("job-assets");
            Files.createDirectories(jobAssets);
            String prepared = prepareSnapshot(snapshotJson, jobAssets);
            Files.writeString(input, prepared == null ? "{}" : prepared, StandardCharsets.UTF_8);
            Map<String, String> extra = new LinkedHashMap<>();
            extra.put("CARBON_REPORT_JOB_ASSETS_DIR", jobAssets.toString());
            run(generateCommand(input, output, aiEnabled), jobDir, output, new byte[] { 'P', 'K' }, extra);
            return new RenderResult(output, aiEnabled);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("生成 Word 失败: " + ex.getMessage());
        }
    }

    @Override
    public RenderResult convertPdf(Path docxPath, Path outputPath)
    {
        Path jobDir = createJobDir();
        Path output = outputPath == null ? jobDir.resolve("report.pdf") : outputPath.toAbsolutePath().normalize();
        ensureInsideWorkRoot(output.getParent());
        try
        {
            Files.createDirectories(output.getParent());
        }
        catch (Exception ex)
        {
            throw new ServiceException("无法创建 PDF 输出目录: " + ex.getMessage());
        }
        run(convertCommand(docxPath, output), jobDir, output, new byte[] { '%', 'P', 'D', 'F' }, Map.of());
        return new RenderResult(output, false);
    }

    List<String> generateCommand(Path input, Path output, boolean aiEnabled)
    {
        List<String> command = new ArrayList<>();
        command.add(properties.getPythonExecutable());
        command.add("-m");
        command.add("carbon_report_agent.cli");
        command.add("generate");
        command.add("--input");
        command.add(input.toString());
        command.add("--output");
        command.add(output.toString());
        command.add("--llm-enabled");
        command.add(aiEnabled ? "true" : "false");
        return command;
    }

    List<String> convertCommand(Path docx, Path output)
    {
        List<String> command = new ArrayList<>();
        command.add(properties.getPythonExecutable());
        command.add("-m");
        command.add("carbon_report_agent.cli");
        command.add("convert-pdf");
        command.add("--input-docx");
        command.add(docx.toString());
        command.add("--output");
        command.add(output.toString());
        return command;
    }

    Path createJobDir()
    {
        Path root = properties.workDirPath();
        Path jobDir = root.resolve("job-" + UUID.randomUUID());
        ensureInsideWorkRoot(jobDir);
        try
        {
            Files.createDirectories(jobDir);
        }
        catch (Exception ex)
        {
            throw new ServiceException("无法创建渲染工作目录: " + ex.getMessage());
        }
        return jobDir;
    }

    void ensureInsideWorkRoot(Path candidate)
    {
        Path root = properties.workDirPath();
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(root))
        {
            throw new ServiceException("渲染路径越出工作目录");
        }
    }

    private void run(List<String> command, Path workDir, Path output, byte[] magic)
    {
        run(command, workDir, output, magic, Map.of());
    }

    String prepareSnapshot(String snapshotJson, Path jobAssets)
    {
        if (fileMapper == null)
        {
            return snapshotJson == null ? "{}" : snapshotJson;
        }
        DiskArtifactStore store = new DiskArtifactStore(properties.artifactDirPath());
        try
        {
            return ReportImageSlots.materialize(snapshotJson, jobAssets, fileId -> {
                ReportFile meta = fileMapper.selectById(fileId);
                if (meta == null)
                {
                    return null;
                }
                Path path = store.resolve(meta);
                return Files.isRegularFile(path) ? path : null;
            });
        }
        catch (Exception ex)
        {
            throw new ServiceException("准备报告图片失败: " + ex.getMessage());
        }
    }

    private void run(List<String> command, Path workDir, Path output, byte[] magic, Map<String, String> extraEnv)
    {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PYTHONPATH", properties.moduleRootPath().toString());
        env.put("PYTHONIOENCODING", "utf-8");
        if (properties.getSofficePath() != null && !properties.getSofficePath().isBlank())
        {
            env.put("CARBON_REPORT_SOFFICE_PATH", properties.getSofficePath());
        }
        if (extraEnv != null)
        {
            env.putAll(extraEnv);
        }
        env.put("MPLCONFIGDIR", workDir.resolve("mplconfig").toString());
        ProcessRunner.ProcessOutcome outcome = processRunner.run(
                command, workDir, env, Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())),
                properties.getMaxLogBytes());
        if (outcome.exitCode != 0)
        {
            String detail = outcome.stderr.isBlank() ? outcome.stdout : outcome.stderr;
            throw new ServiceException("报告渲染失败: " + truncate(detail));
        }
        try
        {
            if (!Files.isRegularFile(output) || Files.size(output) <= 0)
            {
                throw new ServiceException("渲染未生成有效文件");
            }
            byte[] header;
            try (java.io.InputStream in = Files.newInputStream(output))
            {
                header = in.readNBytes(magic.length);
            }
            if (header.length < magic.length)
            {
                throw new ServiceException("渲染输出格式错误");
            }
            for (int i = 0; i < magic.length; i++)
            {
                if (header[i] != magic[i])
                {
                    throw new ServiceException("渲染输出格式错误");
                }
            }
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("读取渲染结果失败: " + ex.getMessage());
        }
    }

    private static String truncate(String text)
    {
        if (text == null)
        {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }
}
