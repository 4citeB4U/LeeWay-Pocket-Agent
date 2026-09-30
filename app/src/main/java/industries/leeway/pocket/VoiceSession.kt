package industries.leeway.pocket

/** An activity owns only its current turn, never the process-retained renderer. */
class VoiceSession<T : Any> {
    var owner: T? = null
        private set
    private var generation = 0
    private var pending: String? = null

    fun attach(next: T) {
        generation++
        owner = next
        pending = null
    }

    fun detach(previous: T): Boolean {
        if (owner !== previous) return false
        generation++
        owner = null
        pending = null
        return true
    }

    fun queue(caller: T, text: String): Boolean {
        if (owner !== caller) return false
        generation++
        pending = text
        return true
    }

    fun takePending(): Pair<Int, String>? {
        if (owner == null) return null
        val text = pending ?: return null
        pending = null
        return generation to text
    }

    fun ownerFor(turn: Int): T? = if (generation == turn) owner else null
}
