package com.ruoyi.carbon.report.generation;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ProcessRunner
{
    ProcessOutcome run(List<String> command, Path workDir, Map<String, String> extraEnv, Duration timeout, int maxLogBytes);

    final class ProcessOutcome
    {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public ProcessOutcome(int exitCode, String stdout, String stderr)
        {
            this.exitCode = exitCode;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
        }
    }
}
