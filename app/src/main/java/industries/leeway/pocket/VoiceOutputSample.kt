/* REGION: LEEWAY.VOICE.NATIVE_OUTPUT; TAG: OWNER_REQUESTED_BUILTIN_SPEAKER_TEST
WHO: LeeWay Voice host under owner request. WHAT: Play the exact current Voice Fabric PCM test sample.
WHEN: Explicit on-screen test; WHERE: native phone audio adapter only.
WHY: Synthesis is not phone playback; this adapter cannot choose a speaker identity or use system TTS.
HOW: Existing employee binding, content verification, built-in speaker routing, real callbacks. LICENSE: MIT */
package industries.leeway.pocket

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.File
import org.json.JSONObject

internal object VoiceOutputSample {
    private var player:MediaPlayer?=null
    private var audioManager:AudioManager?=null
    private var focus:AudioFocusRequest?=null
    private val main=Handler(Looper.getMainLooper())
    fun stop(){player?.let{runCatching{it.stop()};it.release()};player=null;focus?.let{audioManager?.abandonAudioFocusRequest(it)};focus=null}
    fun play(context:Context){
        check(Looper.myLooper()==Looper.getMainLooper());stop()
        val app=context.applicationContext
        val prefs=app.getSharedPreferences("leeway-voice-output-test",Context.MODE_PRIVATE)
        val log=JSONObject().put("scope","OWNER_REQUESTED_VOICE_FABRIC_SAMPLE_NOT_CONVERSATION").put("humanAudibilityConfirmed",false).put("volumeChanged",false).put("systemTtsUsed",false)
        fun state(name:String){log.put("state",name).put("observedAtMs",System.currentTimeMillis());prefs.edit().putString("receipt_json",log.toString()).apply()}
        try{
            val folder=File(app.noBackupFilesDir,"voice-output-test")
            val meta=JSONObject(File(folder,"sample.json").readText());val waveform=File(folder,"sample.wav")
            val binding=AgentVoiceBinding.fromSources(JSONObject(app.assets.open("voice/employee-voice-bindings.v1.json").bufferedReader().use{it.readText()}),JSONObject(app.assets.open("voice/catalog.v1.json").bufferedReader().use{it.readText()}),BuildConfig.DEBUG)
            val hash=VerifiedVoiceSample.validate(meta,waveform.readBytes(),binding,System.currentTimeMillis())
            log.put("audioSha256",hash).put("voicePackageId",binding.voicePackageId).put("provider",binding.provider).put("selectionRevision",meta.getString("selectionRevision"))
            val manager=app.getSystemService(AudioManager::class.java);audioManager=manager
            val volume=manager.getStreamVolume(AudioManager.STREAM_MUSIC);log.put("streamVolume",volume).put("streamMaxVolume",manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            check(volume>0&&!manager.isStreamMute(AudioManager.STREAM_MUSIC)){"MEDIA_VOLUME_MUTED_OWNER_ADJUSTMENT_REQUIRED"}
            val speaker=manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull{it.type==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER}?:error("BUILTIN_SPEAKER_UNAVAILABLE")
            val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).setOnAudioFocusChangeListener{change->if(change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){state("AUDIO_FOCUS_LOST");stop()}}.build()
            check(manager.requestAudioFocus(focus!!)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){"AUDIO_FOCUS_NOT_GRANTED"}
            val media=MediaPlayer();player=media;media.setAudioAttributes(attributes);media.setDataSource(waveform.path)
            check(media.setPreferredDevice(speaker)){"SPEAKER_ROUTE_REQUEST_REJECTED"}
            log.put("requestedOutputType",speaker.type).put("requestedOutputName",speaker.productName.toString())
            media.setOnPreparedListener{
                log.put("durationMs",it.duration);it.start();state("PLAYING_ROUTE_NOT_YET_OBSERVED")
                main.postDelayed({if(player===it){val route=it.routedDevice;log.put("actualOutputType",route?.type).put("actualOutputName",route?.productName?.toString()).put("nativeIsPlaying",it.isPlaying);state(if(route?.type==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)"PLAYING_BUILTIN_SPEAKER" else "PLAYING_ROUTE_UNVERIFIED")}},350)
            }
            media.setOnCompletionListener{log.put("playedPositionMs",it.currentPosition);state("NATIVE_PLAYBACK_COMPLETED");stop()}
            media.setOnErrorListener{_,what,extra->log.put("nativeError",what).put("nativeExtra",extra);state("NATIVE_PLAYBACK_FAILED");stop();true}
            state("VERIFIED_WAVEFORM_PREPARING_OUTPUT");media.prepareAsync()
            Toast.makeText(app,"Playing the selected LeeWay Voice through phone speakers",Toast.LENGTH_SHORT).show()
        }catch(error:Exception){log.put("error",error.message?:error.javaClass.simpleName);state("VOICE_OUTPUT_TEST_BLOCKED");stop();Toast.makeText(app,error.message?:"Voice output unavailable",Toast.LENGTH_LONG).show()}
    }
}
