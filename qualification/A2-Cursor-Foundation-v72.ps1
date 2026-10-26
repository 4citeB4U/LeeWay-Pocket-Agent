$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\java\industries\leeway\pocket\PocketOverlayService.kt'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-cursor-before.bak') -Force
Copy-Item $v ($v+'.a2-cursor-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
$old='private var conversation:RoundBoxWindow?=null'
if(!$s.Contains($old)){throw 'SERVICE_CONFLICT'}
$s=$s.Replace($old,$old+[Environment]::NewLine+'    private var executionCursor:AgentLeeCursorOverlay?=null'+[Environment]::NewLine+'    internal fun showVerifiedCursor(receiptId:String,xPx:Int,yPx:Int){'+[Environment]::NewLine+'        if(executionCursor==null)executionCursor=AgentLeeCursorOverlay(this)'+[Environment]::NewLine+'        executionCursor?.showForExecutedAction(receiptId,xPx,yPx)'+[Environment]::NewLine+'    }'+[Environment]::NewLine+'    internal fun hideVerifiedCursor(receiptId:String){executionCursor?.hide(receiptId)}')
$old2='sphere?.close();sphere=null;conversation?.close();conversation=null;AndroidBrainIngestion.stop();detach()'
if(!$s.Contains($old2)){throw 'LIFECYCLE_CONFLICT'}
$s=$s.Replace($old2,'executionCursor?.close();executionCursor=null;'+$old2)
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 71')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 71','versionCode = 72').Replace('1.0.0-webview-guard-rc44','1.0.0-cursor-foundation-rc45'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash (Join-Path $r 'app\src\main\assets\agent-lee-cursor-v1.svg') -Algorithm SHA256 | Select-Object -ExpandProperty Hash
