package industries.leeway.pocket

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.ViewGroup
import java.lang.ref.WeakReference

/** Installed English TTS is the Creator-authorized default. Voice One is opt-in. */
object PocketSpeech {
    private var owner:PocketVoiceHost.Listener?=null
    private var container=WeakReference<ViewGroup>(null)
    private var context:Context?=null
    private var canonical=false
    private fun selected(app:Context)=app.getSharedPreferences("pocket-speech-provider",Context.MODE_PRIVATE).getBoolean("experimental_voice_one",false)
    fun description()=if(canonical)"Voice One · experimental local model" else EnglishPhoneSpeech.description()
    fun attach(app:Context,listener:PocketVoiceHost.Listener,view:ViewGroup){
        context=app.applicationContext;owner=listener;container=WeakReference(view);canonical=selected(app)
        if(canonical)PocketVoiceHost.attach(app,listener,view)else EnglishPhoneSpeech.attach(app,listener)
    }
    fun speak(listener:PocketVoiceHost.Listener,text:String){
        if(owner!==listener)return
        if(canonical)PocketVoiceHost.speak(listener,text)else EnglishPhoneSpeech.speak(listener,text)
    }
    fun stop(listener:PocketVoiceHost.Listener){
        if(owner!==listener)return
        if(canonical)PocketVoiceHost.stop(listener)else EnglishPhoneSpeech.stop()
    }
    fun detach(listener:PocketVoiceHost.Listener){
        if(owner!==listener)return
        if(canonical)PocketVoiceHost.detach(listener)else EnglishPhoneSpeech.detach(listener)
        owner=null;container.clear()
    }
    private fun select(app:Context,useCanonical:Boolean){
        app.getSharedPreferences("pocket-speech-provider",Context.MODE_PRIVATE).edit().putBoolean("experimental_voice_one",useCanonical).apply()
        val previous=owner;val view=container.get()
        if(previous!=null&&view!=null){detach(previous);if(!useCanonical)PocketVoiceHost.releaseIdle();attach(app,previous,view)}else{
            canonical=useCanonical;if(!useCanonical)PocketVoiceHost.releaseIdle()
        }
    }
    fun settings(activity:Activity){
        AlertDialog.Builder(activity).setTitle("Phone voice")
            .setItems(arrayOf("English phone voice · choose and preview","Voice One · experimental, slow")){_,index->
                select(activity,index==1)
                if(index==0)EnglishPhoneSpeech.showPicker(activity)
            }.setNegativeButton("Cancel",null).show()
    }
}
