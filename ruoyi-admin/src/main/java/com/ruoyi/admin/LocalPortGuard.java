package com.ruoyi.admin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Releases the dev port when it is still held by a previous ruoyi-admin run.
 */
final class LocalPortGuard
{
    private static final int DEFAULT_PORT = 8081;
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(5);

    private LocalPortGuard()
    {
    }

    static void releasePreviousInstance(String[] args)
    {
        if (!isEnabled())
        {
            return;
        }

        int port = resolvePort(args);
        if (port <= 0)
        {
            return;
        }

        Set<Long> listenerPids = findListenerPids(port);
        if (listenerPids.isEmpty())
        {
            return;
        }

        long currentPid = ProcessHandle.current().pid();
        for (Long pid : listenerPids)
        {
            if (pid == currentPid)
            {
                continue;
            }

            String commandLine = getCommandLine(pid);
            if (!isRuoyiAdmin(commandLine))
            {
                System.err.println("Port " + port + " is already used by PID " + pid
                        + ". It is not a ruoyi-admin process, so it was not stopped.");
                continue;
            }

            System.out.println("Port " + port + " is used by previous ruoyi-admin PID " + pid + ". Stopping it first.");
            stopProcess(pid);
        }
    }

    private static boolean isEnabled()
    {
        String property = System.getProperty("ruoyi.port-guard.enabled");
        if (property != null)
        {
            return Boolean.parseBoolean(property);
        }

        String env = System.getenv("RUOYI_PORT_GUARD");
        return env == null || Boolean.parseBoolean(env);
    }

    private static int resolvePort(String[] args)
    {
        String fromArgs = findServerPortArg(args);
        if (fromArgs != null)
        {
            return parsePort(fromArgs);
        }

        String fromProperty = System.getProperty("server.port");
        if (fromProperty != null)
        {
            return parsePort(fromProperty);
        }

        String fromEnv = System.getenv("SERVER_PORT");
        if (fromEnv != null)
        {
            return parsePort(fromEnv);
        }

        return DEFAULT_PORT;
    }

    private static String findServerPortArg(String[] args)
    {
        if (args == null)
        {
            return null;
        }

        for (String arg : args)
        {
            String prefix = "--server.port=";
            if (arg != null && arg.startsWith(prefix))
            {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    private static int parsePort(String value)
    {
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (Exception ignored)
        {
            return DEFAULT_PORT;
        }
    }

    private static Set<Long> findListenerPids(int port)
    {
        if (isWindows())
        {
            return runPidCommand("powershell.exe", "-NoProfile", "-Command",
                    "$ErrorActionPreference='SilentlyContinue'; "
                            + "Get-NetTCPConnection -LocalPort " + port + " -State Listen "
                            + "| Select-Object -ExpandProperty OwningProcess -Unique");
        }

        return runPidCommand("sh", "-c", "lsof -tiTCP:" + port + " -sTCP:LISTEN 2>/dev/null || true");
    }

    private static Set<Long> runPidCommand(String... command)
    {
        Set<Long> pids = new LinkedHashSet<>();
        for (String line : runCommand(command).split("\\R"))
        {
            try
            {
                if (!line.isBlank())
                {
                    pids.add(Long.parseLong(line.trim()));
                }
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return pids;
    }

    private static String getCommandLine(long pid)
    {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isPresent())
        {
            Optional<String> commandLine = handle.get().info().commandLine();
            if (commandLine.isPresent())
            {
                return commandLine.get();
            }
        }

        if (!isWindows())
        {
            return "";
        }

        return runCommand("powershell.exe", "-NoProfile", "-Command",
                "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; "
                        + "$p=Get-CimInstance Win32_Process -Filter \"ProcessId=" + pid + "\"; "
                        + "if ($p) { $p.CommandLine }");
    }

    private static boolean isRuoyiAdmin(String commandLine)
    {
        if (commandLine == null)
        {
            return false;
        }

        String normalized = commandLine.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("com.ruoyi.admin.ruoyiadminapplication")
                || normalized.contains("ruoyi-admin.jar");
    }

    private static void stopProcess(long pid)
    {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty())
        {
            return;
        }

        ProcessHandle process = handle.get();
        process.destroy();
        try
        {
            process.onExit().get(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (Exception ignored)
        {
            if (process.isAlive())
            {
                process.destroyForcibly();
            }
        }
    }

    private static String runCommand(String... command)
    {
        StringBuilder output = new StringBuilder();
        try
        {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished)
            {
                process.destroyForcibly();
                return "";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    output.append(line).append(System.lineSeparator());
                }
            }
        }
        catch (Exception ignored)
        {
            return "";
        }
        return output.toString();
    }

    private static boolean isWindows()
    {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
