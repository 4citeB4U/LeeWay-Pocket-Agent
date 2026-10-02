package industries.leeway.devicebridge

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

object BridgeSecret {
    private const val PREFS = "leeway_device_bridge"
    private const val KEY = "bridge_pairing_secret"

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
}
