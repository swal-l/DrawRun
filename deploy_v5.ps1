# Android Deployment Script v5
$ErrorActionPreference = "Stop"

$gradleFile = "app/build.gradle.kts"
$docsDir = "docs"
$indexFile = "$docsDir/index.html"
$versionInfoFile = "version_info.json"

# 1. Extract Version
Write-Host "Extracting version from $gradleFile..."
$content = Get-Content $gradleFile -Raw
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $versionCode = [int]$matches[1]
} else {
    Write-Error "Could not find versionCode"
    exit 1
}

if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $version = $matches[1]
} else {
    Write-Error "Could not find versionName in $gradleFile"
    exit 1
}

Write-Host "Detected Version: $version (Code: $versionCode)"
$apkName = "DrawRun_v$version.apk"
$apkPath = "app/build/outputs/apk/release/$apkName"

# 2. Build APK
Write-Host "`n[2/6] Building APK..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$buildLog = "build_deploy.log"

Write-Host "  ⚙ Compiling release APK..." -ForegroundColor Gray
cmd /c "gradlew.bat assembleRelease --no-daemon --parallel --build-cache > $buildLog 2>&1"
$buildSuccess = $LASTEXITCODE -eq 0

if (-not $buildSuccess -or -not (Test-Path $apkPath)) {
    Write-Host "  ⚠ Build failed, retrying clean build..." -ForegroundColor DarkYellow
    cmd /c "gradlew.bat clean assembleRelease --no-daemon --parallel --build-cache > $buildLog 2>&1"
    if (-not (Test-Path $apkPath)) {
        Write-Error "Build failed completely."
        exit 1
    }
}
Write-Host "  ✓ Build succeeded" -ForegroundColor Green

# 3. Deploy
Write-Host "`n[3/6] Deploying to $docsDir..." -ForegroundColor Yellow
Copy-Item $apkPath -Destination "$docsDir/$apkName" -Force

# Remove old APKs
Get-ChildItem $docsDir -Filter "DrawRun_v*.apk" | Where-Object { $_.Name -ne $apkName } | Remove-Item -Force
Write-Host "  ✓ Deployed: $apkName" -ForegroundColor Green

# 4. Updates
Write-Host "`n[4/6] Updating files..." -ForegroundColor Yellow
$htmlContent = Get-Content $indexFile -Raw
$newHtml = $htmlContent -replace "DrawRun_v[\d\.]+\.apk", $apkName
Set-Content -Path $indexFile -Value $newHtml
Write-Host "  ✓ Updated index.html" -ForegroundColor Green

# Update version_info.json
$versionInfoPath = "$docsDir/$versionInfoFile"
$releaseNotesText = "DrawRun Pro v$version - Architecture Clean complête, Design System Premium, Migration Room sécurisée et Splash Screen Android 12+."

if (Test-Path $versionInfoPath) {
    $jsonContent = Get-Content $versionInfoPath -Raw | ConvertFrom-Json
} else {
    $jsonContent = @{}
}

$jsonContent | Add-Member -MemberType NoteProperty -Name "versionCode" -Value $versionCode -Force
$jsonContent | Add-Member -MemberType NoteProperty -Name "versionName" -Value $version -Force
$jsonContent | Add-Member -MemberType NoteProperty -Name "downloadUrl" -Value "https://swal-l.github.io/DrawRun/$apkName" -Force
$jsonContent | Add-Member -MemberType NoteProperty -Name "releaseNotes" -Value $releaseNotesText -Force

$jsonContent | ConvertTo-Json -Depth 3 | Set-Content -Path $versionInfoPath -Encoding utf8
Write-Host "  ✓ Updated version_info.json" -ForegroundColor Green

# 5. Git Push
Write-Host "`n[5/6] Pushing to GitHub..." -ForegroundColor Yellow
git add .
git commit -m "🚀 Launch v$version Pro" -q
git push origin main -q

Write-Host "DEPLOIMENT COMPLETE!" -ForegroundColor Green
