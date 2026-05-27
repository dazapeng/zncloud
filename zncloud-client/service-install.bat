@echo off
setlocal enabledelayedexpansion

REM ============================================
REM ZNCloud Agent - Windows Service Installation
REM ============================================

set SERVICE_NAME=ZNCloudAgent
set SERVICE_DISPLAY_NAME="ZNCloud Agent"
set SERVICE_DESCRIPTION="ZNCloud Internet Cafe Client - Device management and remote access agent"
set BINARY_PATH="C:\Program Files\ZNCloud\zncloud-agent.exe"

echo ============================================
echo  ZNCloud Agent Service Installation
echo ============================================
echo.

REM Check for Administrator privileges
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] This script must be run as Administrator!
    echo Please right-click and select "Run as Administrator".
    pause
    exit /b 1
)

echo [INFO] Running with Administrator privileges

REM Check if binary exists at the target location
if not exist %BINARY_PATH% (
    echo [WARNING] Binary not found at %BINARY_PATH%
    echo.
    echo Please copy zncloud-agent.exe to C:\Program Files\ZNCloud\ before installing.
    echo.
    echo To create directory and copy:
    echo   mkdir "C:\Program Files\ZNCloud"
    echo   copy build\zncloud-agent.exe "C:\Program Files\ZNCloud\"
    echo.
    set /p CONTINUE=Continue anyway? (y/N): 
    if /i "!CONTINUE!" neq "Y" (
        echo Installation cancelled.
        pause
        exit /b 0
    )
)

REM Check if service already exists
sc query %SERVICE_NAME% >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [INFO] Service '%SERVICE_NAME%' already exists.
    echo.
    set /p REINSTALL=Do you want to stop, remove and reinstall? (y/N): 
    if /i "!REINSTALL!" neq "Y" (
        echo Installation cancelled.
        pause
        exit /b 0
    )
    
    echo [INFO] Stopping existing service...
    sc stop %SERVICE_NAME% >nul 2>&1
    timeout /t 3 /nobreak >nul
    
    echo [INFO] Removing existing service...
    sc delete %SERVICE_NAME% >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo [INFO] Existing service removed.
    echo.
)

echo [INFO] Creating service '%SERVICE_NAME%'...

REM Create the service
sc create %SERVICE_NAME% binPath= %BINARY_PATH% start= auto displayName= %SERVICE_DISPLAY_NAME%

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to create service. Error code: %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo [INFO] Service created successfully.

REM Set service description
sc description %SERVICE_NAME% %SERVICE_DESCRIPTION%

REM Configure service recovery options (restart on failure)
echo [INFO] Configuring recovery options...
sc failure %SERVICE_NAME% reset= 86400 actions= restart/5000/restart/10000/restart/30000

REM Set the failure actions flag
sc failureflag %SERVICE_NAME% 1

echo.
echo [SUCCESS] Service '%SERVICE_NAME%' has been installed!
echo.
echo ============================================
echo  Service Configuration
echo ============================================
echo  Name:        %SERVICE_NAME%
echo  Display:     %SERVICE_DISPLAY_NAME%
echo  Binary:      %BINARY_PATH%
echo  Start Type:  Automatic
echo  Recovery:    Restart on failure (3 attempts)
echo.
echo  To start the service now, run:
echo    sc start %SERVICE_NAME%
echo.
echo  Or use Services.msc (services.msc) GUI
echo.
echo  To uninstall later, run:
echo    sc stop %SERVICE_NAME%
echo    sc delete %SERVICE_NAME%
echo ============================================

pause
