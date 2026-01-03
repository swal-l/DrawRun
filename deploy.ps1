
# Android Deployment Script
# 1. Extracts version from build.gradle.kts
# 2. Builds APK
# 3. Copies to docs/
# 4. Updates index.html

$ErrorActionPreference = "Stop"

$gradleFile = "app/build.gradle.kts"
$docsDir = "docs"
$indexFile = "$docsDir/index.html"


# 0. Auto-Increment Version
Write-Host "Auto-incrementing version in $gradleFile..."
$content = Get-Content $gradleFile -Raw

# Increment versionCode
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $currentCode = [int]$matches[1]
    $newCode = $currentCode + 1
    $content = $content -replace "versionCode\s*=\s*$currentCode", "versionCode = $newCode"
    Write-Host "  Code: $currentCode -> $newCode"
}

# Increment versionName
if ($content -match 'versionName\s*=\s*"([\d\.]+)"') {
    $currentName = $matches[1]
    $parts = $currentName.Split('.')
    $lastIndex = $parts.Length - 1
    $parts[$lastIndex] = [int]$parts[$lastIndex] + 1
    $newName = $parts -join '.'
    $content = $content -replace "versionName\s*=\s*""$currentName""", "versionName = ""$newName"""
    Write-Host "  Name: $currentName -> $newName"
}

Set-Content -Path $gradleFile -Value $content

# 1. Extract Version
Write-Host "Reading version from $gradleFile..."
$content = Get-Content $gradleFile -Raw
if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $version = $matches[1]
} else {
    Write-Error "Could not find versionName in $gradleFile"
}

Write-Host "Detected Version: $version"
$apkName = "DrawRun_v$version.apk"
$apkPath = "app/build/outputs/apk/release/$apkName"

# ===== 3. BUILD APK (OPTIMIZED - NO CACHE CLEAR) =====
Write-Host "`n[3/6] Building APK..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

Write-Host "  ⚙ Compiling release APK..." -ForegroundColor Gray

# Try build WITHOUT cleaning cache first
./gradlew.bat assembleRelease --no-daemon --parallel --build-cache 2>&1 | Out-Null

if (-not (Test-Path $apkPath)) {
    Write-Host "  ⚠ Build failed, retrying with cache clean..." -ForegroundColor DarkYellow
    
    # Clean cache ONLY if build failed
    ./gradlew.bat --stop 2>&1 | Out-Null
    Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue
    
    # Retry with clean cache
    ./gradlew.bat clean --quiet
    ./gradlew.bat assembleRelease --no-daemon --parallel --build-cache 2>&1 | Out-Null
    
    if (-not (Test-Path $apkPath)) {
        Write-Error "❌ APK build failed even after cache clean at $apkPath"
        exit 1
    }
    
    Write-Host "  ✓ Build succeeded after cache clean" -ForegroundColor Green
} else {
    Write-Host "  ✓ APK built successfully" -ForegroundColor Green
}

# ===== 4. DEPLOY APK =====
Write-Host "`n[4/6] Deploying to $docsDir..." -ForegroundColor Yellow
Copy-Item $apkPath -Destination "$docsDir/$apkName" -Force

# Remove old APKs
$removed = Get-ChildItem $docsDir -Filter "DrawRun_v*.apk" | Where-Object { $_.Name -ne $apkName }
$removed | Remove-Item -Force
if ($removed) {
    Write-Host "  ✓ Removed $($removed.Count) old APK(s)" -ForegroundColor Green
}
Write-Host "  ✓ Deployed: $apkName" -ForegroundColor Green

# ===== 5. UPDATE FILES =====
Write-Host "`n[5/6] Updating website files..." -ForegroundColor Yellow

# Update index.html
$htmlContent = Get-Content $indexFile -Raw
$newHtml = $htmlContent -replace "DrawRun_v[\d\.]+\.apk", $apkName
Set-Content -Path $indexFile -Value $newHtml
Write-Host "  ✓ Updated $indexFile" -ForegroundColor Green

# Update version_info.json
if (Test-Path $versionInfoFile) {
    $jsonContent = Get-Content $versionInfoFile -Raw | ConvertFrom-Json
    $jsonContent.latestVersionCode = $versionCode
    $jsonContent.latestVersionName = $version
    $jsonContent.downloadUrl = "https://swal-l.github.io/DrawRun/$apkName"
    
    $newJsonInfo = $jsonContent | ConvertTo-Json -Depth 5
    Set-Content -Path $versionInfoFile -Value $newJsonInfo
    Write-Host "  ✓ Updated $versionInfoFile" -ForegroundColor Green
}

# ===== 6. GIT PUSH =====
Write-Host "`n[6/6] Pushing to GitHub..." -ForegroundColor Yellow
git add .
git commit -m "🚀 Deploy v$version" -q
git push origin main -q

Write-Host "`n✅ DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "📦 APK: $docsDir/$apkName" -ForegroundColor Cyan
Write-Host "🌐 URL: https://swal-l.github.io/DrawRun/$apkName`n" -ForegroundColor Cyan
