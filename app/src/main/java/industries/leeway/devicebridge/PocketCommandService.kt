package industries.leeway.devicebridge

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import org.json.JSONObject
import java.util.concurrent.Executors

/** No foreground Activity: a signed, owner-granted Pocket can issue one bounded command. */
class PocketCommandService : Service() {
    private val lane = Executors.newSingleThreadExecutor()
    private val lease = PocketCommandLease()
    private val seen = LinkedHashSet<String>()
    private val ownPackages = setOf("industries.leeway.pocket", "industries.leeway.devicebridge")
    private val messenger by lazy { Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            val reply = message.replyTo ?: return
            val data = message.data
            val id = data.getString("id").orEmpty()
            fun reject(reason: String) {
                ReceiptStore.record(applicationContext, "device.pocket.action", "BLOCKED", "reason=$reason")
                send(reply, id, JSONObject().put("ok", false).put("error", reason))
            }
            // Messenger supplies sendingUid across Binder; caller-provided bundle values are not identity.
            val uid = message.sendingUid
            val packages = packageManager.getPackagesForUid(uid)?.toList().orEmpty()
            val signer = uid >= 0 && packageManager.checkSignatures(uid, Process.myUid()) == PackageManager.SIGNATURE_MATCH
            if (!PocketCommandPolicy.authorized(packages, signer,
                    PocketGrantStore.matches(this@PocketCommandService, data.getString("token")),
                    LocalAuthority.agentAccessEnabled(this@PocketCommandService))) {
                reject("POCKET_COMMAND_NOT_AUTHORIZED"); return
            }
            val action = data.getString("action").orEmpty()
            val label = data.getString("label").orEmpty()
            val sent = data.getLong("sent", 0)
            val age = SystemClock.elapsedRealtime() - sent
            if (!id.matches(Regex("^[a-zA-Z0-9-]{16,80}$")) || !PocketCommandPolicy.valid(action, label) || age !in 0..5000) {
                reject("INVALID_OR_EXPIRED_COMMAND"); return
            }
            if (id in seen) { reject("DUPLICATE_COMMAND"); return }
            if (!lease.acquire()) { reject("COMMAND_BUSY"); return }
            seen.add(id)
            if (seen.size > 128) seen.remove(seen.first())
            lane.execute {
                val result = try { execute(id, action, label, SystemClock.elapsedRealtime() + 8000) }
                    catch (error: Exception) { JSONObject().put("ok", false).put("error", error.javaClass.simpleName) }
                finally { lease.release() }
                ReceiptStore.record(applicationContext, "device.pocket.action", if(result.optBoolean("ok")) "PASS" else "BLOCKED",
                    "id=$id action=$action state=${result.optString("state")} verification=${result.optString("verification")} canonicalFormula=NOT_EXECUTED")
                send(reply, id, result)
            }
        }
    }) }
    override fun onBind(intent: Intent?) = messenger.binder
    override fun onDestroy() { lane.shutdownNow(); super.onDestroy() }

    private fun send(reply: Messenger, id: String, result: JSONObject) {
        result.put("canonicalFormulaState", "NOT_EXECUTED").put("selection", "EXPLICIT_COMMAND_GRAMMAR")
        try { reply.send(Message.obtain().apply { data = Bundle().apply { putString("id", id); putString("result", result.toString()) } }) }
        catch (_: RemoteException) { /* Receipt remains even if Pocket disappeared. */ }
    }
    private fun execute(id: String, action: String, label: String, deadline: Long): JSONObject {
        var step = 0
        fun call(capability: String, args: JSONObject = JSONObject()): JSONObject {
            if (SystemClock.elapsedRealtime() >= deadline || Thread.currentThread().isInterrupted) throw IllegalStateException("COMMAND_DEADLINE")
            return RemoteCommandRouter.execute(applicationContext, "$id-${step++}", capability, args, true)
        }
        fun foreground(): String? {
            val snap = call("device.ui.snapshot")
            if (!snap.optBoolean("ok")) return null
            return snap.optJSONObject("result")?.optJSONObject("tree")?.optString("packageName")
                ?.takeIf { it.isNotBlank() && it !in ownPackages }
        }
        // Pocket must yield its window before any action. No own-UI observation is accepted.
        var before: String? = null
        repeat(20) { if (before == null) { before = foreground(); if(before == null) Thread.sleep(100) } }
        if(before == null) return JSONObject().put("ok", false).put("error", "TARGET_WINDOW_NOT_AVAILABLE")
        var target: String? = null
        val capability = if (action == "open") {
            val listed = call("device.apps.list")
            if (!listed.optBoolean("ok")) return listed
            val apps = listed.optJSONObject("result")?.optJSONArray("apps")
                ?: return JSONObject().put("ok", false).put("error", "APP_LIST_UNAVAILABLE")
            val matches = PocketCommandPolicy.matchingPackages(label, (0 until apps.length()).map {
                val row = apps.getJSONObject(it); row.optString("label") to row.optString("packageName")
            })
            if(matches.size != 1) return JSONObject().put("ok", false).put("error", if(matches.isEmpty()) "APP_NOT_FOUND" else "APP_LABEL_AMBIGUOUS")
            target = matches.single()
            if(target in ownPackages) return JSONObject().put("ok", false).put("error", "OWN_UI_TARGET_BLOCKED")
            "device.apps.launch"
        } else "device.ui.$action"
        val result = call(capability, if(target != null) JSONObject().put("packageName", target) else JSONObject())
        if(!result.optBoolean("ok")) return result
        var after: String? = null
        repeat(15) { if(after == null || (target != null && after != target)) { Thread.sleep(100); after = foreground() } }
        val verified = target != null && after == target
        return JSONObject().put("ok", true).put("action", action).put("label", label)
            .put("state", if(target != null) "LAUNCH_REQUESTED" else "ACTION_ACCEPTED")
            .put("verification", if(verified) "FOREGROUND_PACKAGE_MATCH" else "OUTCOME_NOT_VERIFIED")
            .put("observedPackage", after ?: JSONObject.NULL)
            .put("targetPackage", target ?: JSONObject.NULL)
    }
}
