[Reading 64 lines from start (total: 64 lines, 0 remaining)]

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
import android.os.ResultReceiver
import org.json.JSONObject
import java.util.UUID

class DeviceBridgeClient(context: Context) {
    private val prefs=context.getSharedPreferences("leeway-pocket-device-bridge",Context.MODE_PRIVATE)

    fun isGranted(): Boolean = token().isNotBlank()
    fun token(): String = prefs.getString("pocket_token","").orEmpty()

    fun newNonce(): String = UUID.randomUUID().toString().replace("-","")

    fun bootstrapIntent(nonce: String): Intent =
        Intent().setComponent(
            ComponentName(
                "industries.leeway.devicebridge",
                "industries.leeway.devicebridge.MainActivity"
            )
        ).putExtra("leeway_action","POCKET_BOOTSTRAP")
            .putExtra("leeway_nonce",nonce)

    fun acceptBootstrap(data: Intent?, expectedNonce: String): Boolean {
        val returned=data?.getStringExtra("leeway_nonce").orEmpty()
        val scoped=data?.getStringExtra("leeway_pocket_token").orEmpty()
        if(returned!=expectedNonce || scoped.length<20)return false
        prefs.edit().putString("pocket_token",scoped).apply()
        return true
    }

    fun commandIntent(capability: String, arguments: JSONObject, streamReceiver: ResultReceiver? = null): Intent =
        Intent().setComponent(
            ComponentName(
                "industries.leeway.devicebridge",
                "industries.leeway.devicebridge.PocketBridgeActivity"
            )
        ).putExtra("leeway_pocket_token",token())
            .putExtra("leeway_capability",capability)
            .putExtra("leeway_arguments",arguments.toString())
            .apply { if (streamReceiver != null) putExtra("leeway_stream_receiver", streamReceiver) }

    fun parseResult(data: Intent?): JSONObject {
        val raw=data?.getStringExtra("leeway_result").orEmpty()
        return try{if(raw.isBlank()) JSONObject().put("ok",false).put("error","EMPTY_DEVICE_BRIDGE_RESULT") else JSONObject(raw)}
        catch(_:Exception){JSONObject().put("ok",false).put("error","INVALID_DEVICE_BRIDGE_RESULT")}
    }

    fun clearGrant(){prefs.edit().remove("pocket_token").apply()}
}

[executed on device: localhost (0580364a-68c5-43c9-ad40-f0261d31d7b4)]