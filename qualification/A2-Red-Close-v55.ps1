$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\java\industries\leeway\pocket\OwnerSurfaceWindow.kt'
$h=Join-Path $r 'app\src\main\assets\round-box-leeway.html'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($f in @($p,$h,$v)){Copy-Item -LiteralPath $f -Destination ($f+'.a2-red-close-before.bak') -Force}
$t=[IO.File]::ReadAllText($p)
$old='fun button(symbol: String, action: () -> Unit) {'
if(!$t.Contains($old)){throw 'BUTTON_SOURCE_MISMATCH'}
$t=$t.Replace($old,'fun button(symbol: String, danger: Boolean = false, action: () -> Unit) {')
$anchor='text = symbol'
if(!$t.Contains($anchor)){throw 'BUTTON_STYLE_ANCHOR_MISMATCH'}
$t=$t.Replace($anchor,'text = symbol'+[Environment]::NewLine+'                if(danger) { setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(185,28,28)); contentDescription = "Close this window" }')
$t=$t.Replace('button("A-") { activity.finish() }','button("X", danger = true) { activity.finish() }')
[IO.File]::WriteAllText($p,$t,(New-Object System.Text.UTF8Encoding($false)))
$s=[IO.File]::ReadAllText($h)
$old='#roundbox-close { border:1px solid rgba(56,189,248,.5); border-radius:50%; width:30px; height:30px; color:white; background:rgba(0,0,0,.28); cursor:pointer; }'
$new='#roundbox-close { border:2px solid #fca5a5; border-radius:50%; width:36px; height:36px; color:#fff; background:#b91c1c; cursor:pointer; }'
if(!$s.Contains($old)){throw 'ROUND_CLOSE_STYLE_CHANGED'}
$s=$s.Replace($old,$new)
[IO.File]::WriteAllText($h,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 54')){throw 'VERSION_CHANGED'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 54','versionCode = 55').Replace('1.0.0-roundbox-resize-stable-rc27','1.0.0-ui-red-close-rc28'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $p,$h,$v -Algorithm SHA256 | ForEach-Object { $_.Path+' '+$_.Hash }
