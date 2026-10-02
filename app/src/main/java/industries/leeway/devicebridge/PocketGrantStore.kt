/*
LEEWAY
REGION: DEVICE.POCKET.BRIDGE
TAG: DEVICE.LEEWAY.POCKET.GRANT
WHAT: Scoped local credential for LeeWay Pocket Agent to call Device Bridge
WHY: Avoid sharing the owner/remote pairing secret with a sibling UI application
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android Device Bridge private storage
WHEN: 2026-09-29
HOW: Owner-approved explicit activity handoff creates a separate 256-bit local token
*/
package industries.leeway.devicebridge

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

object PocketGrantStore {
    private const val PREFS = "leeway_device_bridge"
    private const val KEY = "pocket_agent_local_secret"

    fun ensure(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val secret = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        prefs.edit().putString(KEY, secret).apply()
        return secret
    }

    fun matches(context: Context, candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        return ensure(context) == candidate
    }

    fun revoke(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
        ReceiptStore.record(
            context.applicationContext,
            "device.pocket.grant.revoke",
            "PASS",
            "Pocket Agent local bridge grant revoked"
        )
    }
}