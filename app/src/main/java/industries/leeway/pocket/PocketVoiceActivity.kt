/*
LEEWAY
REGION: POCKET.VOICE
TAG: POCKET.LEEWAY.AGENT_LEE.VOICE_ACTIVITY
WHAT: Small voice-first Agent Lee interaction surface launched from the side tab
WHY: Provide immediate speech -> governed phone runtime -> Voice One conversation
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android Pocket Agent
WHEN: 2026-09-29
HOW: Android SpeechRecognizer, scoped Device Bridge IPC, canonical ecosystem snapshot, Voice Fabric WebView
*/
package industries.leeway.pocket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.*
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import org.json.JSONObject
import java.util.Locale
import kotlin.concurrent.thread

class PocketVoiceActivity: Activity(){
    private lateinit var status:TextView
    private lateinit var transcript:TextView
    private lateinit var voiceView:WebView
    private lateinit var bridge:DeviceBridgeClient
    private lateinit var memory:MemoryStore
    private lateinit var automation:N8nBridge
    private var recognizer:SpeechRecognizer?=null
    private var bridgePageReady=false
    private var pendingSpeech:String?=null
    private var pendingRequest:String?=null
    private var expectedNonce:String?=null
    @Volatile private var lastSkillEvidence:String="skills=NOT_LOADED"

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        bridge=DeviceBridgeClient(this)
        memory=MemoryStore(this)
        automation=N8nBridge(this)
        buildUi()
        initVoiceFabric()
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_AUDIO)
        }else{
            status.postDelayed({startListening()},250)
        }
    }

    private fun buildUi(){
        val root=FrameLayout(this).apply{setBackgroundColor(Color.TRANSPARENT)}
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER_HORIZONTAL
            setPadding(34,26,34,26)
            background=GradientDrawable().apply{
                setColor(Color.argb(245,3,8,16))
                cornerRadius=34f
                setStroke(2,Color.argb(150,255,255,255))
            }
        }
        status=TextView(this).apply{
            text="Agent Lee"
            textSize=18f
            setTextColor(Color.WHITE)
            gravity=Gravity.CENTER
        }
        transcript=TextView(this).apply{
            text="Opening voice lane…"
            textSize=15f
            setTextColor(Color.LTGRAY)
            gravity=Gravity.CENTER
            setPadding(0,12,0,12)
        }
        val cancel=Button(this).apply{
            text="CLOSE"
            setOnClickListener{finish()}
        }
        card.addView(status)
        card.addView(transcript)
        card.addView(cancel)
        root.addView(card,FrameLayout.LayoutParams(
            resources.displayMetrics.density.let{(330*it).toInt()},
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
        voiceView=WebView(this)
        root.addView(voiceView,FrameLayout.LayoutParams(2,2,Gravity.BOTTOM or Gravity.START))
        setContentView(root)
    }

    private fun initVoiceFabric(){
        WebView.setWebContentsDebuggingEnabled(false)
        voiceView.settings.javaScriptEnabled=true
        voiceView.settings.domStorageEnabled=true
        voiceView.settings.mediaPlaybackRequiresUserGesture=false
        voiceView.addJavascriptInterface(VoiceCallbacks(),"LeeWayPocketNative")
        voiceView.webViewClient=object:WebViewClient(){
            override fun onReceivedError(view:WebView?,request:WebResourceRequest?,error:WebResourceError?){
                if(request?.isForMainFrame==true)runOnUiThread{
                    status.text="Voice One unavailable"
                    transcript.text="Voice Fabric could not load. Text response will remain visible; no substitute voice will be used."
                }
            }
        }
        voiceView.loadUrl(VOICE_BRIDGE_URL)
    }

    inner class VoiceCallbacks{
        @JavascriptInterface fun onBridgeReady(json:String){
            runOnUiThread{
                bridgePageReady=true
                voiceView.evaluateJavascript("window.LeeWayAndroidVoice.prepare().catch(()=>{})",null)
                pendingSpeech?.let{pendingSpeech=null;speakVoiceOne(it)}
            }
        }
        @JavascriptInterface fun onReady(json:String){runOnUiThread{status.text="Voice One ready"}}
        @JavascriptInterface fun onState(json:String){
            val message=runCatching{JSONObject(json).optString("message")}.getOrDefault("")
            if(message.isNotBlank())runOnUiThread{status.text=message.replace('_',' ')}
        }
        @JavascriptInterface fun onSpeakComplete(json:String){
            runOnUiThread{status.text="Ready";status.postDelayed({finish()},700)}
        }
        @JavascriptInterface fun onError(json:String){
            val message=runCatching{JSONObject(json).optString("error")}.getOrDefault("VOICE_UNAVAILABLE")
            runOnUiThread{
                status.text="Voice One unavailable"
                Toast.makeText(this@PocketVoiceActivity,message,Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startListening(){
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            transcript.text="Android speech recognition is unavailable."
            return
        }
        recognizer?.destroy()
        recognizer=SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(params:Bundle?){status.text="Listening";transcript.text="Speak now…"}
            override fun onBeginningOfSpeech(){}
            override fun onRmsChanged(rmsdB:Float){}
            override fun onBufferReceived(buffer:ByteArray?){}
            override fun onEndOfSpeech(){status.text="Thinking"}
            override fun onError(error:Int){status.text="Listening stopped";transcript.text="Speech recognition error: $error"}
            override fun onPartialResults(partialResults:Bundle?){
                val partial=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if(!partial.isNullOrBlank())transcript.text=partial
            }
            override fun onEvent(eventType:Int,params:Bundle?){}
            override fun onResults(results:Bundle?){
                val heard=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                if(heard.isBlank()){status.text="Ready";transcript.text="I did not hear a complete request.";return}
                transcript.text="You: $heard"
                handle(heard)
            }
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US")
        })
    }

    private fun handle(raw:String){
        val q=raw.trim()
        memory.saveConversation("You: $q")
        val l=q.lowercase(Locale.US)
        when{
            l.startsWith("remember ")->{
                memory.savePersonal(q.substringAfter("remember "))
                deliver("Stored in Pocket personal memory.")
            }
            l.startsWith("note ") || l.startsWith("lee note ")->{
                memory.saveNotebook(q.substringAfter("note ").substringAfter("lee note "))
                deliver("Stored in Lee's Pocket notebook.")
            }
            l.startsWith("call ")->{
                startActivity(Intent(Intent.ACTION_DIAL))
                deliver("Dialer opened. You stay in control of the call.")
            }
            l.startsWith("email ") || l.startsWith("send an email") || l.startsWith("send email")->
                routeAutomation("email",q){
                    startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:")))
                    deliver("Email app opened because the governed automation lane is not configured.")
                }
            l.startsWith("remind ") || l.contains(" reminder")->
                routeAutomation("reminder",q){
                    memory.saveNotebook("Reminder request: $q")
                    deliver("The reminder request is preserved in the Pocket notebook; automation is not configured.")
                }
            l.startsWith("search ") || l.startsWith("research ") || l.startsWith("look up ")->
                routeAutomation("research",q){
                    val term=q.substringAfter("search ",q).substringAfter("research ",q).substringAfter("look up ",q)
                    startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(term))))
                    deliver("Research browser opened because the governed research automation lane is not configured.")
                }
            l.contains("schedule") || l.contains("appointment") || l.contains("calendar")->
                routeAutomation("schedule",q){
                    startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE,q))
                    deliver("Calendar handoff opened. Pocket has not claimed the event was created.")
                }
            else->routeAgent(q)
        }
    }

    private fun routeAutomation(action:String,request:String,fallback:()->Unit){
        if(!automation.isConfigured()){fallback();return}
        status.text="Executing $action"
        automation.dispatch(action,request){result->
            runOnUiThread{
                if(result.ok){
                    memory.saveNotebook("n8n $action: ${result.message}")
                    deliver(result.message)
                }else{
                    memory.saveNotebook("n8n failure [$action]: ${result.message}")
                    fallback()
                }
            }
        }
    }

    private fun routeAgent(request:String){
        pendingRequest=request
        if(!bridge.isGranted()){
            val nonce=bridge.newNonce()
            expectedNonce=nonce
            try{
                startActivityForResult(bridge.bootstrapIntent(nonce),REQ_BRIDGE_BOOTSTRAP)
                status.text="Approve Device Bridge"
                transcript.text="One-time scoped Pocket access approval is required."
            }catch(_:Exception){
                deliver("The installed Device Bridge does not yet expose the Pocket workstation adapter. Update Device Bridge before Agent Lee reasoning can run here.")
            }
            return
        }
        executeAgent(request)
    }

    private fun executeAgent(request:String){
        status.text="Loading LeeWay authority"
        thread{
            val authority=EcosystemBindings.promptContext(applicationContext)
            val skillContext=SkillAuthorityClient(applicationContext).contextFor(request)
            lastSkillEvidence=skillContext.evidence
            val prompt=buildString{
                append(authority)
                append("\n\n")
                append(skillContext.promptContext)
                append("\n\nROLE: You are Agent Lee speaking through LeeWay Pocket Agent. ")
                append("Be concise for spoken conversation. Uphold Creator authority and LeeWay truth boundaries. ")
                append("Do not claim a skill, Formula evaluation, tool action, Notebook action, or runtime action occurred unless the current turn contains evidence. ")
                append("\nUSER REQUEST: ")
                append(request)
            }
            val args=JSONObject().put("prompt",prompt).put("speak",false)
            runOnUiThread{
                try{
                    startActivityForResult(bridge.commandIntent("agent.chat",args),REQ_BRIDGE_COMMAND)
                    status.text="Agent Lee thinking"
                }catch(_:Exception){
                    bridge.clearGrant()
                    deliver("Pocket cannot reach the current Device Bridge command adapter. The bridge package needs the workstation update.")
                }
            }
        }
    }

    @Deprecated("Legacy activity-result API retained for minimum-compatible explicit cross-app handoff")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        when(requestCode){
            REQ_BRIDGE_BOOTSTRAP->{
                val nonce=expectedNonce.orEmpty()
                expectedNonce=null
                if(resultCode==RESULT_OK && bridge.acceptBootstrap(data,nonce)){
                    pendingRequest?.let{executeAgent(it)}
                }else{
                    deliver("Pocket Device Bridge access was not approved. No device authority was used.")
                }
            }
            REQ_BRIDGE_COMMAND->{
                val envelope=bridge.parseResult(data)
                if(!envelope.optBoolean("ok")){
                    if(envelope.optString("error").contains("TOKEN"))bridge.clearGrant()
                    deliver("Agent Lee phone runtime is unavailable: "+envelope.optString("error","UNKNOWN"))
                    return
                }
                val result=envelope.optJSONObject("result")
                val response=result?.optString("response").orEmpty()
                if(response.isBlank()){
                    deliver("The phone runtime returned no Agent Lee response.")
                    return
                }
                val trace="Device Bridge model="+result?.optString("modelId")+
                    "; authority="+result?.optString("authority")+
                    "; canonicalFormula="+envelope.optString("canonicalFormulaState","NOT_EXECUTED")+
                    "; "+lastSkillEvidence
                memory.saveNotebook("Pocket turn trace: $trace")
                deliver(response)
            }
        }
    }

    private fun deliver(text:String){
        memory.saveConversation("Lee: $text")
        transcript.text="Agent Lee: $text"
        status.text="Preparing Voice One"
        if(bridgePageReady)speakVoiceOne(text) else pendingSpeech=text
    }

    private fun speakVoiceOne(text:String){
        val quoted=JSONObject.quote(text)
        voiceView.evaluateJavascript("window.LeeWayAndroidVoice.speak($quoted).catch(()=>{})",null)
    }

    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==REQ_AUDIO && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startListening()
    }

    override fun onDestroy(){
        recognizer?.destroy()
        runCatching{voiceView.evaluateJavascript("window.LeeWayAndroidVoice?.stop?.()",null)}
        voiceView.destroy()
        super.onDestroy()
    }

    companion object{
        private const val REQ_AUDIO=701
        private const val REQ_BRIDGE_BOOTSTRAP=702
        private const val REQ_BRIDGE_COMMAND=703
        private const val VOICE_BRIDGE_URL="https://4citeb4u.github.io/LeeWay-Voice-Fabric/android-bridge.html"
    }
}
