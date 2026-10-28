@echo off
echo ========================================
echo REBUILD AND VERIFY JioSaavn Switch
echo ========================================
echo.

echo Step 1: Cleaning build...
call gradlew clean
echo.

echo Step 2: Building project...
call gradlew assembleDebug
echo.

echo Step 3: Checking for compilation errors...
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed! Check errors above.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Uninstall old app: adb uninstall com.musifly.android
echo 2. Install new APK: gradlew installDebug
echo 3. Open app - Settings - Player and Audio
echo 4. Scroll down to "Streaming Engines" section
echo.
pause
