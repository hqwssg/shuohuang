[CmdletBinding()]
param(
    [int]$Port = 8081
)

$ErrorActionPreference = 'Stop'

$listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $listeners) {
    Write-Host "Port $Port is free."
    exit 0
}

$blocked = $false
foreach ($pidValue in ($listeners | Select-Object -ExpandProperty OwningProcess -Unique)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$pidValue" -ErrorAction SilentlyContinue
    $commandLine = if ($process) { $process.CommandLine } else { '' }

    if ($commandLine -match 'com\.ruoyi\.admin\.RuoYiAdminApplication|ruoyi-admin\.jar') {
        Stop-Process -Id $pidValue -Force
        Write-Host "Stopped previous ruoyi-admin process PID $pidValue on port $Port."
    } else {
        Write-Warning "Port $Port is used by PID $pidValue, not ruoyi-admin. Command: $commandLine"
        $blocked = $true
    }
}

if ($blocked) {
    exit 2
}

Start-Sleep -Seconds 1
exit 0
