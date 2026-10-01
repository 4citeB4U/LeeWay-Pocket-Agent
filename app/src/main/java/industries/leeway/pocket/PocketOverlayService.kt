/*
LEEWAY
REGION: POCKET.UI
TAG: POCKET.LEEWAY.FLOATING_MIC.SERVICE
WHAT: Persistent owner-enabled side tab for immediate Agent Lee voice ingress
WHY: Make the phone usable as a secondary workstation without opening a large interface
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android Pocket Agent
WHEN: 2026-09-29
HOW: Foreground special-use service + TYPE_APPLICATION_OVERLAY draggable side tab
*/
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

class PocketOverlayService: Service(){
    private var actionClient:PhoneActionClient?=null
    private var suppressTab=false
    private var destroying=false
    private var windowManager: WindowManager?=null
    private var tab: View?=null
    private var params: WindowManager.LayoutParams?=null

    override fun onCreate(){
        super.onCreate()
        createChannel()
        val notification=buildNotification()
        if(Build.VERSION.SDK_INT>=34){
            startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }else startForeground(NOTIFICATION_ID,notification)
        attach()
    }

    override fun onStartCommand(intent: Intent?,flags:Int,startId:Int):Int{
        if(!isEnabled(this)){stopSelf();return START_NOT_STICKY}
        if(intent?.action=="LEEWAY_EXPLICIT_PHONE_COMMAND"){
            if(actionClient!=null)return START_STICKY
            val command=PhoneActionCommand(intent.getStringExtra("action").orEmpty(),intent.getStringExtra("label").orEmpty())
            suppressTab=true;detach()
            val client=PhoneActionClient(this);actionClient=client
            client.execute(command){result->
                actionClient=null
                if(!destroying){suppressTab=false;attach()}
                val text=if(!result.optBoolean("ok")) "Phone command could not be verified: "+result.optString("error","UNKNOWN")
                    else if(result.optString("verification")=="FOREGROUND_PACKAGE_MATCH") "Opened "+command.label+"; its app is in the foreground."
                    else "Android accepted the "+command.action+" request. The final screen outcome was not verified."
                MemoryStore(this).saveNotebook("Explicit phone command: "+result.toString())
                PocketVoiceActivity.completeDeviceCommand(text)
            }
        }else if(!suppressTab)attach()
        return START_STICKY
    }

    override fun onDestroy(){
        destroying=true;suppressTab=true
        actionClient?.cancel();actionClient=null
        detach()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?):IBinder?=null

    private fun attach(){
        if(suppressTab||tab!=null)return
        if(!Settings.canDrawOverlays(this)){stopSelf();return}
        val wm=getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager=wm
        val density=resources.displayMetrics.density
        fun dp(v:Int)=(v*density).toInt()

        val badge=TextView(this).apply{
            text="🎙\nLEE"
            textSize=13f
            setTextColor(Color.WHITE)
            gravity=Gravity.CENTER
            setPadding(dp(7),dp(8),dp(7),dp(8))
            background=GradientDrawable().apply{
                setColor(Color.argb(232,5,10,18))
                cornerRadii=floatArrayOf(dp(18).toFloat(),dp(18).toFloat(),0f,0f,0f,0f,dp(18).toFloat(),dp(18).toFloat())
                setStroke(dp(1),Color.argb(210,255,255,255))
            }
            contentDescription="Talk to Agent Lee"
        }

        val lp=WindowManager.LayoutParams(
            dp(58),dp(86),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply{
            gravity=Gravity.END or Gravity.CENTER_VERTICAL
            x=0
            y=getSharedPreferences(PREFS,MODE_PRIVATE).getInt(KEY_Y,0)
        }
        params=lp

        var startRawY=0f
        var initialY=0
        var moved=false
        badge.setOnTouchListener{_,event->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{
                    startRawY=event.rawY
                    initialY=lp.y
                    moved=false
                    true
                }
                MotionEvent.ACTION_MOVE->{
                    val delta=(event.rawY-startRawY).toInt()
                    if(abs(delta)>dp(5))moved=true
                    lp.y=initialY+delta
                    runCatching{wm.updateViewLayout(badge,lp)}
                    true
                }
                MotionEvent.ACTION_UP->{
                    getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt(KEY_Y,lp.y).apply()
                    if(!moved)openVoice()
                    true
                }
                else->false
            }
        }

        wm.addView(badge,lp)
        tab=badge
    }

    private fun detach(){
        val view=tab?:return
        runCatching{windowManager?.removeView(view)}
        tab=null
        params=null
        windowManager=null
    }

    private fun openVoice(){
        startActivity(PocketVoiceActivity.launchIntent(this,newTask=true))
    }

    private fun createChannel(){
        if(Build.VERSION.SDK_INT<26)return
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID,"LeeWay Pocket Side Tab",NotificationManager.IMPORTANCE_LOW).apply{
                description="Keeps the owner-enabled Agent Lee side tab available"
            }
        )
    }

    private fun buildNotification():Notification{
        val intent=Intent(this,MainActivity::class.java)
        val pending=PendingIntent.getActivity(
            this,0,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("LeeWay Pocket Agent")
            .setContentText("Agent Lee side tab is ready")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .build()
    }

    companion object{
        private const val PREFS="leeway-pocket-overlay"
        private const val KEY_ENABLED="enabled"
        private const val KEY_Y="y"
        private const val CHANNEL_ID="leeway_pocket_overlay"
        private const val NOTIFICATION_ID=7141

        fun isEnabled(context:Context)=
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY_ENABLED,false)

        fun setEnabled(context:Context,enabled:Boolean){
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,enabled).apply()
            if(enabled)start(context)
            else context.stopService(Intent(context,PocketOverlayService::class.java))
        }

        fun start(context:Context){
            if(!isEnabled(context))return
            if(!Settings.canDrawOverlays(context))return
            ContextCompat.startForegroundService(context,Intent(context,PocketOverlayService::class.java))
        }
    }
}
