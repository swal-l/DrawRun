
# Android Deployment Script
# 1. Extracts version from build.gradle.kts
# 2. Builds APK
# 3. Copies to docs/
# 4. Updates index.html

$ErrorActionPreference = "Stop"

$gradleFile = "app/build.gradle.kts"
$docsDir = "docs"
$indexFile = "$docsDir/index.html"

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
# Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue # Too aggressive for every build?

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
Set-Content -Path $indexFile -Value $newHtml

Write-Host "Deployment Complete! ✅"
Write-Host "New APK: $docsDir/$apkName"
