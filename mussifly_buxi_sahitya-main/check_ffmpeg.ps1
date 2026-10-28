#!/usr/bin/env pwsh
# Quick verification script for FFmpeg .aar file

$aarFile = "app\libs\ffmpeg-kit-full-gpl-6.0-2.LTS.aar"

Write-Host ""
Write-Host "🔍 Checking FFmpeg .aar file..." -ForegroundColor Cyan
Write-Host ""

if (Test-Path $aarFile) {
    $size = (Get-Item $aarFile).Length / 1MB
    Write-Host "✅ FFmpeg .aar file found!" -ForegroundColor Green
    Write-Host "   📁 Location: $aarFile" -ForegroundColor Gray
    Write-Host "   📊 Size: $([math]::Round($size, 2)) MB" -ForegroundColor Gray
    
    if ($size -lt 10) {
        Write-Host ""
        Write-Host "⚠️  WARNING: File size is too small ($([math]::Round($size, 2)) MB)" -ForegroundColor Yellow
        Write-Host "   Expected size: 50-60 MB" -ForegroundColor Yellow
        Write-Host "   The file may be corrupted or incomplete." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   Please re-download from:" -ForegroundColor Yellow
        Write-Host "   https://artifactory.appodeal.com/appodeal-public/com/arthenica/ffmpeg-kit-full-gpl/6.0-2.LTS/" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "✅ File size looks correct!" -ForegroundColor Green
        Write-Host ""
        Write-Host "🚀 Ready to build! Run:" -ForegroundColor Green
        Write-Host "   ./gradlew assembleDebug" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ FFmpeg .aar file NOT FOUND!" -ForegroundColor Red
    Write-Host ""
    Write-Host "📥 Download from:" -ForegroundColor Yellow
    Write-Host "   https://artifactory.appodeal.com/appodeal-public/com/arthenica/ffmpeg-kit-full-gpl/6.0-2.LTS/" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📁 Save as:" -ForegroundColor Yellow
    Write-Host "   $((Get-Location).Path)\$aarFile" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Expected file: ffmpeg-kit-full-gpl-6.0-2.LTS.aar" -ForegroundColor Gray
    Write-Host "Expected size: 50-60 MB" -ForegroundColor Gray
}

Write-Host ""
