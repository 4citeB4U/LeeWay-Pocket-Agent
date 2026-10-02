package industries.leeway.devicebridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import org.json.JSONObject

object WorkstationKeeper {
    const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val KEEPER_SCRIPT =
        "/data/data/com.termux/files/home/.leeway/workstation/desktop-commander-keeper.sh"
    private const val TERMUX_HOME = "/data/data/com.termux/files/home"

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(TERMUX_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun ensure(context: Context): JSONObject {
        if (!hasPermission(context)) {
            return JSONObject().apply {
                put("ok", false)
                put("state", "TERMUX_RUN_COMMAND_PERMISSION_REQUIRED")
                put("permission", TERMUX_PERMISSION)
            }
        }

        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            putExtra(EXTRA_COMMAND_PATH, KEEPER_SCRIPT)
            putExtra(EXTRA_WORKDIR, TERMUX_HOME)
            putExtra(EXTRA_BACKGROUND, true)
        }

        return try {
            context.startService(intent)
            JSONObject().apply {
                put("ok", true)
                put("state", "TERMUX_WORKSTATION_KEEPER_DISPATCHED")
                put("script", KEEPER_SCRIPT)
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("ok", false)
                put("state", "TERMUX_WORKSTATION_KEEPER_FAILED")
                put("error", e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
