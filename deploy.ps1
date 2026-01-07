
# Android Deployment Script
# 1. Extracts version from build.gradle.kts
# 2. Builds APK
# 3. Copies to docs/
# 4. Updates index.html

$ErrorActionPreference = "Stop"

$gradleFile = "app/build.gradle.kts"
$docsDir = "docs"
$indexFile = "$docsDir/index.html"
$versionInfoFile = "version_info.json"


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

# 1. Extract Version (re-read to get updated values)
$content = Get-Content $gradleFile -Raw
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $versionCode = [int]$matches[1]
} else {
    Write-Error "Could not find versionCode"
}

if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $version = $matches[1]
} else {
    Write-Error "Could not find versionName in $gradleFile"
}

Write-Host "Detected Version: $version (Code: $versionCode)"
$apkName = "DrawRun_v$version.apk"
$apkPath = "app/build/outputs/apk/release/$apkName"

# ===== 3. BUILD APK (OPTIMIZED) =====
Write-Host "`n[3/6] Building APK..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

Write-Host "  ⚙ Compiling release APK..." -ForegroundColor Gray

# Try build WITHOUT cleaning cache first (show errors if any)
$buildOutput = ./gradlew.bat assembleRelease --no-daemon --parallel --build-cache 2>&1
$buildSuccess = $LASTEXITCODE -eq 0

if (-not $buildSuccess -or -not (Test-Path $apkPath)) {
    Write-Host "  ⚠ Build failed, analyzing error..." -ForegroundColor DarkYellow
    
    # Show last 10 lines of error
    $buildOutput | Select-Object -Last 10 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
    
    Write-Host "  ⚙ Cleaning LOCAL cache and retrying..." -ForegroundColor DarkYellow
    
    # Clean ONLY local cache (not global cache - too slow!)
    ./gradlew.bat --stop 2>&1 | Out-Null
    Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
    
    # Retry with clean local cache
    Write-Host "  ⚙ Rebuilding..." -ForegroundColor Gray
    ./gradlew.bat clean assembleRelease --no-daemon --parallel --build-cache 2>&1 | Out-Null
    
    if (-not (Test-Path $apkPath)) {
        Write-Host "`n❌ BUILD FAILED - Showing full error:" -ForegroundColor Red
        $buildOutput | Select-Object -Last 30
        Write-Error "APK not found at $apkPath"
        exit 1
    }
    
    Write-Host "  ✓ Build succeeded after cache clean" -ForegroundColor Green
} else {
    Write-Host "  ✓ APK built successfully (no cache clean needed)" -ForegroundColor Green
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

# Update version_info.json with AI-generated release notes
Write-Host "  ⚙ Generating release notes..." -ForegroundColor Gray

# Get recent git commits since last tag/version
$gitLog = git log --pretty=format:"%s" --since="7 days ago" 2>$null
if (-not $gitLog) {
    $gitLog = git log --pretty=format:"%s" -n 10 2>$null
}

# Analyze commits to categorize changes
$features = @()
$fixes = @()

if ($gitLog) {
    $gitLog | ForEach-Object {
        $commit = $_
        # Categorize based on commit message patterns
        if ($commit -match "^(feat|feature|add|new|implement)" -or $commit -match "✨|🎉|⚡|🚀") {
            $cleanMsg = $commit -replace "^(feat|feature|add|new|implement)[:\s]*", "" -replace "[✨🎉⚡🚀]", ""
            if ($cleanMsg.Trim() -and $features.Count -lt 5) {
                $features += $cleanMsg.Trim()
            }
        }
        elseif ($commit -match "^(fix|bug|correct|resolve)" -or $commit -match "🐛|🔧|✅") {
            $cleanMsg = $commit -replace "^(fix|bug|correct|resolve)[:\s]*", "" -replace "[🐛🔧✅]", ""
            if ($cleanMsg.Trim() -and $fixes.Count -lt 5) {
                $fixes += $cleanMsg.Trim()
            }
        }
    }
}

# If no categorized commits, use generic messages
if ($features.Count -eq 0 -and $fixes.Count -eq 0) {
    $features = @("Améliorations de performance", "Optimisations diverses")
    $fixes = @("Corrections de bugs mineurs", "Améliorations de stabilité")
}
elseif ($features.Count -eq 0) {
    $features = @("Améliorations de l'interface")
}
elseif ($fixes.Count -eq 0) {
    $fixes = @("Corrections mineures")
}

# Update version_info.json
if (Test-Path $versionInfoFile) {
    $jsonContent = Get-Content $versionInfoFile -Raw | ConvertFrom-Json
    $jsonContent.latestVersionCode = $versionCode
    $jsonContent.latestVersionName = $version
    $jsonContent.downloadUrl = "https://swal-l.github.io/DrawRun/$apkName"
    
    # Update release notes
    $jsonContent.releaseNotes.features = $features
    $jsonContent.releaseNotes.fixes = $fixes
    
    $newJsonInfo = $jsonContent | ConvertTo-Json -Depth 5
    Set-Content -Path $versionInfoFile -Value $newJsonInfo
    Write-Host "  ✓ Updated $versionInfoFile with AI-generated notes" -ForegroundColor Green
    Write-Host "    Features: $($features.Count) | Fixes: $($fixes.Count)" -ForegroundColor DarkGray
} else {
    Write-Host "  ⚠ $versionInfoFile not found, skipping" -ForegroundColor DarkYellow
}

# ===== 6. GIT PUSH =====
Write-Host "`n[6/6] Pushing to GitHub..." -ForegroundColor Yellow
git add .
git commit -m "🚀 Deploy v$version" -q
git push origin main -q

Write-Host "`n✅ DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "📦 APK: $docsDir/$apkName" -ForegroundColor Cyan
Write-Host "🌐 URL: https://swal-l.github.io/DrawRun/$apkName`n" -ForegroundColor Cyan
