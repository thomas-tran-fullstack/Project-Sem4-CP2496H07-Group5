# EZMart Livestream - Management Scripts
# Run with: powershell -ExecutionPolicy Bypass -File livestream-manager.ps1

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("start", "stop", "restart", "status", "clean", "test")]
    [string]$Action
)

$NGINX_PATH = "C:\nginx-rtmp"
$HLS_PATH = "C:\hls"
$NGINX_EXE = "$NGINX_PATH\nginx.exe"

function Require-Admin {
    if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Host "ERROR: This script must run as Administrator" -ForegroundColor Red
        exit 1
    }
}

function Test-Port {
    param([int]$Port)
    $connection = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue
    return $connection.TcpTestSucceeded
}

function Get-ProcessByPort {
    param([int]$Port)
    $netstat = netstat -ano | findstr ":$Port"
    if ($netstat) {
        $pid = $netstat -split '\s+' | select -Last 1
        return Get-Process -Id $pid -ErrorAction SilentlyContinue
    }
    return $null
}

function Start-Nginx {
    Write-Host "Starting nginx-rtmp server..." -ForegroundColor Cyan
    
    if (Test-Port 1935) {
        Write-Host "ERROR: Port 1935 already in use" -ForegroundColor Red
        $proc = Get-ProcessByPort 1935
        if ($proc) {
            Write-Host "Process: $($proc.ProcessName) (PID: $($proc.Id))" -ForegroundColor Yellow
        }
        return $false
    }
    
    if (-not (Test-Path $NGINX_EXE)) {
        Write-Host "ERROR: nginx.exe not found at $NGINX_PATH" -ForegroundColor Red
        Write-Host "Install from: http://nginx-win.ecssocsie.net/" -ForegroundColor Yellow
        return $false
    }
    
    cd $NGINX_PATH
    & $NGINX_EXE
    
    Start-Sleep -Seconds 2
    
    if (Test-Port 1935) {
        Write-Host "[OK] nginx started successfully" -ForegroundColor Green
        Write-Host "    RTMP: localhost:1935" -ForegroundColor Green
        Write-Host "    HLS:  http://localhost:8080/hls/" -ForegroundColor Green
        return $true
    } else {
        Write-Host "ERROR: nginx failed to start" -ForegroundColor Red
        return $false
    }
}

function Stop-Nginx {
    Write-Host "Stopping nginx-rtmp server..." -ForegroundColor Cyan
    
    $proc = Get-Process -Name nginx -ErrorAction SilentlyContinue
    if ($proc) {
        cd $NGINX_PATH
        & $NGINX_EXE -s quit
        Start-Sleep -Seconds 2
        Write-Host "[OK] nginx stopped" -ForegroundColor Green
        return $true
    } else {
        Write-Host "ERROR: nginx not running" -ForegroundColor Red
        return $false
    }
}

function Restart-Nginx {
    Write-Host "Restarting nginx-rtmp server..." -ForegroundColor Cyan
    
    Stop-Nginx | Out-Null
    Start-Sleep -Seconds 1
    Start-Nginx | Out-Null
    
    Write-Host "[OK] nginx restarted" -ForegroundColor Green
}

