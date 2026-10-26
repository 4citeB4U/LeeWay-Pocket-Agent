/*
REGION: LEEWAY.UI.VISION.CONTROLS
TAG: LEEWAY.LFW.V1.ANDROID_VISION_EDGE_TAG
5WH:
WHAT = Permanent right-edge Vision tag plus its separately expanding native camera-controls popup.
WHY = Camera controls must not move the tag or box the approved 3D eye.
WHO = LeeWay Industries / Creator-authorized Agent Lee Golden Package.
WHERE = Existing PocketOverlayService, Android WindowManager.
WHEN = Explicit foreground Vision session only.
HOW = Two bounded TYPE_APPLICATION_OVERLAY views sharing one existing service, fixed right tag, vertical-only motion.
AUTHORIZED ROLES: OWNER_UI / CAMERA_OWNER; no automatic camera activation.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

internal object VisionEdgeGeometry {
    data class Placement(val right: Int, val top: Int, val width: Int, val height: Int)

    fun place(frame: OverlayPlacement.Frame,width: Int,height: Int,fraction: Float): Placement {
        val safeW=frame.screenWidth-frame.insetLeft-frame.insetRight
        val safeH=frame.screenHeight-frame.insetTop-frame.insetBottom
        require(safeW>0 && safeH>0 && width>0 && height>0){"VISION_EDGE_GEOMETRY_INVALID"}
        val w=width.coerceAtMost(safeW)
        val h=height.coerceAtMost(safeH)
        val f=if(fraction.isFinite()) fraction.coerceIn(0f,1f) else .72f
        return Placement(frame.insetRight,frame.insetTop+((safeH-h)*f).roundToInt(),w,h)
    }
    fun shift(frame: OverlayPlacement.Frame,height: Int,fraction: Float,dy: Float): Float {
        require(dy.isFinite()){"VISION_EDGE_DELTA_INVALID"}
        val safeH=frame.screenHeight-frame.insetTop-frame.insetBottom
        val travel=safeH-height.coerceAtMost(safeH)
        if(travel<=0)return .5f
        val previous=place(frame,1,height,fraction)
        return ((previous.top-frame.insetTop+dy)/travel).coerceIn(0f,1f)
    }
    fun panel(frame: OverlayPlacement.Frame,tag: Placement,width: Int,height: Int,gap: Int): Placement {
        val safeW=frame.screenWidth-frame.insetLeft-frame.insetRight
        val safeH=frame.screenHeight-frame.insetTop-frame.insetBottom
        require(width>0 && height>0 && gap>=0 && safeW>tag.width+gap && safeH>0){"VISION_PANEL_GEOMETRY_INVALID"}
        val w=width.coerceAtMost(safeW-tag.width-gap)
        val h=height.coerceAtMost(safeH)
        val top=(tag.top-h/3).coerceIn(frame.insetTop,frame.screenHeight-frame.insetBottom-h)
        return Placement(frame.insetRight+tag.width+gap,top,w,h)
    }
}

internal class VisionControlsOverlay(private val service: PocketOverlayService) {
    private val manager=service.getSystemService(WindowManager::class.java)
    private val prefs=service.getSharedPreferences("leeway-vision-edge-controls-v1",0)
    private var tag:TextView?=null
    private var panel:ScrollView?=null
    private var tabLayout:WindowManager.LayoutParams?=null
    private var panelLayout:WindowManager.LayoutParams?=null
    private var fraction=prefs.getFloat("verticalFraction",.72f).coerceIn(0f,1f)
    private val density get()=service.resources.displayMetrics.density
    private fun dp(n:Int)=(n*density).roundToInt()

    private fun frame(): OverlayPlacement.Frame {
        val m=manager.currentWindowMetrics
        val i=m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        return OverlayPlacement.Frame(m.bounds.width(),m.bounds.height(),i.left,i.top,i.right,i.bottom)
    }
    private fun tabPlacement():VisionEdgeGeometry.Placement =
        VisionEdgeGeometry.place(frame(),dp(55),dp(153),fraction)
    private fun panelPlacement():VisionEdgeGeometry.Placement =
        VisionEdgeGeometry.panel(frame(),tabPlacement(),dp(246),dp(472),dp(7))
    private fun skin(backgroundColor:Int,radius:Int,stroke:Int=Color.rgb(56,177,198)):GradientDrawable =
        GradientDrawable().apply{
            setColor(backgroundColor);cornerRadius=dp(radius).toFloat()
            setStroke(dp(1),stroke)
        }
    private fun layout(placement:VisionEdgeGeometry.Placement)=
        WindowManager.LayoutParams(placement.width,placement.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
                gravity=Gravity.RIGHT or Gravity.TOP
                x=placement.right
                y=placement.top
            }
    fun show(){
        if(tag!=null){relayout();return}
        if(!Settings.canDrawOverlays(service)||!PocketOverlayService.isEnabled(service))return
        val button=TextView(service).apply{
            text="V\nI\nS\nI\nO\nN"
            textSize=11f
            gravity=Gravity.CENTER
            setTextColor(Color.rgb(132,247,255))
            setTypeface(null,Typeface.BOLD)
            contentDescription="Open Vision camera controls"
            background=skin(Color.argb(247,6,25,42),14)
            isClickable=true;isFocusable=true
            setOnClickListener{togglePanel()}
        }
        val p=tabPlacement()
        val lp=layout(p)
        try{manager.addView(button,lp)}
        catch(error:Exception){throw error}
        tag=button;tabLayout=lp
        val slop=ViewConfiguration.get(service).scaledTouchSlop
        var startY=0f
        var startFraction=fraction
        var moved=false
        button.setOnTouchListener { _,event->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{
                    startY=event.rawY;startFraction=fraction;moved=false;true
                }
                MotionEvent.ACTION_MOVE->{
                    val delta=event.rawY-startY
                    if(abs(delta)>slop)moved=true
                    if(moved){
                        fraction=VisionEdgeGeometry.shift(frame(),tabPlacement().height,startFraction,delta)
                        relayout()
                    }
                    true
                }
                MotionEvent.ACTION_UP->{
                    if(!moved)button.performClick()
                    else prefs.edit().putFloat("verticalFraction",fraction).apply()
                    true
                }
                MotionEvent.ACTION_CANCEL->true
                else->false
            }
        }
    }
    private fun addControl(column:LinearLayout,label:String,action:String,status:TextView){
        val button=Button(service).apply{
            text=label
            textSize=12f
            setAllCaps(false)
            contentDescription=label
            setTextColor(Color.rgb(234,251,255))
            background=skin(Color.rgb(16,51,70),7,Color.rgb(51,112,135))
            setOnClickListener {
                val result=VisionActivity.dispatchOwnerControl(action)
                status.text=when(result){
                    "VISION_COMMAND_REQUESTED"->"Requested; check camera state"
                    "VISION_CAMERA_PERMISSION_REQUESTED"->"Camera approval requested"
                    "VISION_NOT_READY"->"Vision is loading"
                    "VISION_NOT_FOREGROUND"->"Open Vision first"
                    else->"Action unavailable"
                }
            }
        }
        column.addView(button,LinearLayout.LayoutParams(-1,dp(43)).apply{bottomMargin=dp(6)})
    }
    private fun createPanel():ScrollView {
        val scroll=ScrollView(service).apply{
            background=skin(Color.argb(250,6,24,37),14)
            isFillViewport=false
        }
        val content=LinearLayout(service).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(11),dp(8),dp(11),dp(8))
        }
        val close=Button(service).apply{
            text="VISION CONTROLS   X"
            contentDescription="Close Vision controls panel"
            setAllCaps(false)
            setTextColor(Color.rgb(126,244,250))
            background=skin(Color.rgb(11,35,51),8)
            setOnClickListener{hidePanel()}
        }
        content.addView(close,LinearLayout.LayoutParams(-1,dp(43)).apply{bottomMargin=dp(8)})
        val status=TextView(service).apply{
            text="Owner camera controls"
            textSize=11f
            setTextColor(Color.rgb(163,215,226))
            gravity=Gravity.CENTER
        }
        addControl(content,"Start / Stop Camera","camera",status)
        addControl(content,"Switch Camera","flip",status)
        addControl(content,"Local Tracking","tracking",status)
        addControl(content,"3D Eye Motion","motion",status)
        addControl(content,"Camera Wall","wall",status)
        addControl(content,"Camera Settings","settings",status)
        addControl(content,"Recenter Eye","recenter",status)
        content.addView(status,LinearLayout.LayoutParams(-1,dp(36)))
        scroll.addView(content)
        return scroll
    }
    private fun togglePanel(){if(panel==null)showPanel() else hidePanel()}
    private fun showPanel(){
        if(panel!=null)return
        val controls=createPanel()
        val lp=layout(panelPlacement())
        try{manager.addView(controls,lp)}
        catch(error:Exception){throw error}
        panel=controls;panelLayout=lp
        tag?.contentDescription="Collapse Vision camera controls"
    }
    private fun hidePanel(){
        panel?.let{runCatching{manager.removeView(it)}}
        panel=null;panelLayout=null
        tag?.contentDescription="Open Vision camera controls"
    }
    fun relayout(){
        val badge=tag?:return
        val p=tabLayout?:return
        val place=tabPlacement()
        p.width=place.width;p.height=place.height;p.x=place.right;p.y=place.top
        manager.updateViewLayout(badge,p)
        if(panel!=null){
            val content=panel!!
            val settings=panelLayout?:return
            val bounds=panelPlacement()
            settings.width=bounds.width;settings.height=bounds.height
            settings.x=bounds.right;settings.y=bounds.top
            manager.updateViewLayout(content,settings)
        }
    }
    fun close(){
        hidePanel()
        tag?.let{runCatching{manager.removeView(it)}}
        tag=null;tabLayout=null
    }
}
