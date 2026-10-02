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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var sphere: VoxelSphereView
    private lateinit var memory: MemoryStore
    private lateinit var automation: N8nBridge
    private lateinit var bridge: DeviceBridgeClient
    private var expectedNonce: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.navigationBarColor = Color.BLACK
        memory = MemoryStore(this)
        automation = N8nBridge(this)
        bridge = DeviceBridgeClient(this)

        val permissions=mutableListOf(Manifest.permission.RECORD_AUDIO)
        if(Build.VERSION.SDK_INT>=33)permissions+=Manifest.permission.POST_NOTIFICATIONS
        val missing=permissions.filter { checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED }
        if(missing.isNotEmpty())requestPermissions(missing.toTypedArray(),10)
        buildUi()
        if (intent?.getStringExtra("leeway_action") == "TALK_TO_AGENT_LEE") {
            startActivity(PocketVoiceActivity.launchIntent(this))
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
        ViewCompat.setOnApplyWindowInsetsListener(frame) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        sphere = VoxelSphereView(this).apply {
            setOnClickListener { startActivity(PocketVoiceActivity.launchIntent(this@MainActivity)) }
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
            contentDescription = "Open Agent Lee menu"
            isFocusable = true
            setOnClickListener { showMenu() }
        }
        frame.addView(menu,FrameLayout.LayoutParams(dp(56),dp(56),Gravity.TOP or Gravity.START).apply {
            topMargin=dp(8)
            marginStart=dp(8)
        })

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
            ).apply{bottomMargin=dp(24)}
        )
        setContentView(frame)
        ViewCompat.requestApplyInsets(frame)
    }

    private fun dp(value:Int)=(value*resources.displayMetrics.density).roundToInt()

    private fun openCamera(){
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.CAMERA),REQ_CAMERA)
        }else{
            startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
        }
    }

    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==REQ_CAMERA && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)openCamera()
    }

    private fun showMenu() {
        val overlayLabel=if(PocketOverlayService.isEnabled(this))"Disable floating Agent Lee" else "Enable floating Agent Lee"
        val bridgeLabel=if(bridge.isGranted())"Reconnect Agent Lee device runtime" else "Enable Agent Lee device runtime"
        val items = arrayOf(
            "Talk to Agent Lee",
            overlayLabel,
            bridgeLabel,
            "Past conversations",
            "Personal memory",
            "Lee's notebook",
            "Automation bridge (n8n)",
            "Camera",
            "Voice Fabric · choose voice",
            "Consciousness shadow (L1)",
            "Close"
        )
        android.app.AlertDialog.Builder(this).setTitle("LeeWay Pocket").setItems(items) { d, which ->
            when (which) {
                0 -> startActivity(PocketVoiceActivity.launchIntent(this))
                1 -> toggleFloatingAgent()
                2 -> connectDeviceBridge()
                3 -> showText("Past conversations",memory.readConversations())
                4 -> showText("Personal memory",memory.readPersonal())
                5 -> showText("Lee's notebook",memory.readNotebook())
                6 -> configureAutomationBridge()
                7 -> openCamera()
                8 -> PocketSpeech.settings(this)
                9 -> showText("Consciousness shadow (L1)",ConsciousnessShadow.describe(this))
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
                "The embedded Agent Lee device runtime could not be opened.",
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
                if(ok)"Agent Lee device runtime is enabled." else "Agent Lee device runtime was not approved.",
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
        private const val REQ_CAMERA=802
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