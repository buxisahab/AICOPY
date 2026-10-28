@echo off
echo ========================================
echo   OpenTune - Rebuild Script
echo ========================================
echo.
echo This script will:
echo 1. Clean the project
echo 2. Build debug APK
echo 3. Install on connected device
echo.
echo ========================================
echo.

REM Navigate to project directory
cd /d "%~dp0"

echo [1/3] Cleaning project...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo.

echo [2/3] Building debug APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo.

echo [3/3] Installing on device...
call gradlew.bat installDebug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Install failed!
    echo Make sure a device is connected via ADB
    pause
    exit /b 1
)

echo.
echo ========================================
echo  SUCCESS! App rebuilt and installed
echo ========================================
echo.
echo The app has been updated on your device
echo All code fixes are now active
echo.
pause
