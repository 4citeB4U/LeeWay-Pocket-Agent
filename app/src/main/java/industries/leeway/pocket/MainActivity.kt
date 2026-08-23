package industries.leeway.pocket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var sphere: VoxelSphereView
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private lateinit var memory: MemoryStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.BLACK
        tts = TextToSpeech(this, this)
        memory = MemoryStore(this)
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 10)
        buildUi()
    }

    private fun buildUi() {
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        sphere = VoxelSphereView(this).apply {
            setOnClickListener { startListening() }
        }
        frame.addView(sphere, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val menu = TextView(this).apply {
            text = "☰"; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(22,10,22,10)
            setOnClickListener { showMenu() }
        }
        frame.addView(menu, FrameLayout.LayoutParams(96,96,Gravity.TOP or Gravity.START))
        setContentView(frame)
    }

    private fun showMenu() {
        val items = arrayOf("Past conversations","Personal memory","Lee's notebook","Record voice reference","Camera","Close")
        android.app.AlertDialog.Builder(this).setTitle("LeeWay Pocket").setItems(items) { d, which ->
            when(which) {
                0 -> showText("Past conversations", memory.readConversations())
                1 -> showText("Personal memory", memory.readPersonal())
                2 -> showText("Lee's notebook", memory.readNotebook())
                3 -> startListening()
                4 -> startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
                else -> d.dismiss()
            }
        }.show()
    }

    private fun showText(title:String, body:String) {
        android.app.AlertDialog.Builder(this).setTitle(title).setMessage(if(body.isBlank()) "Nothing saved yet." else body).setPositiveButton("Close",null).show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        if (tts.isSpeaking) tts.stop()
        sphere.state = VoxelSphereView.State.LISTENING
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object: RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { sphere.state = VoxelSphereView.State.LISTENING }
            override fun onBeginningOfSpeech() { if(tts.isSpeaking) tts.stop() }
            override fun onRmsChanged(rmsdB: Float) { sphere.voiceLevel = max(0f, rmsdB/12f) }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if(text.isNotBlank()) handle(text) else sphere.state = VoxelSphereView.State.IDLE
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if(tts.isSpeaking) tts.stop()
            }
            override fun onError(error: Int) { sphere.state = VoxelSphereView.State.IDLE }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { sphere.state = VoxelSphereView.State.THINKING }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        recognizer?.startListening(intent)
    }

    private fun handle(raw:String) {
        val q = raw.trim()
        memory.saveConversation("You: $q")
        val l = q.lowercase(Locale.US)
        when {
            l.startsWith("remember ") -> {
                memory.savePersonal(q.substringAfter("remember "))
                say("Yeah, I got that. I'll keep it in memory.")
            }
            l.startsWith("note ") || l.startsWith("lee note ") -> {
                memory.saveNotebook(q.substringAfter("note ").substringAfter("lee note "))
                say("Locked in. I put that in my notebook.")
            }
            l.startsWith("search ") || l.startsWith("research ") || l.startsWith("look up ") -> {
                val term = q.substringAfter("search ",q).substringAfter("research ",q).substringAfter("look up ",q)
                say("Bet. I'm opening the research lane for $term.")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(term))))
            }
            l.startsWith("call ") -> {
                say("I got you. Opening the dialer.")
                startActivity(Intent(Intent.ACTION_DIAL))
            }
            l.contains("schedule") || l.contains("appointment") || l.contains("calendar") -> {
                say("Let's put that on your calendar.")
                startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE,q))
            }
            l.contains("who are you") -> say("I'm Lee. Pocket edition. Same LeeWay bloodline, just built light enough to ride with you.")
            else -> say("I heard you. The Pocket Harness is live. Full model reasoning and cloned neural voice are the next runtime modules getting wired in.")
        }
    }

    private fun say(text:String) {
        memory.saveConversation("Lee: $text")
        sphere.state = VoxelSphereView.State.SPEAKING
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lee")
        sphere.postDelayed({ if(!tts.isSpeaking) sphere.state = VoxelSphereView.State.IDLE }, 2500)
    }

    override fun onDestroy() { recognizer?.destroy(); tts.shutdown(); super.onDestroy() }
}

class MemoryStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("leeway-pocket-memory", android.content.Context.MODE_PRIVATE)
    private fun append(key:String, value:String) {
        val old = prefs.getString(key, "").orEmpty()
        prefs.edit().putString(key, if(old.isBlank()) value else "$old\n$value").apply()
    }
    fun saveConversation(v:String)=append("conversations",v)
    fun savePersonal(v:String)=append("personal",v)
    fun saveNotebook(v:String)=append("notebook",v)
    fun readConversations()=prefs.getString("conversations","").orEmpty()
    fun readPersonal()=prefs.getString("personal","").orEmpty()
    fun readNotebook()=prefs.getString("notebook","").orEmpty()
}

class VoxelSphereView(context: android.content.Context): View(context) {
    enum class State { IDLE, LISTENING, THINKING, SPEAKING }
    var state = State.IDLE
        set(value) { field=value; invalidate() }
    var voiceLevel = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private val voxels = mutableListOf<Triple<Float,Float,Float>>()

    init {
        for(a in 0 until 360 step 14) for(b in -80..80 step 14) {
            val ar=Math.toRadians(a.toDouble()); val br=Math.toRadians(b.toDouble())
            voxels += Triple((cos(br)*cos(ar)).toFloat(), sin(br).toFloat(), (cos(br)*sin(ar)).toFloat())
        }
    }

    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        c.drawColor(Color.BLACK)
        val cx=width/2f; val cy=height/2f
        val base=min(width,height)*0.22f
        val pulse = when(state) {
            State.IDLE -> 1f + 0.03f*sin(t)
            State.LISTENING -> 1.05f + voiceLevel.coerceIn(0f,1f)*0.18f
            State.THINKING -> 0.95f + 0.08f*sin(t*3f)
            State.SPEAKING -> 1.03f + 0.11f*abs(sin(t*4f))
        }
        val rot=t*0.35f
        for((x0,y0,z0) in voxels) {
            val x = x0*cos(rot)-z0*sin(rot)
            val z = x0*sin(rot)+z0*cos(rot)
            val depth=(z+1.4f)/2.8f
            val sx=cx+x*base*pulse
            val sy=cy+y0*base*pulse
            val size=3f+7f*depth
            val intensity=(110+145*depth).toInt().coerceIn(0,255)
            paint.color=Color.rgb(intensity,intensity,intensity)
            c.drawRect(sx-size/2,sy-size/2,sx+size/2,sy+size/2,paint)
        }
        t+=0.045f
        postInvalidateOnAnimation()
    }
}
