<#
REGION: LEEWAY.UI.REPAIR
TAG: EXISTING_PHONE_OVERLAY_RECOVERY
WHO: Creator-authorized Agent Lee. WHAT: Repair the existing native logo service and permission return flow.
WHEN: Floating-tab failure. WHERE: verified Pocket checkout only; no phone mutation or new service.
WHY: Source-only tab, off-screen placement and false ready notifications are not acceptance.
HOW: exact base/clean inputs, staged files, dry-run, backup, hash, patch, scoped build next.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$stage=Join-Path $root 'qualification\phone-overlay-recovery'
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'ORIGIN_MISMATCH'}
$base=(& git -C $root rev-parse HEAD|Out-String).Trim()
if($base -ne 'a9374787e2a2348886b34adc988e4a7fbf2857c6'){throw 'HEAD_CHANGED_REVIEW_REQUIRED'}
$k='app/src/main/java/industries/leeway/pocket/'
$tracked=@(($k+'PocketOverlayService.kt'),($k+'MainActivity.kt'),'app/src/main/assets/agent_lee_sphere_transparent.html','app/build.gradle.kts')
function Read-Source([string]$relative){[IO.File]::ReadAllText((Join-Path $root $relative)).Replace("`r`n","`n")}
function Once([string]$s,[string]$old,[string]$new){if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw ('EXACT_BOUNDARY_NOT_UNIQUE:'+ $old.Substring(0,[Math]::Min(80,$old.Length)))};$s.Replace($old,$new)}
$before=@();foreach($file in $tracked){
 $dirty=@(& git -C $root diff --name-only HEAD -- $file);if($dirty.Count){throw ('INPUT_ALREADY_MODIFIED:'+ $file)}
 $before+=@{path=$file;sha256=(Get-FileHash (Join-Path $root $file) -Algorithm SHA256).Hash}
}
$changes=[ordered]@{}
$service=[IO.File]::ReadAllText((Join-Path $stage 'PocketOverlayService.kt.candidate')).Replace("`r`n","`n")
$service=Once $service '    private var params:WindowManager.LayoutParams?=null' '    private var params:WindowManager.LayoutParams?=null
    private var foregroundReady=false'
$service=Once $service '            else startForeground(NOTIFICATION_ID,notification)' '            else startForeground(NOTIFICATION_ID,notification)
            foregroundReady=true'
$service=Once $service '        if(!isEnabled(this)){record(this,"DISABLED_BY_OWNER");stopSelf();return START_NOT_STICKY}' '        if(!foregroundReady){stopSelf();return START_NOT_STICKY}
        if(!isEnabled(this)){record(this,"DISABLED_BY_OWNER");stopSelf();return START_NOT_STICKY}'
$service=Once $service '        record(this,if(isEnabled(this))"SERVICE_STOPPED" else "DISABLED_BY_OWNER")' '        val state=runCatching{JSONObject(status(this)).optString("state")}.getOrDefault("")
        if(state !in setOf("OWNER_OVERLAY_PERMISSION_REQUIRED","START_FAILED","ATTACH_FAILED","POSITION_FAILED","SERVICE_START_BLOCKED"))record(this,if(isEnabled(this))"SERVICE_STOPPED" else "DISABLED_BY_OWNER")'
$changes[$k+'PocketOverlayService.kt']=$service
$changes[$k+'OverlayPlacement.kt']=[IO.File]::ReadAllText((Join-Path $stage 'OverlayPlacement.kt.candidate'))
$changes['app/src/test/java/industries/leeway/pocket/OverlayPlacementTest.kt']=[IO.File]::ReadAllText((Join-Path $stage 'OverlayPlacementTest.kt.candidate'))
$main=Read-Source ($k+'MainActivity.kt')
$old=@'
        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        if(Settings.canDrawOverlays(this))PocketOverlayService.setEnabled(this,true)
        if(prefs.getBoolean("permission_pending",false) && Settings.canDrawOverlays(this)){
            prefs.edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
        }
'@
$new=@'
        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        val permitted=Settings.canDrawOverlays(this)
        if(prefs.getBoolean("permission_pending",false)&&permitted){
            prefs.edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
        }else if(PocketOverlayService.isEnabled(this)){
            PocketOverlayService.start(this)
        }else if(!prefs.contains("enabled")&&!prefs.getBoolean("permission_explanation_shown",false)){
            prefs.edit().putBoolean("permission_explanation_shown",true).apply()
            android.app.AlertDialog.Builder(this).setTitle("Floating Agent Lee button")
                .setMessage("Keep the Agent Lee logo at the right edge over your home screen and ordinary apps. Android requires your approval to display it over other apps. This permission does not select or change Agent Lee's voice.")
                .setPositiveButton("Enable floating button"){_,_->requestOverlayPermission()}
                .setNegativeButton("Not now",null).show()
        }
