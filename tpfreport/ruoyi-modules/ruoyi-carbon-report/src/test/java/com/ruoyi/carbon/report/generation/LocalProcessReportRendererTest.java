package com.ruoyi.carbon.report.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.common.core.exception.ServiceException;

class LocalProcessReportRendererTest
{
    @TempDir
    Path temp;

    @Test
    void generateCommandKeepsSpacedPathsAsSingleTokens()
    {
        CarbonReportRendererProperties properties = properties();
        LocalProcessReportRenderer renderer = new LocalProcessReportRenderer(properties, noopRunner());
        Path input = temp.resolve("work dir").resolve("input.json");
        Path output = temp.resolve("work dir").resolve("out file.docx");
        List<String> command = renderer.generateCommand(input, output, true);
        assertEquals(properties.getPythonExecutable(), command.get(0));
        assertEquals("generate", command.get(3));
        assertEquals(input.toString(), command.get(5));
        assertEquals(output.toString(), command.get(7));
        assertEquals("--llm-enabled", command.get(8));
        assertEquals("true", command.get(9));
        assertFalse(command.contains("generate-pdf"));
    }

    @Test
    void convertCommandDoesNotRegenerateDocx()
    {
        LocalProcessReportRenderer renderer = new LocalProcessReportRenderer(properties(), noopRunner());
        List<String> command = renderer.convertCommand(Path.of("a.docx"), Path.of("b.pdf"));
        assertEquals("convert-pdf", command.get(3));
        assertFalse(command.contains("generate-pdf"));
        assertFalse(command.contains("generate"));
    }

    @Test
    void rejectsPathOutsideWorkDir()
    {
        LocalProcessReportRenderer renderer = new LocalProcessReportRenderer(properties(), noopRunner());
        assertThrows(ServiceException.class, () -> renderer.ensureInsideWorkRoot(temp.resolve("other")));
    }

    @Test
    void nonZeroExitFails(@TempDir Path dir) throws Exception
    {
        CarbonReportRendererProperties properties = properties();
        LocalProcessReportRenderer renderer = new LocalProcessReportRenderer(properties,
                (command, workDir, env, timeout, maxLogBytes) -> new ProcessRunner.ProcessOutcome(1, "", "boom"));
        Path output = properties.workDirPath().resolve("out.docx");
        Files.createDirectories(output.getParent());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> renderer.generateDocx("{}", false, output));
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    void badMagicFails() throws Exception
    {
        CarbonReportRendererProperties properties = properties();
        Path output = properties.workDirPath().resolve("out.docx");
        LocalProcessReportRenderer renderer = new LocalProcessReportRenderer(properties,
                (command, workDir, env, timeout, maxLogBytes) -> {
                    try
                    {
                        Files.createDirectories(output.getParent());
                        Files.writeString(output, "not-a-docx");
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException(ex);
                    }
                    return new ProcessRunner.ProcessOutcome(0, "{\"ok\":true}", "");
                });
        assertThrows(ServiceException.class, () -> renderer.generateDocx("{}", false, output));
    }

    @Test
    void timeoutSecondsPassedToRunner()
    {
        CarbonReportRendererProperties properties = properties();
        properties.setTimeoutSeconds(12);
        properties.setSofficePath("/usr/bin/libreoffice");
        AtomicReference<Duration> seen = new AtomicReference<>();
        AtomicReference<Map<String, String>> envSeen = new AtomicReference<>();
        Path output = properties.workDirPath().resolve("out.docx");
        LocalProcessReportRenderer renderer = new LocalProcessReportRenderer(properties,
                (command, workDir, env, timeout, maxLogBytes) -> {
                    seen.set(timeout);
                    envSeen.set(env);
                    try
                    {
                        Files.createDirectories(output.getParent());
                        Files.write(output, new byte[] { 'P', 'K', 3, 4 });
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException(ex);
                    }
                    return new ProcessRunner.ProcessOutcome(0, "{\"ok\":true}", "");
                });
        renderer.generateDocx("{}", false, output);
        assertEquals(12, seen.get().toSeconds());
        assertEquals(properties.moduleRootPath().toString(), envSeen.get().get("PYTHONPATH"));
        assertEquals("/usr/bin/libreoffice", envSeen.get().get("CARBON_REPORT_SOFFICE_PATH"));
        assertTrue(envSeen.get().containsKey("MPLCONFIGDIR"));
        assertTrue(envSeen.get().get("CARBON_REPORT_JOB_ASSETS_DIR").endsWith("job-assets"));
        assertFalse(envSeen.get().containsKey("OPENAI_API_KEY"));
    }

    private CarbonReportRendererProperties properties()
    {
        CarbonReportRendererProperties properties = new CarbonReportRendererProperties();
        properties.setPythonExecutable("python");
        properties.setModuleRoot(temp.toString());
        properties.setWorkDir(temp.resolve("work dir").toString());
        properties.setArtifactDir(temp.resolve("artifacts").toString());
        properties.setTimeoutSeconds(30);
        properties.setMaxLogBytes(1024);
        return properties;
    }

    private static ProcessRunner noopRunner()
    {
        return (command, workDir, env, timeout, maxLogBytes) -> new ProcessRunner.ProcessOutcome(0, "", "");
    }
}
