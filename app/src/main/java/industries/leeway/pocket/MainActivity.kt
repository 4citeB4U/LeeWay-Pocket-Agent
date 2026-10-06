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
        when(intent?.getStringExtra("leeway_action")){
            "TALK_TO_AGENT_LEE"->startActivity(PocketVoiceActivity.launchIntent(this))
            "OPEN_DIGITAL_BRAIN"->openBrain(false)
            "OPEN_DIAGNOSTICS"->openBrain(true)
        }
    }

    private fun openBrain(hardware:Boolean){startActivity(Intent(this,DigitalBrainActivity::class.java).putExtra("hardware",hardware))}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);when(intent.getStringExtra("leeway_action")){"OPEN_DIGITAL_BRAIN"->openBrain(false);"OPEN_DIAGNOSTICS"->openBrain(true)}}
    override fun onResume(){
        super.onResume()
        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        val permitted=Settings.canDrawOverlays(this)
        if(prefs.getBoolean("permission_pending",false)&&permitted){
            prefs.edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
        }else if(PocketOverlayService.isEnabled(this)){
            PocketOverlayService.start(this)
        }else if(!prefs.contains("enabled")&&!prefs.getBoolean("permission_explanation_shown",false)){
            prefs.edit().putBoolean("permission_explanation_shown",true).apply()
            android.app.AlertDialog.Builder(this).setTitle("Floating Agent Lee button")
                .setMessage("Keep the Agent Lee logo at the right edge over your home screen and ordinary apps. Android requires your approval to display it over other apps. This permission does not select or change Agent Lee's voice.")
                .setPositiveButton("Enable floating button"){_,_->requestOverlayPermission()}
                .setNegativeButton("Not now",null).show()
        }
    }

    private fun bootstrapDeviceBrain(){
        AndroidDigitalBrainAdapter.bootstrap(this)
        industries.leeway.pocket.devices.DeviceDiagnostics.refresh(this,true)
        val app=applicationContext
        kotlin.concurrent.thread(name="leeway-brain-initial-census",isDaemon=true){
            runCatching{AndroidBrainIngestion.reconcilePrivateFiles(app)}
                .onFailure{android.util.Log.w("LeeWayBrain","Initial metadata census blocked: "+it.javaClass.simpleName)}
        }
    }

    private fun requestOverlayPermission(){
        getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("enabled",true).putBoolean("permission_pending",true).apply()
        if(Settings.canDrawOverlays(this)){
            getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
        }else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)))
    }
    inner class LeeWayBridge {
        @JavascriptInterface fun talk(){ runOnUiThread{ startActivity(PocketVoiceActivity.launchIntent(this@MainActivity)) } }
        @JavascriptInterface fun enableOverlay(){runOnUiThread{requestOverlayPermission()}}
        @JavascriptInterface fun disableOverlay(){runOnUiThread{
            getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this@MainActivity,false)
        }}
        @JavascriptInterface fun overlayStatus():String=PocketOverlayService.status(this@MainActivity)
        @JavascriptInterface fun openDigitalBrain(){runOnUiThread{openBrain(false)}}
        @JavascriptInterface fun openDiagnostics(){runOnUiThread{openBrain(true)}}
        @JavascriptInterface fun digitalBrain():String = AndroidDigitalBrainAdapter.snapshot(this@MainActivity)
        @JavascriptInterface fun bodyId():String = AndroidDigitalBrainAdapter.identity(this@MainActivity).deviceId
    }
}
