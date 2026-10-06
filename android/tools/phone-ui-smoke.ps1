# Phone UI smoke: menu drawer, tabs, open first card.
$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

$serial = $args[0]
if (-not $serial) {
    $serial = (& $adb devices | Select-String "emulator-\d+\s+device" | Select-Object -First 1).ToString().Split()[0]
}
if (-not $serial) { throw "No phone emulator connected" }

function Adb { param([string[]]$Cmd) & $adb -s $serial @Cmd }

Write-Host "Device: $serial"
Adb shell am force-stop sc.fawanews.app
Adb shell am start -n sc.fawanews.app/.MainActivity
Start-Sleep -Seconds 10

$dump = Join-Path $env:TEMP "fawanews-ui.xml"
Adb shell uiautomator dump /sdcard/window_dump.xml | Out-Null
Adb pull /sdcard/window_dump.xml $dump | Out-Null
[xml]$ui = Get-Content $dump
$nodes = $ui.hierarchy.node

function Find-Node([string]$Desc) {
    $nodes | Where-Object { $_.content-desc -eq $Desc -or $_.text -eq $Desc } | Select-Object -First 1
}

$menu = Find-Node "Open menu"
if (-not $menu) { $menu = $nodes | Where-Object { $_.'resource-id' -match 'menu' } | Select-Object -First 1 }
if ($menu) {
    $b = $menu.bounds -replace '\[|\]', '' -split ','
    $x = [int](([int]$b[0] + [int]$b[2]) / 2)
    $y = [int](([int]$b[1] + [int]$b[3]) / 2)
    Write-Host "Open menu tap $x,$y"
    Adb shell input tap $x $y
    Start-Sleep -Seconds 2
} else {
    Write-Host "WARN: menu button not found, tap top-left"
    Adb shell input tap 100 220
    Start-Sleep -Seconds 2
}

foreach ($label in @("News", "Scores", "Live")) {
    Adb shell uiautomator dump /sdcard/window_dump.xml | Out-Null
    Adb pull /sdcard/window_dump.xml $dump | Out-Null
    [xml]$ui = Get-Content $dump
    $tab = $ui.hierarchy.node | Where-Object { $_.text -eq $label } | Select-Object -First 1
    if ($tab) {
        $b = $tab.bounds -replace '\[|\]', '' -split ','
        $x = [int](([int]$b[0] + [int]$b[2]) / 2)
        $y = [int](([int]$b[1] + [int]$b[3]) / 2)
        Write-Host "Tab $label -> $x,$y"
        Adb shell input tap $x $y
        Start-Sleep -Seconds 3
    } else {
        Write-Host "WARN: tab $label not found"
    }
}

Adb shell uiautomator dump /sdcard/window_dump.xml | Out-Null
Adb pull /sdcard/window_dump.xml $dump | Out-Null
[xml]$ui = Get-Content $dump
$clickable = $ui.hierarchy.node | Where-Object { $_.clickable -eq 'true' -and $_..'class' -match 'Button|View' } | Select-Object -First 5
Write-Host "Clickable nodes (sample): $($clickable.Count)"

# Tap center grid area (first card)
Adb shell input tap 540 1200
Start-Sleep -Seconds 4
$focus = Adb shell dumpsys window | Select-String "mCurrentFocus"
Write-Host "Focus after card tap: $focus"
Write-Host "SMOKE DONE"
