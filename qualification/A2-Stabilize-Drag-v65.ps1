$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$h=Join-Path $r 'app\src\main\assets\round-box-leeway.html'
$k=Join-Path $r 'app\src\main\java\industries\leeway\pocket\RoundBoxWindow.kt'
$v=Join-Path $r 'app\build.gradle.kts'
foreach($p in @($h,$k,$v)){Copy-Item $p ($p+'.a2-drag-stability-before.bak') -Force}
$s=[IO.File]::ReadAllText($h)
$old=' let drag=null;'
$new=' let drag=null, pendingX=0, pendingY=0, moveScheduled=false;'+[Environment]::NewLine+@'
 const flushDrag=()=>{
   moveScheduled=false;
   const dx=pendingX,dy=pendingY;
   pendingX=0;pendingY=0;
   if((dx||dy)&&Number.isFinite(dx)&&Number.isFinite(dy))window.LeeWayAndroid.roundBoxMove?.(dx,dy);
 };
'@
if(!$s.Contains($old)){throw 'DRAG_STATE_CHANGED'}
$s=[regex]::Replace($s,'if\(Number\.isFinite\(dx\)&&Number\.isFinite\(dy\)&&Math\.abs\(dx\)\+Math\.abs\(dy\)>0\)\s*window\.LeeWayAndroid\.roundBoxMove\?\.\(dx,dy\);',$new,1)
$old=@'
   if(Number.isFinite(dx)&&Number.isFinite(dy)&&Math.abs(dx)+Math.abs(dy)>0)
     window.LeeWayAndroid.roundBoxMove?.(dx,dy);
'@
$new=@'
   if(Number.isFinite(dx)&&Number.isFinite(dy)&&Math.abs(dx)+Math.abs(dy)>0){
     pendingX+=dx;pendingY+=dy;
     if(!moveScheduled){moveScheduled=true;requestAnimationFrame(flushDrag)}
   }
'@
if(!$s.Contains($old) -and !$s.Contains('window.LeeWayAndroid.roundBoxMove?.(dx,dy);')){throw 'DRAG_CALL_CHANGED'}
$s=[regex]::Replace($s,'if\(Number\.isFinite\(dx\)&&Number\.isFinite\(dy\)&&Math\.abs\(dx\)\+Math\.abs\(dy\)>0\)\s*window\.LeeWayAndroid\.roundBoxMove\?\.\(dx,dy\);',$new,1)
[IO.File]::WriteAllText($h,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($k)
$old='val p=lp?:return@post'+[Environment]::NewLine+'            val b=wm.currentWindowMetrics.bounds'
$new='val p=lp?:return@post'+[Environment]::NewLine+'            if(!dx.isFinite()||!dy.isFinite()||kotlin.math.abs(dx)>800||kotlin.math.abs(dy)>800)return@post'+[Environment]::NewLine+'            val b=wm.currentWindowMetrics.bounds'
if(!$t.Contains('val p=lp?:return@post')){throw 'NATIVE_MOVE_CHANGED'}
$t=[regex]::Replace($t,'(@JavascriptInterface fun roundBoxMove\(dx:Double,dy:Double\)\{main\.post\{\s*val p=lp\?:return@post)','$1'+[Environment]::NewLine+'            if(!dx.isFinite()||!dy.isFinite()||kotlin.math.abs(dx)>800||kotlin.math.abs(dy)>800)return@post',1)
[IO.File]::WriteAllText($k,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 64')){throw 'VERSION_CHANGED'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 64','versionCode = 65').Replace('1.0.0-roundbox-single-visual-rc37','1.0.0-roundbox-drag-stable-rc38'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $h,$k -Algorithm SHA256 | ForEach-Object{$_.Path+' '+$_.Hash}
