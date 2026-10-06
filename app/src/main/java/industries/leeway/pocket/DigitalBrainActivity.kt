/*
REGION: LEEWAY.BRAIN.ADAPTER.ANDROID
TAG: ORIGINAL_VIEWER_LOCAL_ONLY_HOST
WHO: Owner-local Agent Lee app; WHAT: Host the original Brain renderer inside the same APK.
WHEN: Hamburger Digital Brain entry; WHERE: non-exported Android activity, no second launcher.
WHY: A local data bridge must never be exposed to remote pages or arbitrary file paths.
HOW: Fixed internal origin, allowlisted packaged assets, read-only Brain queries, no network fallback.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class DigitalBrainActivity:Activity() {
    private lateinit var web:WebView
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT
        web=WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=false
            settings.allowFileAccess=false;settings.allowContentAccess=false
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically=false
            settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            addJavascriptInterface(LocalBrainBridge(),"LeeWayBrainView")
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean = request.url.toString()!=ENTRY
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                    val uri=request.url
                    if(request.method!="GET" || uri.scheme!="https" || uri.host!=HOST || uri.port!=-1 || uri.encodedQuery!=null || uri.encodedFragment!=null)return deny()
                    val name=uri.path?.removePrefix("/assets/digital-brain/") ?: return deny()
                    if(uri.encodedPath!="/assets/digital-brain/"+name || name !in ALLOWED)return deny()
                    return try {
                        val mime=when{name.endsWith(".html")->"text/html";name.endsWith(".js")->"text/javascript";name.endsWith(".png")->"image/png";name.endsWith(".jpg")->"image/jpeg";else->"text/plain"}
                        WebResourceResponse(mime,if(mime.startsWith("text/"))"UTF-8" else null,200,"OK",mapOf("Cache-Control" to "no-store","X-Content-Type-Options" to "nosniff"),assets.open("digital-brain/"+name))
                    } catch(_:Exception){deny()}
                }
            }
            loadUrl(ENTRY)
        }
        setContentView(web)
    }
    private fun deny()=WebResourceResponse("text/plain","UTF-8",403,"Blocked",mapOf("Cache-Control" to "no-store"),ByteArrayInputStream("LOCAL_BRAIN_ASSET_ORIGIN_REQUIRED".toByteArray()))
    private inner class LocalBrainBridge {
        @JavascriptInterface fun query(operation:String,arguments:String):String=AndroidBrainViewer.request(this@DigitalBrainActivity,operation,arguments)
        @JavascriptInterface fun close(){runOnUiThread{finish()}}
    }
    override fun onPause(){if(::web.isInitialized){web.onPause()};super.onPause()}
    override fun onResume(){super.onResume();if(::web.isInitialized){web.onResume()}}
    override fun onDestroy(){if(::web.isInitialized){web.removeJavascriptInterface("LeeWayBrainView");web.stopLoading();web.destroy()};super.onDestroy()}
    companion object {
        private const val HOST="appassets.androidplatform.net"
        private const val ENTRY="https://appassets.androidplatform.net/assets/digital-brain/brain.html"
        // Derived renderer inputs only. Native queries never accept a local file selector.
        private val ALLOWED=setOf("brain.html","local-brain-binding.js","vendor/three.module.js","vendor/OrbitControls.js","nucleus.png")
    }
}
