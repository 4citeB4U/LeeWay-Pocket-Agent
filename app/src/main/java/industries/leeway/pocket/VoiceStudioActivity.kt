/*
REGION: LEEWAY.VOICE.ANDROID_SURFACE
TAG: CANONICAL_STUDIO_BUNDLE_WITH_PAIRED_API_ADAPTER
WHO: Owner opening Voice in Agent Lee. WHAT: Host the original Voice Fabric Studio and tuning controls.
WHY: A different miniature selector must not replace the canonical Studio.
WHERE: Non-exported activity in the existing Pocket app, no new runtime or voice authority.
HOW: Hash-admitted packaged assets; fixed local HTTPS origin; bounded asynchronous API adapter
through the existing TLS-pinned owner pairing. Never expose pairing tokens or generic native access.
AUTHORIZED ROLES: OWNER_STUDIO_UI. LICENSE: MIT; original assets retain their licenses.
*/
package industries.leeway.pocket

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsets
import android.webkit.*
import android.widget.*
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class VoiceStudioActivity:Activity(){
    private var web:WebView?=null
    @Volatile private var closed=false
    private lateinit var files:JSONObject
    private var chooser:ValueCallback<Array<Uri>>?=null
    private val executor=ThreadPoolExecutor(2,2,15,TimeUnit.SECONDS,ArrayBlockingQueue<Runnable>(2))
    private val host="appassets.androidplatform.net"
    private val entry="https://appassets.androidplatform.net/voice-studio/studio.html"
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val layout=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(8,14,22))
            setOnApplyWindowInsetsListener { view, insets ->
                val safe=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left,safe.top,safe.right,safe.bottom)
                insets.inset(safe.left,safe.top,safe.right,safe.bottom)
            }
        }
        layout.addView(Button(this).apply{text="Return to Agent Lee";setOnClickListener{finish()}})
        setContentView(layout)
        OwnerSurfaceWindow.attach(this, "LeeWay Voice")
        layout.requestApplyInsets()
        val lock=runCatching{JSONObject(assets.open("voice-studio/SOURCE.json").bufferedReader(Charsets.UTF_8).use{it.readText()})}.getOrNull()
        if(lock==null||lock.optString("repository")!="4citeB4U/LeeWay-Voice-Fabric"||lock.optString("schemaVersion")!="leeway.voice-studio-bundle.v1"){
            layout.addView(TextView(this).apply{text="Canonical Voice Studio package is not admitted.";setTextColor(Color.WHITE)});return
        }
        files=lock.getJSONObject("files")
        if(files.length()>512){finish();return}
        web=WebView(this).apply{
            setBackgroundColor(Color.rgb(8,14,22));settings.javaScriptEnabled=true;settings.domStorageEnabled=true
            settings.allowFileAccess=false;settings.allowContentAccess=true
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.javaScriptCanOpenWindowsAutomatically=false;settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture=true
            addJavascriptInterface(StudioBridge(),"LeeWayVoiceStudio")
            webChromeClient=object:WebChromeClient(){
                override fun onPermissionRequest(request:PermissionRequest){request.deny()}
                override fun onShowFileChooser(v:WebView,callback:ValueCallback<Array<Uri>>,params:FileChooserParams):Boolean{
                    chooser?.onReceiveValue(null);chooser=callback
                    return try{startActivityForResult(params.createIntent(),902);true}catch(_:Exception){chooser?.onReceiveValue(null);chooser=null;false}
                }
            }
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest)=r.isForMainFrame&&r.url.toString()!=entry
                override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse?{
                    val u=r.url
                    if(u.scheme in listOf("blob","data","about"))return null
                    if(r.method!="GET"||u.scheme!="https"||u.host!=host||u.port!=-1||u.encodedQuery!=null)return deny()
                    val prefix="/voice-studio/";val raw=u.path?:return deny()
                    if(!raw.startsWith(prefix))return deny();val key=raw.removePrefix(prefix)
                    if(!key.matches(Regex("[A-Za-z0-9_./-]+"))||key.split('/').any{it==".."||it=="."}||!files.has(key))return deny()
                    return try{
                        val record=files.getJSONObject(key);var bytes=verifiedAsset(key,record.getString("sha256"))
                        if(key=="studio.html")bytes=bytes.toString(Charsets.UTF_8).replace("</head>","<script src=\"native-studio-bridge.js\"></script></head>").toByteArray(Charsets.UTF_8)
                        WebResourceResponse(record.getString("mime"),if(record.getString("mime").startsWith("text/"))"UTF-8" else null,200,"OK",mapOf("Cache-Control" to "no-store","X-Content-Type-Options" to "nosniff"),ByteArrayInputStream(bytes))
                    }catch(_:Exception){deny()}
                }
            }
            if(BuildConfig.DEBUG)WebView.setWebContentsDebuggingEnabled(true)
            loadUrl(entry)
        }
        layout.addView(web,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f))
        window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT)
    }
    private fun verifiedAsset(key:String,expected:String):ByteArray{
        require(expected.matches(Regex("[0-9a-f]{64}")))
        val bytes=assets.open("voice-studio/$key").use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=20000000);out.write(buffer,0,n)};out.toByteArray()}
        require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}==expected){"VOICE_STUDIO_RESOURCE_HASH_MISMATCH"};return bytes
    }
    private fun deny()=WebResourceResponse("application/json","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream("{\"error\":\"VOICE_STUDIO_RESOURCE_NOT_ADMITTED\"}".toByteArray()))
    private inner class StudioBridge{
        @JavascriptInterface fun request(id:String,method:String,route:String,body:String,csrf:String){
            if(closed||!id.matches(Regex("[0-9]{1,12}")))return
            val allowed=setOf("GET /api/local/status","GET /api/local/voices","GET /api/provider/status","POST /api/local/synthesize","GET /api/agent-lee/selection/session","POST /api/agent-lee/selection")
            if("$method $route" !in allowed||body.length>8192||csrf.length>256){reply(id,"{\"status\":403,\"body\":{\"error\":\"VOICE_STUDIO_REQUEST_NOT_ADMITTED\"}}");return}
            try{executor.execute{
                val result=try{UnifiedAgentLeeRuntime(applicationContext).studioRequest(method,route,body,csrf)}catch(e:Exception){JSONObject().put("status",503).put("body",JSONObject().put("error",e.message?.take(200)?:"PAIRED_VOICE_STUDIO_UNAVAILABLE")).toString()}
                reply(id,result)
            }}catch(_:Exception){reply(id,"{\"status\":429,\"body\":{\"error\":\"VOICE_STUDIO_BUSY\"}}")}
        }
    }
    private fun reply(id:String,response:String){if(closed)return;runOnUiThread{if(!closed)web?.evaluateJavascript("window.__leewayStudioReply?.("+JSONObject.quote(id)+","+JSONObject.quote(response)+")",null)}}
    override fun onActivityResult(request:Int,result:Int,data:Intent?){super.onActivityResult(request,result,data);if(request==902){chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));chooser=null}}
    override fun onPause(){web?.onPause();super.onPause()}
    override fun onResume(){super.onResume();web?.onResume()}
    override fun onDestroy(){closed=true;chooser?.onReceiveValue(null);chooser=null;executor.shutdownNow();web?.removeJavascriptInterface("LeeWayVoiceStudio");web?.stopLoading();web?.destroy();web=null;super.onDestroy()}
}
