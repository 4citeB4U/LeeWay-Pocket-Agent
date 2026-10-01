package industries.leeway.pocket

import android.app.Activity
import android.content.Context
import android.view.ViewGroup

/** All selection and speech dispatch pass through canonical Voice Fabric. */
object PocketSpeech {
    fun description()=PocketVoiceHost.description()
    fun attach(app:Context,listener:PocketVoiceHost.Listener,view:ViewGroup)=PocketVoiceHost.attach(app,listener,view)
    fun speak(listener:PocketVoiceHost.Listener,text:String)=PocketVoiceHost.speak(listener,text)
    fun stop(listener:PocketVoiceHost.Listener)=PocketVoiceHost.stop(listener)
    fun detach(listener:PocketVoiceHost.Listener)=PocketVoiceHost.detach(listener)
    fun settings(activity:Activity){
        activity.startActivity(PocketVoiceActivity.launchIntent(activity).putExtra("show_fabric_voice_picker",true))
    }
}
