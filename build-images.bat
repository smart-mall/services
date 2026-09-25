@echo off
setlocal EnableExtensions
title Gulimall - Build and Deploy

rem ============================================================
rem  Double-click this file for a one-shot rebuild + redeploy:
rem    1. backend images : build-images.ps1  (builds every gl-* image)
rem    2. admin SPA      : ..\grain-mall-front-end -> grain-mall-admin:latest
rem    3. user  SPA      : ..\user-vue            -> grain-mall-user:latest
rem    4. docker compose : gl-com.yml up -d
rem
rem  Optional arguments are passed through to build-images.ps1:
rem      build-images.bat -Only gl-product,gl-cart
rem      build-images.bat -Tag 0.0.1
rem
rem  IMPORTANT: keep this file ASCII-only and CRLF-terminated.
rem  cmd.exe decodes .bat files with the console code page (GBK on
rem  Chinese Windows), so UTF-8 text can be mis-parsed and break it.
rem ============================================================

set "RC=1"
cd /d "%~dp0"

echo.
echo ==========================================================
echo   Gulimall - build all images and redeploy
echo   Project : %~dp0
echo   Backend : build-images.ps1
echo   Admin   : ..\grain-mall-front-end
echo   User    : ..\user-vue
echo   Compose : gl-com.yml
echo ==========================================================
echo.

if not exist "build-images.ps1" (
    echo [ERROR] build-images.ps1 was not found next to this file.
    echo         Expected: %~dp0build-images.ps1
    goto :done
)

if not exist "gl-com.yml" (
    echo [ERROR] gl-com.yml was not found next to this file.
    echo         Expected: %~dp0gl-com.yml
    goto :done
)

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] The "docker" command was not found.
    echo         Install Docker Desktop, then run this script again.
    goto :done
)

echo [0/4] Checking Docker engine ...
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

echo [1/4] Building backend images (first run downloads Maven dependencies) ...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-images.ps1" %*
if errorlevel 1 goto :failed
echo.

echo [2/4] Building admin SPA image ^(grain-mall-admin:latest^) ...
docker build -t grain-mall-admin:latest ..\grain-mall-front-end
if errorlevel 1 goto :failed
echo.

echo [3/4] Building user SPA image ^(grain-mall-user:latest^) ...
docker build -t grain-mall-user:latest ..\user-vue
if errorlevel 1 goto :failed
echo.

echo [4/4] Starting containers ^(docker compose -f gl-com.yml up -d^) ...
docker compose -f gl-com.yml up -d
if errorlevel 1 goto :failed
echo.

set "RC=0"
echo ==========================================================
echo   ALL DONE
echo   Gateway : http://localhost:53000/api
echo   Admin   : http://localhost:56731
echo   User    : http://localhost:56732
echo ==========================================================
goto :done

:failed
echo.
echo ==========================================================
echo   FINISHED WITH ERRORS
echo   Scroll up to see which step failed.
echo ==========================================================

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
