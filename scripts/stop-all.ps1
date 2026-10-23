# Stops the services started by start-all.ps1. Pass -Infra to also stop the docker compose infrastructure.
param([switch]$Infra)

$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $root "logs\pids.txt"

if (Test-Path $pidFile) {
    foreach ($line in Get-Content $pidFile) {
        $name, $id = $line -split "="
        # taskkill /T stops the process tree: on some machines "java" is a launcher shim that starts the real JVM as a
        # child, and Stop-Process on the shim alone would leave that JVM running (and its jar locked).
        & taskkill /PID $id /T /F *> $null
        if ($LASTEXITCODE -eq 0) { Write-Host "Stopped $name (pid $id and its child processes)" }
        else { Write-Host "$name (pid $id) was not running" }
    }
    Remove-Item $pidFile
} else {
    Write-Host "No logs\pids.txt - nothing started by start-all.ps1"
}

if ($Infra) {
    docker compose -f (Join-Path $root "docker-compose.yml") down
}
