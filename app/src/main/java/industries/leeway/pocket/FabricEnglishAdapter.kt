package industries.leeway.pocket

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import org.json.JSONObject

/** Fixed English speech adapter; only Fabric's trusted page may invoke it. */
class FabricEnglishAdapter(private val context:Context,private val trusted:()->Boolean,private val emit:(JSONObject)->Unit){
    private val main=Handler(Looper.getMainLooper())
    private var active:String?=null
    private var preparing=false
    private var closed=false
    private val listener=object:PocketVoiceHost.Listener{
        override fun onReady(){if(preparing){preparing=false;complete(EnglishPhoneSpeech.binding())}}
        override fun onState(message:String){}
        override fun onComplete(){complete(EnglishPhoneSpeech.binding().put("completed",true))}
        override fun onError(message:String){complete(error=message)}
    }
    private fun complete(result:JSONObject?=null,error:String?=null){
        val id=active?:return;active=null
        if(!closed&&trusted())emit(JSONObject().put("id",id).apply{if(error!=null)put("error",error) else put("result",result)})
    }
    private fun submit(id:String,work:()->Unit){main.post{
        if(closed||!trusted()||!id.matches(Regex("speech-[0-9]+-[0-9]+")))return@post
        if(active!=null){emit(JSONObject().put("id",id).put("error","ANDROID_SPEECH_BUSY"));return@post}
        active=id
        try{work()}catch(error:Exception){complete(error=error.message?.take(240)?:"ANDROID_SPEECH_FAILED")}
    }}
    @JavascriptInterface fun prepare(id:String)=submit(id){preparing=true;EnglishPhoneSpeech.attach(context,listener)}
    @JavascriptInterface fun speak(id:String,text:String)=submit(id){
        preparing=false
        if(text.isBlank()||text.length>4000)complete(error="ANDROID_SPEECH_TEXT_LIMIT")
        else EnglishPhoneSpeech.speak(listener,text)
    }
    @JavascriptInterface fun stop(){main.post{active=null;preparing=false;EnglishPhoneSpeech.stop()}}
    fun close(){main.post{if(!closed){closed=true;active=null;EnglishPhoneSpeech.detach(listener)}}}
}
