@echo off
echo ========================================
echo VERIFYING STREAMING ENGINE SWITCHES
echo ========================================
echo.

echo [1/3] Checking if switches are in PlayerSettings.kt...
findstr /C:"Streaming Engines" "app\src\main\java\com\musifly\android\ui\screens\settings\PlayerSettings.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ "Streaming Engines" section found
) else (
    echo ❌ "Streaming Engines" section NOT found!
    exit /b 1
)

findstr /C:"JioSaavn (320kbps)" "app\src\main\java\com\musifly\android\ui\screens\settings\PlayerSettings.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ JioSaavn switch found
) else (
    echo ❌ JioSaavn switch NOT found!
    exit /b 1
)

findstr /C:"YouTube Music" "app\src\main\java\com\musifly\android\ui\screens\settings\PlayerSettings.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ YouTube Music switch found
) else (
    echo ❌ YouTube Music switch NOT found!
    exit /b 1
)

findstr /C:"YouTube Video" "app\src\main\java\com\musifly\android\ui\screens\settings\PlayerSettings.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ YouTube Video switch found
) else (
    echo ❌ YouTube Video switch NOT found!
    exit /b 1
)

echo.
echo [2/3] Checking StreamingEngineManager...
findstr /C:"setEnabledEngines" "app\src\main\java\com\musifly\android\streamingengines\StreamingEngineManager.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ StreamingEngineManager.setEnabledEngines() found
) else (
    echo ❌ StreamingEngineManager.setEnabledEngines() NOT found!
    exit /b 1
)

echo.
echo [3/3] All checks passed!
echo.
echo ========================================
echo NOW RUN:
echo   1. gradlew clean
echo   2. gradlew assembleDebug
echo   3. adb uninstall com.Arturo254.android.debug
echo   4. gradlew installDebug
echo ========================================
pause
