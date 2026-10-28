# Verify FFmpeg .aar file is correctly placed

$aarPath = "app\libs\ffmpeg-kit-full-gpl-6.0-2.LTS.aar"

Write-Host ""
Write-Host "🔍 Checking FFmpeg .aar file..." -ForegroundColor Cyan
Write-Host ""

if (Test-Path $aarPath) {
    $fileSize = (Get-Item $aarPath).Length / 1MB
    Write-Host "✅ FFmpeg .aar file found!" -ForegroundColor Green
    Write-Host "   Location: $aarPath" -ForegroundColor Gray
    Write-Host "   Size: $([math]::Round($fileSize, 2)) MB" -ForegroundColor Gray
    Write-Host ""
    Write-Host "✅ Ready to build! Run:" -ForegroundColor Green
    Write-Host "   ./gradlew clean assembleDebug" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "❌ FFmpeg .aar file NOT FOUND!" -ForegroundColor Red
    Write-Host ""
    Write-Host "📥 Download from:" -ForegroundColor Yellow
    Write-Host "   https://artifactory.appodeal.com/appodeal-public/com/arthenica/ffmpeg-kit-full-gpl/6.0-2.LTS/" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📁 Place the file at:" -ForegroundColor Yellow
    Write-Host "   $((Get-Location).Path)\$aarPath" -ForegroundColor Cyan
    Write-Host ""
}
