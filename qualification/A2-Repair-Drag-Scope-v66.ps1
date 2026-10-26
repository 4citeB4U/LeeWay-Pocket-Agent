$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\assets\round-box-leeway.html'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-drag-scope-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
$start=$s.IndexOf(' let drag=null;', $s.IndexOf('leeway-roundbox-native-bridge'))
$end=$s.IndexOf(" header?.addEventListener('pointerup',end,true);",$start)
if($start -lt 0 -or $end -lt 0){throw 'DRAG_BLOCK_NOT_FOUND'}
$replacement=@'
 let drag=null,pendingX=0,pendingY=0,moveScheduled=false;
 const header=document.querySelector('.bubble-header');
 const flushDrag=()=>{
   moveScheduled=false;
   const dx=pendingX,dy=pendingY;
   pendingX=0;pendingY=0;
   if((dx||dy)&&Number.isFinite(dx)&&Number.isFinite(dy))window.LeeWayAndroid.roundBoxMove?.(dx,dy);
 };
 header?.style.setProperty('touch-action','none');
 header?.addEventListener('pointerdown',e=>{
   if(e.target.closest('button'))return;
   drag={id:e.pointerId,x:e.screenX,y:e.screenY};
   header.setPointerCapture(e.pointerId);
   e.stopImmediatePropagation();
 },true);
 header?.addEventListener('pointermove',e=>{
   if(!drag||drag.id!==e.pointerId)return;
   const dx=e.screenX-drag.x,dy=e.screenY-drag.y;
   drag.x=e.screenX;drag.y=e.screenY;
   if(Number.isFinite(dx)&&Number.isFinite(dy)&&Math.abs(dx)+Math.abs(dy)>0){
     pendingX+=dx;pendingY+=dy;
     if(!moveScheduled){moveScheduled=true;requestAnimationFrame(flushDrag)}
   }
   e.stopImmediatePropagation();
 },true);
 const end=e=>{if(drag?.id===e.pointerId){drag=null;if(moveScheduled)flushDrag()}};
'@
$s=$s.Substring(0,$start)+$replacement+[Environment]::NewLine+$s.Substring($end)
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 65')){throw 'VERSION_CHANGED'}
Copy-Item $v ($v+'.a2-v66-before.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 65','versionCode = 66').Replace('1.0.0-roundbox-drag-stable-rc38','1.0.0-roundbox-drag-scope-rc39'),(New-Object System.Text.UTF8Encoding($false)))
Write-Output ('HTML_SHA256='+(Get-FileHash $p -Algorithm SHA256).Hash)
