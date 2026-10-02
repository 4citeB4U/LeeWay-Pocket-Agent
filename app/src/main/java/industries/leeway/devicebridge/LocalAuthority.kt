package industries.leeway.devicebridge

import android.content.Context

object LocalAuthority {
    private const val PREFS = "leeway_device_bridge"

    fun agentAccessEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("agent_access_enabled", false)

    fun setAgentAccess(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("agent_access_enabled", enabled).apply()
    }

    fun permissions(context: Context): PermissionState {
        val p=context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return PermissionState(
            filesRead=p.getBoolean("perm_files_read", false),
            filesWrite=p.getBoolean("perm_files_write", false),
            screenObserve=p.getBoolean("perm_screen_observe", false),
            pointerGuidance=p.getBoolean("perm_pointer_guidance", false),
            uiControl=p.getBoolean("perm_ui_control", false),
            diagnostics=true
        )
    }
}
