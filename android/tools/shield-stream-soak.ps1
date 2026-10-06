# Play live games on the Shield one at a time and report player errors and recoveries.
param(
    # Separate titles with "|" (powershell -File passes one string).
    [string]$Titles = "",
    [int]$Seconds = 45,
    [string]$Serial = "10.1.20.11:5555"
)
$titleList = $Titles.Split("|") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$pkg = "sc.fawanews.app"

function Restart-App {
    & $adb -s $Serial shell am force-stop $pkg | Out-Null
    & $adb -s $Serial shell am start -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER -n "$pkg/.TvLauncher" | Out-Null
    Start-Sleep -Seconds 7
}

function Find-Card([string]$title) {
    for ($scroll = 0; $scroll -lt 6; $scroll++) {
        & $adb -s $Serial shell uiautomator dump /sdcard/fawa_ui.xml | Out-Null
        $xml = (& $adb -s $Serial shell cat /sdcard/fawa_ui.xml) -join ""
        $i = $xml.IndexOf("text=`"$title")
        if ($i -ge 0) {
            $chunk = $xml.Substring([Math]::Max(0, $i - 3000), [Math]::Min(3000, $i))
            $hits = [regex]::Matches($chunk, 'clickable="true"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
            if ($hits.Count -gt 0) {
                $h = $hits[$hits.Count - 1]
                $x = ([int]$h.Groups[1].Value + [int]$h.Groups[3].Value) / 2
                $y = ([int]$h.Groups[2].Value + [int]$h.Groups[4].Value) / 2
                return @([int]$x, [int]$y)
            }
        }
        & $adb -s $Serial shell input swipe 1100 900 1100 400 300 | Out-Null
        Start-Sleep -Seconds 1
    }
    return $null
}

foreach ($title in $titleList) {
    Restart-App
    $pos = Find-Card $title
    if (-not $pos) { Write-Output "$title | NOT FOUND ON SCREEN"; continue }
    & $adb -s $Serial logcat -c
    & $adb -s $Serial shell input tap $pos[0] $pos[1] | Out-Null
    Start-Sleep -Seconds $Seconds
    $log = & $adb -s $Serial logcat -d -v brief 2>$null
    $errors = $log | Select-String -Pattern "FawaPlayer.*Playback error" | ForEach-Object {
        ([regex]::Match($_.Line, 'Playback error (\S+)')).Groups[1].Value
    }
    $audioGaps = ($log | Select-String -Pattern "Unexpected audio track timestamp").Count
    $fps = ($log | Select-String -Pattern "AvgFrameRate = ([\d.]+)" | Select-Object -Last 1)
    $avg = if ($fps) { ([regex]::Match($fps.Line, 'AvgFrameRate = ([\d.]+)')).Groups[1].Value } else { "-" }
    & $adb -s $Serial shell uiautomator dump /sdcard/fawa_ui.xml | Out-Null
    $ui = (& $adb -s $Serial shell cat /sdcard/fawa_ui.xml) -join ""
    $screen = if ($ui -match 'No article text') { "NO ARTICLE (bug)" } elseif ($ui -match 'Refresh stream') { "STOPPED (Refresh screen)" } elseif ($ui -match 'Menu') { "BACK AT HOME" } else { "PLAYING" }
    $errText = if ($errors) { ($errors | Group-Object | ForEach-Object { "$($_.Name)x$($_.Count)" }) -join "," } else { "none" }
    Write-Output "$title | $screen | errors=$errText | audioGaps=$audioGaps | avgFps=$avg"
}
