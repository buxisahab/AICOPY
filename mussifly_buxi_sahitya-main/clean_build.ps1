# Clean Build Script for Musifly
# This script cleans all build artifacts and caches

Write-Host "🧹 Cleaning Musifly build artifacts..." -ForegroundColor Cyan

# Delete build folders
$foldersToDelete = @(
    "app\build",
    "build",
    ".gradle",
    "app\.cxx",
    "*.iml"
)

foreach ($folder in $foldersToDelete) {
    if (Test-Path $folder) {
        Write-Host "Deleting: $folder" -ForegroundColor Yellow
        Remove-Item -Recurse -Force $folder -ErrorAction SilentlyContinue
    }
}

# Delete all .iml files in app directory
Get-ChildItem -Path "app" -Filter "*.iml" -Recurse | Remove-Item -Force -ErrorAction SilentlyContinue

Write-Host "✅ Build folders cleaned!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next steps:" -ForegroundColor Cyan
Write-Host "1. In Android Studio: File → Invalidate Caches → Invalidate and Restart" -ForegroundColor White
Write-Host "2. After restart: Build → Clean Project" -ForegroundColor White
Write-Host "3. Then: Build → Rebuild Project" -ForegroundColor White
Write-Host "4. Uninstall old app from device/emulator if installed" -ForegroundColor White
Write-Host "5. Run the app again" -ForegroundColor White
