package industries.leeway.pocket

/** A page being loaded is not evidence that its model worker is usable. */
class VoiceReadiness {
    var pageReady = false
        private set
    var ready = false
        private set
    fun beginPage() { pageReady = false; ready = false }
    fun bridgeReady() { pageReady = true; ready = false }
    fun unavailable() { ready = false }
    fun modelReady(): Boolean {
        ready = pageReady
        return ready
    }
}
