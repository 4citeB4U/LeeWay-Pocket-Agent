package industries.leeway.pocket

import java.util.Locale

enum class PhoneLaunchCommand {
    SETTINGS, CALCULATOR;

    companion object {
        fun parse(request: String): PhoneLaunchCommand? = when (request.trim().lowercase(Locale.US)) {
            "open settings" -> SETTINGS
            "open calculator" -> CALCULATOR
            else -> null
        }
    }
}
