package industries.leeway.pocket

import java.util.Locale

data class PhoneActionCommand(val action: String, val label: String = "") {
    companion object {
        fun parse(raw: String): PhoneActionCommand? {
            val text = raw.trim()
            if(text.length > 165 || text.any { it.isISOControl() }) return null
            return when(text.lowercase(Locale.US)) {
                "go home" -> PhoneActionCommand("home")
                "go back" -> PhoneActionCommand("back")
                "show recent apps" -> PhoneActionCommand("recents")
                else -> if(text.startsWith("open ", ignoreCase = true) && text.substring(5).isNotBlank())
                    PhoneActionCommand("open", text.substring(5).trim()) else null
            }
        }
    }
}

/** A result/timeout/disconnect may race; only the first ends the command. */
class PhoneActionCompletion {
    private val completed = java.util.concurrent.atomic.AtomicBoolean(false)
    fun finish() = completed.compareAndSet(false, true)
    fun isFinished() = completed.get()
}
