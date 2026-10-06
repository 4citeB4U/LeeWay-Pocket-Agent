package industries.leeway.pocket

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.abs

class PocketOverlayService:Service(){
    private var windowManager:WindowManager?=null
    private var tab:View?=null
    override fun onCreate(){super.onCreate();createChannel();val n=buildNotification();if(Build.VERSION.SDK_INT>=34)startForeground(7141,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)else startForeground(7141,n);if(isEnabled(this)){attach();AndroidBrainIngestion.start(this)}}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{if(!isEnabled(this)){stopSelf();return START_NOT_STICKY};attach();return START_STICKY}
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy(){AndroidBrainIngestion.stop();detach();super.onDestroy()}
    private fun attach(){
        if(tab!=null||!Settings.canDrawOverlays(this))return
        val wm=getSystemService(WINDOW_SERVICE) as WindowManager;windowManager=wm;val d=resources.displayMetrics.density;fun dp(v:Int)=(v*d).toInt()
        val badge=ElementalEmblemView(this).apply{contentDescription="Open Agent Lee"}

        val lp=WindowManager.LayoutParams(dp(68),dp(68),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.END or Gravity.CENTER_VERTICAL;y=getSharedPreferences(PREFS,MODE_PRIVATE).getInt(KEY_Y,0)}
        var sy=0f;var iy=0;var moved=false
        badge.setOnTouchListener{_,e->when(e.actionMasked){MotionEvent.ACTION_DOWN->{sy=e.rawY;iy=lp.y;moved=false;true};MotionEvent.ACTION_MOVE->{val delta=(e.rawY-sy).toInt();if(abs(delta)>dp(5))moved=true;lp.y=iy+delta;runCatching{wm.updateViewLayout(badge,lp)};true};MotionEvent.ACTION_UP->{getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt(KEY_Y,lp.y).apply();if(!moved)startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));true};else->false}}
        wm.addView(badge,lp);tab=badge
    }
    private fun detach(){tab?.let{runCatching{windowManager?.removeView(it)}};tab=null;windowManager=null}
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"Agent Lee Side Tab",NotificationManager.IMPORTANCE_LOW))}
    private fun buildNotification():Notification{val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);return Notification.Builder(this,CHANNEL_ID).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Agent Lee").setContentText("Side tab ready").setOngoing(true).setContentIntent(pi).build()}
    companion object{
        private const val PREFS="leeway-pocket-overlay";private const val KEY_ENABLED="enabled";private const val KEY_Y="y";private const val CHANNEL_ID="agent_lee_overlay"
        fun isEnabled(c:Context)=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_ENABLED,false)
        fun setEnabled(c:Context,e:Boolean){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,e).apply();if(e)start(c)else c.stopService(Intent(c,PocketOverlayService::class.java))}
        fun start(c:Context){if(isEnabled(c)&&Settings.canDrawOverlays(c))ContextCompat.startForegroundService(c,Intent(c,PocketOverlayService::class.java))}
    }
}
