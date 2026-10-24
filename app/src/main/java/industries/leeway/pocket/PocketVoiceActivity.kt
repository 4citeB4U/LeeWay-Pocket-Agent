/*
LEEWAY HEADER - DO NOT REMOVE
REGION: LEEWAY.VOICE.CONVERSATION
TAG: ONE_CONSCIOUSNESS_LOCAL_CONTINUUM_OR_PAIRED_TURN
5WH: WHAT = Hear a request, retrieve local Continuum sources or use the existing paired turn, and speak through LeeWay Voice.
WHY = Pocket retrieval and its inspector must use the same retained records and the selected shared voice.
WHO = Agent Lee / the existing installation owner. WHERE = Existing Pocket conversation activity.
WHEN = User speech; the current retained user event is excluded from its own retrieval.
HOW = Existing event writer and local read adapter; route prepared Continuum speech through the existing paired Voice Fabric renderer and Pocket playback lifecycle.
SOURCE: PocketVoiceActivity.kt SHA256 5858f2ddf2adabcd967dabec40c4e3c9ec48f7c08f97a15129c248a44b4b39c0.
LOCAL READ: No new model, voice provider, store, Formula execution or governance receipt.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.CancellationSignal
import android.os.Bundle
import android.speech.*
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.io.File

class PocketVoiceActivity : Activity() {
    companion object {
        @Volatile private var current: PocketVoiceActivity? = null

        fun stopRoundBoxVoice() {
            current?.let { activity -> activity.runOnUiThread { activity.stopVoiceOutput() } }
        }

        fun closeRoundBoxVoice() {
            current?.let { activity -> activity.runOnUiThread {
                activity.stopVoiceOutput()
                activity.finish()
            } }
        }

        fun launchIntent(c: Context, newTask: Boolean = false) = Intent(c, PocketVoiceActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var container: FrameLayout
    private var recognizer: SpeechRecognizer? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var localVoice: PocketVoiceHost.Listener? = null
    @Volatile private var voiceRenderCancellation: CancellationSignal? = null
    private val recognitionSession = RecognitionSession()
    @Volatile private var activeToken = 0
    @Volatile private var closed = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        current = this
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        container = FrameLayout(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32); setBackgroundColor(Color.argb(170, 3, 8, 16))
        }
        status = TextView(this).apply { text = "Agent Lee"; setTextColor(Color.WHITE); textSize = 18f }
        transcript = TextView(this).apply {
            text = "Listening is independent of the visual sphere."
            setTextColor(Color.LTGRAY); textSize = 15f; gravity = Gravity.CENTER
            setTextIsSelectable(true)
        }
        panel.addView(status)
        panel.addView(ScrollView(this).apply { addView(transcript) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(Button(this).apply { text = "LISTEN"; setOnClickListener { startListening() } })
        panel.addView(Button(this).apply { text = "CLOSE VISUAL"; setOnClickListener { finish() } })
        container.addView(panel); setContentView(container)
        // Round Box owns presentation; retain this exact Activity as the voice/runtime executor.
        if(intent.getBooleanExtra("LEEWAY_ROUNDBOX_PRESENTATION",false)) {
            panel.visibility = android.view.View.GONE
            container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val attrs=window.attributes
            attrs.width=1; attrs.height=1
            attrs.gravity=android.view.Gravity.TOP or android.view.Gravity.START
            window.attributes=attrs
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 701)
        else { val typed=intent.getStringExtra("LEEWAY_ROUNDBOX_TEXT")?.trim(); if(!typed.isNullOrEmpty()) handle(typed) else startListening() }
    }

    private fun isActive(token: Int) = !closed && activeToken == token
    private fun nextToken(): Int? = recognitionSession.begin()?.also { activeToken = it }

    private fun stopLocalVoice() {
        voiceRenderCancellation?.cancel()
        voiceRenderCancellation = null
        val listener = localVoice ?: return
        localVoice = null
        PocketSpeech.stop(listener)
        PocketSpeech.detach(listener)
    }

    /** Cancels the existing Voice Fabric request and player from the Round Box speaker control. */
    private fun stopVoiceOutput() {
        recognitionSession.cancel()
        activeToken = -1
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        stopLocalVoice()
        releasePlayer()
        ElementalEmblemView.visualState = "IDLE"
        RoundBoxWindow.deliver("voice-stopped", "")
    }

    private fun releasePlayer() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        audioFile?.delete(); audioFile = null
    }

    private fun startListening() {
        if (closed) return
        val token = nextToken() ?: return
        stopLocalVoice(); releasePlayer()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            transcript.text = "Speech recognition unavailable."; return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            private fun current() = isActive(token) && recognitionSession.accepts(token)
            override fun onReadyForSpeech(p: Bundle?) {
                if (!current()) return
                ElementalEmblemView.visualState = "LISTENING"; status.text = "Listening"; transcript.text = "Speak now…"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (current()) { ElementalEmblemView.visualState = "THINKING"; status.text = "Thinking" }
            }
            override fun onError(e: Int) {
                if (current()) {
                    status.text = "Listening"
                    window.decorView.postDelayed({ if (current()) startListening() }, 700)
                }
            }
            override fun onPartialResults(b: Bundle?) {
                if (current()) b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { transcript.text = it }
            }
            override fun onEvent(t: Int, b: Bundle?) {}
            override fun onResults(b: Bundle?) {
                if (!current()) return
                val q = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                if (q.isNotBlank()) handle(q) else startListening()
            }
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        })
    }

    private fun handle(q: String) {
        val token = nextToken() ?: return
        val isLocalRequest = LocalContinuumRetrieval.query(q) != null
        recognizer?.destroy(); recognizer = null
        stopLocalVoice(); releasePlayer()
        transcript.text = "You: $q"; status.text = "Thinking"; ElementalEmblemView.visualState = "THINKING"
        Thread {
            try {
                val runtime = UnifiedAgentLeeRuntime(applicationContext)
                runtime.requireExistingContinuumBeforeCapture(q)
                if (!isActive(token)) return@Thread
                val userEventId = appendContinuum("user", q)
                val turn = runtime.turn(q, userEventId)
                if (!isActive(token)) return@Thread
                val responseEventId = appendContinuum("agent-lee", turn.text)
                check(responseEventId > 0) { "CONTINUUM_RESPONSE_RETENTION_FAILED" }
                val local = turn.localContinuum
                if (local != null) {
                    runOnUiThread {
                        if (!isActive(token)) return@runOnUiThread
                        transcript.text = "Agent Lee: ${turn.text}"
                        RoundBoxWindow.deliver("response",turn.text)
                        speakLocal(local, token)
                    }
                } else {
                    // Only the existing pinned paired path returns these already-validated voice bytes.
                    val wav = File(cacheDir, "agent-lee-turn-$token.wav")
                    wav.writeBytes(turn.audio)
                    runOnUiThread {
                        if (!isActive(token)) { wav.delete(); return@runOnUiThread }
                        transcript.text = "Agent Lee: ${turn.text}"
                        RoundBoxWindow.deliver("response",turn.text)
                        status.text = "Speaking · ${turn.voicePackageId}"; ElementalEmblemView.visualState = "SPEAKING"
                        audioFile = wav
                        try {
                            val playback = MediaPlayer()
                            player = playback
                            playback.apply {
                                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                                setDataSource(wav.absolutePath)
                                setOnCompletionListener {
                                    if (isActive(token)) {
                                        RoundBoxWindow.deliver("voice", "Playback complete")
                                        startListening()
                                    }
                                }
                                setOnErrorListener { _, _, _ ->
                                    if (isActive(token)) {
                                        releasePlayer(); status.text = "Voice playback failed"
                                        ElementalEmblemView.visualState = "BLOCKED"
                                        RoundBoxWindow.deliver("voice-error", "VOICE_PLAYBACK_FAILED")
                                    }
                                    true
                                }
                                prepare(); start()
                            }
                        } catch (error: Exception) {
                            releasePlayer(); showFailure(error, token, local = false)
                        }
                    }
                }
            } catch (error: Exception) {
                runOnUiThread { showFailure(error, token, isLocalRequest) }
            }
        }.start()
    }

    private fun showFailure(error: Exception, token: Int, local: Boolean) {
        if (!isActive(token)) return
        ElementalEmblemView.visualState = "BLOCKED"
        status.text = if (local) "Continuum read unavailable" else "Agent Lee connection blocked"
        transcript.text = error.message ?: if (local) "Local Continuum unavailable" else "Conversation provider unavailable"
        RoundBoxWindow.deliver(if(local) "voice-error" else "error",error.message ?: "AGENT_LEE_CONNECTION_BLOCKED")
        // Local errors remain visible until LISTEN or CLOSE; they cannot become a paired PC query.
        if (!local) window.decorView.postDelayed({ if (isActive(token)) startListening() }, 1_200)
    }

    private fun speakLocal(reply: LocalContinuumReply, token: Int) {
        status.text = "Preparing LeeWay Voice Fabric"
        RoundBoxWindow.deliver("voice", "Preparing LeeWay Voice Fabric")
        val cancellation=CancellationSignal()
        voiceRenderCancellation=cancellation
        Thread {
            try {
                // Only the prepared extractive reply crosses the paired renderer boundary. The request is not re-run through cognition.
                val turn=UnifiedAgentLeeRuntime(applicationContext).renderSpeechSegment(reply.spokenText,cancellation)
                check(turn.audio.size>44){"LEEWAY_VOICE_AUDIO_EMPTY"}
                val wav=File(cacheDir,"agent-lee-continuum-$token.wav")
                wav.writeBytes(turn.audio)
                runOnUiThread {
                    if(!isActive(token) || cancellation.isCanceled){wav.delete();return@runOnUiThread}
                    voiceRenderCancellation=null
                    status.text="Speaking · ${turn.voicePackageId}"
                    ElementalEmblemView.visualState="SPEAKING"
                    audioFile=wav
                    try {
                        val playback=MediaPlayer();player=playback
                        playback.apply {
                            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                            setDataSource(wav.absolutePath)
                            setOnCompletionListener {
                                if(isActive(token)) {
                                    RoundBoxWindow.deliver("voice", "Playback complete")
                                    startListening()
                                }
                            }
                            setOnErrorListener { _,_,_ ->
                                if(isActive(token)) {
                                    releasePlayer();status.text="Voice playback failed";ElementalEmblemView.visualState="BLOCKED"
                                    RoundBoxWindow.deliver("voice-error", "VOICE_PLAYBACK_FAILED")
                                }
                                true
                            }
                            prepare();start()
                            RoundBoxWindow.deliver("voice", "Speaking · ${turn.voicePackageId}")
                        }
                    } catch(error:Exception) { releasePlayer();showFailure(error,token,local=true) }
                }
            } catch(error:Exception) {
                runOnUiThread { if(isActive(token) && !cancellation.isCanceled) showFailure(error,token,local=true) }
            }
        }.start()
    }

    /** Existing writer and payload shape retained; its returned insertion ID supplies the read cutoff. */
    private fun appendContinuum(actor: String, text: String): Long {
        val db = LeeWayBodyDatabases(this).continuum.writableDatabase
        val v = android.content.ContentValues()
        v.put("universe_id", industries.leeway.brain.DigitalBrain.continuumRootId(AndroidDigitalBrainAdapter.identity(this)) + ":conversation")
        v.put("event_type", "conversation.turn")
        v.put("payload_json", JSONObject().put("actor", actor).put("text", text).toString())
        v.put("captured_at", System.currentTimeMillis())
        return db.insert("continuum_events", null, v)
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        if (r == 701 && g.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
    }

    /** The Round Box reuses this single activity when its Speak control opens listening again. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val typed = intent.getStringExtra("LEEWAY_ROUNDBOX_TEXT")?.trim()
        if (!typed.isNullOrEmpty()) handle(typed) else startListening()
    }

    override fun onDestroy() {
        closed = true; recognitionSession.cancel(); activeToken = -1
        if (current === this) current = null
        ElementalEmblemView.visualState = "IDLE"
        recognizer?.destroy(); recognizer = null
        stopLocalVoice(); releasePlayer(); super.onDestroy()
    }

}
