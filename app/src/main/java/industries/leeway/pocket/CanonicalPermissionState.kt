package industries.leeway.pocket

internal data class PermissionState(
    val filesRead: Boolean = false,
    val filesWrite: Boolean = false,
    val screenObserve: Boolean = false,
    val pointerGuidance: Boolean = false,
    val uiControl: Boolean = false,
    val diagnostics: Boolean = true
)

internal fun PermissionState.admitsRemoteCapability(capability: String): Boolean = when (capability) {
    "device.ui.tap", "device.ui.swipe", "device.ui.text",
    "device.ui.back", "device.ui.home", "device.ui.recents" -> uiControl
    "device.ui.snapshot", "device.screen.capture" -> screenObserve
    else -> true
}