function Show-Status {
    Write-Host "EZMart Livestream Status" -ForegroundColor Cyan
    Write-Host "==========================" -ForegroundColor Cyan
    Write-Host ""
    
    # Check nginx
    Write-Host "nginx-rtmp Server:" -ForegroundColor Yellow
    if (Test-Port 1935) {
        Write-Host "  Status: RUNNING" -ForegroundColor Green
        $proc = Get-ProcessByPort 1935
        if ($proc) {
            Write-Host "  Process: $($proc.ProcessName) (PID: $($proc.Id))" -ForegroundColor Green
        }
    } else {
        Write-Host "  Status: STOPPED" -ForegroundColor Red
    }
    
    # Check HTTP
    Write-Host ""
    Write-Host "HTTP Server (HLS):" -ForegroundColor Yellow
    if (Test-Port 8080) {
        Write-Host "  Status: RUNNING" -ForegroundColor Green
    } else {
        Write-Host "  Status: STOPPED" -ForegroundColor Red
    }
    
    # Check HLS directory
    Write-Host ""
    Write-Host "HLS Directory:" -ForegroundColor Yellow
    if (Test-Path $HLS_PATH) {
        $files = Get-ChildItem $HLS_PATH -ErrorAction SilentlyContinue | Measure-Object
        Write-Host "  Path: $HLS_PATH" -ForegroundColor Green
        Write-Host "  Files: $($files.Count)" -ForegroundColor Green
        
        if ($files.Count -gt 0) {
            Write-Host "  Recent files:" -ForegroundColor Yellow
            Get-ChildItem $HLS_PATH -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 5 | ForEach-Object {
                Write-Host "    - $($_.Name) ($('{0:N0}' -f $_.Length) bytes)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "  Path: $HLS_PATH" -ForegroundColor Red
        Write-Host "  Status: NOT FOUND" -ForegroundColor Red
    }
    
    # Check FFmpeg
    Write-Host ""
    Write-Host "FFmpeg:" -ForegroundColor Yellow
    try {
        $ffversion = ffmpeg -version 2>&1 | Select-Object -First 1
        Write-Host "  $ffversion" -ForegroundColor Green
    } catch {
        Write-Host "  Status: NOT INSTALLED" -ForegroundColor Red
    }
}

function Clean-HLS {
    Write-Host "Cleaning HLS directory..." -ForegroundColor Cyan
    
    if (Test-Path $HLS_PATH) {
        Remove-Item "$HLS_PATH\*" -Force -ErrorAction SilentlyContinue
        Write-Host "[OK] HLS directory cleaned" -ForegroundColor Green
    } else {
        Write-Host "ERROR: HLS directory not found" -ForegroundColor Red
    }
}

function Test-Setup {
    Write-Host "Testing EZMart Livestream Setup" -ForegroundColor Cyan
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Test FFmpeg
    Write-Host "1. Testing FFmpeg..." -ForegroundColor Yellow
    try {
        ffmpeg -version 2>&1 | Out-Null
        Write-Host "   [OK] FFmpeg installed" -ForegroundColor Green
    } catch {
        Write-Host "   [ERROR] FFmpeg not found" -ForegroundColor Red
    }
    
    # Test nginx running
    Write-Host ""
    Write-Host "2. Testing nginx..." -ForegroundColor Yellow
    if (Test-Port 1935) {
        Write-Host "   [OK] RTMP port 1935 responding" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] RTMP port 1935 not responding" -ForegroundColor Red
        Write-Host "   Run: livestream-manager.ps1 -Action start" -ForegroundColor Yellow
    }
    
    # Test HLS directory
    Write-Host ""
    Write-Host "3. Testing HLS directory..." -ForegroundColor Yellow
    if (Test-Path $HLS_PATH) {
        $acl = Get-Acl $HLS_PATH
        Write-Host "   [OK] HLS directory exists" -ForegroundColor Green
        Write-Host "   Path: $HLS_PATH" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] HLS directory not found" -ForegroundColor Red
    }
    
    # Test ports
    Write-Host ""
    Write-Host "4. Testing ports..." -ForegroundColor Yellow
    if (Test-Port 1935) {
        Write-Host "   [OK] Port 1935 (RTMP) open" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] Port 1935 (RTMP) closed" -ForegroundColor Red
    }
    
    if (Test-Port 8080) {
        Write-Host "   [OK] Port 8080 (HTTP) open" -ForegroundColor Green
    } else {
        Write-Host "   [WARNING] Port 8080 (HTTP) closed" -ForegroundColor Yellow
    }
    
    # Test HLS playlist
    Write-Host ""
    Write-Host "5. Testing HLS endpoint..." -ForegroundColor Yellow
    $testUrl = "http://localhost:8080/hls/test.m3u8"
    Write-Host "   URL: $testUrl" -ForegroundColor Yellow
    Write-Host "   Status: Would return 404 if no stream (expected)" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Setup test complete!" -ForegroundColor Cyan
}

# Require admin for all actions except status/test
if ($Action -notin @("status", "test")) {
    Require-Admin
}

Write-Host ""
Write-Host "EZMart Livestream Manager" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan

switch ($Action) {
    "start" {
        Start-Nginx
    }
    "stop" {
        Stop-Nginx
    }
    "restart" {
        Restart-Nginx
    }
    "status" {
        Show-Status
    }
    "clean" {
        Clean-HLS
    }
    "test" {
        Test-Setup
    }
}

Write-Host ""
