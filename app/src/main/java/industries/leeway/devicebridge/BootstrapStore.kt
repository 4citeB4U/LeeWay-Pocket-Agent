package industries.leeway.devicebridge

import android.content.Context
import org.json.JSONObject

object BootstrapStore {
    private const val PREFS = "leeway_device_bridge"
    private const val PASSPORT = "native_device_passport"

    fun savePassport(context: Context, passport: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PASSPORT, passport.toString()).apply()
    }

    fun loadPassport(context: Context): JSONObject? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PASSPORT, null)?.let(::JSONObject)
}
