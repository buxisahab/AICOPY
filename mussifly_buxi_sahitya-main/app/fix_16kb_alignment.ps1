# PowerShell script to fix 16 KB page size alignment for native libraries
# This script extracts the FFmpeg Kit AAR and provides instructions for alignment

param(
    [string]$AarPath = "libs\ffmpeg-kit-full-gpl-6.0-2.LTS.aar",
    [string]$OutputDir = "build\temp\ffmpeg-aligned"
)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "16 KB Page Size Alignment Fix for FFmpeg Kit" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $AarPath)) {
    Write-Host "ERROR: AAR file not found: $AarPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please ensure the FFmpeg Kit AAR file exists at:" -ForegroundColor Yellow
    Write-Host "  app\$AarPath" -ForegroundColor Yellow
    exit 1
}

Write-Host "Found AAR file: $AarPath" -ForegroundColor Green
Write-Host ""

# Create output directory
$extractDir = Join-Path $OutputDir "extracted"
$libsDir = Join-Path $extractDir "jni"

if (Test-Path $extractDir) {
    Remove-Item -Recurse -Force $extractDir
}
New-Item -ItemType Directory -Path $extractDir -Force | Out-Null

Write-Host "Extracting AAR file..." -ForegroundColor Yellow
try {
    # AAR files are ZIP archives
    Expand-Archive -Path $AarPath -DestinationPath $extractDir -Force
    Write-Host "✓ AAR extracted successfully" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to extract AAR: $_" -ForegroundColor Red
    exit 1
}

# Find all .so files
$soFiles = Get-ChildItem -Path $extractDir -Recurse -Filter "*.so"
Write-Host ""
Write-Host "Found $($soFiles.Count) native library file(s):" -ForegroundColor Cyan

foreach ($soFile in $soFiles) {
    $relativePath = $soFile.FullName.Replace((Resolve-Path $extractDir).Path + "\", "")
    Write-Host "  - $relativePath" -ForegroundColor Gray
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Yellow
Write-Host "IMPORTANT: Alignment Instructions" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "The native libraries in FFmpeg Kit are not aligned for 16 KB page sizes." -ForegroundColor White
Write-Host ""
Write-Host "SOLUTIONS:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Option 1: Use WSL (Windows Subsystem for Linux) with patchelf" -ForegroundColor Green
Write-Host "  1. Install WSL and Ubuntu from Microsoft Store" -ForegroundColor Gray
Write-Host "  2. Install patchelf: sudo apt-get install patchelf" -ForegroundColor Gray
Write-Host "  3. Run this script in WSL to align libraries" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 2: Use Android NDK llvm-objcopy (if available)" -ForegroundColor Green
Write-Host "  1. Locate llvm-objcopy in your Android NDK" -ForegroundColor Gray
Write-Host "  2. Use: llvm-objcopy --set-section-alignment .text=16384 <library.so>" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 3: Rebuild FFmpeg Kit with 16 KB alignment" -ForegroundColor Green
Write-Host "  1. Build FFmpeg with: -Wl,-z,max-page-size=16384" -ForegroundColor Gray
Write-Host "  2. Repackage as AAR" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 4: Use an alternative library" -ForegroundColor Green
Write-Host "  - Consider using Media3 or other alternatives" -ForegroundColor Gray
Write-Host "  - Or find a community-maintained FFmpeg Kit build" -ForegroundColor Gray
Write-Host ""
Write-Host "Extracted libraries are in: $extractDir" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Yellow

