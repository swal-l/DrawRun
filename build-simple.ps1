# Simple Build Script for Android Studio
# Just builds the APK without deployment

$ErrorActionPreference = "Stop"

Write-Host "`n🔨 SIMPLE BUILD - DrawRun" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Read current version
$gradleFile = "app/build.gradle.kts"
$content = Get-Content $gradleFile -Raw

if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $version = $matches[1]
} else {
    Write-Error "Could not find versionName"
    exit 1
}

if ($content -match 'versionCode\s*=\s*(\d+)') {
    $versionCode = $matches[1]
} else {
    Write-Error "Could not find versionCode"
    exit 1
}

Write-Host "📦 Version: $version (Code: $versionCode)" -ForegroundColor Green

# Build APK
Write-Host "`n⚙ Building APK..." -ForegroundColor Yellow
./gradlew.bat assembleRelease --no-daemon --parallel --build-cache

$apkName = "DrawRun_v$version.apk"
$apkPath = "app/build/outputs/apk/release/$apkName"

if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length / 1MB
    Write-Host "`n✅ BUILD SUCCESS!" -ForegroundColor Green
    Write-Host "📦 APK: $apkPath" -ForegroundColor Cyan
    Write-Host "📊 Size: $([math]::Round($apkSize, 2)) MB`n" -ForegroundColor Cyan
} else {
    Write-Host "`n❌ BUILD FAILED - APK not found at $apkPath" -ForegroundColor Red
    exit 1
}
