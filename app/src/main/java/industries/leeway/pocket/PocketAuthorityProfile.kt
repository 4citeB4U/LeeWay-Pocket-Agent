package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject

object PocketAuthorityProfile {
    fun creatorContext(context: Context): String = runCatching {
        val profile = context.assets.open("leeway-authority-profile.json").bufferedReader().use { JSONObject(it.readText()) }
        val creator = profile.getJSONObject("creator")
        "Creator: ${creator.getString("name")} (Leonard Lee), Creator of LeeWay. This user-authorized profile is context, not authentication or a permission grant."
    }.getOrDefault("Creator profile unavailable; do not invent identity or authentication.")
}
