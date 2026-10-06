param(
    [string]$Serial = "emulator-5554"
)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

function Get-FocusedLabel {
    & $adb -s $Serial shell uiautomator dump /sdcard/focus.xml 2>$null | Out-Null
    $xml = & $adb -s $Serial shell cat /sdcard/focus.xml 2>$null
    if (-not $xml) { return "(no dump)" }
    if ($xml -match 'focused="true"[^>]*>.*?text="([^"]*)"') { return $Matches[1] }
    if ($xml -match 'focused="true"') {
        if ($xml -match 'content-desc="([^"]+)"[^>]*focused="true"') { return $Matches[1] }
        return "(focused, no text)"
    }
    return "(nothing focused)"
}

function Send-Key([int]$code, [string]$name) {
    Start-Sleep -Milliseconds 450
    & $adb -s $Serial shell input keyevent $code | Out-Null
    Start-Sleep -Milliseconds 550
    $focus = Get-FocusedLabel
    [pscustomobject]@{ Key = $name; Focus = $focus }
}

Write-Host "=== FawaNews TV D-pad test ($Serial) ===" -ForegroundColor Cyan
$results = @()

# Home / Live
$results += Send-Key 20 "DPAD_DOWN"      # menu Live -> News/Scores area
$results += Send-Key 20 "DPAD_DOWN"
$results += Send-Key 22 "DPAD_RIGHT"     # into grid
$results += Send-Key 20 "DPAD_DOWN"      # grid down
$results += Send-Key 21 "DPAD_LEFT"      # grid left
$results += Send-Key 19 "DPAD_UP"        # grid up
$results += Send-Key 22 "DPAD_RIGHT"
$results += Send-Key 23 "DPAD_CENTER"    # open stream

Start-Sleep -Seconds 8
Write-Host "`n--- Player screen ---" -ForegroundColor Yellow
$playerFocus = Get-FocusedLabel
Write-Host "Focus after open: $playerFocus"

$results += Send-Key 23 "DPAD_CENTER (play/pause)"
$results += Send-Key 20 "DPAD_DOWN"
$results += Send-Key 19 "DPAD_UP"
$results += Send-Key 4  "BACK"

Start-Sleep -Seconds 2
Write-Host "`n--- Back on home ---" -ForegroundColor Yellow

# Scores tab via menu
$results += Send-Key 20 "DPAD_DOWN"  # menu
$results += Send-Key 20 "DPAD_DOWN"
$results += Send-Key 23 "DPAD_CENTER"  # Scores (depends on focus order)

Start-Sleep -Seconds 3
$results += Send-Key 22 "DPAD_RIGHT"   # league chips
$results += Send-Key 22 "DPAD_RIGHT"
$results += Send-Key 20 "DPAD_DOWN"    # score list

Write-Host "`n=== Focus trail ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize

Write-Host "`n=== Logcat (errors) ===" -ForegroundColor Cyan
& $adb -s $Serial logcat -d -t 40 | Select-String -Pattern "FATAL|AndroidRuntime|403 Forbidden|ExoPlayerImplInternal.*Error" | Select-Object -Last 10

$shotDir = "C:\Users\opencode\Documents\opencode\Projects\fawanews\screenshots"
New-Item -ItemType Directory -Force -Path $shotDir | Out-Null
cmd /c "`"$adb`" -s $Serial exec-out screencap -p > `"$shotDir\tv-dpad-test.png`""
