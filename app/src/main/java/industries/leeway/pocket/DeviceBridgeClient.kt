/*
LEEWAY
REGION: POCKET.DEVICE.BRIDGE
TAG: POCKET.LEEWAY.DEVICE_BRIDGE.CLIENT
WHAT: Scoped Pocket client for the canonical LeeWay Device Bridge Android app
WHY: Reuse the phone-local model/device authority without copying it into Pocket
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: LeeWay Pocket Agent
WHEN: 2026-09-29
HOW: Owner-approved activity-result bootstrap + scoped explicit IPC command activity
*/
package industries.leeway.pocket

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import industries.leeway.devicebridge.LocalAuthority
import industries.leeway.devicebridge.LocalBridgeServer
import industries.leeway.devicebridge.PocketGrantStore
import industries.leeway.devicebridge.ReceiptStore
import org.json.JSONObject
import java.util.UUID

class DeviceBridgeClient(context: Context) {
    private val appContext=context.applicationContext
    private val prefs=appContext.getSharedPreferences("leeway-pocket-device-bridge",Context.MODE_PRIVATE)

    fun isGranted(): Boolean {
        val saved = token()
        if (saved.isBlank()) return false
        val valid = PocketGrantStore.matches(appContext, saved)
        if (!valid) prefs.edit().remove("pocket_token").apply()
        return valid
    }
    fun token(): String = prefs.getString("pocket_token","").orEmpty()

    fun enableEmbeddedRuntime(): JSONObject {
        return try {
            LocalAuthority.setAgentAccess(appContext, true)
            val local = LocalBridgeServer.start(appContext)
            val scoped = PocketGrantStore.ensure(appContext)
            if (scoped.length < 20 || !PocketGrantStore.matches(appContext, scoped)) {
                return JSONObject().put("ok", false).put("error", "EMBEDDED_GRANT_FAILED")
            }
            prefs.edit().putString("pocket_token", scoped).apply()
            ReceiptStore.record(
                appContext,
                "device.pocket.grant",
                if (local.optBoolean("ok")) "PASS" else "BLOCKED",
                "Owner enabled embedded Agent Lee runtime"
            )
            JSONObject().apply {
                put("ok", local.optBoolean("ok"))
                put("runtime", local)
                put("grantReady", true)
            }
        } catch (error: Exception) {
            JSONObject().put("ok", false)
                .put("error", "EMBEDDED_RUNTIME_START_FAILED:" + error.javaClass.simpleName)
        }
    }

    fun commandIntent(capability: String, arguments: JSONObject): Intent =
        Intent().setComponent(
            ComponentName(
                appContext.packageName,
                "industries.leeway.devicebridge.PocketBridgeActivity"
            )
        ).putExtra("leeway_pocket_token",token())
            .putExtra("leeway_capability",capability)
            .putExtra("leeway_arguments",arguments.toString())

    fun parseResult(data: Intent?): JSONObject {
        val raw=data?.getStringExtra("leeway_result").orEmpty()
        return try{if(raw.isBlank()) JSONObject().put("ok",false).put("error","EMPTY_DEVICE_BRIDGE_RESULT") else JSONObject(raw)}
        catch(_:Exception){JSONObject().put("ok",false).put("error","INVALID_DEVICE_BRIDGE_RESULT")}
    }

    fun clearGrant(){prefs.edit().remove("pocket_token").apply()}
}
