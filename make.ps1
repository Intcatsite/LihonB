# ==============================================================================
# LihonB Plugin Compiler (make.ps1)
# One-liner builder: irm https://lihont.space/make.ps1 | iex
# ==============================================================================

Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  LihonB Plugin Builder (.lbp) v1.0" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Magenta

# 1. Locate plugin root (where manifest.json lives)
$pluginDir = (Get-Location).Path
$manifestPath = Join-Path $pluginDir "manifest.json"

if (-not (Test-Path $manifestPath)) {
    # Check if there is a child folder with manifest.json
    $sub = Get-ChildItem -Directory -Recurse -Depth 2 -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName "manifest.json") } | Select-Object -First 1
    if ($sub) {
        $pluginDir = $sub.FullName
        $manifestPath = Join-Path $pluginDir "manifest.json"
        Write-Host "-> Found plugin in: $pluginDir" -ForegroundColor Yellow
    } else {
        Write-Host "[ERROR] manifest.json not found in current directory ($pluginDir)!" -ForegroundColor Red
        Write-Host "Make sure you run this command inside or near your plugin folder." -ForegroundColor Yellow
        return
    }
}

# 2. Parse manifest.json
try {
    $manifestContent = Get-Content -Raw -Encoding UTF8 $manifestPath
    $manifest = $manifestContent | ConvertFrom-Json
} catch {
    Write-Host "[ERROR] Failed to parse manifest.json: $_" -ForegroundColor Red
    return
}

$pluginId = $manifest.id
$pluginName = if ($manifest.name_ru) { $manifest.name_ru } elseif ($manifest.name) { $manifest.name } else { $pluginId }
$pluginVer = if ($manifest.version) { $manifest.version } else { "1.0" }

Write-Host "-> Plugin: $pluginName ($pluginId) v$pluginVer" -ForegroundColor Green

# 3. Setup SDK Cache Directory
$sdkDir = Join-Path $env:LOCALAPPDATA "LihonB\sdk"
if (-not (Test-Path $sdkDir)) {
    New-Item -ItemType Directory -Path $sdkDir -Force | Out-Null
}

$androidJar = Join-Path $sdkDir "android.jar"
$apiJar = Join-Path $sdkDir "lihonb-api.jar"
$r8Jar = Join-Path $sdkDir "r8.jar"

# Check local SDK first if available on this machine
$localSdkAndroid = "$env:LOCALAPPDATA\Android\Sdk\platforms\android-34\android.jar"
if (-not (Test-Path $androidJar) -and (Test-Path $localSdkAndroid)) {
    Copy-Item $localSdkAndroid $androidJar -Force
}

$baseUrl = "https://lihont.space/sdk"

function Ensure-SdkFile($file, $url, $name) {
    if (-not (Test-Path $file) -or (Get-Item $file).Length -eq 0) {
        Write-Host "-> Downloading $name..." -ForegroundColor Cyan
        Invoke-WebRequest -Uri $url -OutFile $file -UseBasicParsing
    }
}

Ensure-SdkFile $apiJar "$baseUrl/lihonb-api.jar" "LihonB Plugin API"
Ensure-SdkFile $androidJar "$baseUrl/android.jar" "Android SDK Stub"
Ensure-SdkFile $r8Jar "$baseUrl/r8.jar" "D8 Dex Compiler"

# 4. Find Java sources
$srcDir = Join-Path $pluginDir "src"
$javaFiles = if (Test-Path $srcDir) {
    Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
} else {
    Get-ChildItem -Path $pluginDir -Filter "*.java" | ForEach-Object { $_.FullName }
}

if (-not $javaFiles) {
    Write-Host "[ERROR] No .java source files found in $srcDir or $pluginDir!" -ForegroundColor Red
    return
}

Write-Host "-> Found $($javaFiles.Count) Java source files" -ForegroundColor Gray

