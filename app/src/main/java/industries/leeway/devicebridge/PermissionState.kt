package industries.leeway.devicebridge

data class PermissionState(
    val filesRead: Boolean = false,
    val filesWrite: Boolean = false,
    val screenObserve: Boolean = false,
    val pointerGuidance: Boolean = false,
    val uiControl: Boolean = false,
    val diagnostics: Boolean = true
)
