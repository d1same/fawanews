# Check every live stream on fawanews.sc: playlist type, codecs, resolutions, segment format.
$ErrorActionPreference = "Continue"
$ua = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
$base = "http://www.fawanews.sc/"

function Get-Text([string]$url, [int]$maxBytes = 65536) {
    $curlArgs = @("-sS", "-L", "--max-time", "12", "-A", $ua, "-H", "Referer: $base", "-H", "Origin: http://www.fawanews.sc",
        "-r", "0-$($maxBytes - 1)", "-w", "`n__STATUS__%{http_code} %{content_type} %{url_effective}", $url)
    $raw = & curl.exe @curlArgs 2>&1 | Out-String
    $marker = $raw.LastIndexOf("__STATUS__")
    if ($marker -lt 0) { return @{ Status = "000"; Type = ""; Url = $url; Body = $raw } }
    $meta = $raw.Substring($marker + 10).Trim().Split(" ", 3)
    return @{ Status = $meta[0]; Type = $meta[1]; Url = $meta[2]; Body = $raw.Substring(0, $marker) }
}

function Resolve([string]$from, [string]$ref) {
    if ($ref -match '^https?://') { return $ref }
    return ([Uri]::new([Uri]$from, $ref)).AbsoluteUri
}

function Describe-Media([string]$url) {
    $r = Get-Text $url
    $b = $r.Body
    $segments = [regex]::Matches($b, '(?m)^[^#\s].*$') | ForEach-Object { $_.Value.Trim() }
    $first = $segments | Select-Object -First 1
    $ext = if ($first) { ([regex]::Match($first, '\.([a-z0-9]+)(\?|$)', 'IgnoreCase')).Groups[1].Value } else { "" }
    $dur = [regex]::Matches($b, '#EXTINF:([\d.]+)') | ForEach-Object { [double]$_.Groups[1].Value }
    $avg = if ($dur.Count) { [math]::Round(($dur | Measure-Object -Average).Average, 1) } else { 0 }
    $target = ([regex]::Match($b, '#EXT-X-TARGETDURATION:(\d+)')).Groups[1].Value
    $seq = ([regex]::Match($b, '#EXT-X-MEDIA-SEQUENCE:(\d+)')).Groups[1].Value
    $flags = @()
    if ($b -match '#EXT-X-DISCONTINUITY') { $flags += "discontinuity" }
    if ($b -match '#EXT-X-KEY') { $flags += "encrypted:" + ([regex]::Match($b, 'METHOD=([A-Z0-9-]+)')).Groups[1].Value }
    if ($b -match '#EXT-X-MAP') { $flags += "fmp4-init" }
    if ($b -match '#EXT-X-PROGRAM-DATE-TIME') { $flags += "pdt" }
    if ($b -match '#EXT-X-ENDLIST') { $flags += "ENDLIST" }
    $segInfo = ""
    if ($first) {
        $segUrl = Resolve $r.Url $first
        $s = Get-Text $segUrl 4096
        $head = $s.Body
        $magic = if ($head.Length -gt 0 -and [int][char]$head[0] -eq 0x47) { "mpegts" } elseif ($head -match 'ftyp|moof|styp') { "fmp4" } elseif ($head -match '^\x89PNG|PNG') { "PNG-disguised" } elseif ($head -match 'JFIF|GIF8') { "image-disguised" } else { "unknown" }
        $segInfo = "seg=$($s.Status) $($s.Type) magic=$magic host=$(([Uri]$segUrl).Host)"
    }
    return "media=$($r.Status) segs=$($segments.Count) ext=$ext avgDur=$avg target=$target seq=$seq $($flags -join ',') $segInfo"
}

