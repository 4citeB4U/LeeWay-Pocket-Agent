$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$html=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$native=Join-Path $r 'app\src\main\java\industries\leeway\pocket\FloatingSphereWindow.kt'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($html,$native,$v)){Copy-Item $p ($p+'.a2-3d-nav-before.bak') -Force}
$s=[IO.File]::ReadAllText($html)
$old='movePending=movePending?[movePending[0]+dx,movePending[1]+dy]:[dx,dy];flushMove()'
$new='yaw+=dx*.014;pitch=Math.max(-1.25,Math.min(1.25,pitch+dy*.012))'
if(!$s.Contains($old)){throw 'HTML_GESTURE_ANCHOR_CHANGED'}
$s=$s.Replace($old,$new)
[IO.File]::WriteAllText($html,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($native)
$old='else if(dragPointer>=0&&event.pointerCount==1){val dx=event.rawX-startRawX;val dy=event.rawY-startRawY;if(kotlin.math.hypot(dx,dy)>ViewConfiguration.get(service).scaledTouchSlop){gestureMoved=true;lp.x=startX+dx.toInt();lp.y=startY+dy.toInt();relayout();true}else false}else false'
if(!$t.Contains($old)){throw 'NATIVE_GESTURE_ANCHOR_CHANGED'}
$t=$t.Replace($old,'else false')
[IO.File]::WriteAllText($native,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 57')){throw 'VERSION_CHANGED'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 57','versionCode = 58').Replace('1.0.0-continuum-hash-restore-rc30','1.0.0-brain-3d-gesture-rc31'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_HASH='+(Get-FileHash $html -Algorithm SHA256).Hash)
Write-Output ('NATIVE_HASH='+(Get-FileHash $native -Algorithm SHA256).Hash)
