/*
REGION: LEEWAY.UI.ROUND_BOX
TAG: POCKET.AGENT_LEE.FLOATING_DUAL_CHAT
5WH: WHAT=Display the creator-supplied Round Box in the existing Pocket Agent overlay.
WHY=Conversation must remain available when the Digital Brain is collapsed.
WHO=LeeWay Industries / installation owner; WHERE=Existing PocketOverlayService.
WHEN=Owner taps Agent Lee; HOW=One bounded WebView, existing governed runtime and Voice Fabric.
AUTHORIZED ROLES: OWNER_UI. This view grants no new execution capability.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

internal class FloatingRoundBoxWindow(private val service:android.app.Service) {
    private val main=Handler(Looper.getMainLooper())
    private val manager=service.getSystemService(WindowManager::class.java)
    private val density=service.resources.displayMetrics.density
    private var root:FrameLayout?=null
    private var web:WebView?=null
    private var audioHost:FrameLayout?=null
    private var params:WindowManager.LayoutParams?=null
    private var recognizer:SpeechRecognizer?=null
    private var player:MediaPlayer?=null
    private var audioFile:File?=null
    private var voiceListener:PocketVoiceHost.Listener?=null
    private val busy=AtomicBoolean(false)
    private var closed=false
    private fun dp(v:Int)=(v*density).toInt()
    private fun bounds()=manager.currentWindowMetrics.bounds
    fun isVisible()=root?.isAttachedToWindow==true
    fun show(){
        if(root!=null)return
        check(Settings.canDrawOverlays(service)){"OWNER_OVERLAY_PERMISSION_REQUIRED"}
        closed=false
        val b=bounds()
        val width=(b.width()-dp(12)).coerceAtMost(dp(620)).coerceAtLeast(dp(220))
        val height=(b.height()-dp(100)).coerceAtMost(dp(510)).coerceAtLeast(dp(240))
        val lp=WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply{
                gravity=Gravity.TOP or Gravity.LEFT
                x=((b.width()-width)/2).coerceAtLeast(0)
                y=(b.height()/7).coerceAtLeast(dp(18))
                title="Agent Lee Round Box"
            }
        params=lp
        val host=FrameLayout(service).apply{setBackgroundColor(Color.TRANSPARENT)}
        val view=WebView(service).apply{
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=true
            settings.allowContentAccess=false
            settings.mediaPlaybackRequiresUserGesture=false
            addJavascriptInterface(Bridge(),"LeeWayAndroid")
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest)=
                    request.url.toString()!="file:///android_asset/round-box-leeway.html"
                override fun shouldInterceptRequest(v:WebView,request:WebResourceRequest):WebResourceResponse?{
                    val uri=request.url
                    if(uri.scheme=="file"&&uri.path?.startsWith("/android_asset/")==true)return null
                    return WebResourceResponse("text/plain","UTF-8",java.io.ByteArrayInputStream("LOCAL_ASSET_ONLY".toByteArray()))
                }
            }
            webChromeClient=WebChromeClient()
            loadUrl("file:///android_asset/round-box-leeway.html")
        }
        val voiceFrame=FrameLayout(service).apply{setBackgroundColor(Color.TRANSPARENT)}
        host.addView(view,FrameLayout.LayoutParams(-1,-1))
        host.addView(voiceFrame,FrameLayout.LayoutParams(1,1,Gravity.BOTTOM or Gravity.RIGHT))
        web=view;audioHost=voiceFrame;root=host
        manager.addView(host,lp)
    }
    fun relayout(){
        val p=params?:return
        val b=bounds()
        p.width=p.width.coerceIn(dp(220).coerceAtMost(b.width()),b.width())
        p.height=p.height.coerceIn(dp(220).coerceAtMost(b.height()),b.height())
        p.x=p.x.coerceIn(0,(b.width()-p.width).coerceAtLeast(0))
        p.y=p.y.coerceIn(0,(b.height()-p.height).coerceAtLeast(0))
        root?.let{manager.updateViewLayout(it,p)}
    }
    private fun emit(kind:String,message:String){
        if(closed)return
        val k=JSONObject.quote(kind)
        val t=JSONObject.quote(message)
        main.post{if(!closed)web?.evaluateJavascript("window.LeeWayRoundBoxReceive?.($k,$t)",null)}
    }
    private fun appendContinuum(actor:String,message:String):Long{
        val db=LeeWayBodyDatabases(service).continuum.writableDatabase
        val values=ContentValues().apply{
            put("universe_id",industries.leeway.brain.DigitalBrain.continuumRootId(AndroidDigitalBrainAdapter.identity(service))+":conversation")
            put("event_type","conversation.turn")
            put("payload_json",JSONObject().put("actor",actor).put("text",message).toString())
            put("captured_at",System.currentTimeMillis())
        }
        return db.insert("continuum_events",null,values)
    }
    private fun submit(prompt:String){
        val q=prompt.trim()
        if(q.isBlank()||q.length>10000){emit("error","Message must contain 1 to 10,000 characters.");return}
        if(!busy.compareAndSet(false,true)){emit("error","Agent Lee is still answering.");return}
        emit("input",q);emit("thinking","")
        thread(name="leeway-roundbox-governed-turn",isDaemon=true){
            try{
                val runtime=UnifiedAgentLeeRuntime(service.applicationContext)
                runtime.requireExistingContinuumBeforeCapture(q)
                val userEvent=appendContinuum("user",q)
                check(userEvent>0){"CONTINUUM_USER_RETENTION_FAILED"}
                val turn=runtime.turn(q,userEvent)
                check(appendContinuum("agent-lee",turn.text)>0){"CONTINUUM_RESPONSE_RETENTION_FAILED"}
                emit("response",turn.text)
                main.post{
                    if(closed)return@post
                    if(turn.localContinuum!=null)playLocal(turn.localContinuum)
                    else playPaired(turn.audio,turn.voicePackageId)
                }
            }catch(error:Exception){
                emit("error",error.message?:"AGENT_LEE_RUNTIME_UNAVAILABLE")
            }finally{busy.set(false)}
        }
    }
    private fun playLocal(reply:LocalContinuumReply){
        val host=audioHost?:run{emit("error","VOICE_FABRIC_HOST_UNAVAILABLE");return}
        val listener=object:PocketVoiceHost.Listener{
            private var requested=false
            override fun onReady(){if(closed||requested)return;requested=true;PocketSpeech.speak(this,reply.spokenText)}
            override fun onState(message:String){if(!closed)emit("voice",message)}
            override fun onComplete(){emit("voice","Ready")}
            override fun onError(message:String){emit("voice-error",message)}
        }
        voiceListener=listener
        try{PocketSpeech.attach(service.applicationContext,listener,host)}
        catch(error:Exception){emit("voice-error",error.message?:"VOICE_FABRIC_UNAVAILABLE")}
    }
    private fun playPaired(bytes:ByteArray,voicePackageId:String){
        try{
            player?.release();audioFile?.delete()
            val f=File(service.cacheDir,"roundbox-paired-"+System.nanoTime()+".wav")
            f.writeBytes(bytes);audioFile=f
            val playback=MediaPlayer();player=playback
            playback.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            playback.setDataSource(f.absolutePath)
            playback.setOnCompletionListener{it.release();if(player===it)player=null;f.delete();emit("voice","Ready")}
            playback.setOnErrorListener{mp,_,_->mp.release();if(player===mp)player=null;f.delete();emit("voice-error","VOICE_PLAYBACK_FAILED");true}
            playback.prepare();playback.start();emit("voice","Speaking: "+voicePackageId)
        }catch(error:Exception){emit("voice-error",error.message?:"VOICE_PLAYBACK_FAILED")}
    }
    private fun listen(){
        if(ContextCompat.checkSelfPermission(service,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            emit("error","Microphone permission required. Open Agent Lee permissions.");return
        }
        recognizer?.destroy()
        val r=SpeechRecognizer.createSpeechRecognizer(service);recognizer=r
        r.setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(params:Bundle?){emit("listening","Speak now")}
            override fun onBeginningOfSpeech(){}
            override fun onRmsChanged(rmsdB:Float){}
            override fun onBufferReceived(buffer:ByteArray?){}
            override fun onEndOfSpeech(){}
            override fun onError(error:Int){emit("error","Speech recognition unavailable: "+error)}
            override fun onResults(results:Bundle?){
                val q=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if(q.isNotBlank())submit(q) else emit("error","No speech recognized.")
            }
            override fun onPartialResults(results:Bundle?){
                val q=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if(q.isNotBlank())emit("dictation",q)
            }
            override fun onEvent(eventType:Int,params:Bundle?){}
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
        })
    }
    fun close(){
        closed=true
        recognizer?.destroy();recognizer=null
        voiceListener?.let{PocketSpeech.stop(it);PocketSpeech.detach(it)};voiceListener=null
        player?.release();player=null;audioFile?.delete();audioFile=null
        web?.removeJavascriptInterface("LeeWayAndroid")
        root?.let{manager.removeView(it)}
        web?.destroy();web=null;root=null;params=null;audioHost=null
    }
    private inner class Bridge{
        @JavascriptInterface fun roundBoxMessage(text:String){main.post{submit(text)}}
        @JavascriptInterface fun roundBoxListen(){main.post{listen()}}
        @JavascriptInterface fun roundBoxClose(){main.post{close()}}
        @JavascriptInterface fun roundBoxMove(dx:Double,dy:Double){main.post{
            val p=params?:return@post
            if(!dx.isFinite()||!dy.isFinite()||kotlin.math.abs(dx)>300||kotlin.math.abs(dy)>300)return@post
            p.x+=(dx*density).toInt();p.y+=(dy*density).toInt();relayout()
        }}
        @JavascriptInterface fun roundBoxResize(w:Double,h:Double){main.post{
            val p=params?:return@post
            if(!w.isFinite()||!h.isFinite())return@post
            p.width=dp(w.toInt().coerceIn(220,800));p.height=dp(h.toInt().coerceIn(220,800));relayout()
        }}
    }
}
