/*
REGION: LeeWay Android platform adapter
TAG: LEEWAY-DEVICE-OPERATOR-ANDROID-ACCESSIBILITY
WHO: Owner-authorized LeeWay Device Bridge
WHAT: Android AccessibilityService adapter for UI observation and control.
WHEN: Only while the owner has enabled the service and LeeWay agent access is authorized.
WHERE: Native Android runtime inside canonical LEEWAY-DEVICE-BRIDGE.
WHY: Provide legitimate non-root cross-app accessibility control without pretending to be system/root.
HOW: Android AccessibilityService APIs + LeeWay authority checks + receipts.
LICENSE: MIT
*/
package industries.leeway.devicebridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import android.view.Display
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

class DeviceOperatorAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        current = this
        runCatching { LocalBridgeServer.start(applicationContext) }
        ReceiptStore.record(this, "device.ui.control", "PASS", "Owner-authorized accessibility service connected; loopback diagnostics ensured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var current: DeviceOperatorAccessibilityService? = null

        fun status(): JSONObject = JSONObject().apply {
            put("active", current != null)
            put("authority", if (current != null) "OWNER_AUTHORIZED_ACCESSIBILITY" else "NOT_AUTHORIZED")
        }

        fun snapshot(): JSONObject {
            val service = current ?: return blocked("ACCESSIBILITY_SERVICE_NOT_ACTIVE")
            val root = service.rootInActiveWindow ?: return blocked("NO_ACTIVE_WINDOW")
            var count = 0
            fun nodeJson(node: AccessibilityNodeInfo?, depth: Int): JSONObject {
                if (node == null || depth > 20 || count >= 750) return JSONObject().put("truncated", true)
                count += 1
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val out = JSONObject().apply {
                    put("className", node.className?.toString())
                    put("packageName", node.packageName?.toString())
                    put("text", node.text?.toString())
                    put("contentDescription", node.contentDescription?.toString())
                    put("clickable", node.isClickable)
                    put("scrollable", node.isScrollable)
                    put("editable", node.isEditable)
                    put("enabled", node.isEnabled)
                    put("visibleToUser", node.isVisibleToUser)
                    put("boundsInScreen", JSONObject().apply {
                        put("left", bounds.left)
                        put("top", bounds.top)
                        put("right", bounds.right)
                        put("bottom", bounds.bottom)
                    })
                }
                val children = JSONArray()
                for (i in 0 until node.childCount) children.put(nodeJson(node.getChild(i), depth + 1))
                out.put("children", children)
                return out
            }
            val tree = nodeJson(root, 0)
            return JSONObject().apply {
                put("ok", true)
                put("nodeCount", count)
                put("tree", tree)
            }
        }


        fun captureScreen(): JSONObject {
            val service = current ?: return blocked("ACCESSIBILITY_SERVICE_NOT_ACTIVE")
            val latch = CountDownLatch(1)
            var result: JSONObject = blocked("SCREENSHOT_TIMEOUT")
            val executor = Executors.newSingleThreadExecutor()
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        try {
                            val hardware = screenshot.hardwareBuffer
                            val wrapped = Bitmap.wrapHardwareBuffer(hardware, screenshot.colorSpace)
                            if (wrapped == null) {
                                result = blocked("SCREENSHOT_BITMAP_UNAVAILABLE")
                            } else {
                                val software = wrapped.copy(Bitmap.Config.ARGB_8888, false)
                                val maxWidth = 1440
                                val scaled = if (software.width > maxWidth) {
                                    val height = (software.height.toDouble() * maxWidth / software.width).toInt()
                                    Bitmap.createScaledBitmap(software, maxWidth, height, true)
                                } else software
                                val bytes = ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, 72, bytes)
                                result = JSONObject().apply {
                                    put("ok", true)
                                    put("mimeType", "image/jpeg")
                                    put("width", scaled.width)
                                    put("height", scaled.height)
                                    put("base64", Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP))
                                }
                                if (scaled !== software) scaled.recycle()
                                software.recycle()
                            }
                            hardware.close()
                        } catch (e: Exception) {
                            result = blocked("SCREENSHOT_FAILED:" + e.javaClass.simpleName)
                        } finally {
                            latch.countDown()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        result = JSONObject().put("ok", false)
                            .put("error", "SCREENSHOT_ERROR")
                            .put("errorCode", errorCode)
                        latch.countDown()
                    }
                }
            )
            latch.await(4, TimeUnit.SECONDS)
            executor.shutdown()
            return result
        }

        fun global(action: String): JSONObject {
            val service = current ?: return blocked("ACCESSIBILITY_SERVICE_NOT_ACTIVE")
            val code = when (action.lowercase()) {
                "back" -> GLOBAL_ACTION_BACK
                "home" -> GLOBAL_ACTION_HOME
                "recents" -> GLOBAL_ACTION_RECENTS
                "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
                "quick-settings" -> GLOBAL_ACTION_QUICK_SETTINGS
                else -> return blocked("UNKNOWN_GLOBAL_ACTION")
            }
            val ok = service.performGlobalAction(code)
            return JSONObject().put("ok", ok).put("action", action)
        }

        fun tap(x: Float, y: Float): JSONObject {
            val service = current ?: return blocked("ACCESSIBILITY_SERVICE_NOT_ACTIVE")
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
                .build()
            val accepted = service.dispatchGesture(gesture, null, null)
            return JSONObject().put("ok", accepted).put("x", x).put("y", y)
        }

        fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): JSONObject {
            val service = current ?: return blocked("ACCESSIBILITY_SERVICE_NOT_ACTIVE")
            val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(50, 5000)))
                .build()
            val accepted = service.dispatchGesture(gesture, null, null)
            return JSONObject().put("ok", accepted)
        }

        fun setFocusedText(text: String): JSONObject {
            val service = current ?: return blocked("ACCESSIBILITY_SERVICE_NOT_ACTIVE")
            val node = service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: return blocked("NO_FOCUSED_INPUT")
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            return JSONObject().put("ok", ok).put("length", text.length)
        }

        private fun blocked(reason: String) =
            JSONObject().put("ok", false).put("error", reason)
    }
}
