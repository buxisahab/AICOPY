#!/usr/bin/env pwsh
# Download FFmpeg .aar file automatically

$url = "https://artifactory.appodeal.com/appodeal-public/com/arthenica/ffmpeg-kit-full-gpl/6.0-2.LTS/ffmpeg-kit-full-gpl-6.0-2.LTS.aar"
$output = "app\libs\ffmpeg-kit-full-gpl-6.0-2.LTS.aar"

Write-Host ""
Write-Host "📥 Downloading FFmpeg .aar file..." -ForegroundColor Cyan
Write-Host ""
Write-Host "From: $url" -ForegroundColor Gray
Write-Host "To:   $output" -ForegroundColor Gray
Write-Host ""

# Create directory if it doesn't exist
$dir = Split-Path $output
if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

try {
    # Use Invoke-WebRequest to download
    Write-Host "Downloading... (This may take 1-2 minutes for ~50-60 MB file)" -ForegroundColor Yellow
    Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
    
    $size = (Get-Item $output).Length / 1MB
    
    Write-Host ""
    Write-Host "✅ Download complete!" -ForegroundColor Green
    Write-Host "   Size: $([math]::Round($size, 2)) MB" -ForegroundColor Gray
    
    if ($size -lt 10) {
        Write-Host ""
        Write-Host "⚠️  WARNING: Downloaded file is too small!" -ForegroundColor Yellow
        Write-Host "   Something may have gone wrong." -ForegroundColor Yellow
        Remove-Item $output -Force
    } else {
        Write-Host ""
        Write-Host "🚀 Ready to build! Run:" -ForegroundColor Green
        Write-Host "   ./gradlew assembleDebug" -ForegroundColor Yellow
    }
} catch {
    Write-Host ""
    Write-Host "❌ Download failed: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please download manually from:" -ForegroundColor Yellow
    Write-Host "   https://artifactory.appodeal.com/appodeal-public/com/arthenica/ffmpeg-kit-full-gpl/6.0-2.LTS/" -ForegroundColor Cyan
}

Write-Host ""
