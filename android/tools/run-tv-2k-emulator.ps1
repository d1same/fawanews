# Launch TV AVD and set 2560x1440 (2K) for layout/player testing
$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$Sdk\platform-tools\adb.exe"
$emu = "$Sdk\emulator\emulator.exe"
$Avd = "Triboon_TV_API_36"

Start-Process -FilePath $emu -ArgumentList "-avd", $Avd -WindowStyle Normal
Write-Host "Waiting for emulator..."
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 3
    $devices = & $adb devices 2>&1 | Out-String
    if ($devices -match "emulator-\d+\s+device") { break }
}

$serial = (& $adb devices | Select-String "emulator-\d+\s+device" | ForEach-Object { ($_ -split "\s+")[0] } | Select-Object -First 1)
if (-not $serial) { Write-Error "No emulator"; exit 1 }

& $adb -s $serial shell wm size 2560x1440
& $adb -s $serial shell wm density 320
Write-Host "TV emulator $serial at 2560x1440 (2K). Install: .\gradlew.bat installDebug"
