@echo off
setlocal enabledelayedexpansion

REM ============================================
REM ZNCloud Agent - Windows Build Script
REM ============================================

set PROJECT_DIR=%~dp0
set OUTPUT_DIR=%PROJECT_DIR%build
set BINARY_NAME=zncloud-agent.exe

echo ============================================
echo  ZNCloud Agent Build Script
echo ============================================
echo.

REM Check for Go
where go >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Go is not installed or not in PATH.
    echo Please install Go 1.21+ from https://golang.org/dl/
    pause
    exit /b 1
)

REM Check Go version
for /f "tokens=3" %%i in ('go version') do set GOVERSION=%%i
echo [INFO] Go version: %GOVERSION%

REM Create output directory
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
echo [INFO] Output directory: %OUTPUT_DIR%

REM Clean previous build
if exist "%OUTPUT_DIR%\%BINARY_NAME%" (
    del /f /q "%OUTPUT_DIR%\%BINARY_NAME%"
    echo [INFO] Removed previous build
)

echo [INFO] Building ZNCloud Agent...
echo [INFO] Build flags: -s -w -H windowsgui
echo.

REM Build the binary
go build -o "%OUTPUT_DIR%\%BINARY_NAME%" -ldflags "-s -w -H windowsgui" .

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed with error code: %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCESS] Build completed successfully!
echo [INFO] Binary: %OUTPUT_DIR%\%BINARY_NAME%

REM Show file info
for %%f in ("%OUTPUT_DIR%\%BINARY_NAME%") do (
    echo [INFO] Size: %%~zf bytes
)

echo.
echo ============================================
echo  Build Summary
echo ============================================
echo  Binary: %OUTPUT_DIR%\%BINARY_NAME%
echo  Type:   Windows GUI (no console window)
echo.
echo  To install as a service, run: service-install.bat
echo ============================================

pause
