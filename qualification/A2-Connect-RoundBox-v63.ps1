$ErrorActionPreference='Stop'
$r='E:\LeeWay-Work\agent-lee-fold6-unified'
$base=Join-Path $r 'app\src\main\java\industries\leeway\pocket'
$voice=Join-Path $base 'PocketVoiceActivity.kt'
$round=Join-Path $base 'RoundBoxWindow.kt'
$version=Join-Path $r 'app\build.gradle.kts'
foreach($f in @($voice,$round,$version)){Copy-Item $f ($f+'.a2-conversation-before.bak') -Force}
$s=[IO.File]::ReadAllText($voice)
$anchor='        else startListening()'
if(!$s.Contains($anchor)){throw 'VOICE_START_CHANGED'}
$s=$s.Replace($anchor,'        else { val typed=intent.getStringExtra("LEEWAY_ROUNDBOX_TEXT")?.trim(); if(!typed.isNullOrEmpty()) handle(typed) else startListening() }')
$anchor='                        transcript.text = "Agent Lee: ${turn.text}"'
if(!$s.Contains($anchor)){throw 'VOICE_TURN_ANCHOR_CHANGED'}
$s=$s.Replace($anchor,$anchor+[Environment]::NewLine+'                        RoundBoxWindow.deliver("response",turn.text)')
$anchor='        transcript.text = error.message ?: if (local) "Local Continuum unavailable" else "Conversation provider unavailable"'
if(!$s.Contains($anchor)){throw 'VOICE_ERROR_CHANGED'}
$s=$s.Replace($anchor,$anchor+[Environment]::NewLine+'        RoundBoxWindow.deliver("error",error.message ?: "AGENT_LEE_CONNECTION_BLOCKED")')
[IO.File]::WriteAllText($voice,$s,(New-Object System.Text.UTF8Encoding($false)))
$t=[IO.File]::ReadAllText($round)
$t=$t.Replace('import android.app.Service','import android.app.Service'+[Environment]::NewLine+'import android.content.Intent')
$anchor='internal class RoundBoxWindow(private val service:Service) {'
if(!$t.Contains($anchor)){throw 'ROUND_OWNER_CHANGED'}
$t=$t.Replace($anchor,$anchor+[Environment]::NewLine+@'
    companion object {
        private var active:RoundBoxWindow?=null
        fun deliver(kind:String,message:String) {
            val window=active?:return
            window.main.post{
                val script="window.LeeWayRoundBoxReceive?.("+org.json.JSONObject.quote(kind)+","+org.json.JSONObject.quote(message)+")"
                window.web?.evaluateJavascript(script,null)
            }
        }
    }
'@)
$t=$t.Replace('lp=p;web=view;wm.addView(view,p)','lp=p;web=view;wm.addView(view,p);active=this')
$t=$t.Replace('web=null;lp=null','web=null;lp=null;if(active===this)active=null')
$old=@'
        @JavascriptInterface fun roundBoxMessage(message:String){main.post{
            val payload=org.json.JSONObject.quote("UI_CONNECTED_EXECUTOR_NOT_BOUND")
            web?.evaluateJavascript("window.LeeWayRoundBoxReceive?.('error',"+payload+")",null)
        }}
        @JavascriptInterface fun roundBoxListen(){main.post{
            val payload=org.json.JSONObject.quote("VOICE_CONTROL_NOT_BOUND_TO_THIS_SURFACE")
            web?.evaluateJavascript("window.LeeWayRoundBoxReceive?.('voice-error',"+payload+")",null)
        }}
'@
if(!$t.Contains($old)){throw 'ROUND_CALLBACK_CHANGED'}
$new=@'
        private fun openCanonical(text:String?){
            val intent=PocketVoiceActivity.launchIntent(service,true)
            if(text!=null)intent.putExtra("LEEWAY_ROUNDBOX_TEXT",text)
            service.startActivity(intent)
        }
        @JavascriptInterface fun roundBoxMessage(message:String){main.post{
            val q=message.trim()
            if(q.isBlank()||q.length>4096){deliver("error","INVALID_REQUEST_LENGTH");return@post}
            deliver("thinking","")
            openCanonical(q)
        }}
        @JavascriptInterface fun roundBoxListen(){main.post{
            deliver("listening","")
            openCanonical(null)
        }}
'@
$t=$t.Replace($old,$new)
[IO.File]::WriteAllText($round,$t,(New-Object System.Text.UTF8Encoding($false)))
$a=[IO.File]::ReadAllText($version)
if(!$a.Contains('versionCode = 62')){throw 'VERSION_CONFLICT'}
[IO.File]::WriteAllText($version,$a.Replace('versionCode = 62','versionCode = 63').Replace('1.0.0-three-independent-screens-rc35','1.0.0-roundbox-canonical-turn-rc36'),(New-Object System.Text.UTF8Encoding($false)))
Get-FileHash $voice,$round,$version -Algorithm SHA256 | ForEach-Object{$_.Path+' '+$_.Hash}
