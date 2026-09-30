package industries.leeway.pocket

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.webkit.*
import android.util.Log
import org.json.JSONObject

/** One canonical Voice Fabric renderer per Pocket process; no Activity context retained. */
object PocketVoiceHost {
    interface Listener {
        fun onReady()
        fun onState(message: String)
        fun onComplete()
        fun onError(message: String)
    }

    private const val URL = "https://4citeb4u.github.io/LeeWay-Voice-Fabric/android-bridge.html"
    private val main = Handler(Looper.getMainLooper())
    private val session = VoiceSession<Listener>()
    private var view: WebView? = null
    private var ready = false
    private var pageReady = false
    private var rendererGeneration = 0
    private var diagnostics: SharedPreferences? = null
    private var lastDiagnosticKey = ""
    private var lastDiagnosticAt = 0L
    private var lastState = "NOT_STARTED"
    private var lastError = ""
    private var progress = JSONObject()

    private fun record(state: String = lastState, error: String = lastError) {
        lastState = VoiceProgress.safe(state)
        lastError = VoiceProgress.safe(error)
        val now = System.currentTimeMillis()
        val key = "$lastState|$lastError|$ready|${progress.optInt("percent", -1) / 5}|${progress.optString("file")}"
        if (key == lastDiagnosticKey && now - lastDiagnosticAt < 2000) return
        lastDiagnosticKey = key
        lastDiagnosticAt = now
        val snapshot = JSONObject().put("updatedAtMs", now).put("state", lastState)
            .put("ready", ready).put("pageReady", pageReady).put("error", lastError)
            .put("voicePackageId", "agent-lee-voice-one").put("progress", progress)
            .put("rendererGeneration", rendererGeneration)
        diagnostics?.edit()?.putString("latest_json", snapshot.toString())?.apply()
        Log.i("LeeWayPocketVoice", "$lastState ready=$ready progress=${progress.optInt("percent", -1)} error=$lastError")
    }

    fun attach(context: Context, listener: Listener) {
        check(Looper.myLooper() == Looper.getMainLooper())
        diagnostics = context.applicationContext.getSharedPreferences("leeway-pocket-voice-status", Context.MODE_PRIVATE)
        session.attach(listener)
        stopPlayback()
        if (view == null) create(context.applicationContext)
        if (ready) listener.onReady() else {
            if(lastError.isNotBlank())listener.onError(lastError)
            else listener.onState(VoiceProgress.label(lastState, progress))
            prepare()
        }
    }

    fun detach(listener: Listener) {
        if (session.detach(listener)) stopPlayback()
        // Keep model weights and worker warm; closing a conversation only stops speech.
    }

    fun speak(listener: Listener, text: String) {
        if (!session.queue(listener, text)) return
        stopPlayback()
        if (ready) dispatch() else prepare()
    }

    private fun prepare() {
        if (pageReady && !ready) view?.evaluateJavascript(
            "window.LeeWayAndroidVoice.prepare().catch(()=>{})", null
        )
    }

    private fun dispatch() {
        val (turn, text) = session.takePending() ?: return
        // Turn-bound callbacks cannot finish or speak for a subsequently opened activity.
        view?.evaluateJavascript(
            "window.LeeWayAndroidVoice.speak(${JSONObject.quote(text)})" +
                ".then(()=>LeeWayPocketNative.onPocketComplete($turn))" +
                ".catch(e=>LeeWayPocketNative.onPocketError($turn,String(e.message||e)))", null
        )
    }

    private fun stopPlayback() {
        // stop() advances the worker epoch. During prepare this can invalidate speaker
        // conditioning; queued speech is already revoked by VoiceSession on detach.
        if (pageReady && ready) view?.evaluateJavascript("window.LeeWayAndroidVoice?.stop?.()", null)
    }

    private fun create(context: Context) {
        val renderer = ++rendererGeneration
        record("LOADING_VOICE_FABRIC", "")
        WebView.setWebContentsDebuggingEnabled(false)
        view = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(Callbacks(renderer), "LeeWayPocketNative")
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(v: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        pageReady = false
                        ready = false
                        record("PAGE_LOAD_FAILED", "WebView error ${error?.errorCode}")
                        session.owner?.onError("Voice Fabric could not load. Close and reopen to retry.")
                        destroyRenderer()
                    }
                }
                override fun onRenderProcessGone(v: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    record("RENDERER_STOPPED", "Renderer exited; crashed=${detail?.didCrash()}")
                    session.owner?.onError("Voice renderer stopped. Close and reopen to reload Voice One.")
                    destroyRenderer()
                    return true
                }
            }
            loadUrl(URL)
        }
    }

    private fun destroyRenderer() {
        rendererGeneration++
        ready = false
        pageReady = false
        record()
        view?.removeJavascriptInterface("LeeWayPocketNative")
        view?.destroy()
        view = null
    }

    private class Callbacks(private val renderer: Int) {
        private fun deliver(block: () -> Unit) {
            main.post { if (renderer == rendererGeneration) block() }
        }
        @JavascriptInterface fun onBridgeReady(json: String) = deliver {
            pageReady = true
            record("ANDROID_BRIDGE_READY", "")
            prepare()
        }
        @JavascriptInterface fun onReady(json: String) = deliver {
            ready = true
            record("VOICE_ONE_READY", "")
            session.owner?.onReady()
            dispatch()
        }
        @JavascriptInterface fun onState(json: String) = deliver {
            val payload = runCatching { JSONObject(json) }.getOrNull() ?: return@deliver
            val message = VoiceProgress.safe(payload.optString("message"))
            payload.optJSONObject("progress")?.let { progress = VoiceProgress.fields(it) }
            if (message.isNotBlank()) {
                record(message)
                session.owner?.onState(VoiceProgress.label(message, progress))
            }
        }
        @JavascriptInterface fun onError(json: String) = deliver {
            val error = runCatching { JSONObject(json) }.getOrNull()
            if (error?.optString("stage") == "prepare") {
                ready = false
                val message = VoiceProgress.safe(error.optString("error", "VOICE_UNAVAILABLE"))
                record("PREPARE_FAILED", message)
                session.owner?.onError(message)
            }
        }
        @JavascriptInterface fun onPocketComplete(turn: Int) = deliver {
            if (session.ownerFor(turn) != null) record("PLAYBACK_COMPLETED", "")
            session.ownerFor(turn)?.onComplete()
        }
        @JavascriptInterface fun onPocketError(turn: Int, error: String) = deliver {
            if (session.ownerFor(turn) != null) {
                val message = VoiceProgress.safe(error)
                record("PLAYBACK_FAILED", message)
                session.ownerFor(turn)?.onError(message)
            }
        }
    }
}
