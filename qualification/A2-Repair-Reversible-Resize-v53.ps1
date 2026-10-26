$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$h=Join-Path $r 'app\src\main\assets\round-box-leeway.html'
$k=Join-Path $r 'app\src\main\java\industries\leeway\pocket\RoundBoxWindow.kt'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($h,$k,$v)){Copy-Item -LiteralPath $p -Destination ($p+'.a2-resize-v53-before.bak') -Force}
$s=[IO.File]::ReadAllText($h)
$old='Math.min(window.innerWidth * 0.94, w)'
$new='Math.min((window.LeeWayAndroid?.roundBoxMaxWidth?.() || window.innerWidth * 0.94), w)'
if(!$s.Contains($old)){throw 'UNEXPECTED_WIDTH_SOURCE'}
$s=$s.Replace($old,$new)
$old='Math.min(window.innerHeight * 0.85, h)'
$new='Math.min((window.LeeWayAndroid?.roundBoxMaxHeight?.() || window.innerHeight * 0.85), h)'
if(!$s.Contains($old)){throw 'UNEXPECTED_HEIGHT_SOURCE'}
$s=$s.Replace($old,$new)
[IO.File]::WriteAllText($h,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($k)
$anchor='        @JavascriptInterface fun roundBoxResize(width:Double,height:Double){main.post{'
if(!$t.Contains($anchor)){throw 'UNEXPECTED_NATIVE_BRIDGE'}
$t=$t.Replace($anchor, '        @JavascriptInterface fun roundBoxMaxWidth():Double=(wm.currentWindowMetrics.bounds.width()/density-24).toDouble()' + [Environment]::NewLine + '        @JavascriptInterface fun roundBoxMaxHeight():Double=(wm.currentWindowMetrics.bounds.height()/density-36).toDouble()'+[Environment]::NewLine+$anchor)
$t=$t.Replace('dp(width.toInt().coerceIn(220,600)).coerceAtMost(b.width())','dp(width.toInt().coerceAtLeast(220)).coerceAtMost(b.width())')
$t=$t.Replace('dp(height.toInt().coerceIn(220,700)).coerceAtMost(b.height())','dp(height.toInt().coerceAtLeast(220)).coerceAtMost(b.height())')
[IO.File]::WriteAllText($k,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 52')){throw 'VERSION_CHANGED'}
$a=$a.Replace('versionCode = 52','versionCode = 53').Replace('1.0.0-roundbox-edge-resize-rc25','1.0.0-roundbox-edge-resize-rc26')
[IO.File]::WriteAllText($v,$a,(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_SHA256='+(Get-FileHash $h -Algorithm SHA256).Hash)
Write-Output ('NATIVE_SHA256='+(Get-FileHash $k -Algorithm SHA256).Hash)
Write-Output ('VERSION_SHA256='+(Get-FileHash $v -Algorithm SHA256).Hash)
