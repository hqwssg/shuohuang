$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$logs = Join-Path $root ('.runtime/logs/start-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$java = (Get-Command java.exe).Source
$node = (Get-Command node.exe).Source
$python = 'C:\Users\30496\ai-infra\.venv\Scripts\python.exe'
$env:GOVIEW_AUDIT_SECRET = 'local-integration-secret'
$env:RUOYI_SECURITY_ENDPOINT = 'http://127.0.0.1:8081/internal/security/context'
$env:RUOYI_AUDIT_ENDPOINT = 'http://127.0.0.1:8081/internal/carbon/audit/operation'
$env:RUOYI_AUDIT_URL = 'http://127.0.0.1:8081/internal/goview/audit'
$env:CARBON_REPORT_PYTHON = $python
$env:CARBON_REPORT_RENDERER_ROOT = $root
$env:CARBON_REPORT_WORK_DIR = Join-Path $root 'report-work'
$env:CARBON_REPORT_ARTIFACT_DIR = Join-Path $root 'uploadPath/reports'
$env:PYTHON_PATH = $python
$env:STEAM_CALCULATOR_SCRIPT = Join-Path $root 'tpfhs-9/backend/steam_calculator.py'
$env:BROWSER = 'none'

function Start-ServiceProcess($name, $port, $executable, $directory, $arguments) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        Write-Output "$name already listening on $port"
        return
    }
    $process = Start-Process -FilePath $executable -ArgumentList $arguments -WorkingDirectory (Join-Path $root $directory) -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs "$name.out.log") -RedirectStandardError (Join-Path $logs "$name.err.log") -PassThru
    Write-Output "$name PID=$($process.Id) PORT=$port"
}

Start-ServiceProcess 'main-backend' 8081 $java 'ruoyi-admin' '-jar target/ruoyi-admin.jar --server.port=8081'
Start-ServiceProcess 'model-backend' 8082 $java 'tpfhs-9/backend' '-jar target/carbon-emission-model-1.0.0.jar --spring.profiles.active=local --server.port=8082'
Start-ServiceProcess 'goview-backend' 8083 $java 'goview/go-view-serve-master' '-jar target/goview_admin-0.0.1-SNAPSHOT.war --server.port=8083'
Start-ServiceProcess 'main-frontend' 80 $node 'ruoyi-ui' 'node_modules/@vue/cli-service/bin/vue-cli-service.js serve --host 127.0.0.1 --port 80'
Start-ServiceProcess 'model-frontend' 5177 $node 'tpfhs-9/frontend' 'node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5177 --strictPort'
Start-ServiceProcess 'goview-frontend' 3000 $node 'goview/go-view-master-fetch' 'node_modules/vite/bin/vite.js --host 127.0.0.1 --port 3000 --strictPort'
Write-Output "Logs: $logs"
