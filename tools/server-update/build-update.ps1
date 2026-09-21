param([switch]$SkipBuild)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$Name = "server-update-$Stamp"
$Destination = Join-Path $Root "deploy-package/$Name"
$LogDir = Join-Path $Root "deploy-package/build-logs-$Stamp"
New-Item -ItemType Directory -Path $Destination, $LogDir | Out-Null

function Invoke-Build($Label, $Directory, $Executable, $Arguments) {
    Write-Host "Building $Label ..."
    Push-Location $Directory
    $Previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $Executable @Arguments *> (Join-Path $LogDir "$Label.log")
        $Code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $Previous
        Pop-Location
    }
    if ($Code -ne 0) { throw "$Label failed ($Code). See $LogDir/$Label.log" }
}

if (-not $SkipBuild) {
    $Maven = (Get-Command mvnd.exe).Source
    $Npm = (Get-Command npm.cmd).Source
    Invoke-Build 'main-backend' $Root $Maven @('-pl', 'ruoyi-admin', '-am', 'package')
    Invoke-Build 'model-backend' "$Root/tpfhs-9/backend" $Maven @('package')
    Invoke-Build 'goview-backend' "$Root/goview/go-view-serve-master" $Maven @('package')
    foreach ($Frontend in @('ruoyi-ui', 'tpfhs-9/frontend', 'goview/go-view-master-fetch')) {
        if (-not (Test-Path "$Root/$Frontend/node_modules")) {
            throw "Missing node_modules: $Frontend. Install dependencies before building."
        }
    }
    Invoke-Build 'main-frontend' "$Root/ruoyi-ui" $Npm @('run', 'build:prod')
    Invoke-Build 'model-frontend' "$Root/tpfhs-9/frontend" $Npm @('run', 'build', '--', '--base=/carbon-model/')
    $Previous = $env:VITE_PRO_PATH
    try {
        $env:VITE_PRO_PATH = ''
        Invoke-Build 'goview-frontend' "$Root/goview/go-view-master-fetch" $Npm @('run', 'build')
    } finally { $env:VITE_PRO_PATH = $Previous }
}

function Copy-Payload($Source, $Target) {
    $SourcePath = Join-Path $Root $Source
    $TargetPath = Join-Path $Destination $Target
    if (-not (Test-Path -LiteralPath $SourcePath)) { throw "Missing artifact: $SourcePath" }
    New-Item -ItemType Directory -Force -Path (Split-Path $TargetPath -Parent) | Out-Null
    Copy-Item -LiteralPath $SourcePath -Destination $TargetPath -Recurse
}

Copy-Payload 'ruoyi-admin/target/ruoyi-admin.jar' 'web-main/jar/ruoyi-admin.jar'
Copy-Payload 'tpfhs-9/backend/target/carbon-emission-model-1.0.0.jar' 'web-main/jar/carbon-emission-model.jar'
Copy-Payload 'goview/go-view-serve-master/target/goview_admin-0.0.1-SNAPSHOT.war' 'web-screen/jar/goview.war'
Copy-Payload 'ruoyi-ui/dist' 'web-main/html/dist'
Copy-Payload 'tpfhs-9/frontend/dist' 'web-main/html/carbon-model'
Copy-Payload 'goview/go-view-master-fetch/dist' 'web-screen/html/dist'
Copy-Payload 'docker/web-main/conf/nginx.conf' 'web-main/conf/nginx.conf'
Copy-Payload 'docker/web-screen/conf/nginx.conf' 'web-screen/conf/nginx.conf'
Copy-Payload 'tpfhs-9/backend/steam_calculator.py' 'web-main/python/steam_calculator.py'
Copy-Payload 'docker/web-main/requirements-cli.txt' 'web-main/requirements-cli.txt'

# Copy only renderer source/assets, never local generated outputs or Python caches.
$RendererRoot = Join-Path $Root 'carbon_report_agent'
Get-ChildItem -LiteralPath $RendererRoot -File -Recurse | Where-Object {
    $_.FullName -notmatch '[\\/]__pycache__[\\/]' -and $_.Extension -ne '.pyc'
} | ForEach-Object {
    $Relative = $_.FullName.Substring($RendererRoot.Length + 1)
    Copy-Payload "carbon_report_agent/$Relative" "web-main/renderer/carbon_report_agent/$Relative"
}

