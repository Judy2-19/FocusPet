@echo off
setlocal
chcp 65001 >nul

set "PORT=8090"
set "SERVER_DIR=%~dp0..\server"

echo ============================================
echo   FocusPets backend start (port %PORT%)
echo ============================================

pushd "%SERVER_DIR%" || (
    echo [ERROR] server dir not found: %SERVER_DIR%
    goto :END
)

echo [1/3] checking port %PORT% for old processes...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
    echo     killing old PID=%%a
    taskkill /F /PID %%a >nul 2>&1
)

echo [2/3] adb reverse forward (optional, for real device)...
set "ADB="
where adb >nul 2>&1
if not errorlevel 1 (
    set "ADB=adb"
) else (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
        if defined ANDROID_HOME (
            if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
                set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
            )
        )
    )
)
if defined ADB (
    %ADB% reverse tcp:%PORT% tcp:%PORT% >nul 2>&1
    if not errorlevel 1 (echo     adb reverse ok) else (echo     no device connected, skip)
) else (
    echo     adb not found, skip
)

echo [3/3] finding a working Python (skip broken installs)...
set "PY="
REM 3a - WorkBuddy managed Python, bundled, known good on this machine
if exist "C:\Users\13671\.workbuddy\binaries\python\versions\3.13.12\python.exe" (
    "C:\Users\13671\.workbuddy\binaries\python\versions\3.13.12\python.exe" -c "import encodings" >nul 2>&1
    if not errorlevel 1 set "PY=C:\Users\13671\.workbuddy\binaries\python\versions\3.13.12\python.exe"
)
REM 3b - system python then py then python3, only if they can import encodings
if not defined PY (
    where python >nul 2>&1
    if not errorlevel 1 (
        python -c "import encodings" >nul 2>&1
        if not errorlevel 1 set "PY=python"
    )
)
if not defined PY (
    where py >nul 2>&1
    if not errorlevel 1 (
        py -c "import encodings" >nul 2>&1
        if not errorlevel 1 set "PY=py"
    )
)
if not defined PY (
    where python3 >nul 2>&1
    if not errorlevel 1 (
        python3 -c "import encodings" >nul 2>&1
        if not errorlevel 1 set "PY=python3"
    )
)
if not defined PY (
    echo [ERROR] No working Python found.
    echo         The system Python at C:\Program Files\Python312 is missing its standard library.
    echo         Lib\encodings is gone. Repair or reinstall Python, or keep WorkBuddy
    echo         installed so its bundled Python can be used.
    goto :END
)
echo     using: %PY%

echo     server log below. Close this window to stop the backend.
echo     emulator: http://10.0.2.2:%PORT%/
echo     device:   http://127.0.0.1:%PORT%/
echo --------------------------------------------

%PY% leaderboard_server.py %PORT%
echo [exit] backend stopped, code=%errorlevel%

:END
popd >nul 2>&1
echo.
echo ============================================
echo   The window stays open on any error so you can read it.
echo   Close this window to stop the backend.
echo ============================================
pause
