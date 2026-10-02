package industries.leeway.devicebridge

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

object FloatingAgentLeeOverlay {
    private var view: TextView? = null

    fun attach(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        if (view != null) return true
        val app = context.applicationContext
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val button = TextView(app).apply {
            text = "MIC"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(18, 22, 18, 22)
            setBackgroundColor(0xDD0B2740.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val intent = Intent(app, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("leeway_action", "TALK_TO_AGENT_LEE")
                }
                app.startActivity(intent)
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }
        wm.addView(button, params)
        view = button
        ReceiptStore.record(app, "agent.lee.side.mic", "PASS", "Floating side microphone attached")
        return true
    }

    fun detach(context: Context) {
        val current = view ?: return
        val app = context.applicationContext
        runCatching {
            (app.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(current)
        }
        view = null
        ReceiptStore.record(app, "agent.lee.side.mic", "PASS", "Floating side microphone detached")
    }
}
