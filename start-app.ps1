# One-click start for the Optical Shop backend + permanent Cloudflare tunnel.
# Your permanent URL never changes across restarts:
#   https://demo.register-form.com/request-form
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\start-app.ps1

$ErrorActionPreference = 'Continue'

$projectDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$tunnelConfig = "C:\Users\MSI\.cloudflared\config.yml"
$mvnw         = Join-Path $projectDir 'mvnw.cmd'

$permalink    = 'https://demo.register-form.com/request-form'
$qrUrl        = 'https://demo.register-form.com/api/qr/request'

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Optical Shop - Starting app + permanent tunnel"   -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

if (Test-Path $mvnw) {
    Write-Host "[1/2] Starting Spring Boot backend (mvnw spring-boot:run) ..." -ForegroundColor Yellow
    Start-Process cmd -ArgumentList '/c', "`"$mvnw`"", 'spring-boot:run'
} else {
    Write-Warning "mvnw.cmd not found at $mvnw - backend not started. Run it manually: ./mvnw spring-boot:run"
}

Start-Sleep -Seconds 3

if (Test-Path $tunnelConfig) {
    Write-Host "[2/2] Starting permanent Cloudflare tunnel (glasses-demo) ..." -ForegroundColor Yellow
    Start-Process cloudflared -ArgumentList 'tunnel', '--config', "`"$tunnelConfig`"", 'run', 'glasses-demo'
} else {
    Write-Warning "Tunnel config not found at $tunnelConfig - tunnel not started. Run manually: cloudflared tunnel run glasses-demo"
}

Write-Host ""
Write-Host "Permanent customer URL (QR points here): $permalink" -ForegroundColor Green
Write-Host "QR image endpoint (for printing):        $qrUrl"     -ForegroundColor Green
Write-Host ""
Write-Host "This URL stays the same every time you start. No new Cloudflare link needed." -ForegroundColor Green
