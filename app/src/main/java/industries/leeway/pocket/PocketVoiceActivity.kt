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
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.*
import android.view.Gravity
import android.view.ViewGroup

import android.widget.*
import org.json.JSONObject
import java.util.Locale
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class PocketVoiceActivity: Activity(){
    private lateinit var status:TextView
    private lateinit var transcript:TextView
    private lateinit var voiceDetails:TextView
    private lateinit var voiceContainer:FrameLayout
    private var voiceFailed=false
    private var agentRequestInFlight=false
    private val recognitionSession=RecognitionSession()
    private var typingDialog:android.app.AlertDialog?=null
    private val initialListening=Runnable{startListening()}

    private lateinit var bridge:DeviceBridgeClient
    private lateinit var memory:MemoryStore
    private lateinit var automation:N8nBridge
    private var recognizer:SpeechRecognizer?=null


    private var pendingRequest:String?=null
    private var expectedNonce:String?=null
    @Volatile private var lastSkillEvidence:String="skills=NOT_LOADED"

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        // Conversation-scoped: prevent model preparation/playback being interrupted
        // by screen timeout. Android releases the flag when this window closes.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFinishOnTouchOutside(false)
        val existing=activeVoiceActivity?.get()
        if(existing!=null && !existing.isDestroyed && !existing.isFinishing){
            finish()
            return
        }
        activeVoiceActivity=WeakReference(this)
        bridge=DeviceBridgeClient(this)
        memory=MemoryStore(this)
        automation=N8nBridge(this)
        buildUi()
        initVoiceFabric()
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_AUDIO)
        }else{
            status.postDelayed(initialListening,250)
        }
    }

    private fun buildUi(){
        val root=FrameLayout(this).apply{setBackgroundColor(Color.TRANSPARENT)}
        voiceContainer=root
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
        voiceDetails=TextView(this).apply{
            text="Preparing phone voice"
            textSize=13f
            setTextColor(Color.LTGRAY)
            gravity=Gravity.CENTER
            setTextIsSelectable(true)
        }
        val retry=Button(this).apply{
            text="RETRY MICROPHONE"
            setOnClickListener{
                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_AUDIO)
                else startListening()
            }
        }
        val typeInstead=Button(this).apply{
            text="TYPE INSTEAD"
            setOnClickListener{
                if(agentRequestInFlight){status.text="Agent Lee is still answering";return@setOnClickListener}
                if(typingDialog!=null)return@setOnClickListener
                PocketSpeech.stop(voiceListener)
                recognitionSession.beginTyping()
                status.removeCallbacks(initialListening)
                recognizer?.cancel()
                recognizer?.destroy()
                recognizer=null
                val input=EditText(this@PocketVoiceActivity).apply{
                    hint="Ask Agent Lee"
                    maxLines=4
                }
                status.text="Type your question"
                typingDialog=android.app.AlertDialog.Builder(this@PocketVoiceActivity)
                    .setTitle("Ask Agent Lee").setView(input)
                    .setPositiveButton("Ask"){_,_->
                        val request=input.text.toString().trim()
                        if(request.isNotBlank() && !agentRequestInFlight){
                            transcript.text="You: $request"
                            handle(request)
                        }
                    }.setNegativeButton("Cancel",null).create().apply{
                        setCanceledOnTouchOutside(false)
                        setOnDismissListener{
                            recognitionSession.endTyping()
                            typingDialog=null
                        }
                        show()
                        input.requestFocus()
                    }
            }
        }
        card.addView(status)
        card.addView(transcript)
        card.addView(voiceDetails)
        card.addView(retry)
        card.addView(typeInstead)
        card.addView(cancel)
        root.addView(card,FrameLayout.LayoutParams(
            resources.displayMetrics.density.let{(330*it).toInt()},
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))


        setContentView(root)
    }

    private val voiceListener = object : PocketVoiceHost.Listener {
        override fun onReady(){voiceFailed=false;voiceDetails.text=PocketSpeech.description()}
        override fun onState(message:String){if(!voiceFailed)voiceDetails.text=message.replace('_',' ')}
        override fun onComplete(){
            status.text="Ready"
        }
        override fun onError(message:String){
            voiceFailed=true
            voiceDetails.text="Phone voice unavailable\n$message"
        }
    }

    private fun initVoiceFabric(){
        PocketSpeech.attach(applicationContext, voiceListener, voiceContainer)
    }
    private fun startListening(){
        if (isFinishing || isDestroyed) return
        PocketSpeech.stop(voiceListener)
        if(typingDialog!=null)return
        if (agentRequestInFlight) {
            status.text="Agent Lee is still answering"
            return
        }
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            transcript.text="Android speech recognition is unavailable."
            return
        }
        recognizer?.destroy()
        val recognitionToken=recognitionSession.begin()?:return
        recognizer=SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object:RecognitionListener{
            private fun current()=recognitionSession.accepts(recognitionToken) && !isFinishing && !isDestroyed && !agentRequestInFlight
            override fun onReadyForSpeech(params:Bundle?){if(current()){status.text="Listening";transcript.text="Speak now…"}}
            override fun onBeginningOfSpeech(){}
            override fun onRmsChanged(rmsdB:Float){}
            override fun onBufferReceived(buffer:ByteArray?){}
            override fun onEndOfSpeech(){if(current())status.text="Thinking"}
            override fun onError(error:Int){
                if(!current())return
                status.text="Listening stopped"
                transcript.text=if(error==SpeechRecognizer.ERROR_NO_MATCH || error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    "No speech detected. Tap Retry microphone and speak again."
                else "Speech recognition error: $error. Tap Retry microphone to try again."
            }
            override fun onPartialResults(partialResults:Bundle?){
                if(!current())return
                val partial=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if(!partial.isNullOrBlank())transcript.text=partial
            }
            override fun onEvent(eventType:Int,params:Bundle?){}
            override fun onResults(results:Bundle?){
                if(!current())return
                recognitionSession.cancel()
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
        val phoneAction=PhoneActionCommand.parse(q)
        if(phoneAction!=null){
            if(agentRequestInFlight)return
            if(!bridge.isGranted()||!PocketOverlayService.isEnabled(this)){
                deliver("Connect Device Bridge and enable the Pocket side tab before using phone commands.")
                return
            }
            agentRequestInFlight=true
            status.text="Executing explicit phone command"
            // Yield the active app window; the existing service owns IPC and the result.
            moveTaskToBack(true)
            try{
                startService(Intent(this,PocketOverlayService::class.java)
                    .setAction("LEEWAY_EXPLICIT_PHONE_COMMAND")
                    .putExtra("action",phoneAction.action).putExtra("label",phoneAction.label))
            }catch(_:Exception){deliver("The Pocket command service could not be reached. No result is verified.")}
            return
        }
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
        if(agentRequestInFlight)return
        agentRequestInFlight=true
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
            val skillContext=SkillAuthorityClient(applicationContext).contextFor(request)
            lastSkillEvidence=skillContext.evidence.replace("skills=CONTEXT_USED","skills=SOURCE_LOADED")
            // Model system instructions belong in Device Bridge's conversation API.
            // Keep retrieved source text out of the small model's user-message channel.
            val args=JSONObject().put("prompt",request.take(600)).put("speak",false)
                .put("userRequest",request.take(600))
                .put("creatorContext",PocketAuthorityProfile.creatorContext(applicationContext))
                .put("authorityEvidence",JSONObject().put("skills",lastSkillEvidence)
                    .put("contextOnly",true).put("canonicalFormulaState","NOT_EXECUTED"))
            runOnUiThread{
                try{
                    if (isFinishing || isDestroyed) return@runOnUiThread
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
        agentRequestInFlight=false
        if (isFinishing || isDestroyed) return
        memory.saveConversation("Lee: $text")
        transcript.text="Agent Lee: $text"
        status.text="Preparing response audio"
        PocketSpeech.speak(voiceListener, text)
    }

    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==REQ_AUDIO && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startListening()
    }

    override fun onNewIntent(intent:Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        // Reopening brings the existing conversation forward. Do not restart recognition,
        // replace a typed question, or interrupt an outstanding Device Bridge result.
    }

    override fun onDestroy(){
        recognitionSession.cancel()
        if(::status.isInitialized)status.removeCallbacks(initialListening)
        typingDialog?.dismiss()
        if(activeVoiceActivity?.get()===this)activeVoiceActivity=null
        recognizer?.destroy()
        PocketSpeech.detach(voiceListener)

        super.onDestroy()
    }

    companion object{
        fun completeDeviceCommand(text:String){
            activeVoiceActivity?.get()?.let { activity ->
                activity.runOnUiThread { if(!activity.isDestroyed&&!activity.isFinishing)activity.deliver(text) }
            }
        }
        fun launchIntent(context:Context, newTask:Boolean=false)=
            Intent(context,PocketVoiceActivity::class.java).apply{
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if(newTask)addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        private var activeVoiceActivity:WeakReference<PocketVoiceActivity>?=null
        private const val REQ_AUDIO=701
        private const val REQ_BRIDGE_BOOTSTRAP=702
        private const val REQ_BRIDGE_COMMAND=703

    }
}
