
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

# 2. Build APK
Write-Host "Building APK for version $version..."
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Clean Cache (Optional but recommended if build fails)
Write-Host "Cleaning Gradle Cache..."
./gradlew.bat --stop
Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\8.11.1\transforms" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue

./gradlew.bat clean
./gradlew.bat assembleRelease --no-daemon

if (-not (Test-Path $apkPath)) {
    Write-Error "APK build failed or output not found at $apkPath"
}

# 3. Deploy APK
Write-Host "Deploying $apkName to $docsDir..."
Copy-Item $apkPath -Destination "$docsDir/$apkName" -Force

# Remove old APKs
Get-ChildItem $docsDir -Filter "DrawRun_v*.apk" | Where-Object { $_.Name -ne $apkName } | Remove-Item -Force

# 4. Update index.html
Write-Host "Updating downloads links in $indexFile..."
$htmlContent = Get-Content $indexFile -Raw
# Replace any DrawRun_vX.X.apk with new version
$newHtml = $htmlContent -replace "DrawRun_v[\d\.]+\.apk", $apkName
$newHtml = $htmlContent -replace "DrawRun_v[\d\.]+\.apk", $apkName
Set-Content -Path $indexFile -Value $newHtml

# 4.5 Update version_info.json
$versionInfoFile = "version_info.json"
Write-Host "Updating $versionInfoFile..."
if (Test-Path $versionInfoFile) {
    $jsonContent = Get-Content $versionInfoFile -Raw | ConvertFrom-Json
    $jsonContent.latestVersionCode = [int]$currentCode + 1
    $jsonContent.latestVersionName = $version
    $jsonContent.downloadUrl = "https://swal-l.github.io/DrawRun/$apkName"
    
    $newJsonInfo = $jsonContent | ConvertTo-Json -Depth 5
    Set-Content -Path $versionInfoFile -Value $newJsonInfo
} else {
    Write-Warning "$versionInfoFile not found, skipping update."
}

Write-Host "Deployment Complete! ✅"
Write-Host "New APK: $docsDir/$apkName"

# 5. Git Automation
Write-Host "Committing and Pushing to GitHub..."
git add .
git commit -m "Auto-Deploy Version $version"
git push origin main

Write-Host "Deployment & Git Push Complete! ✅"
