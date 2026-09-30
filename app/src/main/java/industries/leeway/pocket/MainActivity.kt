/*
LEEWAY
REGION: POCKET.UI
TAG: POCKET.LEEWAY.MAIN
WHAT: Compact Pocket Agent home/configuration surface
WHY: Keep the main app lightweight while the persistent side tab owns immediate voice ingress
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android LeeWay Pocket Agent
WHEN: 2026-09-29
HOW: Voxel home surface + scoped Device Bridge setup + floating mic permission + memory/automation controls
*/
package industries.leeway.pocket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var sphere: VoxelSphereView
    private lateinit var memory: MemoryStore
    private lateinit var automation: N8nBridge
    private lateinit var bridge: DeviceBridgeClient
    private var expectedNonce: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.BLACK
        memory = MemoryStore(this)
        automation = N8nBridge(this)
        bridge = DeviceBridgeClient(this)

        val permissions=mutableListOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA)
        if(Build.VERSION.SDK_INT>=33)permissions+=Manifest.permission.POST_NOTIFICATIONS
        requestPermissions(permissions.toTypedArray(),10)
        buildUi()
        if (intent?.getStringExtra("leeway_action") == "TALK_TO_AGENT_LEE") {
            startActivity(Intent(this, PocketVoiceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        if(prefs.getBoolean("permission_pending",false) && Settings.canDrawOverlays(this)){
            prefs.edit().putBoolean("permission_pending",false).apply()
            PocketOverlayService.setEnabled(this,true)
            Toast.makeText(this,"Agent Lee side tab enabled.",Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildUi() {
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        sphere = VoxelSphereView(this).apply {
            setOnClickListener { startActivity(Intent(this@MainActivity,PocketVoiceActivity::class.java)) }
        }
        frame.addView(
            sphere,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT)
        )

        val menu = TextView(this).apply {
            text = "☰"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(22,10,22,10)
            setOnClickListener { showMenu() }
        }
        frame.addView(menu,FrameLayout.LayoutParams(96,96,Gravity.TOP or Gravity.START))

        val hint=TextView(this).apply{
            text="Tap Agent Lee to talk"
            textSize=14f
            setTextColor(Color.LTGRAY)
            gravity=Gravity.CENTER
        }
        frame.addView(
            hint,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply{bottomMargin=52}
        )
        setContentView(frame)
    }

    private fun showMenu() {
        val overlayLabel=if(PocketOverlayService.isEnabled(this))"Disable floating Agent Lee" else "Enable floating Agent Lee"
        val bridgeLabel=if(bridge.isGranted())"Reconnect Device Bridge" else "Connect Device Bridge"
        val items = arrayOf(
            "Talk to Agent Lee",
            overlayLabel,
            bridgeLabel,
            "Past conversations",
            "Personal memory",
            "Lee's notebook",
            "Automation bridge (n8n)",
            "Camera",
            "Close"
        )
        android.app.AlertDialog.Builder(this).setTitle("LeeWay Pocket").setItems(items) { d, which ->
            when (which) {
                0 -> startActivity(Intent(this,PocketVoiceActivity::class.java))
                1 -> toggleFloatingAgent()
                2 -> connectDeviceBridge()
                3 -> showText("Past conversations",memory.readConversations())
                4 -> showText("Personal memory",memory.readPersonal())
                5 -> showText("Lee's notebook",memory.readNotebook())
                6 -> configureAutomationBridge()
                7 -> startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
                else -> d.dismiss()
            }
        }.show()
    }

    private fun toggleFloatingAgent(){
        if(PocketOverlayService.isEnabled(this)){
            PocketOverlayService.setEnabled(this,false)
            Toast.makeText(this,"Agent Lee side tab disabled.",Toast.LENGTH_SHORT).show()
            return
        }
        if(Settings.canDrawOverlays(this)){
            PocketOverlayService.setEnabled(this,true)
            Toast.makeText(this,"Agent Lee side tab enabled.",Toast.LENGTH_SHORT).show()
            return
        }
        getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
            .edit().putBoolean("permission_pending",true).apply()
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun connectDeviceBridge(){
        val nonce=bridge.newNonce()
        expectedNonce=nonce
        try{
            startActivityForResult(bridge.bootstrapIntent(nonce),REQ_BRIDGE_BOOTSTRAP)
        }catch(_:Exception){
            Toast.makeText(
                this,
                "Current Device Bridge does not yet expose the Pocket workstation adapter.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Legacy activity-result API retained for minimum-compatible explicit cross-app handoff")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==REQ_BRIDGE_BOOTSTRAP){
            val nonce=expectedNonce.orEmpty()
            expectedNonce=null
            val ok=resultCode==RESULT_OK && bridge.acceptBootstrap(data,nonce)
            Toast.makeText(
                this,
                if(ok)"Pocket is connected to Device Bridge." else "Device Bridge connection was not approved.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun configureAutomationBridge() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32,16,32,0)
        }
        val endpoint = EditText(this).apply {
            hint = "n8n webhook URL"
            setText(automation.endpoint())
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val token = EditText(this).apply {
            hint = "Bearer token (optional)"
            setText(automation.token())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(endpoint)
        layout.addView(token)

        android.app.AlertDialog.Builder(this)
            .setTitle("LeeWay n8n Automation Bridge")
            .setMessage("Use one canonical Pocket Lee webhook. The endpoint stays in private app storage.")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                automation.configure(endpoint.text.toString(),token.text.toString())
                Toast.makeText(
                    this,
                    if(automation.isConfigured())"Automation bridge saved." else "Endpoint not configured.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun showText(title:String,body:String){
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(if(body.isBlank())"Nothing saved yet." else body)
            .setPositiveButton("Close",null)
            .show()
    }

    companion object{
        private const val REQ_BRIDGE_BOOTSTRAP=801
    }
}

class MemoryStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("leeway-pocket-memory",android.content.Context.MODE_PRIVATE)
    private fun append(key:String,value:String){
        val old=prefs.getString(key,"").orEmpty()
        prefs.edit().putString(key,if(old.isBlank())value else "$old\n$value").apply()
    }
    fun saveConversation(v:String)=append("conversations",v)
    fun savePersonal(v:String)=append("personal",v)
    fun saveNotebook(v:String)=append("notebook",v)
    fun readConversations()=prefs.getString("conversations","").orEmpty()
    fun readPersonal()=prefs.getString("personal","").orEmpty()
    fun readNotebook()=prefs.getString("notebook","").orEmpty()
}

class VoxelSphereView(context: android.content.Context) : View(context) {
    enum class State { IDLE, LISTENING, THINKING, SPEAKING }
    var state = State.IDLE
        set(value) { field = value; invalidate() }
    var voiceLevel = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private val voxels = mutableListOf<Triple<Float,Float,Float>>()

    init {
        for (a in 0 until 360 step 14) for (b in -80..80 step 14) {
            val ar = Math.toRadians(a.toDouble())
            val br = Math.toRadians(b.toDouble())
            voxels += Triple(
                (cos(br) * cos(ar)).toFloat(),
                sin(br).toFloat(),
                (cos(br) * sin(ar)).toFloat()
            )
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(Color.BLACK)
        val cx = width / 2f
        val cy = height / 2f
        val base = min(width,height) * 0.22f
        val pulse = when (state) {
            State.IDLE -> 1f + 0.03f * sin(t)
            State.LISTENING -> 1.05f + voiceLevel.coerceIn(0f,1f) * 0.18f
            State.THINKING -> 0.95f + 0.08f * sin(t * 3f)
            State.SPEAKING -> 1.03f + 0.11f * abs(sin(t * 4f))
        }
        val rot = t * 0.35f
        for ((x0,y0,z0) in voxels) {
            val x = x0 * cos(rot) - z0 * sin(rot)
            val z = x0 * sin(rot) + z0 * cos(rot)
            val depth = (z + 1.4f) / 2.8f
            val sx = cx + x * base * pulse
            val sy = cy + y0 * base * pulse
            val size = 3f + 7f * depth
            val intensity = (110 + 145 * depth).toInt().coerceIn(0,255)
            paint.color = Color.rgb(intensity,intensity,intensity)
            c.drawRect(sx-size/2,sy-size/2,sx+size/2,sy+size/2,paint)
        }
        t += 0.045f
        postInvalidateOnAnimation()
    }
}
