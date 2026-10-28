#!/usr/bin/env pwsh
# Build and Install APK

Write-Host ""
Write-Host "🔨 Building OpenTune with FFmpeg..." -ForegroundColor Cyan
Write-Host ""

# Stop all Gradle daemons
Write-Host "Stopping Gradle daemons..." -ForegroundColor Yellow
./gradlew --stop | Out-Null

# Build
Write-Host "Building APK (this may take 3-5 minutes)..." -ForegroundColor Yellow
$buildOutput = ./gradlew assembleDebug 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host ""
    
    # Find the APK
    $apk = Get-ChildItem -Path "app\build\outputs\apk\debug" -Filter "*.apk" -Recurse | Select-Object -First 1
    
    if ($apk) {
        $apkSize = [math]::Round($apk.Length / 1MB, 2)
        Write-Host "📦 APK Created:" -ForegroundColor Green
        Write-Host "   Location: $($apk.FullName)" -ForegroundColor Gray
        Write-Host "   Size: $apkSize MB" -ForegroundColor Gray
        Write-Host ""
        
        # Ask to install
        $install = Read-Host "Install APK to connected device? (y/n)"
        if ($install -eq "y") {
            Write-Host ""
            Write-Host "📲 Installing APK..." -ForegroundColor Yellow
            ./gradlew installDebug
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host ""
                Write-Host "✅ APK INSTALLED!" -ForegroundColor Green
                Write-Host ""
                Write-Host "🎉 अब app खोलें और test करें:" -ForegroundColor Cyan
                Write-Host "   1. Advanced Mode ON करें" -ForegroundColor Gray
                Write-Host "   2. कोई video download करें" -ForegroundColor Gray
                Write-Host "   3. Video+Audio दोनों होने चाहिए!" -ForegroundColor Gray
                Write-Host ""
            } else {
                Write-Host ""
                Write-Host "❌ Installation failed!" -ForegroundColor Red
                Write-Host "   Connect your device and enable USB debugging" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "⚠️  APK file not found!" -ForegroundColor Yellow
    }
} else {
    Write-Host ""
    Write-Host "❌ BUILD FAILED!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Showing last 20 lines of error:" -ForegroundColor Yellow
    $buildOutput | Select-Object -Last 20 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
}

Write-Host ""