$index = Get-Text ($base + "?_=" + [DateTimeOffset]::Now.ToUnixTimeMilliseconds()) 400000
$pages = [regex]::Matches($index.Body, 'href="([^"]+\.html)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique

foreach ($page in $pages) {
    if ($page -in @("index.html", "contact.html", "privacy_policy.html")) { continue }
    $pageUrl = $base + ($page -replace ' ', '%20')
    $p = Get-Text $pageUrl 400000
    # Follow whichever array the player reads (the site keeps renaming it).
    $sourceNames = [regex]::Matches($p.Body, 'source\s*:\s*(\w+)\s*\[') | ForEach-Object { $_.Groups[1].Value }
    $arrays = @([regex]::Matches($p.Body, 'var\s+(\w+)\s*=\s*(\[[^\]]*\])') |
        Where-Object { $_.Groups[1].Value -in $sourceNames -or $_.Groups[1].Value -like 'videos*' })
    if ($arrays.Count -eq 0) {
        if ($p.Body -match 'm3u8') { Write-Output "$page | NO player array but page mentions m3u8" }
        continue
    }
    $urls = $arrays | ForEach-Object { [regex]::Matches($_.Groups[2].Value, 'https?://[^"''\s,\]]+') | ForEach-Object { $_.Value } } | Select-Object -Unique
    if (-not $urls) {
        # Split links: var p1 = "http://.../hls/"; var p2 = "name"; var p3 = ".m3u8"; [p1 + p2 + p3]
        $scriptStart = $p.Body.LastIndexOf("<script", $arrays[0].Index)
        $declared = $p.Body.Substring($scriptStart, $arrays[0].Index - $scriptStart)
        $joined = ([regex]::Matches($declared, 'var\s+\w+\s*=\s*["'']([^"'']*)["'']') | ForEach-Object { $_.Groups[1].Value }) -join ""
        if ($joined -match '^https?://') { $urls = @($joined) } else { Write-Output "$page | videos array is empty"; continue }
    }
    $n = 0
    foreach ($u in $urls) {
        $n++
        $top = Get-Text $u
        $kind = if ($top.Body -match '#EXT-X-STREAM-INF') { "master" } elseif ($top.Body -match '#EXTINF') { "media" } elseif ($top.Body -match '<MPD') { "dash" } else { "other" }
        $line = "$page | src$n | $($top.Status) $($top.Type) $kind host=$(([Uri]$u).Host) scheme=$(([Uri]$u).Scheme)"
        if ($kind -eq "master") {
            $variants = [regex]::Matches($top.Body, '#EXT-X-STREAM-INF:([^\r\n]+)\r?\n([^\r\n]+)')
            $desc = $variants | ForEach-Object {
                $attrs = $_.Groups[1].Value
                $res = ([regex]::Match($attrs, 'RESOLUTION=(\d+x\d+)')).Groups[1].Value
                $bw = ([regex]::Match($attrs, '(?<!AVERAGE-)BANDWIDTH=(\d+)')).Groups[1].Value
                $codecs = ([regex]::Match($attrs, 'CODECS="([^"]+)"')).Groups[1].Value
                $fps = ([regex]::Match($attrs, 'FRAME-RATE=([\d.]+)')).Groups[1].Value
                "$res@$fps bw=$bw [$codecs]"
            }
            $line += " variants=" + ($desc -join " ; ")
            if ($top.Body -match 'TYPE=AUDIO') { $line += " separate-audio" }
            $best = $variants | Sort-Object { [int](([regex]::Match($_.Groups[1].Value, '(?<!AVERAGE-)BANDWIDTH=(\d+)')).Groups[1].Value) } | Select-Object -Last 1
            if ($best) { $line += " || best: " + (Describe-Media (Resolve $top.Url $best.Groups[2].Value.Trim())) }
        } elseif ($kind -eq "media") {
            $line += " || " + (Describe-Media $top.Url)
        } else {
            $snippet = ($top.Body -replace '\s+', ' ')
            if ($snippet.Length -gt 120) { $snippet = $snippet.Substring(0, 120) }
            $line += " body='$snippet'"
        }
        Write-Output $line
    }
}
