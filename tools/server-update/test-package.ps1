param([Parameter(Mandatory=$true)][string]$Directory)
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Directory).Path
if ([IO.File]::ReadAllText("$Root/SHA256SUMS").Contains("`r")) { throw 'SHA256SUMS must use Linux LF line endings.' }
$Expected = [IO.File]::ReadAllLines("$Root/SHA256SUMS")
$Files = Get-ChildItem -LiteralPath $Root -File -Recurse | Where-Object { $_.Name -notin @('SHA256SUMS', 'MANIFEST.txt') }
if ($Expected.Count -ne $Files.Count) { throw 'Checksum coverage does not match payload file count.' }
foreach ($Line in $Expected) {
    if ($Line -notmatch '^([0-9a-f]{64})  (.+)$') { throw 'Invalid SHA256 record.' }
    $Hash = $Matches[1]
    $Path = [IO.Path]::GetFullPath((Join-Path $Root $Matches[2]))
    if (-not $Path.StartsWith($Root + [IO.Path]::DirectorySeparatorChar)) { throw 'Unsafe manifest path.' }
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLower() -ne $Hash) { throw "Hash mismatch: $Path" }
}
$Forbidden = $Files | Where-Object {
    $_.Name -eq 'goview.db' -or $_.Extension -eq '.pyc' -or
    $_.FullName -match '__pycache__|report-work|carbon_emissions.sql|schema_1.sql|[\\/]\.env$'
}
if ($Forbidden) { throw 'Production state or generated cache leaked into package.' }
$CheckedAssets = 0
$AssetPattern = @'
(?:src|href)\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s>]+))
'@
foreach ($Page in @(
    @('web-main/html/dist', ''),
    @('web-main/html/carbon-model', '/carbon-model/'),
    @('web-screen/html/dist', '')
)) {
    $PageRoot = Join-Path $Root $Page[0]
    $Html = [IO.File]::ReadAllText("$PageRoot/index.html")
    foreach ($Match in [regex]::Matches($Html, $AssetPattern.Trim())) {
        $Url = ($Match.Groups | Select-Object -Skip 1 | Where-Object Success | Select-Object -First 1).Value
        if ($Url -match '^(https?:|//|data:)' -or $Url -notmatch '\.(js|css|ico|png|jpg)(\?|$)') { continue }
        $Url = ($Url -split '\?')[0]
        if ($Page[1] -and $Url.StartsWith($Page[1])) { $Url = $Url.Substring($Page[1].Length) }
        $Asset = Join-Path $PageRoot $Url.TrimStart('/')
        if (-not (Test-Path -LiteralPath $Asset -PathType Leaf)) { throw "Missing HTML asset: $Asset" }
        $CheckedAssets++
    }
}
if ([IO.File]::ReadAllText("$Root/mysql/migrations/40_report_integration.sql") -match 'CAST\(.+ AS JSON\)') {
    throw 'MariaDB-incompatible JSON cast remains.'
}
$Archive = "$Root.tar.gz"
$Sidecar = [IO.File]::ReadAllText("$Archive.sha256").Split(' ')[0]
if ((Get-FileHash $Archive -Algorithm SHA256).Hash.ToLower() -ne $Sidecar) { throw 'Archive checksum mismatch.' }
$Entries = & tar.exe -tzf $Archive
if ($LASTEXITCODE -ne 0 -or -not $Entries) { throw 'Archive is unreadable.' }
$Prefix = (Split-Path $Root -Leaf) + '/'
if ($Entries | Where-Object { -not $_.StartsWith($Prefix) -or $_ -match '(^|/)\.\.(/|$)' }) { throw 'Unsafe archive structure.' }
"PASS: $($Files.Count) payload hashes, $CheckedAssets HTML assets, archive structure/hash, no local state, MariaDB JSON compatibility."
