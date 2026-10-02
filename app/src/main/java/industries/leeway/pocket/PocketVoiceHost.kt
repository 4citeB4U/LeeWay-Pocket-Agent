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
    private const val DEFAULT_VOICE_ID = "agent-lee-voice-one"
    private const val DEFAULT_VOICE_NAME = "Agent Lee · Voice One"
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
    private var nativeDecoder:NativeVoiceDecoder?=null
    private var englishAdapter:FabricEnglishAdapter?=null
    @Volatile private var trustedPage=false
    private var selectionPrefs:SharedPreferences?=null
    private var requestedVoiceId=DEFAULT_VOICE_ID
    private var selectedVoiceId=""
    private var selectedVoiceName=DEFAULT_VOICE_NAME
    private var selectionConfirmed=false
    private var previewAfterSelection=false
    private var catalogCallback:((List<FabricVoiceCatalog.Choice>?,String?)->Unit)?=null
    fun description()="$selectedVoiceName · LeeWay Voice Fabric"

    private fun canonicalPage(url:String?):Boolean {
        val uri=android.net.Uri.parse(url ?: return false)
        return uri.scheme=="https" && uri.host=="4citeb4u.github.io" && uri.path=="/LeeWay-Voice-Fabric/android-bridge.html"
    }

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
            .put("voicePackageId", selectedVoiceId).put("requestedVoicePackageId",requestedVoiceId).put("progress", progress)
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
        selectionPrefs=context.applicationContext.getSharedPreferences("pocket-fabric-voice",Context.MODE_PRIVATE)
        val savedVoiceId=selectionPrefs?.getString("selected_id",null)
        if(selectionPrefs?.getBoolean("agent_voice_one_default_v2",false)!=true){
            val nextVoiceId=if(savedVoiceId.isNullOrBlank() || savedVoiceId=="android-installed-english") DEFAULT_VOICE_ID else savedVoiceId
            selectionPrefs?.edit()?.putString("selected_id",nextVoiceId)?.putBoolean("agent_voice_one_default_v2",true)?.apply()
        }
        requestedVoiceId=selectionPrefs?.getString("selected_id",DEFAULT_VOICE_ID) ?: DEFAULT_VOICE_ID
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
        if(pageReady && selectedVoiceId!=requestedVoiceId)requestSelection()
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

    fun stop(listener:Listener){if(session.cancel(listener)){stopPlayback();record("VOICE_STOPPED","")}}
    fun releaseIdle(){if(session.owner==null&&view!=null)destroyRenderer()}

    fun speak(listener: Listener, text: String) {
        if (!session.queue(listener, text)) return
        stopPlayback()
        if (ready) dispatch() else prepare()
    }

    private fun prepare() {
        if (pageReady && selectionConfirmed && !ready) view?.evaluateJavascript(
            "window.LeeWayAndroidVoice.prepare().catch(()=>{})", null
        )
    }

    private fun requestSelection(){
        if(!pageReady)return
        selectionConfirmed=false;readiness.unavailable()
        view?.evaluateJavascript("window.LeeWayAndroidVoice.select(${JSONObject.quote(requestedVoiceId)}).catch(e=>LeeWayPocketNative.onSelectionError(String(e.message||e)))",null)
    }
    private fun requestCatalog(){
        if(!pageReady)return
        view?.evaluateJavascript("window.LeeWayAndroidVoice.list().then(v=>LeeWayPocketNative.onCatalog(JSON.stringify(v))).catch(e=>LeeWayPocketNative.onCatalogError(String(e.message||e)))",null)
    }
    fun showVoicePicker(activity:android.app.Activity){
        val loading=android.app.AlertDialog.Builder(activity).setTitle("LeeWay Voice Fabric")
            .setMessage("Loading the canonical voice catalog…").setNegativeButton("Cancel",null).create()
        val timeout=Runnable{catalogCallback?.invoke(null,"Voice Fabric catalog timed out.");catalogCallback=null}
        catalogCallback={ choices,error ->
            main.removeCallbacks(timeout);catalogCallback=null
            if(!activity.isFinishing&&!activity.isDestroyed){
                loading.dismiss()
                if(error!=null||choices.isNullOrEmpty())android.app.AlertDialog.Builder(activity).setTitle("Voice Fabric")
                    .setMessage(error ?: "No available voice adapters were returned.").setPositiveButton("Close",null).show()
                else android.app.AlertDialog.Builder(activity).setTitle("Voice Fabric voices")
                    .setSingleChoiceItems(choices.map{"${it.name} · ${it.provider}"}.toTypedArray(),choices.indexOfFirst{it.id==requestedVoiceId}){ dialog,index ->
                        requestedVoiceId=choices[index].id
                        previewAfterSelection=true
                        session.owner?.onState("Preparing ${choices[index].name}")
                        requestSelection()
                        dialog.dismiss()
                    }.setNegativeButton("Cancel",null).show()
            }
        }
        loading.setOnDismissListener{catalogCallback=null;main.removeCallbacks(timeout)}
        loading.show();main.postDelayed(timeout,20000);requestCatalog()
    }

    private fun dispatch() {
        val (turn, text) = session.takePending() ?: return
        // Turn-bound callbacks cannot finish or speak for a subsequently opened activity.
        view?.evaluateJavascript(
            "window.LeeWayAndroidVoice.speak(${JSONObject.quote(text)})" +
                ".then(()=>LeeWayPocketNative.onPocketComplete($turn))" +
                ".catch(e=>e.name==='AbortError'?LeeWayPocketNative.onPocketStopped($turn):LeeWayPocketNative.onPocketError($turn,String(e.message||e)))", null
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
        selectionConfirmed=false
        actualBackend = null
        nativeDecoder=NativeVoiceDecoder(context,{trustedPage}) { payload ->
            main.post {
                if(renderer==rendererGeneration && trustedPage)
                    view?.evaluateJavascript("window.LeeWayNativeDecoderResult?.(${payload})",null)
            }
        }
        englishAdapter=FabricEnglishAdapter(context,{trustedPage}) { payload ->
            main.post { if(renderer==rendererGeneration&&trustedPage)view?.evaluateJavascript("window.LeeWayEnglishResult?.(${payload})",null) }
        }
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
            addJavascriptInterface(nativeDecoder!!,"LeeWayPocketDecoder")
            addJavascriptInterface(englishAdapter!!,"LeeWayPocketEnglish")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    nativeDecoder?.cancelActive()
                    englishAdapter?.stop()
                    trustedPage=canonicalPage(url)
                    readiness.beginPage()
                    selectionConfirmed=false
                    actualBackend = null
                    PocketVoiceHost.progress = JSONObject()
                    record("LOADING_VOICE_FABRIC", "")
                }
                override fun shouldOverrideUrlLoading(v:WebView?,request:WebResourceRequest?):Boolean =
                    request?.isForMainFrame==true && !canonicalPage(request.url.toString())
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
                    session.owner?.onError("Voice renderer stopped. Close and reopen to reload the selected voice.")
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
        previewAfterSelection=false
        actualBackend = null
        trustedPage=false
        englishAdapter?.close();englishAdapter=null
        nativeDecoder?.close()
        nativeDecoder=null
        record()
        view?.removeJavascriptInterface("LeeWayPocketNative")
        view?.removeJavascriptInterface("LeeWayPocketDecoder")
        view?.removeJavascriptInterface("LeeWayPocketEnglish")
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
            requestSelection()
            if(catalogCallback!=null)requestCatalog()
        }
        @JavascriptInterface fun onSelection(json:String)=deliver {
            val payload=runCatching{JSONObject(json)}.getOrNull() ?: return@deliver
            if(payload.optString("voicePackageId")!=requestedVoiceId)return@deliver
            selectedVoiceId=requestedVoiceId;selectedVoiceName=VoiceProgress.safe(payload.optString("name",selectedVoiceId))
            selectionConfirmed=true;readiness.unavailable()
            selectionPrefs?.edit()?.putString("selected_id",selectedVoiceId)?.apply()
            record("VOICE_SELECTED","")
            if(previewAfterSelection){
                previewAfterSelection=false
                session.owner?.let { owner ->
                    val phrase=if(selectedVoiceId==DEFAULT_VOICE_ID) "Hello. I am Agent Lee. Voice One is ready." else "Agent Lee voice test. This selected voice is ready."
                    speak(owner,phrase)
                    return@deliver
                }
            }
            if(payload.optBoolean("ready")&&readiness.modelReady()){
                record("VOICE_READY","");session.owner?.onReady();dispatch()
            }else if(session.owner!=null)prepare()
        }
        @JavascriptInterface fun onSelectionError(error:String)=deliver {
            previewAfterSelection=false;selectionConfirmed=false;readiness.unavailable();record("VOICE_SELECTION_FAILED",VoiceProgress.safe(error))
            session.owner?.onError(VoiceProgress.safe(error))
        }
        @JavascriptInterface fun onCatalog(json:String)=deliver {
            val parsed=runCatching{FabricVoiceCatalog.parse(json)}
            catalogCallback?.invoke(parsed.getOrNull(),parsed.exceptionOrNull()?.message)
        }
        @JavascriptInterface fun onCatalogError(error:String)=deliver {catalogCallback?.invoke(null,VoiceProgress.safe(error))}
        @JavascriptInterface fun onReady(json: String) = deliver {
            val payload=runCatching{JSONObject(json)}.getOrNull()
            if(!selectionConfirmed||payload?.optString("voicePackageId")!=requestedVoiceId)return@deliver
            val reported=payload?.optString("device")?.ifBlank{payload.optString("backend")}.orEmpty()
            actualBackend=reported.takeIf{it=="wasm" || it=="webgpu" || it=="android-native"}
            if(payload?.optString("provider")=="android-tts")selectedVoiceName="${VoiceProgress.safe(payload.optString("voiceName"))} � ${VoiceProgress.safe(payload.optString("actualVoice"))} � ${VoiceProgress.safe(payload.optString("actualEngine"))}"
            if (!readiness.modelReady()) return@deliver
            record("VOICE_READY", "")
            session.owner?.onReady()
            dispatch()
        }
        @JavascriptInterface fun onState(json: String) = deliver {
            val payload = runCatching { JSONObject(json) }.getOrNull() ?: return@deliver
            if(payload.optString("voicePackageId")!=requestedVoiceId)return@deliver
            val message = VoiceProgress.safe(payload.optString("message"))
            payload.optJSONObject("progress")?.let { progress = VoiceProgress.fields(it) }
            if (message.isNotBlank()) {
                if (message == "PREPARING_VOICE_ONE" || message == "PREPARING_VOICE") readiness.unavailable()
                if(message=="VOICE_STOPPED"&&!payload.optBoolean("ready",true))readiness.unavailable()
                record(message)
                session.owner?.onState(VoiceProgress.label(message, progress))
            }
        }
        @JavascriptInterface fun onError(json: String) = deliver {
            val error = runCatching { JSONObject(json) }.getOrNull()
            if(error?.optString("voicePackageId").orEmpty().let{it.isNotBlank()&&it!=requestedVoiceId})return@deliver
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
        @JavascriptInterface fun onPocketStopped(turn:Int)=deliver {
            if(session.ownerFor(turn)!=null){record("VOICE_STOPPED","");session.ownerFor(turn)?.onState("Stopped")}
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