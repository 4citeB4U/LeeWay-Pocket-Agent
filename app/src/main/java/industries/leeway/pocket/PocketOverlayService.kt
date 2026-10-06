/*
REGION: LEEWAY.UI.OVERLAY
TAG: EXISTING_OWNER_ENABLED_FLOATING_EMBLEM
WHO: Owner-authorized Agent Lee. WHAT: Repair the existing overlay service, not another app.
WHEN: Explicit enable, restart and display changes. WHERE: Android device adapter only.
WHY: An in-page button is not an overlay, and a foreground notification is not attachment proof.
HOW: One bounded native overlay, owner permission, clamped geometry and honest local status.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import androidx.core.content.ContextCompat
import org.json.JSONObject
import kotlin.math.abs

class PocketOverlayService:Service(){
    private var windowManager:WindowManager?=null
    private var tab:View?=null
    private var params:WindowManager.LayoutParams?=null
    private var foregroundReady=false
    override fun onCreate(){
        super.onCreate();createChannel()
        try {
            val notification=buildNotification("Starting floating button")
            if(Build.VERSION.SDK_INT>=34)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTIFICATION_ID,notification)
            foregroundReady=true
        }catch(error:Exception){record(this,"START_FAILED",error.javaClass.simpleName);stopSelf()}
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(!foregroundReady){stopSelf();return START_NOT_STICKY}
        if(!isEnabled(this)){record(this,"DISABLED_BY_OWNER");stopSelf();return START_NOT_STICKY}
        if(!Settings.canDrawOverlays(this)){record(this,"OWNER_OVERLAY_PERMISSION_REQUIRED");stopSelf();return START_NOT_STICKY}
        attach()
        if(tab==null){stopSelf();return START_NOT_STICKY}
        AndroidBrainIngestion.start(this)
        return START_STICKY
    }
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy(){
        AndroidBrainIngestion.stop();detach()
        val state=runCatching{JSONObject(status(this)).optString("state")}.getOrDefault("")
        if(state !in setOf("OWNER_OVERLAY_PERMISSION_REQUIRED","START_FAILED","ATTACH_FAILED","POSITION_FAILED","SERVICE_START_BLOCKED"))record(this,if(isEnabled(this))"SERVICE_STOPPED" else "DISABLED_BY_OWNER")
        super.onDestroy()
    }
    override fun onConfigurationChanged(configuration:Configuration){super.onConfigurationChanged(configuration);tab?.let{applyPosition(it)}}
    private fun requestedSize()=(68*resources.displayMetrics.density).toInt().coerceAtLeast(1)
    private fun frame():OverlayPlacement.Frame {
        val metrics=(windowManager?:getSystemService(WindowManager::class.java)).currentWindowMetrics
        val insets=metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        return OverlayPlacement.Frame(metrics.bounds.width(),metrics.bounds.height(),insets.left,insets.top,insets.right,insets.bottom)
    }
    private fun vertical():Float {
        val p=getSharedPreferences(PREFS,MODE_PRIVATE)
        if(p.contains(KEY_FRACTION))return p.getFloat(KEY_FRACTION,.5f)
        // Migrate the old center-relative position as device data; clamp it to current bounds.
        val f=frame();val old=p.getInt("y",0);return OverlayPlacement.fraction(f,requestedSize(),f.screenHeight/2f-requestedSize()/2f+old)
    }
    private fun applyPosition(view:View){
        val lp=params?:return
        try {
            val place=OverlayPlacement.place(frame(),requestedSize(),vertical())
            lp.width=place.size;lp.height=place.size;lp.x=place.right;lp.y=place.top
            windowManager?.updateViewLayout(view,lp)
            reportAttachment(view)
        }catch(error:Exception){record(this,"POSITION_FAILED",error.javaClass.simpleName);detach();stopSelf()}
    }
    private fun attach(){
        if(tab!=null){applyPosition(tab!!);return}
        if(!OverlayPlacement.shouldRestore(isEnabled(this),Settings.canDrawOverlays(this)))return
        try {
            val wm=getSystemService(WindowManager::class.java);windowManager=wm
            val place=OverlayPlacement.place(frame(),requestedSize(),vertical())
            val badge=ElementalEmblemView(this).apply{contentDescription="Open Agent Lee floating button"}
            val lp=WindowManager.LayoutParams(place.size,place.size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT).apply{gravity=Gravity.RIGHT or Gravity.TOP;x=place.right;y=place.top}
            params=lp
            var startY=0f;var initialY=0;var moved=false;var cancelled=false;var began=0L;var pointer=-1
            badge.setOnTouchListener{_,event->
                when(event.actionMasked){
                    MotionEvent.ACTION_DOWN->{pointer=event.getPointerId(0);startY=event.rawY;initialY=lp.y;moved=false;cancelled=false;began=SystemClock.elapsedRealtime();true}
                    MotionEvent.ACTION_POINTER_DOWN->{cancelled=true;true}
                    MotionEvent.ACTION_MOVE->{
                        if(pointer<0||cancelled)return@setOnTouchListener true
                        val delta=event.rawY-startY
                        if(abs(delta)>ViewConfiguration.get(this).scaledTouchSlop)moved=true
                        if(moved){
                            val current=frame();val fraction=OverlayPlacement.fraction(current,requestedSize(),initialY+delta)
                            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putFloat(KEY_FRACTION,fraction).apply()
                            applyPosition(badge)
                        };true
                    }
                    MotionEvent.ACTION_CANCEL->{cancelled=true;pointer=-1;true}
                    MotionEvent.ACTION_UP->{
                        val click=pointer>=0&&OverlayPlacement.tapAllowed(cancelled,moved,SystemClock.elapsedRealtime()-began)
                        pointer=-1
                        if(click){badge.performClick();startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}
                        true
                    }
                    else->false
                }
            }
            wm.addView(badge,lp);tab=badge
            badge.post{reportAttachment(badge)}
        }catch(error:Exception){record(this,"ATTACH_FAILED",error.javaClass.simpleName);detach()}
    }
    private fun reportAttachment(view:View){
        val lp=params?:return
        val attached=view.isAttachedToWindow
        record(this,if(attached)"NATIVE_OVERLAY_ATTACHED" else "ATTACHMENT_PENDING",attached=attached,
            geometry=JSONObject().put("rightInset",lp.x).put("top",lp.y).put("width",lp.width).put("height",lp.height)
                .put("type","TYPE_APPLICATION_OVERLAY").put("focusable",false).put("blocksOutsideTouches",false))
        if(attached)getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,buildNotification("Floating button attached · tap to open"))
    }
    private fun detach(){tab?.let{runCatching{windowManager?.removeView(it)}};tab=null;params=null;windowManager=null}
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"Agent Lee floating button",NotificationManager.IMPORTANCE_LOW))}
    private fun buildNotification(message:String):Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Agent Lee").setContentText(message).setOngoing(true).setContentIntent(open).build()
    }
    companion object {
        private const val PREFS="leeway-pocket-overlay"
        private const val KEY_ENABLED="enabled"
        private const val KEY_FRACTION="vertical_fraction_v1"
        private const val CHANNEL_ID="agent_lee_overlay"
        private const val NOTIFICATION_ID=7141
        fun isEnabled(context:Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_ENABLED,false)
        private fun record(context:Context,state:String,error:String?=null,attached:Boolean=false,geometry:JSONObject?=null){
            val payload=JSONObject().put("state",state).put("observedAtMs",System.currentTimeMillis()).put("ownerEnabled",isEnabled(context))
                .put("permissionGranted",Settings.canDrawOverlays(context)).put("attached",attached)
                .put("visualAcceptance","NOT_PROVEN_BY_SERVICE_STATE").put("voiceAuthority","UNCHANGED_LEEWAY_VOICE_FABRIC")
            error?.let{payload.put("error",it)};geometry?.let{payload.put("geometry",it)}
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("status_json",payload.toString()).apply()
        }
        fun status(context:Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("status_json","{\"state\":\"NOT_OBSERVED\"}")!!
        fun setEnabled(context:Context,enabled:Boolean){
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,enabled).apply()
            if(enabled)start(context)else{context.stopService(Intent(context,PocketOverlayService::class.java));record(context,"DISABLED_BY_OWNER")}
        }
        fun start(context:Context){
            if(!isEnabled(context)){record(context,"DISABLED_BY_OWNER");return}
            if(!Settings.canDrawOverlays(context)){record(context,"OWNER_OVERLAY_PERMISSION_REQUIRED");return}
            try{ContextCompat.startForegroundService(context,Intent(context,PocketOverlayService::class.java))}
            catch(error:Exception){record(context,"SERVICE_START_BLOCKED",error.javaClass.simpleName)}
        }
    }
}
