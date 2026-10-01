package industries.leeway.pocket

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/** User-authorized installed English voices. Never falls back to another language. */
object EnglishPhoneSpeech {
    private val main=Handler(Looper.getMainLooper())
    private var context:Context?=null
    private var tts:TextToSpeech?=null
    private var owner:PocketVoiceHost.Listener?=null
    private var initialized=false
    private var initializing=false
    private var generation=0
    private var utterance=0
    private var pending:String?=null
    private var readyWaiters=mutableListOf<(Boolean)->Unit>()
    private var engine=""
    private var description="English phone voice"
    private var selected:Voice?=null
    private val prefs get()=context!!.getSharedPreferences("pocket-english-voice",Context.MODE_PRIVATE)
    fun description()=description
    private fun record(state:String){
        val voice=selected
        prefs.edit().putString("latest_state",state).putString("active_engine",engine)
            .putString("active_voice",voice?.name).putString("active_locale",voice?.locale?.toLanguageTag()).apply()
        android.util.Log.i("LeeWayPocketSpeech","$state provider=android-tts engine=$engine voice=${voice?.name} locale=${voice?.locale?.toLanguageTag()}")
    }

    fun attach(app:Context,listener:PocketVoiceHost.Listener){
        context=app.applicationContext;stop();owner=listener
        initialize { ok -> if(owner===listener){if(ok)listener.onReady() else listener.onError("No installed English voice is ready. Open English voice settings.")} }
    }
    fun detach(listener:PocketVoiceHost.Listener){if(owner===listener){stop();owner=null}}
    fun stop(){utterance++;pending=null;tts?.stop()}
    fun speak(listener:PocketVoiceHost.Listener,text:String){
        if(owner!==listener)return
        stop();pending=text
        initialize { ok ->
            if(owner!==listener)return@initialize
            val next=pending?:return@initialize;pending=null
            if(!ok){listener.onError("English phone speech is unavailable.");return@initialize}
            say(next,false)
        }
    }
    private fun engines():List<Pair<String,String>> {
        val app=context?:return emptyList()
        return app.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),0)
            .map{it.serviceInfo.packageName to it.loadLabel(app.packageManager).toString()}.distinctBy{it.first}
            .sortedBy{when(it.first){"com.google.android.tts"->0;"com.samsung.SMT"->1;else->2}}
    }
    private fun voices():List<Voice> = tts?.voices.orEmpty().filter{
        it.locale.language=="en"&&!it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
    }.sortedWith(compareBy<Voice>{it.isNetworkConnectionRequired}.thenBy{it.locale.country!="US"}.thenByDescending{it.quality}.thenBy{it.name})

    private fun initialize(done:(Boolean)->Unit){
        if(initialized){done(true);return}
        readyWaiters.add(done)
        if(initializing)return
        initializing=true
        val list=engines().map{it.first}.toMutableList()
        prefs.getString("engine",null)?.let { if(list.remove(it))list.add(0,it) }
        if(list.isEmpty()){finishInitialization(false);return}
        tryEngine(list,0)
    }
    private fun tryEngine(engines:List<String>,index:Int){
        if(index>=engines.size){finishInitialization(false);return}
        tts?.shutdown();tts=null;selected=null
        val current=++generation
        engine=engines[index]
        owner?.onState("Preparing installed English voice")
        val timeout=Runnable { if(current==generation&&!initialized){generation++;tryEngine(engines,index+1)} }
        main.postDelayed(timeout,8000)
        tts=TextToSpeech(context, { status -> main.post {
            if(current!=generation)return@post
            main.removeCallbacks(timeout)
            val instance=tts
            if(status!=TextToSpeech.SUCCESS||instance==null){tryEngine(engines,index+1);return@post}
            val candidates=voices()
            val saved=if(prefs.getString("engine",null)==engine)prefs.getString("voice",null)else null
            val choice=EnglishVoicePolicy.choose(candidates.map{EnglishVoicePolicy.Choice(it.name,it.locale.language,it.locale.country,it.isNetworkConnectionRequired,it.quality)},saved)
            val voice=candidates.firstOrNull{it.name==choice?.name}
            if(voice==null||instance.setLanguage(Locale.US)<TextToSpeech.LANG_AVAILABLE||instance.setVoice(voice)!=TextToSpeech.SUCCESS){tryEngine(engines,index+1);return@post}
            selected=voice
            description="${voice.locale.toLanguageTag()} · ${voice.name}\n$engine"
            record("READY")
            instance.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
                override fun onStart(id:String?) { main.post {if(id=="turn-$utterance"){record("PLAYBACK_STARTED");owner?.onState("Speaking · ${selected?.locale?.toLanguageTag()}")}} }
                override fun onDone(id:String?) { main.post {if(id=="turn-$utterance"){record("PLAYBACK_COMPLETED");owner?.onComplete()}} }
                @Deprecated("Android legacy callback") override fun onError(id:String?)=onError(id,TextToSpeech.ERROR)
                override fun onError(id:String?,code:Int){main.post {if(id=="turn-$utterance"){record("PLAYBACK_FAILED_$code");owner?.onError("English speech failed ($code). Choose another installed English voice.")}}}
            })
            finishInitialization(true)
        } },engine)
    }
    private fun finishInitialization(ok:Boolean){
        initialized=ok;initializing=false
        if(!ok){tts?.shutdown();tts=null;description="English phone voice unavailable"}
        val waiting=readyWaiters;readyWaiters=mutableListOf();waiting.forEach{it(ok)}
    }
    private fun say(text:String,preview:Boolean){
        if(text.isBlank()||text.length>TextToSpeech.getMaxSpeechInputLength()){
            if(!preview)owner?.onError("Response is outside the phone speech length limit.");return
        }
        val id=(if(preview)"preview-" else "turn-")+utterance
        if(tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,id)!=TextToSpeech.SUCCESS&&!preview)
            owner?.onError("English speech could not start.")
    }
    fun showPicker(activity:Activity){
        context=activity.applicationContext
        initialize { ok ->
            if(activity.isFinishing||activity.isDestroyed)return@initialize
            if(!ok){
                AlertDialog.Builder(activity).setTitle("English phone voices")
                    .setMessage("No installed English voice is ready. Install English voice data in your phone's Text-to-speech settings.")
                    .setPositiveButton("Settings"){_,_->activity.startActivity(Intent("com.android.settings.TTS_SETTINGS"))}.setNegativeButton("Close",null).show()
                return@initialize
            }
            val available=voices()
            val labels=available.map{ "${it.locale.toLanguageTag()} · ${it.name}"+(if(it.isNetworkConnectionRequired)" · network" else " · offline") }.toTypedArray()
            var choice=available.indexOfFirst{it.name==selected?.name}.coerceAtLeast(0)
            AlertDialog.Builder(activity).setTitle("English voices · $engine")
                .setSingleChoiceItems(labels,choice){_,index->
                    choice=index;stop();val voice=available[index]
                    if(tts?.setVoice(voice)==TextToSpeech.SUCCESS){
                        selected=voice;prefs.edit().putString("engine",engine).putString("voice",voice.name).apply()
                        description="${voice.locale.toLanguageTag()} · ${voice.name}\n$engine";owner?.onReady()
                    }
                }
                .setPositiveButton("Preview",null)
                .setNeutralButton("Speech engine"){_,_->
                    val engines=engines()
                    AlertDialog.Builder(activity).setTitle("Installed speech engines")
                        .setItems(engines.map{it.second}.toTypedArray()){_,index->
                            stop();prefs.edit().putString("engine",engines[index].first).remove("voice").apply()
                            initialized=false;generation++;tts?.shutdown();tts=null;showPicker(activity)
                        }.setNegativeButton("Cancel",null).show()
                }.setNegativeButton("Done",null).create().apply{
                    setOnShowListener{getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
                        stop();say("Hello. I am Agent Lee. This English phone voice is ready.",true)
                    }}
                    show()
                }
        }
    }
}
