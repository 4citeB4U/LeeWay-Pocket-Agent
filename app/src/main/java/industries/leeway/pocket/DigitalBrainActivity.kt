
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
    private val selectedEntry:String get()=if(intent.getBooleanExtra("hardware",false))DIAGNOSTICS_ENTRY else ENTRY
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
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean = request.url.toString()!=selectedEntry
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                    val uri=request.url
                    if(request.method!="GET" || uri.scheme!="https" || uri.host!=HOST || uri.port!=-1 || uri.encodedQuery!=null || uri.encodedFragment!=null)return deny()
                    val name=uri.path?.removePrefix("/assets/digital-brain/") ?: return deny()
                    if(uri.encodedPath!="/assets/digital-brain/"+name || name !in ALLOWED)return deny()
                    return try {
                        val mime=when{name.endsWith(".html")->"text/html";name.endsWith(".js")->"text/javascript";name.endsWith(".css")->"text/css";name.endsWith(".png")->"image/png";name.endsWith(".jpg")->"image/jpeg";else->"text/plain"}
                        WebResourceResponse(mime,if(mime.startsWith("text/"))"UTF-8" else null,200,"OK",mapOf("Cache-Control" to "no-store","X-Content-Type-Options" to "nosniff"),assets.open("digital-brain/"+name))
                    } catch(_:Exception){deny()}
                }
            }
            loadUrl(selectedEntry)
        }
        setContentView(web)
        // A floating translucent dialog otherwise measures this all-absolute WebView at zero height.
        window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        OwnerSurfaceWindow.attach(this, "Digital Brain / Diagnostics")
    }
    private fun deny()=WebResourceResponse("text/plain","UTF-8",403,"Blocked",mapOf("Cache-Control" to "no-store"),ByteArrayInputStream("LOCAL_BRAIN_ASSET_ORIGIN_REQUIRED".toByteArray()))
    private inner class LocalBrainBridge {
        @JavascriptInterface fun query(operation:String,arguments:String):String=AndroidBrainViewer.request(this@DigitalBrainActivity,operation,arguments)
        @JavascriptInterface fun initialSurface():String=if(intent.getBooleanExtra("hardware",false))"hardware" else "brain"
        @JavascriptInterface fun openDiagnostics(){runOnUiThread{
            startActivity(android.content.Intent(this@DigitalBrainActivity,DigitalBrainActivity::class.java).putExtra("hardware",true))
        }}
        @JavascriptInterface fun browseFiles(){runOnUiThread{
            val picker=android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply{
                addCategory(android.content.Intent.CATEGORY_OPENABLE);type="*/*"
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivityForResult(picker,8127)
        }}
        @JavascriptInterface fun openFileNode(id:String){runOnUiThread{
            try {
                val reply=org.json.JSONObject(AndroidBrainViewer.request(this@DigitalBrainActivity,"node",org.json.JSONObject().put("id",id).toString()))
                check(reply.optBoolean("ok"))
                val node=reply.getJSONObject("result").getJSONObject("node")
                check(node.optString("type")=="file")
                val source=node.getString("source_path")
                val uri=java.net.URI(source)
                check(uri.scheme=="file" && uri.rawAuthority.isNullOrBlank())
                val path=java.io.File(uri).toPath()
                val root=filesDir.toPath().toRealPath()
                check(!java.nio.file.Files.isSymbolicLink(path) && java.nio.file.Files.isRegularFile(path,java.nio.file.LinkOption.NOFOLLOW_LINKS))
                check(path.toRealPath().startsWith(root))
                startActivity(android.content.Intent(this@DigitalBrainActivity,BrainFilePreviewActivity::class.java).setData(android.net.Uri.fromFile(path.toFile())))
            }catch(_:Exception){android.widget.Toast.makeText(this@DigitalBrainActivity,"File not available under this Brain's approved local scope",android.widget.Toast.LENGTH_LONG).show()}
        }}
        @JavascriptInterface fun close(){runOnUiThread{finish()}}
    }
    @Deprecated("Android file picker callback maintained for existing Activity host")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:android.content.Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==8127 && resultCode==RESULT_OK){
            val selected=data?.data?:return
            val preview=android.content.Intent(this,BrainFilePreviewActivity::class.java)
                .setDataAndType(selected,contentResolver.getType(selected)?:"application/octet-stream")
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(preview)
        }
    }
    override fun onPause(){if(::web.isInitialized){web.onPause()};super.onPause()}
    override fun onResume(){super.onResume();if(::web.isInitialized){web.onResume()}}
    override fun onDestroy(){if(::web.isInitialized){web.removeJavascriptInterface("LeeWayBrainView");web.stopLoading();web.destroy()};super.onDestroy()}
    companion object {
        private const val HOST="appassets.androidplatform.net"
        private const val ENTRY="https://appassets.androidplatform.net/assets/digital-brain/brain.html"
        private const val DIAGNOSTICS_ENTRY="https://appassets.androidplatform.net/assets/digital-brain/diagnostics/index.html"
        // Derived renderer inputs only. Native queries never accept a local file selector.
        private val ALLOWED=setOf("brain.html","local-brain-binding.js","brain-live-overrides.css","brain-live-return.js","vendor/three.module.js","vendor/OrbitControls.js","nucleus.png","diagnostics/index.html","diagnostics/app.js","diagnostics/app.css")
    }
}

