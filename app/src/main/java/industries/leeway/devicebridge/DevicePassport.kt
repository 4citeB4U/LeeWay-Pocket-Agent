package industries.leeway.devicebridge

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import org.json.JSONArray
import org.json.JSONObject

object DevicePassport {
    fun capture(context: Context): JSONObject {
        val activity = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also(activity::getMemoryInfo)
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val operatorActive = DeviceOperatorAccessibilityService.status().optBoolean("active")

        return JSONObject().apply {
            put("schemaVersion", "0.1.0")
            put("identity", DeviceIdentity.ensure(context))
            put("authority", "NATIVE_ANDROID_OBSERVED")
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("product", Build.PRODUCT)
            put("hardware", Build.HARDWARE)
            put("board", Build.BOARD)
            put("androidRelease", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("securityPatch", Build.VERSION.SECURITY_PATCH)
            put("supportedAbis", JSONArray(Build.SUPPORTED_ABIS.toList()))
            put("memory", JSONObject().apply {
                put("totalBytes", memory.totalMem)
                put("availableBytes", memory.availMem)
                put("lowMemory", memory.lowMemory)
            })
            put("storage", JSONObject().apply {
                put("totalBytes", stat.totalBytes)
                put("availableBytes", stat.availableBytes)
            })
            put("display", JSONObject().apply {
                put("widthPixels", metrics.widthPixels)
                put("heightPixels", metrics.heightPixels)
                put("densityDpi", metrics.densityDpi)
            })
            put("capabilityClaims", JSONArray().apply {
                put(capability("device.info", true, true, true))
                put(capability("device.health", true, false, false))
                put(capability("device.files.read", true, false, false))
                put(capability("device.files.write", true, false, false))
                put(capability("device.screen.observe", true, operatorActive, false, operatorActive))
                put(capability("device.ui.control", true, operatorActive, false, operatorActive))
                put(capability("device.apps.install", true, false, false))
                put(capability("device.apps.launch", true, true, false, true))
            })
        }
    }

    private fun capability(
        name: String,
        supported: Boolean,
        authorized: Boolean,
        verified: Boolean,
        active: Boolean = false
    ) = JSONObject().apply {
        put("name", name)
        put("supported", supported)
        put("available", supported)
        put("authorized", authorized)
        put("active", active)
        put("healthy", verified || active)
        put("verified", verified)
    }
}
