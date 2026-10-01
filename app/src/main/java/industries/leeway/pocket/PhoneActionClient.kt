package industries.leeway.pocket

import android.content.*
import android.content.pm.PackageManager
import android.os.*
import org.json.JSONObject
import java.util.UUID

/** Bound by Pocket's existing foreground service, never by a transient Activity. */
class PhoneActionClient(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val completion = PhoneActionCompletion()
    private var bound = false
    private var connection: ServiceConnection? = null
    private var callback: ((JSONObject) -> Unit)? = null
    private val timeout = Runnable { finish(failure("COMMAND_TIMEOUT_OUTCOME_UNKNOWN")) }

    fun execute(command: PhoneActionCommand, result: (JSONObject) -> Unit) {
        callback = result
        val id = UUID.randomUUID().toString()
        val bridgePackage = "industries.leeway.devicebridge"
        if(context.packageManager.checkSignatures(context.packageName, bridgePackage) != PackageManager.SIGNATURE_MATCH) {
            finish(failure("BRIDGE_SIGNATURE_NOT_TRUSTED")); return
        }
        val token = DeviceBridgeClient(context).token()
        if(token.isBlank()) { finish(failure("POCKET_GRANT_REQUIRED")); return }
        val replies = Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                if(message.data.getString("id") != id) return
                val raw = message.data.getString("result").orEmpty()
                if(raw.length > 16000) { finish(failure("RESULT_TOO_LARGE")); return }
                finish(runCatching { JSONObject(raw) }.getOrElse { failure("INVALID_RESULT") })
            }
        })
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                // A late connection callback after cancellation must never dispatch an action.
                if(completion.isFinished()) return
                try {
                    Messenger(binder).send(Message.obtain().apply {
                        replyTo = replies
                        data = Bundle().apply {
                            putString("id", id); putString("token", token)
                            putString("action", command.action); putString("label", command.label)
                            putLong("sent", SystemClock.elapsedRealtime())
                        }
                    })
                } catch (_: RemoteException) { finish(failure("BRIDGE_DISCONNECTED_OUTCOME_UNKNOWN")) }
            }
            override fun onServiceDisconnected(name: ComponentName) { finish(failure("BRIDGE_DISCONNECTED_OUTCOME_UNKNOWN")) }
            override fun onNullBinding(name: ComponentName) { finish(failure("BRIDGE_BINDING_REJECTED")) }
            override fun onBindingDied(name: ComponentName) { finish(failure("BRIDGE_BINDING_DIED_OUTCOME_UNKNOWN")) }
        }
        handler.postDelayed(timeout, 12000)
        try {
            bound = context.bindService(Intent().setComponent(ComponentName(bridgePackage, "$bridgePackage.PocketCommandService")), connection!!, Context.BIND_AUTO_CREATE)
            if(!bound) finish(failure("BRIDGE_COMMAND_SERVICE_UNAVAILABLE"))
        } catch (_: Exception) { finish(failure("BRIDGE_BIND_FAILED")) }
    }
    fun cancel() { finish(failure("COMMAND_CANCELLED_OUTCOME_UNKNOWN")) }
    private fun failure(reason: String) = JSONObject().put("ok", false).put("error", reason).put("canonicalFormulaState", "NOT_EXECUTED")
    private fun finish(result: JSONObject) {
        if(!completion.finish()) return
        handler.removeCallbacks(timeout)
        if(bound) runCatching { context.unbindService(connection!!) }
        bound = false
        callback?.invoke(result)
        callback = null
    }
}
