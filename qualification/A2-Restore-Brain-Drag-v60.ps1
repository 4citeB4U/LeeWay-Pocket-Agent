$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$h=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$k=Join-Path $r 'app\src\main\java\industries\leeway\pocket\FloatingSphereWindow.kt'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($h,$k,$v)){Copy-Item $p ($p+'.a2-v60-before.bak') -Force}
$s=[IO.File]::ReadAllText($h)
$old='if(g.mode==="rotate"){yaw+=dx*.014;pitch=Math.max(-1.25,Math.min(1.25,pitch+dy*.012))}else{movePending=movePending?[movePending[0]+dx,movePending[1]+dy]:[dx,dy];flushMove()}'
if(!$s.Contains($old) -and !$s.Contains('yaw+=dx*.014;pitch=')){throw 'HTML_GESTURE_CHANGED'}
$s=$s.Replace($old,'yaw+=dx*.014;pitch=Math.max(-1.25,Math.min(1.25,pitch+dy*.012))')
[IO.File]::WriteAllText($h,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($k)
$old='else false'
$native='else if(dragPointer>=0&&event.pointerCount==1){val dx=event.rawX-startRawX;val dy=event.rawY-startRawY;if(kotlin.math.hypot(dx,dy)>ViewConfiguration.get(service).scaledTouchSlop){gestureMoved=true;lp.x=startX+dx.toInt();lp.y=startY+dy.toInt();relayout();true}else false}else false'
$anchor='                    else false'
if(!$t.Contains($anchor)){throw 'NATIVE_MOVE_CHANGED'}
$t=$t.Replace($anchor,'                    '+$native)
[IO.File]::WriteAllText($k,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 59')){throw 'VERSION_CHANGED'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 59','versionCode = 60').Replace('1.0.0-brain-move-rotate-rc32','1.0.0-brain-native-drag-rc33'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_HASH='+(Get-FileHash $h -Algorithm SHA256).Hash)
Write-Output ('NATIVE_HASH='+(Get-FileHash $k -Algorithm SHA256).Hash)
