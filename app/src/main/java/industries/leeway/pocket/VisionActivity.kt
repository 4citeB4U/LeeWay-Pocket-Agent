/*
REGION: LEEWAY.VISION.ANDROID
TAG: CANONICAL_GOLDEN_VISION_FOREGROUND_SURFACE
WHO: Agent Lee Golden Package owner. WHAT: Foreground camera viewer for existing bundled Vision UI.
WHEN: Explicit owner Vision menu selection. WHERE: Non-exported Pocket Agent Activity.
WHY: Android restricts camera capture from background overlays.
HOW: Reuse signed Vision HTML, synthetic HTTPS local asset, scoped VIDEO_CAPTURE permission,
     existing movable OwnerSurfaceWindow; no parallel package, registry or RTC authority.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import java.lang.ref.WeakReference
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowInsets
import kotlin.math.hypot
import kotlin.math.roundToInt
import android.webkit.*
import java.io.ByteArrayInputStream

class VisionActivity : Activity() {
    private lateinit var view: WebView
    private var foreground = false
    private var pageReady = false
    private var pendingCameraAfterPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        view = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(v: WebView?, url: String?) {
                    super.onPageFinished(v, url)
                    pageReady = url == ENTRY
                    dispatchDeferredCameraIfReady()
                }
                override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean =
                    request.url.toString() != ENTRY && request.url.toString() != "about:blank"

                override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse {
                    if (request.method != "GET" || request.url.toString() != ENTRY) return deny()
                    return try {
                        WebResourceResponse(
                            "text/html", "UTF-8", 200, "OK",
                            mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff"),
                            assets.open("vision/index.html")
                        )
                    } catch (_: Exception) { deny() }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val ownerForeground = this@VisionActivity.foreground && !this@VisionActivity.isFinishing && !this@VisionActivity.isDestroyed
                        val exactOrigin = request.origin.toString().trimEnd('/') == "https://vision.leeway.invalid"
                        val onlyVideo = request.resources.size == 1 &&
                            request.resources[0] == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        val cameraGranted = checkSelfPermission(android.Manifest.permission.CAMERA) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (ownerForeground && exactOrigin && onlyVideo && cameraGranted)
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        else request.deny()
                    }
                }
            }
            loadUrl(ENTRY)
        }
        setContentView(view)
        window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        OwnerSurfaceWindow.attach(this, "Agent Lee Vision")
        installPinchResize()
    }

    // LFW-v1.0 phone: preserve the original Eye/camera DOM and scale the entire
    // existing native Vision activity (rather than resizing a clipping box).
    private fun installPinchResize() {
        var anchor: VisionPinchGeometry.Rect? = null
        var startSpan=0f
        var intercepting=false
        val density=resources.displayMetrics.density
        view.setOnTouchListener { _, event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if(event.pointerCount==2 && !intercepting) {
                        val state=window.attributes
                        val span=hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1))
                        if(span>8f && state.width>0 && state.height>0) {
                            anchor=VisionPinchGeometry.Rect(state.x,state.y,state.width,state.height)
                            startSpan=span
                            intercepting=true
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    intercepting
                }
                MotionEvent.ACTION_MOVE -> {
                    if(intercepting) {
                        val base=anchor
                        if(base!=null && startSpan>0f && event.pointerCount>=2) {
                            val span=hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1))
                            val metrics=windowManager.currentWindowMetrics
                            val inset=metrics.windowInsets.getInsetsIgnoringVisibility(
                                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                            )
                            val usableW=metrics.bounds.width()-inset.left-inset.right
                            val usableH=metrics.bounds.height()-inset.top-inset.bottom
                            val changed=VisionPinchGeometry.scaled(base,span/startSpan,
                                (110f*density).roundToInt().coerceAtLeast(1),
                                (130f*density).roundToInt().coerceAtLeast(1),usableW,usableH)
                            val params=window.attributes
                            params.width=changed.width
                            params.height=changed.height
                            params.x=changed.x
                            params.y=changed.y
                            window.attributes=params
                        }
                        true
                    } else false
                }
                MotionEvent.ACTION_POINTER_UP -> intercepting
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                    val used=intercepting
                    intercepting=false;anchor=null;startSpan=0f
                    used
                }
                else -> intercepting
            }
        }
    }
    private fun dispatchDeferredCameraIfReady() {
        if (!pendingCameraAfterPermission || !pageReady || !foreground) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        pendingCameraAfterPermission = false
        dispatchOriginalButton("cameraButton")
    }

    private fun dispatchOriginalButton(id: String) {
        // ID comes only from the fixed allowlist. The original WebView stays the capability owner.
        val script = "(function(){const b=document.getElementById('" + id +
            "');if(!b)return 'CONTROL_UNBOUND';b.click();return 'BUTTON_CLICK_REQUESTED'})()"
        view.evaluateJavascript(script) { raw ->
            getSharedPreferences("leeway-vision-control-evidence", MODE_PRIVATE)
                .edit().putString("lastBrowserClickResult", raw).apply()
        }
    }

    private fun requestOwnerControl(action: String): String {
        val id = ACTION_IDS[action] ?: return "VISION_ACTION_DENIED"
        if (!foreground || isFinishing || isDestroyed) return "VISION_NOT_FOREGROUND"
        if (!pageReady) return "VISION_NOT_READY"
        if (action == "camera" &&
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingCameraAfterPermission = true
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            return "VISION_CAMERA_PERMISSION_REQUESTED"
        }
        dispatchOriginalButton(id)
        return "VISION_COMMAND_REQUESTED"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED)
                pendingCameraAfterPermission = false
            else dispatchDeferredCameraIfReady()
        }
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        liveVision = WeakReference(this)
        view.onResume()
        dispatchDeferredCameraIfReady()
        PocketOverlayService.showVisionControls(this)
    }

    override fun onPause() {
        foreground = false
        if (liveVision?.get() === this) liveVision = null
        PocketOverlayService.hideVisionControls()
        view.evaluateJavascript(
            "document.querySelectorAll('video').forEach(v=>v.srcObject?.getTracks().forEach(t=>t.stop()))",
            null
        )
        view.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (liveVision?.get() === this) liveVision = null
        PocketOverlayService.hideVisionControls()
        view.loadUrl("about:blank")
        view.destroy()
        super.onDestroy()
    }

    private fun deny() = WebResourceResponse(
        "text/plain", "UTF-8", 403, "Blocked",
        mapOf("Cache-Control" to "no-store"),
        ByteArrayInputStream("LOCAL_VISION_ASSET_ONLY".toByteArray())
    )

    companion object {
        private const val ENTRY = "https://vision.leeway.invalid/vision/index.html"
        private const val CAMERA_PERMISSION_REQUEST = 1824
        private val ACTION_IDS = mapOf(
            "camera" to "cameraButton",
            "flip" to "flipButton",
            "tracking" to "scanButton",
            "motion" to "motionButton",
            "wall" to "matrixButton",
            "settings" to "settingsButton",
            "recenter" to "centerButton"
        )
        @Volatile private var liveVision: WeakReference<VisionActivity>? = null
        internal fun dispatchOwnerControl(action: String): String =
            liveVision?.get()?.requestOwnerControl(action) ?: "VISION_NOT_FOREGROUND"
        internal fun hasForegroundOwner(): Boolean =
            liveVision?.get()?.foreground == true
    }
}
