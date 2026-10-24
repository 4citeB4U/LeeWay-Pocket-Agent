package industries.leeway.pocket

import android.app.Service
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView

/**
 * One LeeWay cursor identity shared with PC via assets/agent-lee-cursor-v1.svg.
 * Presentation only. Never synthesizes touch events or indicates action without a receipt.
 */
internal class AgentLeeCursorOverlay(private val service: Service) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private var view: WebView? = null
    private var params: WindowManager.LayoutParams? = null
    private var currentReceipt: String? = null
    private val expiryHandler = Handler(Looper.getMainLooper())
    private var expiry: Runnable? = null

    /** Only a bounded, authorized executor should call this after producing its action receipt. */
    fun showForExecutedAction(receiptId: String, xPx: Int, yPx: Int) {
        if (receiptId.isBlank() || receiptId.length > 128 || !Settings.canDrawOverlays(service)) return
        val metrics = wm.currentWindowMetrics.bounds
        val size = (62 * density).toInt().coerceAtLeast(1)
        val p = params ?: WindowManager.LayoutParams(
            size, (76 * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.LEFT; title = "Agent Lee Executing Cursor" }
        p.x = xPx.coerceIn(0, (metrics.width() - p.width).coerceAtLeast(0))
        p.y = yPx.coerceIn(0, (metrics.height() - p.height).coerceAtLeast(0))
        currentReceipt = receiptId
        expiry?.let { expiryHandler.removeCallbacks(it) }
        val expectedReceipt = receiptId
        expiry = Runnable { if (currentReceipt == expectedReceipt) close() }
        expiryHandler.postDelayed(expiry!!, 5000L)
        if (view == null) {
            val w = WebView(service).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                loadUrl("file:///android_asset/agent-lee-cursor-v1.svg")
            }
            wm.addView(w, p)
            view = w
            params = p
        } else {
            view?.let { wm.updateViewLayout(it, p) }
        }
    }

    fun hide(receiptId: String) {
        if (receiptId != currentReceipt) return
        close()
    }

    fun close() {
        expiry?.let { expiryHandler.removeCallbacks(it) }
        expiry = null
        view?.let { wm.removeView(it); it.destroy() }
        view = null
        params = null
        currentReceipt = null
    }
}
