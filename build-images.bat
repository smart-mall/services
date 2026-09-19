@echo off
setlocal EnableExtensions
title Gulimall - Build Docker Images

rem ============================================================
rem  Double-click this file to build all gl-* images in one go.
rem  It is only a wrapper around build-images.ps1, so keep both
rem  files in the same folder (the project root).
rem
rem  Optional arguments are passed through, for example:
rem      build-images.bat -Tag 0.0.1
rem      build-images.bat -Only gl-product,gl-cart
rem
rem  IMPORTANT: keep this file ASCII-only and CRLF-terminated.
rem  cmd.exe decodes .bat files with the console code page (GBK on
rem  Chinese Windows), so UTF-8 text can be mis-parsed and break it.
rem ============================================================

set "RC=1"
set "ROOT=%~dp0"
set "PS1=%ROOT%build-images.ps1"

echo.
echo ==========================================================
echo   Gulimall - build all Docker images  ^(gl-*^)
echo   Project: %ROOT%
echo ==========================================================
echo.

if not exist "%PS1%" (
    echo [ERROR] build-images.ps1 was not found next to this file.
    echo         Expected: %PS1%
    echo         Keep both files in the project root directory.
    goto :done
)

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] The "docker" command was not found.
    echo         Install Docker Desktop, then run this script again.
    goto :done
)

echo [1/2] Checking Docker engine ...
docker info >nul 2>&1
if errorlevel 1 (
    echo       Docker is not running. Trying to start Docker Desktop ...
    call :start_docker
    docker info >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Docker engine is still not available.
        echo         Start Docker Desktop manually, wait until it says
        echo         "Engine running", then run this script again.
        goto :done
    )
)
echo       Docker is ready.
echo.

echo [2/2] Building images (first run downloads Maven dependencies) ...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%" %*
set "RC=%ERRORLEVEL%"
echo.

if "%RC%"=="0" (
    echo ==========================================================
    echo   ALL DONE
    echo ==========================================================
) else (
    echo ==========================================================
    echo   FINISHED WITH ERRORS  ^(exit code %RC%^)
    echo   Scroll up to see which image failed.
    echo ==========================================================
)

:done
echo.
pause
endlocal & exit /b %RC%


rem ---------- helper: start Docker Desktop and wait for the engine ----------
:start_docker
set "DD=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
if not exist "%DD%" set "DD=%LOCALAPPDATA%\Docker\Docker Desktop.exe"
if not exist "%DD%" exit /b 1

start "" "%DD%"
rem "timeout" needs an interactive console, "ping" works everywhere
for /l %%i in (1,1,60) do (
    ping -n 4 127.0.0.1 >nul 2>&1
    docker info >nul 2>&1
    if not errorlevel 1 exit /b 0
)
exit /b 1
