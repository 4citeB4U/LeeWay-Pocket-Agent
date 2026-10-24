/*
REGION: LEEWAY.SKILLS.ANDROID_SURFACE
TAG: CANONICAL_GRAPEVINE_PAGES_BUNDLE
5WH:
WHAT = Host the current LeeWay Agent Skills Grapevine inside the existing Agent Lee package.
WHY = Skills must open the same qualified GitHub Pages interface instead of a text placeholder.
WHO = LeeWay Industries / installation owner.
WHERE = Existing Pocket APK; no second Agent Lee, runtime, registry, or execution authority.
WHEN = Owner selects Skills.
HOW = Hash-admitted local Pages bundle pinned to commit 95ea833c...; bounded HTTPS asset origin.
AUTHORIZED ROLES: OWNER_SKILLS_UI. LICENSE: MIT; bundled upstream assets retain their licenses.
*/
package industries.leeway.pocket

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsets
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

class SkillsActivity:Activity(){
    private var web:WebView?=null
    private lateinit var files:JSONObject
    private val host="appassets.androidplatform.net"
    private val entry="https://appassets.androidplatform.net/skills-ui/index.html"
    private val expectedCommit="a01154b67fd01c0f5e83ee15fe3d7dc4464cd687"

    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val layout=FrameLayout(this).apply{
            setBackgroundColor(Color.rgb(3,7,8))
            setOnApplyWindowInsetsListener { view, insets ->
                val safe=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(safe.left,safe.top,safe.right,safe.bottom)
                insets.inset(safe.left,safe.top,safe.right,safe.bottom)
            }
        }
        setContentView(layout)
        OwnerSurfaceWindow.attach(this, "Agent Skills")
        layout.requestApplyInsets()

        val lock=runCatching{
            JSONObject(assets.open("skills-ui/SOURCE.json").bufferedReader(Charsets.UTF_8).use{it.readText()})
        }.getOrNull()
        if(lock==null ||
            lock.optString("schemaVersion")!="leeway.skills-grapevine-bundle.v1" ||
            lock.optString("repository")!="4citeB4U/LeeWay-Agent-Skills" ||
            lock.optString("commit")!=expectedCommit ||
            lock.optString("registrySchema")!="leeway.grapevine.registry.v2" ||
            lock.optInt("canonicalSkillMd")!=499){
            layout.addView(TextView(this).apply{
                text="The LeeWay Skills Grapevine package could not be verified."
                setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(3,7,8));setPadding(24,48,24,24)
            })
            return
        }
        files=lock.getJSONObject("files")
        if(files.length()!=4){finish();return}

        web=WebView(this).apply{
            setBackgroundColor(Color.rgb(3,7,8))
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=true
            settings.setSupportZoom(true)
            settings.builtInZoomControls=true
            settings.displayZoomControls=false
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.javaScriptCanOpenWindowsAutomatically=false
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture=true
            webChromeClient=object:WebChromeClient(){
                override fun onPermissionRequest(request:PermissionRequest){request.deny()}
            }
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean{
                    val u=r.url
                    if(!r.isForMainFrame)return false
                    if(u.toString()==entry)return false
                    if(u.scheme=="https" && u.host=="github.com"){
                        runCatching{startActivity(Intent(Intent.ACTION_VIEW,u))}
                    }
                    return true
                }
                override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse?{
                    val u=r.url
                    if(r.method!="GET")return deny()
                    if(u.scheme=="https" && (u.host=="fonts.googleapis.com" || u.host=="fonts.gstatic.com"))return null
                    if(u.scheme!="https" || u.host!=host || u.port!=-1 || u.encodedQuery!=null || u.fragment!=null)return deny()
                    val prefix="/skills-ui/"
                    val raw=u.path?:return deny()
                    if(!raw.startsWith(prefix))return deny()
                    val key=raw.removePrefix(prefix).ifBlank{"index.html"}
                    if(!files.has(key))return deny()
                    return try{
                        val record=files.getJSONObject(key)
                        val verified=verifiedAsset(key,record.getString("sha256"))
                        val bytes=if(key=="index.html") verified.toString(Charsets.UTF_8)
                            .replace("maximum-scale=1.0, user-scalable=no", "maximum-scale=5.0, user-scalable=yes")
                            .toByteArray(Charsets.UTF_8) else verified
                        WebResourceResponse(
                            record.getString("mime"),
                            if(record.getString("mime").startsWith("text/") || record.getString("mime").contains("json") || record.getString("mime").contains("javascript"))"UTF-8" else null,
                            200,"OK",
                            mapOf("Cache-Control" to "no-store","X-Content-Type-Options" to "nosniff"),
                            ByteArrayInputStream(bytes)
                        )
                    }catch(_:Exception){deny()}
                }
            }
            if(BuildConfig.DEBUG)WebView.setWebContentsDebuggingEnabled(true)
            loadUrl(entry)
        }
        layout.addView(web,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun verifiedAsset(key:String,expected:String):ByteArray{
        require(key.matches(Regex("[A-Za-z0-9_./-]+")) && key.split('/').none{it=="."||it==".."})
        require(expected.matches(Regex("[0-9a-f]{64}")))
        val bytes=assets.open("skills-ui/$key").use{input->
            val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
            while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=2_000_000);out.write(buffer,0,n)}
            out.toByteArray()
        }
        val actual=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        require(actual==expected){"SKILLS_UI_RESOURCE_HASH_MISMATCH"}
        return bytes
    }

    private fun deny()=WebResourceResponse(
        "application/json","UTF-8",403,"Blocked",emptyMap(),
        ByteArrayInputStream("{\"error\":\"SKILLS_UI_RESOURCE_NOT_ADMITTED\"}".toByteArray())
    )

    override fun onPause(){web?.onPause();super.onPause()}
    override fun onResume(){super.onResume();web?.onResume()}
    override fun onDestroy(){web?.stopLoading();web?.destroy();web=null;super.onDestroy()}
}
