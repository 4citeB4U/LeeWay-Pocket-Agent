package industries.leeway.pocket

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.webkit.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import org.json.JSONObject

/** One canonical Voice Fabric renderer per Pocket process; no Activity context retained. */
object PocketVoiceHost {
    interface Listener {
        fun onReady()
        fun onState(message: String)
        fun onComplete()
        fun onError(message: String)
    }

    private const val URL = "https://4citeb4u.github.io/LeeWay-Voice-Fabric/android-bridge.html?device=wasm"
    private val main = Handler(Looper.getMainLooper())
    private val session = VoiceSession<Listener>()
    private var view: WebView? = null
    private val readiness = VoiceReadiness()
    private val ready get() = readiness.ready
    private val pageReady get() = readiness.pageReady
    private var rendererGeneration = 0
    private var diagnostics: SharedPreferences? = null
    private var lastDiagnosticKey = ""
    private var lastDiagnosticAt = 0L
    private var lastState = "NOT_STARTED"
    private var lastError = ""
    private var progress = JSONObject()
    private var actualBackend:String?=null

    private fun record(state: String = lastState, error: String = lastError) {
        lastState = VoiceProgress.safe(state)
        lastError = VoiceProgress.safe(error)
        val now = System.currentTimeMillis()
        val key = "$lastState|$lastError|$ready|${progress.optInt("percent", -1) / 5}|${progress.optString("file")}|${progress.optString("message")}"
        if (key == lastDiagnosticKey && now - lastDiagnosticAt < 2000) return
        lastDiagnosticKey = key
        lastDiagnosticAt = now
        val snapshot = JSONObject().put("updatedAtMs", now).put("state", lastState)
            .put("ready", ready).put("pageReady", pageReady).put("error", lastError)
            .put("voicePackageId", "agent-lee-voice-one").put("progress", progress)
            .put("requestedBackend", "wasm")
            .put("actualBackend", actualBackend ?: JSONObject.NULL)
            .put("rendererGeneration", rendererGeneration)
            .put("attachedToWindow", view?.isAttachedToWindow == true)
        diagnostics?.edit()?.putString("latest_json", snapshot.toString())?.apply()
        Log.i("LeeWayPocketVoice", "$lastState ready=$ready progress=${progress.optInt("percent", -1)} error=$lastError")
    }

    fun attach(context: Context, listener: Listener, container: ViewGroup) {
        check(Looper.myLooper() == Looper.getMainLooper())
        diagnostics = context.applicationContext.getSharedPreferences("leeway-pocket-voice-status", Context.MODE_PRIVATE)
        session.attach(listener)
        stopPlayback()
        if (view == null) create(context.applicationContext)
        view?.let { renderer ->
            // Give Chromium a real window lifecycle without retaining an Activity context.
            // The compact voice panel owns UI; this child only hosts its canonical audio runtime.
            if (renderer.parent !== container) {
                (renderer.parent as? ViewGroup)?.removeView(renderer)
                container.addView(renderer, ViewGroup.LayoutParams(1, 1))
            }
        }
        if (ready) listener.onReady() else {
            if(lastError.isNotBlank())listener.onError(lastError)
            else listener.onState(VoiceProgress.label(lastState, progress))
            prepare()
        }
    }

    fun detach(listener: Listener) {
        if (session.detach(listener)) {
            stopPlayback()
            view?.let { (it.parent as? ViewGroup)?.removeView(it) }
        }
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
        readiness.beginPage()
        actualBackend = null
        record("LOADING_VOICE_FABRIC", "")
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        view = WebView(context).apply {
            isFocusable = false
            isFocusableInTouchMode = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setOnTouchListener { _, _ -> true }
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    lastDiagnosticKey = ""
                    record()
                }
                override fun onViewDetachedFromWindow(v: View) {
                    lastDiagnosticKey = ""
                    record()
                }
            })
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(Callbacks(renderer), "LeeWayPocketNative")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    readiness.beginPage()
                    actualBackend = null
                    PocketVoiceHost.progress = JSONObject()
                    record("LOADING_VOICE_FABRIC", "")
                }
                override fun onReceivedError(v: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        readiness.beginPage()
                        record("PAGE_LOAD_FAILED", "WebView error ${error?.errorCode}")
                        session.owner?.onError("Voice Fabric could not load. Close and reopen to retry.")
                        destroyRenderer()
                    }
                }
                override fun onRenderProcessGone(v: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    readiness.beginPage()
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
        readiness.beginPage()
        actualBackend = null
        record()
        view?.removeJavascriptInterface("LeeWayPocketNative")
        view?.let { (it.parent as? ViewGroup)?.removeView(it) }
        view?.destroy()
        view = null
    }

    private class Callbacks(private val renderer: Int) {
        private fun deliver(block: () -> Unit) {
            main.post { if (renderer == rendererGeneration) block() }
        }
        @JavascriptInterface fun onBridgeReady(json: String) = deliver {
            readiness.bridgeReady()
            actualBackend = null
            record("ANDROID_BRIDGE_READY", "")
            prepare()
        }
        @JavascriptInterface fun onReady(json: String) = deliver {
            val payload=runCatching{JSONObject(json)}.getOrNull()
            val reported=payload?.optString("device")?.ifBlank{payload.optString("backend")}.orEmpty()
            actualBackend=reported.takeIf{it=="wasm" || it=="webgpu"}
            if (!readiness.modelReady()) return@deliver
            record("VOICE_ONE_READY", "")
            session.owner?.onReady()
            dispatch()
        }
        @JavascriptInterface fun onState(json: String) = deliver {
            val payload = runCatching { JSONObject(json) }.getOrNull() ?: return@deliver
            val message = VoiceProgress.safe(payload.optString("message"))
            payload.optJSONObject("progress")?.let { progress = VoiceProgress.fields(it) }
            if (message.isNotBlank()) {
                if (message == "PREPARING_VOICE_ONE") readiness.unavailable()
                record(message)
                session.owner?.onState(VoiceProgress.label(message, progress))
            }
        }
        @JavascriptInterface fun onError(json: String) = deliver {
            val error = runCatching { JSONObject(json) }.getOrNull()
            if (error?.optString("stage") in setOf("prepare", "runtime")) {
                readiness.unavailable()
                actualBackend = null
                val message = VoiceProgress.safe(error?.optString("error", "VOICE_UNAVAILABLE") ?: "VOICE_UNAVAILABLE")
                record(if(error?.optString("stage") == "runtime") "RUNTIME_FAILED" else "PREPARE_FAILED", message)
                session.owner?.onError(message)
            }
        }
        @JavascriptInterface fun onPocketComplete(turn: Int) = deliver {
            if (session.ownerFor(turn) != null) record("PLAYBACK_COMPLETED", "")
            session.ownerFor(turn)?.onComplete()
        }
        @JavascriptInterface fun onPocketError(turn: Int, error: String) = deliver {
            if (session.ownerFor(turn) != null) {
                readiness.unavailable()
                actualBackend = null
                val message = VoiceProgress.safe(error)
                record("PLAYBACK_FAILED", message)
                session.ownerFor(turn)?.onError(message)
            }
        }
    }
}
