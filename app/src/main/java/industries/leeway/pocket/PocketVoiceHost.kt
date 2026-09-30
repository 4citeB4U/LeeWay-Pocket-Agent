package industries.leeway.pocket

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.*
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

    fun attach(context: Context, listener: Listener) {
        check(Looper.myLooper() == Looper.getMainLooper())
        session.attach(listener)
        stopPlayback()
        if (view == null) create(context.applicationContext)
        if (ready) listener.onReady() else prepare()
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
        if (pageReady) view?.evaluateJavascript("window.LeeWayAndroidVoice?.stop?.()", null)
    }

    private fun create(context: Context) {
        val renderer = ++rendererGeneration
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
                        session.owner?.onError("Voice Fabric could not load. Close and reopen to retry.")
                        destroyRenderer()
                    }
                }
                override fun onRenderProcessGone(v: WebView?, detail: RenderProcessGoneDetail?): Boolean {
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
            prepare()
        }
        @JavascriptInterface fun onReady(json: String) = deliver {
            ready = true
            session.owner?.onReady()
            dispatch()
        }
        @JavascriptInterface fun onState(json: String) = deliver {
            val message = runCatching { JSONObject(json).optString("message") }.getOrDefault("")
            if (message.isNotBlank()) session.owner?.onState(message)
        }
        @JavascriptInterface fun onError(json: String) = deliver {
            val error = runCatching { JSONObject(json) }.getOrNull()
            if (error?.optString("stage") == "prepare") {
                ready = false
                session.owner?.onError(error.optString("error", "VOICE_UNAVAILABLE"))
            }
        }
        @JavascriptInterface fun onPocketComplete(turn: Int) = deliver {
            session.ownerFor(turn)?.onComplete()
        }
        @JavascriptInterface fun onPocketError(turn: Int, error: String) = deliver {
            session.ownerFor(turn)?.onError(error)
        }
    }
}
