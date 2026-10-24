package industries.leeway.pocket

/** Gesture callback completion alone is not independently verified task success. */
internal object AgentLeeGestureOutcome {
    enum class State { DENIED, DISPATCH_FAILED, GESTURE_COMPLETED_UNVERIFIED, VERIFIED }
    data class Result(val actionId: String, val state: State, val postStateVerified: Boolean, val receiptHash: String?)
    private val sha256 = Regex("^[a-fA-F0-9]{64}$")
    fun denied(actionId: String) = Result(actionId, State.DENIED, false, null)
    fun callback(actionId: String, completed: Boolean) =
        Result(actionId, if (completed) State.GESTURE_COMPLETED_UNVERIFIED else State.DISPATCH_FAILED, false, null)
    fun verify(result: Result, independentEvidence: Boolean, receiptHash: String?): Result {
        if (result.state != State.GESTURE_COMPLETED_UNVERIFIED || !independentEvidence ||
            receiptHash == null || !sha256.matches(receiptHash)) return result
        return Result(result.actionId, State.VERIFIED, true, receiptHash)
    }
}
