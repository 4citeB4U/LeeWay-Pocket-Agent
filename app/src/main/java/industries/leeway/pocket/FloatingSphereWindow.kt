/*
REGION: LEEWAY.UI.NATIVE_OVERLAY
TAG: EXISTING_SERVICE_FLOATING_SPHERE
WHO: Owner-enabled Agent Lee. WHAT: Host the approved sphere inside the existing overlay service.
WHEN: Right-edge emblem or main launcher. WHERE: Android window adapter, one package and service.
WHY: A transparent Activity still displaces applications; bounded overlay windows do not.
HOW: Same bundled UI, native geometry, owner consent, no independent voice or cognition.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.webkit.*
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

internal class FloatingSphereWindow(private val service:Service) {
    private val main=Handler(Looper.getMainLooper())
    private val manager=service.getSystemService(WindowManager::class.java)
    private val prefs=service.getSharedPreferences("leeway-floating-sphere",Service.MODE_PRIVATE)
    private val density get()=service.resources.displayMetrics.density
    private var web:WebView?=null
    private var params:WindowManager.LayoutParams?=null
    private var panel=false
    private var expanded=false
    private var normal=IntArray(4)
    private fun bounds()=manager.currentWindowMetrics.bounds
    private fun maximum()=sqrt(bounds().width().toDouble()*bounds().height()*.15).toInt()
    private fun dp(value:Int)=(value*density).toInt()
    fun show(){
        if(web!=null){report();return}
        check(Settings.canDrawOverlays(service)){"OWNER_OVERLAY_PERMISSION_REQUIRED"}
        val b=bounds();val side=prefs.getInt("width",dp(230)).coerceIn(dp(110).coerceAtMost(maximum()),maximum())
        val lp=WindowManager.LayoutParams(side,side,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.LEFT;x=prefs.getInt("x",b.width()-side-dp(80)).coerceIn(0,(b.width()-side).coerceAtLeast(0));y=prefs.getInt("y",b.height()/4).coerceIn(dp(30),(b.height()-side-dp(30)).coerceAtLeast(dp(30)));title="Agent Lee Floating Sphere"}
        params=lp;normal=intArrayOf(lp.x,lp.y,lp.width,lp.height)
        val view=WebView(ContextThemeWrapper(service,R.style.AppTheme)).apply{
            setBackgroundColor(Color.TRANSPARENT);settings.javaScriptEnabled=true;settings.domStorageEnabled=true
            settings.allowContentAccess=false;settings.mediaPlaybackRequiresUserGesture=false
            addJavascriptInterface(Bridge(),"LeeWayAndroid")
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest)=request.url.toString()!="file:///android_asset/agent_lee_sphere_transparent.html"
                override fun shouldInterceptRequest(v:WebView,request:WebResourceRequest):WebResourceResponse?{
                    val uri=request.url
                    if(uri.scheme=="file"&&uri.path?.startsWith("/android_asset/")==true)return null
                    return WebResourceResponse("text/plain","UTF-8",java.io.ByteArrayInputStream("LOCAL_OVERLAY_ASSET_REQUIRED".toByteArray()))
                }
                override fun onPageFinished(v:WebView,url:String){report()}
            }
            loadUrl("file:///android_asset/agent_lee_sphere_transparent.html")
        }
        if(BuildConfig.DEBUG)WebView.setWebContentsDebuggingEnabled(true)
        
        var dragPointer=-1;var startRawX=0f;var startRawY=0f;var startX=lp.x;var startY=lp.y
        var pinchStart=0f;var pinchSize=lp.width;var gestureMoved=false
        view.setOnTouchListener{_,event->
            // The open drawer/surface owns scroll gestures; never move the overlay beneath it.
            if(panel||expanded){dragPointer=-1;pinchStart=0f;gestureMoved=false;return@setOnTouchListener false}
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{dragPointer=event.getPointerId(0);startRawX=event.rawX;startRawY=event.rawY;startX=lp.x;startY=lp.y;gestureMoved=false;false}
                MotionEvent.ACTION_POINTER_DOWN->{if(event.pointerCount==2){pinchStart=kotlin.math.hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1));pinchSize=lp.width;gestureMoved=true;true}else false}
                MotionEvent.ACTION_MOVE->{
                    if(event.pointerCount>=2&&pinchStart>0f){val d=kotlin.math.hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1));val side=(pinchSize*(d/pinchStart)).toInt().coerceIn(dp(110).coerceAtMost(maximum()),maximum());lp.width=side;lp.height=side;expanded=false;gestureMoved=true;relayout();true}
                    else if(dragPointer>=0&&event.pointerCount==1){val dx=event.rawX-startRawX;val dy=event.rawY-startRawY;if(kotlin.math.hypot(dx,dy)>ViewConfiguration.get(service).scaledTouchSlop){gestureMoved=true;lp.x=startX+dx.toInt();lp.y=startY+dy.toInt();relayout();true}else false}else false
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(gestureMoved){prefs.edit().putInt("x",lp.x).putInt("y",lp.y).putInt("width",lp.width).apply();dragPointer=-1;pinchStart=0f;true}else{dragPointer=-1;pinchStart=0f;false}}
                else->false
            }
        }
        web=view;manager.addView(view,lp);view.post{report()}
    }
    fun relayout(){params?.let{lp->val b=bounds();if(!panel&&!expanded){val size=lp.width.coerceAtMost(maximum());lp.width=size;lp.height=size};lp.x=lp.x.coerceIn(0,(b.width()-lp.width).coerceAtLeast(0));lp.y=lp.y.coerceIn(0,(b.height()-lp.height).coerceAtLeast(0));web?.let{manager.updateViewLayout(it,lp)};report()}}
    fun isVisible()=web?.isAttachedToWindow==true
    fun close(){web?.let{it.removeJavascriptInterface("LeeWayAndroid");manager.removeView(it);it.destroy()};web=null;params=null}
    private fun onMain(block:()->String):String{
        if(Looper.myLooper()==Looper.getMainLooper())return block()
        var answer="{\"state\":\"NATIVE_UI_TIMED_OUT\"}";val latch=CountDownLatch(1)
        main.post{try{answer=block()}catch(e:Exception){answer=JSONObject().put("state","BLOCKED").put("error",e.message).toString()}finally{latch.countDown()}}
        latch.await(3,TimeUnit.SECONDS);return answer
    }
    private fun report():String{
        val p=params?:return "{\"state\":\"NOT_ATTACHED\"}"
        val b=bounds();val result=JSONObject().put("state",if(web?.isAttachedToWindow==true)"NATIVE_SPHERE_ATTACHED" else "ATTACHING")
            .put("x",p.x).put("y",p.y).put("width",p.width/density).put("height",p.height/density)
            .put("physicalWidth",p.width).put("physicalHeight",p.height).put("screenAreaRatio",p.width.toDouble()*p.height/(b.width().toDouble()*b.height()))
            .put("panel",panel).put("expanded",expanded).put("type","TYPE_APPLICATION_OVERLAY").put("blocksOutsideTouches",false).put("observedAtMs",System.currentTimeMillis())
        prefs.edit().putString("status_json",result.toString()).apply();return result.toString()
    }
    private inner class Bridge {
        @JavascriptInterface fun geometry(action:String,value:Double,second:Double):String=onMain{
            val p=params?:error("OVERLAY_NOT_ATTACHED");require(value.isFinite()&&second.isFinite())
            when(action){
                "status"->{}
                "move"->{require(kotlin.math.abs(value)<=300&&kotlin.math.abs(second)<=300);p.x+=(value*density).toInt();p.y+=(second*density).toInt()}
                "resize"->{if(!panel){val side=(value*density).toInt().coerceIn(dp(110).coerceAtMost(maximum()),maximum());p.width=side;p.height=side;expanded=false}}
                "expand"->{if(!expanded){normal=intArrayOf(p.x,p.y,p.width,p.height);p.x=0;p.y=0;p.width=bounds().width();p.height=bounds().height();expanded=true}else{p.x=normal[0];p.y=normal[1];p.width=normal[2];p.height=normal[3];expanded=false}}
                "panel"->{val open=value!=0.0;if(open&&!panel){if(!expanded)normal=intArrayOf(p.x,p.y,p.width,p.height);p.width=dp(360).coerceAtMost(bounds().width());p.height=dp(610).coerceAtMost(bounds().height());panel=true;p.flags=p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()}
                    else if(!open&&panel){panel=false;p.flags=p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;if(!expanded){p.x=normal[0];p.y=normal[1];p.width=normal[2];p.height=normal[3]}}}
                else->error("GEOMETRY_ACTION_NOT_ALLOWED")
            }
            relayout();if(!panel&&!expanded)prefs.edit().putInt("x",p.x).putInt("y",p.y).putInt("width",p.width).apply();report()
        }
        @JavascriptInterface fun talk(){main.post{service.startActivity(PocketVoiceActivity.launchIntent(service,true))}}
        @JavascriptInterface fun testVoice(){main.post{PocketVoiceHost.playVerifiedOutputSample(service)}}
        @JavascriptInterface fun stopVoice(){main.post{PocketVoiceHost.stopVerifiedOutputSample()}}
        @JavascriptInterface fun openVision(){main.post{service.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://leeway-agent-lee-vision.vercel.app/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openVoiceStudio(){main.post{service.startActivity(Intent(service,VoiceStudioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openModels(){main.post{service.startActivity(Intent(service,ModelsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openSkills(){main.post{service.startActivity(Intent(service,SkillsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openWorkstation(){main.post{service.startActivity(Intent(service,AgentTabletActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openContinuum(){main.post{service.startActivity(Intent(service,AgentTabletActivity::class.java).putExtra(AgentTabletActivity.EXTRA_SURFACE,"continuum").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun openDigitalBrain(){open(false)}
        @JavascriptInterface fun openDiagnostics(){open(true)}
        @JavascriptInterface fun deviceName():String=android.os.Build.MODEL
        @JavascriptInterface fun evidenceSummary():String=OwnerRecordsMenu.evidence(service)
        @JavascriptInterface fun openPermissionSettings(){main.post{service.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+service.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))}}
        @JavascriptInterface fun authoritySummary():String=OwnerRecordsMenu.authority(service)
        @JavascriptInterface fun bodyId():String=AndroidDigitalBrainAdapter.identity(service).deviceId
        @JavascriptInterface fun enableOverlay(){main.post{service.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+service.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
        private fun open(hardware:Boolean){main.post{service.startActivity(Intent(service,DigitalBrainActivity::class.java).putExtra("hardware",hardware).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
    }
}
