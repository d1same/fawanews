# D-pad smoke test on NVIDIA Shield — saves screenshots under android/tools/shield-captures/
param(
    [string]$ShieldIp = "10.1.20.11",
    [string]$OutDir = "$PSScriptRoot\shield-captures"
)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$serial = "${ShieldIp}:5555"
$pkg = "sc.fawanews.app/sc.fawanews.app.MainActivity"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
& $adb connect $serial | Out-Null
Start-Sleep -Seconds 1

function Capture([string]$name) {
    $path = Join-Path $OutDir "$name.png"
    & $adb -s $serial shell screencap -p /sdcard/fawa_cap.png | Out-Null
    & $adb -s $serial pull /sdcard/fawa_cap.png $path | Out-Null
    Write-Host "capture: $name -> $path"
}

function Key([int]$code, [string]$label) {
    Write-Host "key: $label"
    & $adb -s $serial shell input keyevent $code | Out-Null
    Start-Sleep -Milliseconds 650
}

& $adb -s $serial shell am force-stop sc.fawanews.app
Start-Sleep -Milliseconds 400
& $adb -s $serial shell am start -n $pkg
Start-Sleep -Seconds 3
Capture "01-launch"

# Menu: Live (focused) -> Scores -> News -> Live
Key 20 "DPAD_DOWN"   # Scores
Capture "02-menu-scores"
Key 23 "DPAD_CENTER"
Start-Sleep -Seconds 2
Capture "03-scores-tab"

Key 19 "DPAD_UP"     # back toward Live - may need multiple
Key 19 "DPAD_UP"
Key 20 "DPAD_DOWN"
Key 20 "DPAD_DOWN"   # News
Capture "04-menu-news"
Key 23 "DPAD_CENTER"
Start-Sleep -Seconds 2
Capture "05-news-tab"

Key 19 "DPAD_UP"
Key 19 "DPAD_UP"
Key 23 "DPAD_CENTER" # Live
Start-Sleep -Seconds 2
Capture "06-live-tab"

# Move to grid
Key 22 "DPAD_RIGHT"
Start-Sleep -Milliseconds 500
Capture "07-grid-focus"
Key 23 "DPAD_CENTER"
Start-Sleep -Seconds 4
Capture "08-after-select-item"

Key 4 "BACK"
Start-Sleep -Seconds 2
Capture "09-back-home"

Write-Host "Done. Review PNGs in $OutDir"
