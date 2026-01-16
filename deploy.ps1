# Android Deployment Script
# 1. Extracts version from build.gradle.kts
# 2. Builds APK
# 3. Copies to docs/
# 4. Updates index.html
# 5. Updates version_info.json
# 6. Pushes to GitHub

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

# ===== 3. BUILD APK (OPTIMIZED) =====
Write-Host "`n[3/6] Building APK..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

Write-Host "  ⚙ Compiling release APK..." -ForegroundColor Gray

# Try build WITHOUT cleaning cache first
$buildLog = "build_temp.log"
cmd /c "gradlew.bat assembleRelease --no-daemon --parallel --build-cache > $buildLog 2>&1"
$buildSuccess = $LASTEXITCODE -eq 0
$buildOutput = Get-Content $buildLog -ErrorAction SilentlyContinue

if (-not $buildSuccess -or -not (Test-Path $apkPath)) {
    Write-Host "  ⚠ Build failed, analyzing error..." -ForegroundColor DarkYellow
    $buildOutput | Select-Object -Last 10 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
    
    Write-Host "  ⚙ Cleaning LOCAL cache and retrying..." -ForegroundColor DarkYellow
    ./gradlew.bat --stop | Out-Null
    Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
    
    Write-Host "  ⚙ Rebuilding..." -ForegroundColor Gray
    cmd /c "gradlew.bat clean assembleRelease --no-daemon --parallel --build-cache > $buildLog 2>&1"
    
    if (-not (Test-Path $apkPath)) {
        $buildOutput = Get-Content $buildLog -ErrorAction SilentlyContinue
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
if ($removed) {
    $removed | Remove-Item -Force
    Write-Host "  ✓ Removed $($removed.Count) old APKs" -ForegroundColor Green
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
$lastTag = git describe --tags --abbrev=0 2>$null
if ($lastTag) {
    $gitLog = git log "$lastTag..HEAD" --pretty=format:"%s" 2>$null
} else {
    $gitLog = git log -n 20 --pretty=format:"%s" 2>$null
}

$features = @()
$fixes = @()

if ($gitLog) {
    $gitLog | ForEach-Object {
        $commit = $_
        if ($commit -match "^Merge " -or $commit -match "^🚀 Deploy") { return }
        $cleanMsg = ($commit -replace "^(feat|fix|docs|style|refactor|perf|test|chore)(\(.*\))?:", "").Trim()
        if (-not $cleanMsg) { return }

        if ($commit -match "feat" -or $commit -match "add" -or $commit -match "new" -or $commit -match "implement") {
            if ($features.Count -lt 8 -and $features -notcontains $cleanMsg) { $features += $cleanMsg }
        }
        elseif ($commit -match "fix" -or $commit -match "bug" -or $commit -match "resolve" -or $commit -match "correct") {
            if ($fixes.Count -lt 8 -and $fixes -notcontains $cleanMsg) { $fixes += $cleanMsg }
        }
    }
}

if ($features.Count -eq 0) { $features = @("Améliorations diverses et optimisations") }
if ($fixes.Count -eq 0) { $fixes = @("Corrections mineures de stabilité") }

$versionInfoPath = "$docsDir/$versionInfoFile"
if (Test-Path $versionInfoPath) {
    $jsonContent = Get-Content $versionInfoPath -Raw | ConvertFrom-Json
} else {
    $jsonContent = @{}
}

$jsonContent | Add-Member -MemberType NoteProperty -Name "versionCode" -Value $versionCode -Force
$jsonContent | Add-Member -MemberType NoteProperty -Name "versionName" -Value $version -Force
$jsonContent | Add-Member -MemberType NoteProperty -Name "downloadUrl" -Value "https://swal-l.github.io/DrawRun/$apkName" -Force

$releaseNotesText = "Version $version`n"
if ($features.Count -gt 0) {
    $releaseNotesText += "`nNouvelles fonctionnalités :`n"
    $features | ForEach-Object { $releaseNotesText += "- $_`n" }
}
if ($fixes.Count -gt 0) {
    $releaseNotesText += "`nCorrections :`n"
    $fixes | ForEach-Object { $releaseNotesText += "- $_`n" }
}

$jsonContent | Add-Member -MemberType NoteProperty -Name "releaseNotes" -Value $releaseNotesText -Force
$jsonContent | ConvertTo-Json -Depth 3 | Set-Content -Path $versionInfoPath -Encoding utf8
Write-Host "  ✓ Updated $versionInfoFile" -ForegroundColor Green

# ===== 6. GIT PUSH =====
Write-Host "`n[6/6] Pushing to GitHub..." -ForegroundColor Yellow
git add .
git commit -m "🚀 Deploy v$version" -q
git push origin main -q

Write-Host "DEPLOIMENT COMPLETE!" -ForegroundColor Green
Write-Host "APK: $docsDir/$apkName" -ForegroundColor Cyan
Write-Host "URL: https://swal-l.github.io/DrawRun/$apkName" -ForegroundColor Cyan
