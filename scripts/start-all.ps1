# Starts infrastructure (docker compose) and all seven services in the background.
# Usage (from the repo root):  .\scripts\start-all.ps1          # build jars, then start
#                              .\scripts\start-all.ps1 -NoBuild # start existing jars
# Logs go to logs\<service>.log; process ids to logs\pids.txt (used by stop-all.ps1).
param([switch]$NoBuild)

# Native tools (docker, mvnw) write progress to stderr; with "Stop", Windows PowerShell 5.1 would
# treat that as a terminating error. Failures are detected through $LASTEXITCODE instead.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root "logs"
$services = "user-service", "market-data-service", "order-service", "portfolio-service", "notification-service", "account-service", "api-gateway"

New-Item -ItemType Directory -Force $logs | Out-Null

Write-Host "Starting infrastructure..."
docker compose -f (Join-Path $root "docker-compose.yml") up -d 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "docker compose failed - is Docker Desktop running?" }

if (-not $NoBuild) {
    foreach ($s in $services) {
        Write-Host "Building $s..."
        Push-Location (Join-Path $root $s)
        & .\mvnw.cmd -q package -DskipTests
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -ne 0) { throw "Build failed for $s" }
    }
}

$pids = @()
foreach ($s in $services) {
    $dir = Join-Path $root $s
    $jar = Get-ChildItem (Join-Path $dir "target\*.jar") | Where-Object { $_.Name -notlike "*plain*" } | Select-Object -First 1
    if (-not $jar) { throw "No jar for $s - run without -NoBuild" }

    # Started from the service directory so the ../.env import resolves.
    $p = Start-Process java -ArgumentList "-jar", $jar.FullName -WorkingDirectory $dir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs "$s.log") -RedirectStandardError (Join-Path $logs "$s.err.log") `
        -ErrorAction Stop
    $pids += "$s=$($p.Id)"
    Write-Host "Started $s (pid $($p.Id))"
}
$pids | Set-Content -Encoding utf8 (Join-Path $logs "pids.txt")

Write-Host "Waiting for services to report healthy..."
$ports = @{ "api-gateway" = 8080; "user-service" = 8081; "market-data-service" = 8082; "order-service" = 8083; "portfolio-service" = 8084; "notification-service" = 8085; "account-service" = 8086 }
foreach ($s in $services) {
    $ok = $false
    for ($i = 0; $i -lt 60 -and -not $ok; $i++) {
        try { $ok = (Invoke-RestMethod "http://localhost:$($ports[$s])/actuator/health" -TimeoutSec 2).status -eq "UP" } catch { Start-Sleep -Seconds 2 }
    }
    if ($ok) { Write-Host "  $s UP" } else { Write-Host "  $s NOT UP - check logs\$s.log" -ForegroundColor Red }
}