Copy-Payload 'sql/logging_migration.sql' 'mysql/migrations/10_logging.sql'
Copy-Payload 'sql/carbon-report/V1__report_tables.sql' 'mysql/migrations/20_report_tables.sql'
Copy-Payload 'sql/carbon_permission_model.sql' 'mysql/migrations/30_permissions.sql'
Copy-Payload 'sql/carbon-report/V11__shuohuang_project_integration.sql' 'mysql/migrations/40_report_integration.sql'
Copy-Payload 'tpfhs-9/backend/src/main/resources/schema-update-20260912.sql' 'mysql/migrations/50_factor_precision.sql'
Copy-Payload 'sql/carbon_collection_scope_permissions.sql' 'mysql/migrations/55_collection_scopes.sql'
Copy-Payload 'sql/carbon_project_cleanup_and_test_users.sql' 'mysql/optional/60_cleanup_and_test_users.sql'
Copy-Payload 'tools/server-update/deploy-update.sh' 'deploy-update.sh'
Copy-Payload 'tools/server-update/container-update.sh' 'scripts/container-update.sh'
Copy-Payload 'tools/server-update/runtime-common.sh' 'scripts/runtime-common.sh'
Copy-Payload 'tools/server-update/start-web.sh' 'scripts/start-web.sh'
Copy-Payload 'tools/server-update/start-screen.sh' 'scripts/start-screen.sh'
Copy-Payload 'docs/server-update-instructions.md' 'SERVER-UPDATE.md'

$Utf8 = New-Object System.Text.UTF8Encoding($false)
$Readme = Join-Path $Destination 'SERVER-UPDATE.md'
$Instructions = [IO.File]::ReadAllText($Readme).Replace('server-update-TIMESTAMP', $Name).Replace('build-logs-TIMESTAMP', "build-logs-$Stamp")
[IO.File]::WriteAllText($Readme, $Instructions, $Utf8)
$Integration = Join-Path $Destination 'mysql/migrations/40_report_integration.sql'
$Sql = [IO.File]::ReadAllText($Integration)
# JSON columns accept a JSON string literal on both MySQL and MariaDB.
$Sql = [regex]::Replace($Sql, "CAST\(('(?:[^']|'')*') AS JSON\)", '$1')
[IO.File]::WriteAllText($Integration, $Sql, $Utf8)
Get-ChildItem $Destination -File -Recurse | Where-Object { $_.Extension -in @('.sh', '.sql') } | ForEach-Object {
    $Content = [IO.File]::ReadAllText($_.FullName).Replace("`r`n", "`n")
    [IO.File]::WriteAllText($_.FullName, $Content, $Utf8)
}

$ModelHtml = [IO.File]::ReadAllText("$Destination/web-main/html/carbon-model/index.html")
if ($ModelHtml -notmatch '/carbon-model/assets/') { throw 'Model frontend has an incorrect production base path.' }
$Files = Get-ChildItem $Destination -File -Recurse | Sort-Object FullName
$Manifest = @(
    ('BuiltAt=' + (Get-Date -Format 'o'))
    ('GitCommit=' + (& git -C $Root rev-parse HEAD))
    'IncludesUncommittedWorktree=true'
)
foreach ($File in $Files) {
    $Relative = $File.FullName.Substring($Destination.Length + 1).Replace('\', '/')
    $Manifest += "$( (Get-FileHash -LiteralPath $File.FullName -Algorithm SHA256).Hash.ToLower() )  $Relative"
}
[IO.File]::WriteAllText("$Destination/MANIFEST.txt", ($Manifest -join "`n") + "`n", $Utf8)
$Checksums = $Manifest | Select-Object -Skip 3
[IO.File]::WriteAllText("$Destination/SHA256SUMS", ($Checksums -join "`n") + "`n", $Utf8)
$Archive = "$Destination.tar.gz"
& tar.exe -czf $Archive -C (Split-Path $Destination -Parent) $Name
if ($LASTEXITCODE -ne 0) { throw 'Archive creation failed.' }
$Hash = (Get-FileHash $Archive -Algorithm SHA256).Hash.ToLower()
[IO.File]::WriteAllText("$Archive.sha256", "$Hash  $Name.tar.gz`n", $Utf8)
Write-Host "PACKAGE=$Archive"
Write-Host "DIRECTORY=$Destination"
Write-Host "LOGS=$LogDir"
Write-Host "SHA256=$Hash"
