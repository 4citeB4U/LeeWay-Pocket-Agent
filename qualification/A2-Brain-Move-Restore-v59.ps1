$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\agent_lee_sphere_transparent.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-move-restore-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
$old='if(!hit(e))return;gesture={id:e.pointerId,'
$new='gesture={mode:hit(e)?"rotate":"move",id:e.pointerId,'
if(!$s.Contains($old)){throw 'POINTER_DOWN_CHANGED'}
$s=$s.Replace($old,$new)
$old='yaw+=dx*.014;pitch=Math.max(-1.25,Math.min(1.25,pitch+dy*.012))'
$new='if(g.mode==="rotate"){yaw+=dx*.014;pitch=Math.max(-1.25,Math.min(1.25,pitch+dy*.012))}else{movePending=movePending?[movePending[0]+dx,movePending[1]+dy]:[dx,dy];flushMove()}'
if(!$s.Contains($old)){throw 'POINTER_MOVE_CHANGED'}
$s=$s.Replace($old,$new)
$old='!g.moved&&performance.now()-g.at<600&&!blocked()&&hit(e)'
$new='!g.moved&&g.mode==="rotate"&&performance.now()-g.at<600&&!blocked()&&hit(e)'
if(!$s.Contains($old)){throw 'POINTER_UP_CHANGED'}
$s=$s.Replace($old,$new)
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 58')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-before-v59.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 58','versionCode = 59').Replace('1.0.0-brain-3d-gesture-rc31','1.0.0-brain-move-rotate-rc32'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_HASH='+(Get-FileHash $p -Algorithm SHA256).Hash)
