/*
LEEWAY
REGION: DEVICE.POCKET.BRIDGE
TAG: DEVICE.LEEWAY.POCKET.COMMAND_ACTIVITY
WHAT: Scoped Android IPC execution surface for the installed LeeWay Pocket Agent
WHY: Let Pocket use the canonical Device Bridge runtime without copying its model or owner credential
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android Device Bridge
WHEN: 2026-09-29
HOW: Explicit activity-for-result, caller package check, scoped token, bounded capability allowlist, existing governed router
*/
package industries.leeway.devicebridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import java.util.UUID
import kotlin.concurrent.thread

class PocketBridgeActivity : Activity() {
    private val allowed = setOf(
        "device.health",
        "device.info",
        "device.capabilities",
        "device.receipts",
        "model.status",
        "model.inference",
        "agent.chat"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caller = callingPackage.orEmpty()
        val token = intent?.getStringExtra("leeway_pocket_token")
        val capability = intent?.getStringExtra("leeway_capability").orEmpty()
        val rawArgs = intent?.getStringExtra("leeway_arguments").orEmpty()

        // This component is exported=false in the unified APK. Android already
        // confines launches to this application; the scoped grant token remains required.
        if (!PocketGrantStore.matches(this, token)) {
            finishBlocked("POCKET_TOKEN_INVALID", caller)
            return
        }
        if (capability !in allowed) {
            finishBlocked("CAPABILITY_NOT_POCKET_QUALIFIED", caller, capability)
            return
        }

        val arguments = try {
            if (rawArgs.isBlank()) JSONObject() else JSONObject(rawArgs)
        } catch (_: Exception) {
            finishBlocked("ARGUMENTS_INVALID_JSON", caller, capability)
            return
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            addView(ProgressBar(this@PocketBridgeActivity))
            addView(TextView(this@PocketBridgeActivity).apply {
                text = "Agent Lee is working on your phone.\nPreparing a short response…"
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 0)
            })
        })

        thread(name = "leeway-pocket-command") {
            val commandId = "pocket-" + UUID.randomUUID().toString()
            val result = RemoteCommandRouter.execute(
                applicationContext,
                commandId,
                capability,
                arguments,
                true,
                conversationRequest = true
            )
            ReceiptStore.record(
                applicationContext,
                capability,
                if (result.optBoolean("ok")) "PASS" else "BLOCKED",
                "Pocket Agent IPC command id=" + commandId
            )
            runOnUiThread {
                val data = Intent().putExtra("leeway_result", result.toString())
                setResult(if (result.optBoolean("ok")) RESULT_OK else RESULT_CANCELED, data)
                finish()
            }
        }
    }

    private fun finishBlocked(error: String, caller: String, capability: String = "") {
        ReceiptStore.record(
            applicationContext,
            "device.pocket.command",
            "BLOCKED",
            "error=$error caller=" + caller.ifBlank { "UNKNOWN" } + " capability=" + capability
        )
        val payload = JSONObject()
            .put("ok", false)
            .put("error", error)
            .put("capability", capability)
        setResult(RESULT_CANCELED, Intent().putExtra("leeway_result", payload.toString()))
        finish()
    }
}