'@
$main=Once $main $old $new
$main=Once $main '    inner class LeeWayBridge {' @'
    private fun requestOverlayPermission(){
        getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("enabled",true).putBoolean("permission_pending",true).apply()
        if(Settings.canDrawOverlays(this)){
            getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
        }else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)))
    }
    inner class LeeWayBridge {
'@
$old=@'
        @JavascriptInterface fun enableOverlay(){
            runOnUiThread{
                if(Settings.canDrawOverlays(this@MainActivity)) PocketOverlayService.setEnabled(this@MainActivity,true)
                else {
                    getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",true).apply()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        }
'@
$main=Once $main $old @'
        @JavascriptInterface fun enableOverlay(){runOnUiThread{requestOverlayPermission()}}
        @JavascriptInterface fun disableOverlay(){runOnUiThread{
            getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this@MainActivity,false)
        }}
        @JavascriptInterface fun overlayStatus():String=PocketOverlayService.status(this@MainActivity)
'@
$changes[$k+'MainActivity.kt']=$main
$html=Read-Source 'app/src/main/assets/agent_lee_sphere_transparent.html'
$buttonPattern='<button class="agent-side-tab" id="agent-emblem"[^>]*>[\s\S]*?</button>'
if(([regex]::Matches($html,$buttonPattern)).Count -ne 1){throw 'DUPLICATE_BUTTON_BOUNDARY_CHANGED'}
$html=[regex]::Replace($html,$buttonPattern,'')
$stylePattern='<style id="leeway-shared-emblem-style">[\s\S]*?</style>'
if(([regex]::Matches($html,$stylePattern)).Count -ne 1){throw 'DUPLICATE_STYLE_BOUNDARY_CHANGED'}
$html=[regex]::Replace($html,$stylePattern,'')
# Keep the hamburger at its already-approved position; do not change sphere geometry.
$html=Once $html '</head>' '<style>.hamburger-btn{top:22px!important;left:22px!important;right:auto!important;transform:none!important}</style>
</head>'
$html=[regex]::Replace($html,'(?m)^[ \t]+(?=\r?$)','')
$changes['app/src/main/assets/agent_lee_sphere_transparent.html']=$html
$gradle=Read-Source 'app/build.gradle.kts';$gradle=Once $gradle 'versionCode = 28' 'versionCode = 29';$gradle=Once $gradle 'versionName = "1.0.0-live-surface-repair-rc1"' 'versionName = "1.0.0-overlay-recovery-rc2"';$changes['app/build.gradle.kts']=$gradle
$plan=[ordered]@{schemaVersion='leeway.overlay-recovery-repair.v1';status='DRY_RUN';sourceBase=$base;before=$before;changedPaths=@($changes.Keys);newService=$false;voiceCodeChanged=$false;permissionBypass=$false;phoneInstalled=$false;formulaExecuted=$false}
if(!$Apply){$plan|ConvertTo-Json -Depth 8;return}
$backup=Join-Path $stage 'source-before'
if(Test-Path $backup){throw 'BACKUP_EXISTS_REVIEW_BEFORE_REAPPLY'}
[void][IO.Directory]::CreateDirectory($backup)
$i=0;foreach($file in $tracked){Copy-Item (Join-Path $root $file) (Join-Path $backup (([string]$i)+'-'+[IO.Path]::GetFileName($file)));$i++}
$encoding=New-Object Text.UTF8Encoding($false)
foreach($file in $changes.Keys){$full=Join-Path $root $file;[void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($full));[IO.File]::WriteAllText($full,$changes[$file],$encoding)}
$plan.status='SOURCE_APPLIED_QUALIFICATION_REQUIRED';$plan['after']=@(foreach($file in $changes.Keys){@{path=$file;sha256=(Get-FileHash (Join-Path $root $file) -Algorithm SHA256).Hash}})
$plan|ConvertTo-Json -Depth 8|Set-Content (Join-Path $stage 'repair-receipt.json') -Encoding UTF8
$plan|ConvertTo-Json -Depth 8
