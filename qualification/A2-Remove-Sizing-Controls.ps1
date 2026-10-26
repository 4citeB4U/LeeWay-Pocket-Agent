$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$h=Join-Path $r 'app\src\main\assets\round-box-leeway.html'
$k=Join-Path $r 'app\src\main\java\industries\leeway\pocket\RoundBoxWindow.kt'
Copy-Item $h ($h+'.a2-before-remove-sizing-controls.bak') -Force
Copy-Item $k ($k+'.a2-before-remove-sizing-controls.bak') -Force
$s=[IO.File]::ReadAllLines($h)
$s=@($s | Where-Object {$_ -notmatch '^#roundbox-size-tools' -and $_ -notmatch '^<div id="roundbox-size-tools"'})
[IO.File]::WriteAllLines($h,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($k)
$start=$t.IndexOf('        @JavascriptInterface fun roundBoxScale(')
$end=$t.IndexOf('        @JavascriptInterface fun roundBoxResize(', $start)
if($start -lt 0 -or $end -lt 0){throw 'EXPECTED_CALLBACK_NOT_FOUND'}
$t=$t.Remove($start,$end-$start)
[IO.File]::WriteAllText($k,$t,(New-Object System.Text.UTF8Encoding($false)))
$v=Join-Path $r 'app\build.gradle.kts'
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 51')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-before-v52.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 51','versionCode = 52').Replace('1.0.0-roundbox-legibility-rc24','1.0.0-roundbox-edge-resize-rc25'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('NO_PLUS_MINUS='+(-not ([IO.File]::ReadAllText($h)).Contains('roundbox-size-tools')))
Write-Output ('NO_SCALE_CALLBACK='+(-not ([IO.File]::ReadAllText($k)).Contains('roundBoxScale')))
Get-FileHash $h,$k -Algorithm SHA256 | ForEach-Object {$_.Hash}
