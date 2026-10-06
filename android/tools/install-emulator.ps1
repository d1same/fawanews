# Install debug APK on the first running phone emulator only.
$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"

$serial = (& $adb devices | Select-String "emulator-\d+\s+device" | Select-Object -First 1).ToString().Split()[0]
if (-not $serial) { throw "No emulator connected" }

if (-not (Test-Path $apk)) {
    Push-Location (Join-Path $PSScriptRoot "..")
    .\gradlew.bat assembleDebug
    Pop-Location
}

& $adb -s $serial install -r $apk
& $adb -s $serial shell am force-stop sc.fawanews.app
& $adb -s $serial shell am start -n sc.fawanews.app/.MainActivity
Write-Host "Installed on $serial"
