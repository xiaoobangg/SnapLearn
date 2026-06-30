# ============================================
# SnapLearn local observability stack control script (Windows PowerShell)
# Manages Prometheus + Grafana via observability/docker-compose.yml
#
# Usage (run inside observability/ directory):
#   .\restart.ps1                    Restart all services
#   .\restart.ps1 prometheus         Restart Prometheus only
#   .\restart.ps1 grafana            Restart Grafana only
#   .\restart.ps1 recreate           down + up (volume kept)
#   .\restart.ps1 start              Start services
#   .\restart.ps1 stop               Stop services
#   .\restart.ps1 down               Stop and remove containers (volume kept)
#   .\restart.ps1 status             Show container status
#   .\restart.ps1 logs prometheus    Tail logs of a service
#
# If blocked by execution policy:
#   PowerShell -ExecutionPolicy Bypass -File .\restart.ps1
# Or unlock for current user permanently:
#   Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
# ============================================

param(
    [Parameter(Position = 0)]
    [string]$Action = 'restart-all',

    [Parameter(Position = 1)]
    [string]$Service = ''
)

$ErrorActionPreference = 'Stop'

# cd into the script's own dir so docker compose finds docker-compose.yml
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Push-Location $ScriptDir

try {
    if (-not (Test-Path 'docker-compose.yml')) {
        Write-Host "docker-compose.yml not found (script must live next to it)" -ForegroundColor Red
        exit 1
    }

    switch -Regex ($Action.ToLower()) {
        '^(|restart-all)$' {
            Write-Host "==> Restart all observability services" -ForegroundColor Cyan
            docker compose restart
            break
        }

        '^(prometheus|grafana)$' {
            Write-Host "==> Restart $Action" -ForegroundColor Cyan
            docker compose restart $Action
            break
        }

        '^recreate$' {
            Write-Host "==> Recreate (down then up, volumes kept)" -ForegroundColor Cyan
            docker compose down
            docker compose up -d --remove-orphans
            break
        }

        '^start$' {
            Write-Host "==> Start all" -ForegroundColor Cyan
            docker compose up -d
            break
        }

        '^stop$' {
            Write-Host "==> Stop all (containers kept)" -ForegroundColor Yellow
            docker compose stop
            break
        }

        '^down$' {
            Write-Host "==> Down (containers removed, volumes kept)" -ForegroundColor Yellow
            docker compose down
            exit 0
        }

        '^status$' {
            docker compose ps
            exit 0
        }

        '^logs$' {
            if ([string]::IsNullOrWhiteSpace($Service)) {
                Write-Host "Usage: .\restart.ps1 logs <prometheus|grafana>" -ForegroundColor Red
                exit 1
            }
            docker compose logs -f --tail=100 $Service
            exit 0
        }

        default {
            Write-Host "Unknown action: $Action" -ForegroundColor Red
            Write-Host ""
            Write-Host "Usage:"
            Write-Host "  .\restart.ps1                       Restart all services"
            Write-Host "  .\restart.ps1 prometheus            Restart Prometheus only"
            Write-Host "  .\restart.ps1 grafana               Restart Grafana only"
            Write-Host "  .\restart.ps1 recreate              Down + up (volume kept)"
            Write-Host "  .\restart.ps1 start                 Start"
            Write-Host "  .\restart.ps1 stop                  Stop"
            Write-Host "  .\restart.ps1 down                  Stop and remove containers"
            Write-Host "  .\restart.ps1 status                Show status"
            Write-Host "  .\restart.ps1 logs <service>        Tail logs"
            exit 1
        }
    }

    Write-Host ""
    Write-Host "==> Wait 5 seconds then check status..." -ForegroundColor Cyan
    Start-Sleep -Seconds 5

    Write-Host ""
    Write-Host "Container status:" -ForegroundColor Cyan
    docker compose ps

    function Test-Endpoint([string]$Name, [string]$Url) {
        Write-Host "$Name health:" -ForegroundColor Cyan -NoNewline
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            Write-Host "  OK ($($resp.StatusCode))" -ForegroundColor Green
        }
        catch {
            Write-Host "  starting or unreachable" -ForegroundColor Yellow
        }
    }

    Write-Host ""
    $isPrometheusAffected = $Action -match '^(|restart-all|recreate|start|prometheus)$'
    $isGrafanaAffected    = $Action -match '^(|restart-all|recreate|start|grafana)$'

    if ($isPrometheusAffected) {
        Test-Endpoint 'Prometheus' 'http://localhost:9090/-/healthy'
    }
    if ($isGrafanaAffected) {
        Test-Endpoint 'Grafana   ' 'http://localhost:3100/api/health'
    }

    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host " Done"                                       -ForegroundColor Green
    Write-Host " Prometheus:  http://localhost:9090"         -ForegroundColor Green
    Write-Host " Grafana:     http://localhost:3100  (admin / admin)" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
}
finally {
    Pop-Location
}
