package industries.leeway.devicebridge

import android.content.Context
import org.json.JSONObject

object BridgeSelfTest {
    fun run(context: Context): JSONObject {
        val appContext = context.applicationContext
        val secret = BridgeSecret.ensure(appContext)
        val bridgeRunning = LocalBridgeServer.isRunning()
        val accessEnabled = LocalAuthority.agentAccessEnabled(appContext)
        val missingRejected = !BridgeSecret.matches(appContext, null)
        val wrongRejected = !BridgeSecret.matches(appContext, "invalid-token")
        val ownerTokenAccepted = BridgeSecret.matches(appContext, secret)
        val passport = BootstrapStore.loadPassport(appContext)
            ?: DevicePassport.capture(appContext)

        val executeGate = FormulaF8Gate.evaluate(
            trigger = true,
            governance = accessEnabled,
            conditions = listOf(
                bridgeRunning,
                ownerTokenAccepted,
                passport.optString("authority") == "NATIVE_ANDROID_OBSERVED"
            )
        )
        val emptyGate = FormulaF8Gate.evaluate(
            trigger = true,
            governance = true,
            conditions = emptyList()
        )

        val passed =
            bridgeRunning &&
            accessEnabled &&
            missingRejected &&
            wrongRejected &&
            ownerTokenAccepted &&
            executeGate.optBoolean("fire") &&
            !emptyGate.optBoolean("fire") &&
            !executeGate.optBoolean("canonicalFormulaExecuted")

        val detail =
            "running=$bridgeRunning access=$accessEnabled " +
            "missingRejected=$missingRejected wrongRejected=$wrongRejected " +
            "ownerTokenAccepted=$ownerTokenAccepted localFire=${executeGate.optBoolean("fire")} " +
            "emptyFire=${emptyGate.optBoolean("fire")} canonicalFormula=NOT_EXECUTED"

        ReceiptStore.record(
            appContext,
            "device.bridge.selftest",
            if (passed) "PASS" else "FAIL",
            detail
        )
        return JSONObject().apply {
            put("ok", passed)
            put("bridgeRunning", bridgeRunning)
            put("agentAccessEnabled", accessEnabled)
            put("missingTokenRejected", missingRejected)
            put("wrongTokenRejected", wrongRejected)
            put("ownerTokenAcceptedInternally", ownerTokenAccepted)
            put("nativePassportAuthority", passport.optString("authority"))
            put("f8ExecuteState", executeGate)
            put("f8EmptyConditionState", emptyGate)
            put("secretExported", false)
        }
    }
}