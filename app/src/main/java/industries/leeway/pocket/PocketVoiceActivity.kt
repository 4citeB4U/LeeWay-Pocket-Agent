package industries.leeway.pocket

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.*
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class PocketVoiceActivity:Activity(){
    private lateinit var status:TextView;private lateinit var transcript:TextView;private lateinit var container:FrameLayout
    private var recognizer:SpeechRecognizer?=null
    private val voiceListener=object:PocketVoiceHost.Listener{
        override fun onReady(){status.text="Ready"}
        override fun onState(message:String){status.text=message.replace('_',' ')}
        override fun onComplete(){status.text="Ready"}
        override fun onError(message:String){status.text="Voice unavailable";transcript.text=message}
    }
    override fun onCreate(b:Bundle?){super.onCreate(b);window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        container=FrameLayout(this);val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(32,32,32,32);setBackgroundColor(Color.argb(170,3,8,16))}
        status=TextView(this).apply{text="Agent Lee";setTextColor(Color.WHITE);textSize=18f};transcript=TextView(this).apply{text="Speak when prompted.";setTextColor(Color.LTGRAY);textSize=15f;gravity=Gravity.CENTER}
        panel.addView(status);panel.addView(transcript);panel.addView(Button(this).apply{text="LISTEN";setOnClickListener{startListening()}});panel.addView(Button(this).apply{text="CLOSE";setOnClickListener{finish()}})
        container.addView(panel);setContentView(container);PocketSpeech.attach(applicationContext,voiceListener,container)
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),701)else startListening()
    }
    private fun startListening(){PocketSpeech.stop(voiceListener);if(!SpeechRecognizer.isRecognitionAvailable(this)){transcript.text="Speech recognition unavailable.";return};recognizer?.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(p:Bundle?){status.text="Listening";transcript.text="Speak now…"};override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){}
            override fun onEndOfSpeech(){status.text="Thinking"};override fun onError(e:Int){status.text="Ready";transcript.text="Speech recognition stopped ("+e+"). Tap Listen."}
            override fun onPartialResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{transcript.text=it}};override fun onEvent(t:Int,b:Bundle?){}
            override fun onResults(b:Bundle?){val q=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim();if(q.isNotBlank())handle(q)}
        });recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)})}
    private fun handle(q:String){
        appendContinuum("user",q)
        transcript.text="You: "+q
        val reply=try { UnifiedAgentLeeRuntime(this).respond(q) }
        catch(error:IllegalStateException){
            status.text="Conversation connection blocked"
            transcript.text="The canonical Agent Lee conversation provider is not bound in this candidate. No answer or execution is being claimed."
            return
        }
        appendContinuum("agent-lee",reply)
        transcript.text="Agent Lee: "+reply
        status.text="Speaking"
        PocketSpeech.speak(voiceListener,reply)
    }
    private fun appendContinuum(actor:String,text:String){val db=LeeWayBodyDatabases(this).continuum.writableDatabase;val v=android.content.ContentValues();v.put("universe_id",(industries.leeway.brain.DigitalBrain.continuumRootId(AndroidDigitalBrainAdapter.identity(this))+":conversation"));v.put("event_type","conversation.turn");v.put("payload_json",JSONObject().put("actor",actor).put("text",text).toString());v.put("captured_at",System.currentTimeMillis());db.insert("continuum_events",null,v)}
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==701&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)startListening()}
    override fun onDestroy(){recognizer?.destroy();PocketSpeech.stop(voiceListener);PocketSpeech.detach(voiceListener);super.onDestroy()}
    companion object{fun launchIntent(c:Context,newTask:Boolean=false)=Intent(c,PocketVoiceActivity::class.java).apply{addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP);if(newTask)addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)}}
}
