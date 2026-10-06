# Open and close a live game several times on the Shield, then report LeakCanary findings and memory.
param(
    [string]$Title = "New Orleans Saints vs Atlanta Falcons",
    [int]$Rounds = 5,
    [string]$Serial = "10.1.20.11:5555"
)
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$pkg = "sc.fawanews.app"

function Find-Card([string]$text) {
    & $adb -s $Serial shell uiautomator dump /sdcard/fawa_ui.xml | Out-Null
    $xml = (& $adb -s $Serial shell cat /sdcard/fawa_ui.xml) -join ""
    $i = $xml.IndexOf("text=`"$text")
    if ($i -lt 0) { return $null }
    $chunk = $xml.Substring([Math]::Max(0, $i - 3000), [Math]::Min(3000, $i))
    $hits = [regex]::Matches($chunk, 'clickable="true"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if ($hits.Count -eq 0) { return $null }
    $h = $hits[$hits.Count - 1]
    return @([int](([int]$h.Groups[1].Value + [int]$h.Groups[3].Value) / 2), [int](([int]$h.Groups[2].Value + [int]$h.Groups[4].Value) / 2))
}

function Pss {
    $m = (& $adb -s $Serial shell dumpsys meminfo $pkg) -join "`n"
    return ([regex]::Match($m, 'TOTAL PSS:\s+(\d+)|TOTAL\s+(\d+)')).Groups | Where-Object { $_.Success -and $_.Value -match '^\d+$' } | Select-Object -First 1 -ExpandProperty Value
}

& $adb -s $Serial shell am force-stop $pkg | Out-Null
& $adb -s $Serial logcat -c
& $adb -s $Serial shell am start -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -n "$pkg/.TvLauncher" | Out-Null
Start-Sleep -Seconds 8
$startPss = Pss
Write-Output "start PSS KB: $startPss"

for ($r = 1; $r -le $Rounds; $r++) {
    $pos = Find-Card $Title
    if (-not $pos) { Write-Output "round ${r}: card not found"; break }
    & $adb -s $Serial shell input tap $pos[0] $pos[1] | Out-Null
    Start-Sleep -Seconds 12
    $playPss = Pss
    & $adb -s $Serial shell input keyevent KEYCODE_BACK | Out-Null
    Start-Sleep -Seconds 4
    $homePss = Pss
    Write-Output "round ${r}: playing PSS $playPss KB, after close $homePss KB"
}

& $adb -s $Serial shell input keyevent KEYCODE_HOME | Out-Null
Start-Sleep -Seconds 4
& $adb -s $Serial shell am start -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -n "$pkg/.TvLauncher" | Out-Null
Start-Sleep -Seconds 15
Write-Output "end PSS KB: $(Pss)"

$log = & $adb -s $Serial logcat -d -s LeakCanary 2>$null
$log | Select-String -Pattern "retained|Retained|LEAK|leak|HEAP ANALYSIS|Watching" | Select-Object -Last 25 | ForEach-Object { $_.Line }
