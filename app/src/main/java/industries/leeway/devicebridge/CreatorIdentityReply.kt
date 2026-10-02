package industries.leeway.devicebridge

import org.json.JSONObject
import java.util.Locale

/** Exact English identity lookup; not biography inference, authentication or a tool grant. */
internal object CreatorIdentityReply {
    private val questions = setOf("who created agent lee", "who created you", "who is your creator", "who's your creator")
    fun matches(request: String): Boolean = request.length <= 80 &&
        request.trim().removeSuffix("?").trim().lowercase(Locale.US) in questions

    fun fromProfile(profile: JSONObject?): String {
        val creator = profile?.optJSONObject("creator")
        val name = creator?.optString("name").orEmpty()
        if (creator?.optString("classification") != "USER_AUTHORIZED_PROFILE_CONTEXT_NOT_AUTHENTICATION" ||
            creator.optString("relationship") != "Creator of the LeeWay ecosystem" ||
            name.isBlank() || name.length > 120 || name.any { it.isISOControl() }) {
            return "The verified creator profile is unavailable."
        }
        return "Agent Lee was created by $name, the creator of LeeWay."
    }
}
