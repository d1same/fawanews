# Quick check: event pages -> m3u8 probe (same Referer rules as the app)
$Base = "http://www.fawanews.sc/"
$Headers = @{
    Referer  = $Base
    Origin   = "http://www.fawanews.sc"
    "User-Agent" = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
}

$homeHtml = curl.exe -sL $Base
$links = [regex]::Matches($homeHtml, 'href="([^"]+\.html)"') | ForEach-Object { $_.Groups[1].Value } |
    Where-Object { $_ -ne "index.html" } | Select-Object -Unique -First 12

foreach ($link in $links) {
    $page = curl.exe -sL "$Base$link"
    if ($page -notmatch 'var videos =\s*(\[[^\]]+\])') {
        Write-Host "[skip] $link (no videos array)"
        continue
    }
    $json = $Matches[1]
    $urls = [regex]::Matches($json, 'https?://[^"\s]+') | ForEach-Object { $_.Value }
    foreach ($u in $urls) {
        $body = curl.exe -sL $u -H "Referer: $($Headers.Referer)" -H "Origin: $($Headers.Origin)" -H "User-Agent: $($Headers['User-Agent'])" -r 0-512
        $ok = $body -match '^#EXTM3U' -and ($body -match '#EXTINF|#EXT-X-STREAM-INF')
        Write-Host ("[{0}] {1} -> {2}" -f ($(if ($ok) { 'OK' } else { 'FAIL' })), $link, $u)
    }
}
