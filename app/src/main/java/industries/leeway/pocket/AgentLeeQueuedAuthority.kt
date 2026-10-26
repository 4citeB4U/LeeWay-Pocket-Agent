package industries.leeway.pocket

/** Shared fail-closed queued-dispatch predicate used by Android actuator. */
internal object AgentLeeQueuedAuthority {
 fun admitted(stopCurrent:Boolean, ownerAccess:Boolean, uiControl:Boolean, trustedIssuer:Boolean):Boolean =
  stopCurrent && ownerAccess && uiControl && trustedIssuer
}
