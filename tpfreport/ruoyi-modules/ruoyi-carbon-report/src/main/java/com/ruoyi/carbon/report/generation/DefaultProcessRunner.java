package com.ruoyi.carbon.report.generation;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.exception.ServiceException;

@Component
public class DefaultProcessRunner implements ProcessRunner
{
    @Override
    public ProcessOutcome run(List<String> command, Path workDir, Map<String, String> extraEnv,
            Duration timeout, int maxLogBytes)
    {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        if (extraEnv != null)
        {
            builder.environment().putAll(extraEnv);
        }
        try
        {
            Process process = builder.start();
            ExecutorService io = Executors.newFixedThreadPool(2);
            Future<String> stdout = io.submit(() -> readLimited(process.getInputStream(), maxLogBytes));
            Future<String> stderr = io.submit(() -> readLimited(process.getErrorStream(), maxLogBytes));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished)
            {
                destroyTree(process);
                io.shutdownNow();
                throw new ServiceException("报告渲染超时");
            }
            String out = stdout.get(2, TimeUnit.SECONDS);
            String err = stderr.get(2, TimeUnit.SECONDS);
            io.shutdownNow();
            return new ProcessOutcome(process.exitValue(), out, err);
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("无法启动报告渲染进程: " + ex.getMessage());
        }
    }

    static String readLimited(InputStream stream, int maxBytes) throws Exception
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int remaining = Math.max(0, maxBytes);
        int read;
        while ((read = stream.read(chunk)) >= 0)
        {
            int take = Math.min(read, remaining);
            if (take > 0)
            {
                buffer.write(chunk, 0, take);
                remaining -= take;
            }
            if (remaining <= 0)
            {
                while (stream.read(chunk) >= 0)
                {
                    // drain leftover bytes so the child process is not blocked on a full pipe
                }
                break;
            }
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void destroyTree(Process process)
    {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        try
        {
            if (!process.waitFor(5, TimeUnit.SECONDS))
            {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }
}
