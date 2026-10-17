# Stops the services started by start-all.ps1. Pass -Infra to also stop the docker compose infrastructure.
param([switch]$Infra)

$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $root "logs\pids.txt"

if (Test-Path $pidFile) {
    foreach ($line in Get-Content $pidFile) {
        $name, $id = $line -split "="
        try { Stop-Process -Id ([int]$id) -Force -ErrorAction Stop; Write-Host "Stopped $name (pid $id)" }
        catch { Write-Host "$name (pid $id) was not running" }
    }
    Remove-Item $pidFile
} else {
    Write-Host "No logs\pids.txt - nothing started by start-all.ps1"
}

if ($Infra) {
    docker compose -f (Join-Path $root "docker-compose.yml") down
}
