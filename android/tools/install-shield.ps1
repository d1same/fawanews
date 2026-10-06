# Install debug APK on NVIDIA Shield only (avoids pushing to phone emulators).
$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
$shield = "10.1.20.11:5555"

if (-not (Test-Path $adb)) { throw "adb not found" }
if (-not (Test-Path $apk)) {
    Push-Location (Join-Path $PSScriptRoot "..")
    .\gradlew.bat assembleDebug
    Pop-Location
}

& $adb connect $shield | Out-Null
& $adb -s $shield install -r $apk
& $adb -s $shield shell am force-stop sc.fawanews.app
# Bootstrap once so PhoneLauncher alias is disabled on TV (Projectivy mobile panel fix).
& $adb -s $shield shell am start -n sc.fawanews.app/.MainActivity
Start-Sleep -Seconds 2
& $adb -s $shield shell am force-stop sc.fawanews.app
& $adb -s $shield shell am start -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -n sc.fawanews.app/.TvLauncher
Write-Host "Installed on Shield ($shield). v1.0.8 — if still a narrow strip: Settings > Apps > FawaNews > Aspect ratio > Full screen."
