package industries.leeway.devicebridge

import java.util.concurrent.atomic.AtomicBoolean

/** Explicit one-step commands, not model-generated tool calls. */
object PocketCommandPolicy {
    val actions = setOf("open", "back", "home", "recents")
    fun authorized(packages: List<String>, sameSigner: Boolean, token: Boolean, owner: Boolean) =
        packages == listOf("industries.leeway.pocket") && sameSigner && token && owner
    fun valid(action: String, label: String) = action in actions &&
        if (action == "open") label.isNotBlank() && label.length <= 160 && label.none { it.isISOControl() }
        else label.isEmpty()
    fun matchingPackages(label: String, apps: List<Pair<String, String>>): List<String> =
        apps.filter { it.first.equals(label.trim(), ignoreCase = true) }
            .map { it.second }.distinct()
}

class PocketCommandLease {
    private val active = AtomicBoolean(false)
    fun acquire() = active.compareAndSet(false, true)
    fun release() { active.set(false) }
}
