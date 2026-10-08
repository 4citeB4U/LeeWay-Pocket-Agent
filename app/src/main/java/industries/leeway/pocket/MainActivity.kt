/*
REGION: LEEWAY.POCKET.OWNER_NAVIGATION
TAG: EXISTING_AGENT_VT_CONTINUUM_SURFACE
5WH: WHAT=Open Continuum inside the existing Agent VT activity; WHY=Retained data needs an actual view;
WHO=LeeWay Industries / installation owner; WHERE=Existing Pocket menu bridge; WHEN=Owner selects Continuum;
HOW=An internal activity intent selects the hash-admitted, read-only Continuum surface.
AUTHORIZED ROLES: OWNER_UI. This navigation does not grant ingestion, Formula or generic device execution.
LICENSE: MIT
*/
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
        if(Settings.canDrawOverlays(this)&&PocketOverlayService.isEnabled(this)){
            // The persistent sphere is the sole visual owner; dispatch requested tools directly.
            PocketOverlayService.showSphere(this)
            when(intent?.getStringExtra("leeway_action")){
                "TALK_TO_AGENT_LEE"->startActivity(PocketVoiceActivity.launchIntent(this))
                "OPEN_DIGITAL_BRAIN"->openBrain(false)
                "OPEN_WORKSTATION"->openWorkstation()
                "OPEN_CONTINUUM"->openContinuum()
                "OPEN_DIAGNOSTICS"->openBrain(true)
                "OPEN_SKILLS"->openSkills()
            }
            finish()
            return
        }
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
            "OPEN_WORKSTATION"->openWorkstation()
            "OPEN_CONTINUUM"->openContinuum()
            "OPEN_DIAGNOSTICS"->openBrain(true)
            "OPEN_SKILLS"->openSkills()
        }
    }

    private fun openWorkstation(){startActivity(Intent(this,AgentTabletActivity::class.java))}
    private fun openContinuum(){startActivity(Intent(this,AgentTabletActivity::class.java).putExtra(AgentTabletActivity.EXTRA_SURFACE,"continuum"))}
    private fun openBrain(hardware:Boolean){startActivity(Intent(this,DigitalBrainActivity::class.java).putExtra("hardware",hardware))}
    private fun openSkills(){startActivity(Intent(this,SkillsActivity::class.java))}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);when(intent.getStringExtra("leeway_action")){"OPEN_DIGITAL_BRAIN"->openBrain(false);"OPEN_WORKSTATION"->openWorkstation();"OPEN_CONTINUUM"->openContinuum();"OPEN_DIAGNOSTICS"->openBrain(true);"OPEN_SKILLS"->openSkills()}}
    override fun onResume(){
        super.onResume()
        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        val permitted=Settings.canDrawOverlays(this)
        if(prefs.getBoolean("permission_pending",false)&&permitted){
            prefs.edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true);PocketOverlayService.showSphere(this);finish()
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
        @JavascriptInterface fun testVoice(){runOnUiThread{PocketVoiceHost.playVerifiedOutputSample(this@MainActivity)}}
        @JavascriptInterface fun stopVoice(){runOnUiThread{PocketVoiceHost.stopVerifiedOutputSample()}}
        @JavascriptInterface fun deviceName():String=android.os.Build.MODEL
        @JavascriptInterface fun talk(){ runOnUiThread{ if(Settings.canDrawOverlays(this@MainActivity)&&PocketOverlayService.isEnabled(this@MainActivity))PocketOverlayService.showRoundBox(this@MainActivity) else startActivity(PocketVoiceActivity.launchIntent(this@MainActivity)) } }
        @JavascriptInterface fun enableOverlay(){runOnUiThread{requestOverlayPermission()}}
        @JavascriptInterface fun disableOverlay(){runOnUiThread{
            getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE).edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this@MainActivity,false)
        }}
        @JavascriptInterface fun overlayStatus():String=PocketOverlayService.status(this@MainActivity)
        @JavascriptInterface fun openVoiceStudio(){runOnUiThread{startActivity(Intent(this@MainActivity,VoiceStudioActivity::class.java))}}
        @JavascriptInterface fun openModels(){runOnUiThread{startActivity(Intent(this@MainActivity,ModelsActivity::class.java))}}
        @JavascriptInterface fun openSkills(){runOnUiThread{this@MainActivity.openSkills()}}
        @JavascriptInterface fun openWorkstation(){runOnUiThread{this@MainActivity.openWorkstation()}}
        @JavascriptInterface fun openContinuum(){runOnUiThread{this@MainActivity.openContinuum()}}
        @JavascriptInterface fun openDigitalBrain(){runOnUiThread{openBrain(false)}}
        @JavascriptInterface fun openDiagnostics(){runOnUiThread{openBrain(true)}}
        @JavascriptInterface fun digitalBrain():String = AndroidDigitalBrainAdapter.snapshot(this@MainActivity)
        @JavascriptInterface fun evidenceSummary():String=OwnerRecordsMenu.evidence(this@MainActivity)
        @JavascriptInterface fun openPermissionSettings(){runOnUiThread{this@MainActivity.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+this@MainActivity.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun authoritySummary():String=OwnerRecordsMenu.authority(this@MainActivity)
        @JavascriptInterface fun bodyId():String = AndroidDigitalBrainAdapter.identity(this@MainActivity).deviceId
    }
}
