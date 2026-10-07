/*
REGION: LEEWAY.MODELS.ANDROID_SURFACE
TAG: READ_ONLY_PAIRED_PC_MODEL_INVENTORY
WHO: Owner opening Models in Agent Lee. WHAT: Show actual installed/loaded PC models.
HOW: Hash-admitted Runtime Fabric HTML plus one fixed read through the existing
TLS-pinned, per-body paired gateway. This activity is not a model execution owner.
No model download, inference, credentials, generic URL bridge, or second state store.
LICENSE: Existing repository terms.
*/
package industries.leeway.pocket

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ModelsActivity:Activity(){
    private var web:WebView?=null
    @Volatile private var closed=false
    private lateinit var files:JSONObject
    private val executor=ThreadPoolExecutor(1,1,15,TimeUnit.SECONDS,ArrayBlockingQueue<Runnable>(1))
    private val host="appassets.androidplatform.net"
    private val entry="https://appassets.androidplatform.net/models-ui/index.html"
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val layout=FrameLayout(this).apply{
            setBackgroundColor(Color.rgb(9,15,23))
            setOnApplyWindowInsetsListener { view, insets ->
                val safe=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left,safe.top,safe.right,safe.bottom)
                insets.inset(safe.left,safe.top,safe.right,safe.bottom)
            }
        }
        setContentView(layout)
        layout.requestApplyInsets()
        val lock=runCatching{JSONObject(assets.open("models-ui/SOURCE.json").bufferedReader(Charsets.UTF_8).use{it.readText()})}.getOrNull()
        if(lock==null||lock.optString("repository")!="4citeB4U/Leeway-Runtime-Fabric"||lock.optString("schemaVersion")!="leeway.models-ui-bundle.v1"){
            layout.addView(TextView(this).apply{text="The Models interface package could not be verified.";setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(9,15,23));setPadding(24,48,24,24)});return
        }
        files=lock.getJSONObject("files")
        if(files.length()!=2||!files.has("index.html")||!files.has("native-models-bridge.js")){finish();return}
        web=WebView(this).apply{
            setBackgroundColor(Color.rgb(9,15,23));settings.javaScriptEnabled=true;settings.domStorageEnabled=false
            settings.allowFileAccess=false;settings.allowContentAccess=false;settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.javaScriptCanOpenWindowsAutomatically=false;settings.setSupportMultipleWindows(false)
            addJavascriptInterface(ModelsBridge(),"LeeWayModels")
            webChromeClient=object:WebChromeClient(){override fun onPermissionRequest(request:PermissionRequest){request.deny()}}
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean{
                    if(r.isForMainFrame&&r.url.toString()=="https://$host/ui"){runOnUiThread{finish()};return true}
                    return r.isForMainFrame&&r.url.toString()!=entry
                }
                override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse?{
                    val u=r.url
                    if(r.method!="GET"||u.scheme!="https"||u.host!=host||u.port!=-1||u.encodedQuery!=null||u.fragment!=null)return deny()
                    val name=when(u.path){"/models-ui/index.html"->"index.html";"/models-ui/native-models-bridge.js"->"native-models-bridge.js";else->return deny()}
                    return try{
                        val record=files.getJSONObject(name);var bytes=verifiedAsset(name,record.getString("sha256"))
                        if(name=="index.html")bytes=bytes.toString(Charsets.UTF_8).replace("</head>","<script src=\"native-models-bridge.js\"></script></head>").toByteArray(Charsets.UTF_8)
                        WebResourceResponse(record.getString("mime"),"UTF-8",200,"OK",mapOf("Cache-Control" to "no-store","X-Content-Type-Options" to "nosniff"),ByteArrayInputStream(bytes))
                    }catch(_:Exception){deny()}
                }
            }
            if(BuildConfig.DEBUG)WebView.setWebContentsDebuggingEnabled(true)
            loadUrl(entry)
        }
        layout.addView(web,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT))
    }
    private fun verifiedAsset(name:String,expected:String):ByteArray{
        require(expected.matches(Regex("[0-9a-f]{64}")))
        val bytes=assets.open("models-ui/$name").use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=128000);out.write(buffer,0,n)};out.toByteArray()}
        require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}==expected){"MODELS_UI_RESOURCE_HASH_MISMATCH"};return bytes
    }
    private fun deny()=WebResourceResponse("application/json","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream("{\"error\":\"MODELS_UI_RESOURCE_NOT_ADMITTED\"}".toByteArray()))
    private inner class ModelsBridge{
        @JavascriptInterface fun close(){runOnUiThread{finish()}}
        @JavascriptInterface fun readInventory(id:String){
            if(closed||!id.matches(Regex("[0-9]{1,12}")))return
            try{executor.execute{
                val result=try{
                    val inventory=UnifiedAgentLeeRuntime(applicationContext).modelInventory()
                    require(inventory.length<=2400000){"MODEL_INVENTORY_RESPONSE_LIMIT"}
                    JSONObject().put("status",200).put("body",JSONObject(inventory)).toString()
                }catch(_:Exception){JSONObject().put("status",503).put("body",JSONObject().put("error","PAIRED_PC_MODEL_INVENTORY_UNAVAILABLE")).toString()}
                reply(id,result)
            }}catch(_:Exception){reply(id,"{\"status\":429,\"body\":{\"error\":\"MODEL_INVENTORY_BUSY\"}}")}
        }
    }
    private fun reply(id:String,response:String){if(closed)return;runOnUiThread{if(!closed)web?.evaluateJavascript("window.__leewayModelsReply?.("+JSONObject.quote(id)+","+JSONObject.quote(response)+")",null)}}
    override fun onPause(){web?.onPause();super.onPause()}
    override fun onResume(){super.onResume();web?.onResume()}
    override fun onDestroy(){closed=true;executor.shutdownNow();web?.removeJavascriptInterface("LeeWayModels");web?.stopLoading();web?.destroy();web=null;super.onDestroy()}
}
