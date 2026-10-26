$root='E:\LeeWay-Work\agent-lee-fold6-unified'
$html=Join-Path $root 'app\src\main\assets\round-box-leeway.html'
$native=Join-Path $root 'app\src\main\java\industries\leeway\pocket\RoundBoxWindow.kt'
foreach($p in @($html,$native)){ Copy-Item $p ($p+'.a2-before-visibility-resize.bak') -Force }
$s=[IO.File]::ReadAllText($html)
$anchor='#roundbox-drag { display:none !important; } .bubble-header { cursor:move; touch-action:none; }'
if(!$s.Contains($anchor)){throw 'HTML_ANCHOR_CHANGED'}
$add=@'
/* A2 Fold6 legibility and reversible native sizing, without changing the approved bubble shape. */
.cloud-card{background:linear-gradient(160deg,rgba(3,9,22,.97),rgba(5,14,31,.96))!important;color:#f7fbff!important}
.bubble-inline-textarea{background:rgba(1,7,19,.94)!important;color:#ffffff!important;border:1px solid rgba(96,208,255,.64)!important;border-radius:13px;padding:12px!important;text-shadow:none!important;caret-color:#67e8f9}
.bubble-inline-textarea::placeholder{color:#bcd5e5!important;opacity:1}
#stream-content,#text-container{color:#f7fbff!important;text-shadow:0 1px 2px #000}
.bubble-mic-btn{min-width:64px!important;min-height:58px!important;justify-content:center!important;background:#075d85!important;border:2px solid #9aeaff!important;color:white!important;font-size:17px!important;font-weight:850!important;box-shadow:0 0 16px rgba(14,201,255,.6)!important}
.bubble-mic-btn:focus-visible{outline:3px solid #fff;outline-offset:2px}
.bubble-mic-btn.listening{background:#a41432!important;border-color:#ffd2d2!important;color:white!important}
#roundbox-size-tools{position:fixed;left:12px;bottom:5px;z-index:70;display:flex;gap:6px;pointer-events:auto}
#roundbox-size-tools button{min-width:51px;min-height:46px;border:2px solid #8be9ff;border-radius:12px;background:#092942;color:white;font-size:23px;font-weight:900}
'@
$s=$s.Replace($anchor,$anchor+[Environment]::NewLine+$add)
$bodyAnchor='<button type="button" id="roundbox-reopen">Open Agent Lee chat</button>'
if(!$s.Contains($bodyAnchor)){throw 'BODY_ANCHOR_CHANGED'}
$s=$s.Replace($bodyAnchor,$bodyAnchor+[Environment]::NewLine+'<div id="roundbox-size-tools"><button type="button" aria-label="Enlarge Agent Lee conversation box" onclick="window.LeeWayAndroid.roundBoxScale(1)">+</button><button type="button" aria-label="Shrink Agent Lee conversation box" onclick="window.LeeWayAndroid.roundBoxScale(-1)">-</button></div>')
[IO.File]::WriteAllText($html,$s,(New-Object System.Text.UTF8Encoding($false)))
$n=[IO.File]::ReadAllText($native)
$anchor2='        @JavascriptInterface fun roundBoxResize(width:Double,height:Double){main.post{'
if(!$n.Contains($anchor2)){throw 'NATIVE_ANCHOR_CHANGED'}
$bridge=@'
        @JavascriptInterface fun roundBoxScale(direction:Int){main.post{
            val p=lp?:return@post
            if(direction!=1 && direction!=-1)return@post
            val b=wm.currentWindowMetrics.bounds
            p.width=(p.width+dp(65)*direction).coerceIn(dp(220).coerceAtMost(b.width()),b.width())
            p.height=(p.height+dp(75)*direction).coerceIn(dp(220).coerceAtMost(b.height()),b.height())
            p.x=p.x.coerceIn(0,(b.width()-p.width).coerceAtLeast(0))
            p.y=p.y.coerceIn(0,(b.height()-p.height).coerceAtLeast(0))
            web?.let{wm.updateViewLayout(it,p)}
        }}
'@
$n=$n.Replace($anchor2,$bridge+$anchor2)
[IO.File]::WriteAllText($native,$n,(New-Object System.Text.UTF8Encoding($false)))
$v=Join-Path $root 'app\build.gradle.kts'
Copy-Item $v ($v+'.a2-before-v51.bak') -Force
$t=[IO.File]::ReadAllText($v)
if(!$t.Contains('versionCode = 50')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($v,$t.Replace('versionCode = 50','versionCode = 51').Replace('1.0.0-brain-first-roundbox-rc23','1.0.0-roundbox-legibility-rc24'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $html,$native,$v -Algorithm SHA256 | ForEach-Object {$_.Path+' '+$_.Hash}
