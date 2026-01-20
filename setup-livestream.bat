@echo off
REM EZMart Livestream - FFmpeg & nginx Setup Script for Windows
REM Run as Administrator

echo.
echo ========================================
echo EZMart Livestream Setup
echo ========================================
echo.

REM Check if running as admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: This script must run as Administrator
    echo Please right-click and choose "Run as Administrator"
    pause
    exit /b 1
)

setlocal enabledelayedexpansion

REM Set installation paths
set FFMPEG_PATH=C:\ffmpeg
set NGINX_PATH=C:\nginx-rtmp
set HLS_PATH=C:\hls

echo.
echo Step 1: Checking FFmpeg Installation
echo ======================================
echo.

if exist "%FFMPEG_PATH%\bin\ffmpeg.exe" (
    echo [OK] FFmpeg found at %FFMPEG_PATH%
    ffmpeg -version | findstr /R "ffmpeg version"
) else (
    echo [WARNING] FFmpeg not found at %FFMPEG_PATH%
    echo.
    echo ACTION REQUIRED:
    echo 1. Download from: https://www.gyan.dev/ffmpeg/builds/
    echo 2. Extract to: C:\ffmpeg\
    echo 3. Add C:\ffmpeg\bin to Windows PATH
    echo 4. Restart PowerShell
    echo 5. Run this script again
    echo.
    pause
    exit /b 1
)

REM Verify FFmpeg in PATH
ffmpeg -version >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERROR] FFmpeg not in system PATH
    echo Fix: Add C:\ffmpeg\bin to Environment Variables
    pause
    exit /b 1
)

echo.
echo Step 2: Checking nginx-rtmp Installation
echo ==========================================
echo.

if exist "%NGINX_PATH%\nginx.exe" (
    echo [OK] nginx-rtmp found at %NGINX_PATH%
) else (
    echo [WARNING] nginx-rtmp not found at %NGINX_PATH%
    echo.
    echo ACTION REQUIRED:
    echo 1. Download from: http://nginx-win.ecssocsie.net/
    echo 2. Extract to: C:\nginx-rtmp\
    echo 3. Run this script again
    echo.
    pause
    exit /b 1
)

echo.
echo Step 3: Creating HLS Output Directory
echo ======================================
echo.

if not exist "%HLS_PATH%" (
    mkdir "%HLS_PATH%"
    echo [OK] Created HLS directory at %HLS_PATH%
    
    REM Set permissions
    icacls "%HLS_PATH%" /grant "Everyone":(OI)(CI)F >nul 2>&1
    echo [OK] Set directory permissions
) else (
    echo [OK] HLS directory already exists at %HLS_PATH%
)

echo.
echo Step 4: Checking RTMP Port (1935)
echo ==================================
echo.

netstat -ano | findstr ":1935" >nul 2>&1
if %errorLevel% equ 0 (
    echo [WARNING] Port 1935 is already in use
    echo This might be nginx already running
    netstat -ano | findstr ":1935"
) else (
    echo [OK] Port 1935 is available
)

echo.
echo Step 5: Checking HTTP Port (8080)
echo ==================================
echo.

netstat -ano | findstr ":8080" >nul 2>&1
if %errorLevel% equ 0 (
    echo [WARNING] Port 8080 is already in use
    netstat -ano | findstr ":8080"
) else (
    echo [OK] Port 8080 is available
)

echo.
echo Step 6: Checking nginx Configuration
echo =====================================
echo.

if exist "%NGINX_PATH%\conf\nginx.conf" (
    echo [OK] nginx.conf found
    echo.
    echo Current RTMP configuration:
    findstr /R "application live" "%NGINX_PATH%\conf\nginx.conf"
) else (
    echo [ERROR] nginx.conf not found
    echo Check your nginx installation
    pause
    exit /b 1
)

echo.
echo Step 7: Checking Firewall Rules
echo ================================
echo.

netsh advfirewall firewall show rule name="RTMP" >nul 2>&1
if %errorLevel% equ 0 (
    echo [OK] RTMP firewall rule exists
) else (
    echo [INFO] Creating RTMP firewall rule...
    netsh advfirewall firewall add rule name="RTMP" dir=in action=allow protocol=tcp localport=1935 >nul 2>&1
    echo [OK] RTMP firewall rule created
)

netsh advfirewall firewall show rule name="HLS" >nul 2>&1
if %errorLevel% equ 0 (
    echo [OK] HLS firewall rule exists
) else (
    echo [INFO] Creating HLS firewall rule...
    netsh advfirewall firewall add rule name="HLS" dir=in action=allow protocol=tcp localport=8080 >nul 2>&1
    echo [OK] HLS firewall rule created
)

echo.
echo ========================================
echo All Checks Complete!
echo ========================================
echo.
echo Summary:
echo - FFmpeg: OK
echo - nginx-rtmp: OK
echo - HLS Directory: %HLS_PATH%
echo - RTMP Port: 1935
echo - HTTP Port: 8080
echo - Firewall Rules: Configured
echo.
echo Next Steps:
echo 1. Start nginx: cd %NGINX_PATH% ^&^& start nginx.exe
echo 2. Configure OBS with RTMP settings
echo 3. Create livestream session in EZMart admin panel
echo 4. Start broadcasting from OBS
echo 5. Verify in EZMart customer app
echo.
echo For detailed setup guide, read:
echo - FFMPEG_SETUP_GUIDE.md
echo - OBS_CONFIGURATION_GUIDE.md
echo.
pause
