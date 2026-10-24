package industries.leeway.pocket

/** No trusted Device Bridge issuer is bound to Pocket yet; physical actions fail closed. */
internal object AgentLeeTrustedExecutionAdmission {
    fun admitted(): Boolean = false
}