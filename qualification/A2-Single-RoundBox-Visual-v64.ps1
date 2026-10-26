$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$p=Join-Path $r 'app\src\main\java\industries\leeway\pocket\PocketVoiceActivity.kt'
$v=Join-Path $r 'app\build.gradle.kts'
Copy-Item $p ($p+'.a2-hidden-presentation-before.bak') -Force
$s=[IO.File]::ReadAllText($p)
$anchor='        container.addView(panel); setContentView(container)'
if(!$s.Contains($anchor)){throw 'VOICE_PANEL_ANCHOR_CHANGED'}
$patch=@'
        container.addView(panel); setContentView(container)
        // Round Box owns presentation; retain this exact Activity as the voice/runtime executor.
        if(intent.getBooleanExtra("LEEWAY_ROUNDBOX_PRESENTATION",false)) {
            panel.visibility = android.view.View.GONE
            container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val attrs=window.attributes
            attrs.width=1; attrs.height=1
            attrs.gravity=android.view.Gravity.TOP or android.view.Gravity.START
            window.attributes=attrs
        }
'@
$s=$s.Replace($anchor,$patch.TrimEnd())
[IO.File]::WriteAllText($p,$s,(New-Object System.Text.UTF8Encoding($false)))
$q=Join-Path $r 'app\src\main\java\industries\leeway\pocket\RoundBoxWindow.kt'
Copy-Item $q ($q+'.a2-hidden-presentation-before.bak') -Force
$t=[IO.File]::ReadAllText($q)
$old='val intent=PocketVoiceActivity.launchIntent(service,true)'
if(!$t.Contains($old)){throw 'ROUND_ACTIVITY_LAUNCH_CHANGED'}
$t=$t.Replace($old,$old+[Environment]::NewLine+'            intent.putExtra("LEEWAY_ROUNDBOX_PRESENTATION",true)')
[IO.File]::WriteAllText($q,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($v)
if(!$a.Contains('versionCode = 63')){throw 'VERSION_CONFLICT'}
Copy-Item $v ($v+'.a2-before-v64.bak') -Force
[IO.File]::WriteAllText($v,$a.Replace('versionCode = 63','versionCode = 64').Replace('1.0.0-roundbox-canonical-turn-rc36','1.0.0-roundbox-single-visual-rc37'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $p,$q -Algorithm SHA256|ForEach-Object{$_.Path+' '+$_.Hash}