# 5. Check JDK / javac
$javacCmd = Get-Command javac -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
if (-not $javacCmd) {
    $candidates = @(
        "$env:JAVA_HOME\bin\javac.exe",
        "$env:ProgramFiles\Java\*\bin\javac.exe",
        "$env:ProgramFiles(x86)\Java\*\bin\javac.exe"
    )
    foreach ($cand in $candidates) {
        $found = Get-Item $cand -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $javacCmd = $found.FullName
            break
        }
    }
}

if (-not $javacCmd) {
    Write-Host "[ERROR] javac not found! Please install JDK 8+ or configure JAVA_HOME." -ForegroundColor Red
    return
}

# 6. Compile Java sources
$buildDir = Join-Path $pluginDir "build"
$classesDir = Join-Path $buildDir "classes"
$dexDir = Join-Path $buildDir "dex"

if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
New-Item -ItemType Directory -Path $dexDir -Force | Out-Null

Write-Host "-> Compiling Java bytecode..." -ForegroundColor Cyan
$classpath = "$androidJar;$apiJar"
$javacArgs = @("--release", "8", "-cp", $classpath, "-d", $classesDir) + $javaFiles

$javacProcess = Start-Process -FilePath $javacCmd -ArgumentList $javacArgs -NoNewWindow -Wait -PassThru
if ($javacProcess.ExitCode -ne 0) {
    Write-Host "[ERROR] Compilation failed!" -ForegroundColor Red
    return
}

# 7. Convert class files to classes.dex using D8
Write-Host "-> Generating classes.dex with modern D8..." -ForegroundColor Cyan
$classFiles = Get-ChildItem -Path $classesDir -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }

$d8Args = @(
    "-cp", $r8Jar,
    "com.android.tools.r8.D8",
    "--release",
    "--min-api", "26",
    "--output", $dexDir,
    "--lib", $androidJar,
    "--lib", $apiJar
) + $classFiles

$javaCmd = Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
if (-not $javaCmd) { $javaCmd = "java" }

$d8Process = Start-Process -FilePath $javaCmd -ArgumentList $d8Args -NoNewWindow -Wait -PassThru
if ($d8Process.ExitCode -ne 0) {
    Write-Host "[ERROR] D8 dex conversion failed!" -ForegroundColor Red
    return
}

$dexFile = Join-Path $dexDir "classes.dex"
if (-not (Test-Path $dexFile)) {
    Write-Host "[ERROR] classes.dex was not generated!" -ForegroundColor Red
    return
}

# 8. Create .lbp ZIP package
$outLbp = Join-Path $pluginDir "$pluginId.lbp"
$tempZip = Join-Path $pluginDir "temp_plugin_pack.zip"
if (Test-Path $outLbp) { Remove-Item $outLbp -Force }
if (Test-Path $tempZip) { Remove-Item $tempZip -Force }

Write-Host "-> Packaging $pluginId.lbp..." -ForegroundColor Cyan

$filesToPack = @($manifestPath, $dexFile)
$iconPath = Join-Path $pluginDir "icon.png"
if (Test-Path $iconPath) { $filesToPack += $iconPath }

Compress-Archive -Path $filesToPack -DestinationPath $tempZip -Force
Move-Item $tempZip $outLbp -Force

$sizeKb = [Math]::Round((Get-Item $outLbp).Length / 1024, 2)
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "  SUCCESS! Plugin built successfully:" -ForegroundColor Green
Write-Host "  $outLbp ($sizeKb KB)" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Magenta

# 9. ADB Device push (if device connected)
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
if (-not $adbCmd -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")) {
    $adbCmd = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}

if ($adbCmd) {
    try {
        $devices = & $adbCmd devices 2>$null | Out-String
        if ($devices -match "\bdevice\b") {
            Write-Host "-> Connected phone detected! Pushing $pluginId.lbp to /sdcard/Download/..." -ForegroundColor Yellow
            & $adbCmd push $outLbp "/sdcard/Download/$pluginId.lbp"
            Write-Host "-> Plugin uploaded to phone! Open LihonB -> Menu -> Plugins to install." -ForegroundColor Green
        }
    } catch {}
}
