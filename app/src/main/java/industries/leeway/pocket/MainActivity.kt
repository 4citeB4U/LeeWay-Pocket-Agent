package industries.leeway.pocket

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.TRANSPARENT
        window.navigationBarColor=Color.TRANSPARENT
        bootstrapDeviceBrain()
        web=WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=true
            webViewClient=WebViewClient()
            webChromeClient=WebChromeClient()
            addJavascriptInterface(LeeWayBridge(),"LeeWayAndroid")
            loadUrl("file:///android_asset/agent_lee_sphere_transparent.html")
        }
        setContentView(web)
        if(intent?.getStringExtra("leeway_action")=="TALK_TO_AGENT_LEE") startActivity(PocketVoiceActivity.launchIntent(this))
    }

    override fun onResume(){
        super.onResume()
        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        if(prefs.getBoolean("permission_pending",false) && Settings.canDrawOverlays(this)){
            prefs.edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
        }
    }

    private fun bootstrapDeviceBrain(){
        AndroidDigitalBrainAdapter.bootstrap(this)
    }

    inner class LeeWayBridge {
        @JavascriptInterface fun talk(){ runOnUiThread{ startActivity(PocketVoiceActivity.launchIntent(this@MainActivity)) } }
        @JavascriptInterface fun enableOverlay(){
            runOnUiThread{
                if(Settings.canDrawOverlays(this@MainActivity)) PocketOverlayService.setEnabled(this@MainActivity,true)
                else {
                    getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",true).apply()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        }
        @JavascriptInterface fun digitalBrain():String = AndroidDigitalBrainAdapter.snapshot(this@MainActivity)
        @JavascriptInterface fun bodyId():String = AndroidDigitalBrainAdapter.identity(this@MainActivity).deviceId
    }
}
