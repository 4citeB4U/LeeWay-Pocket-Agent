$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$h=Join-Path $r 'app\src\main\assets\round-box-leeway.html'
$k=Join-Path $r 'app\src\main\java\industries\leeway\pocket\RoundBoxWindow.kt'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($h,$k,$v)){Copy-Item $p ($p+'.a2-v54-before.bak') -Force}
$s=[IO.File]::ReadAllText($h)
$old='currentWidth = Math.max(260, Math.min((window.LeeWayAndroid?.roundBoxMaxWidth?.() || window.innerWidth * 0.94), w));'
$new='currentWidth = Math.max(280, Math.min((window.LeeWayAndroid?.roundBoxMaxWidth?.() || window.innerWidth * 0.94), w));'
if(!$s.Contains($old)){throw 'WIDTH_ANCHOR_CHANGED'}
$s=$s.Replace($old,$new)
$old='currentHeight = Math.max(200, Math.min((window.LeeWayAndroid?.roundBoxMaxHeight?.() || window.innerHeight * 0.85), h));'
$new='currentHeight = Math.max(250, Math.min((window.LeeWayAndroid?.roundBoxMaxHeight?.() || window.innerHeight * 0.85), h, currentWidth*1.5));'
if(!$s.Contains($old)){throw 'HEIGHT_ANCHOR_CHANGED'}
$s=$s.Replace($old,$new)
$old='boxWrapper.style.height = currentHeight + ''px'';'
$new=$old+[Environment]::NewLine+'        window.LeeWayAndroid?.roundBoxResize?.(currentWidth,currentHeight);'
if(!$s.Contains($old)){throw 'SIZE_DISPATCH_ANCHOR_CHANGED'}
$s=$s.Replace($old,$new)
$s=[regex]::Replace($s,'(?s) const oldSize=window\.updateBoxSize;.*?\n };','')
[IO.File]::WriteAllText($h,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($k)
$old='p.height=dp(height.toInt().coerceAtLeast(220)).coerceAtMost(b.height())'
$new='p.height=dp(height.toInt().coerceAtLeast(250)).coerceAtMost(b.height()).coerceAtMost((p.width*1.5).toInt())'
if(!$t.Contains($old)){throw 'NATIVE_HEIGHT_ANCHOR_CHANGED'}
$t=$t.Replace($old,$new)
[IO.File]::WriteAllText($k,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 53')){throw 'VERSION_CHANGED'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 53','versionCode = 54').Replace('1.0.0-roundbox-edge-resize-rc26','1.0.0-roundbox-resize-stable-rc27'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML='+ (Get-FileHash $h -Algorithm SHA256).Hash)
Write-Output ('NATIVE='+ (Get-FileHash $k -Algorithm SHA256).Hash)