/*
REGION: LeeWay Android runtime update surface
TAG: LEEWAY-AGENT-LEE-ONE-TAP-UPDATE
WHO: Owner-authorized LeeWay Device Bridge
WHAT: Check verified release metadata and hand a newer Agent Lee APK to Android's installer.
WHEN: Owner presses UPDATE AGENT LEE.
WHERE: Canonical LEEWAY-DEVICE-BRIDGE Android runtime.
WHY: Replace repeated manual download/bootstrap cycles with one governed update action.
HOW: HTTPS metadata -> version compare -> SHA-256-qualified PackageInstallBroker -> Android installer.
LICENSE: MIT
*/
package industries.leeway.devicebridge

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

object AgentLeeUpdate {
    const val METADATA_URL =
        "https://raw.githubusercontent.com/4citeB4U/LeeWay-Pocket-Agent/main/docs/download/LeeWay-Agent-Lee-latest.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun current(context: Context): JSONObject = JSONObject().apply {
        put("ok", true)
        put("packageName", context.packageName)
        put("versionName", BuildConfig.VERSION_NAME)
        put("versionCode", BuildConfig.VERSION_CODE)
        put("metadataUrl", METADATA_URL)
        put("authority", "LEEWAY_SELF_UPDATE")
    }
    fun check(context: Context): JSONObject {
        return try {
            val request = Request.Builder()
                .url(METADATA_URL)
                .header("Cache-Control", "no-cache")
                .build()
            val metadata = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return blocked("UPDATE_METADATA_HTTP_" + response.code)
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return blocked("UPDATE_METADATA_EMPTY")
                JSONObject(body)
            }

            val availableName = metadata.optString("versionName").trim()
            val availableCode = metadata.optInt("versionCode", -1)
            val sha256 = metadata.optString("sha256").trim().lowercase()
            val rawDownload = metadata.optString("downloadUrl").trim()
            if (availableName.isBlank() || availableCode < 1) return blocked("UPDATE_METADATA_VERSION_INVALID")
            if (!sha256.matches(Regex("^[a-f0-9]{64}$"))) return blocked("UPDATE_METADATA_SHA256_INVALID")
            val downloadUrl = resolveDownloadUrl(rawDownload)
                ?: return blocked("UPDATE_METADATA_URL_INVALID")
            val updateAvailable = availableCode > BuildConfig.VERSION_CODE

            JSONObject().apply {
                put("ok", true)
                put("currentVersionName", BuildConfig.VERSION_NAME)
                put("currentVersionCode", BuildConfig.VERSION_CODE)
                put("availableVersionName", availableName)
                put("availableVersionCode", availableCode)
                put("updateAvailable", updateAvailable)
                put("downloadUrl", downloadUrl)
                put("sha256", sha256)
                put("sourceRunId", metadata.optString("sourceRunId"))
                put("sourceCommit", metadata.optString("sourceCommit"))
                put("state", if (updateAvailable) "UPDATE_AVAILABLE" else "CURRENT_OR_NEWER")
            }
        } catch (error: Exception) {
            blocked("UPDATE_CHECK_FAILED:" + error.javaClass.simpleName)
        }
    }
    fun checkAndInstall(context: Context): JSONObject {
        val checked = check(context)
        if (!checked.optBoolean("ok")) {
            ReceiptStore.record(
                context,
                "agent.update.check",
                "BLOCKED",
                checked.optString("error", "UPDATE_CHECK_FAILED")
            )
            return checked
        }

        ReceiptStore.record(
            context,
            "agent.update.check",
            "PASS",
            "current=" + checked.optInt("currentVersionCode") +
                " available=" + checked.optInt("availableVersionCode")
        )

        if (!checked.optBoolean("updateAvailable")) {
            return checked.put("installState", "NO_UPDATE_REQUIRED")
        }

        val install = PackageInstallBroker.installFromUrl(
            context,
            checked.getString("downloadUrl"),
            checked.getString("sha256")
        )
        return JSONObject().apply {
            put("ok", install.optBoolean("ok"))
            put("state", checked.optString("state"))
            put("currentVersionName", checked.optString("currentVersionName"))
            put("currentVersionCode", checked.optInt("currentVersionCode"))
            put("availableVersionName", checked.optString("availableVersionName"))
            put("availableVersionCode", checked.optInt("availableVersionCode"))
            put("installer", install)
            if (!install.optBoolean("ok")) {
                put("error", install.optString("error", "UPDATE_INSTALLER_BLOCKED"))
            }
        }
    }
    private fun resolveDownloadUrl(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val resolved = URI(METADATA_URL).resolve(raw).toString()
            resolved.takeIf { it.startsWith("https://") }
        }.getOrNull()
    }

    private fun blocked(reason: String): JSONObject =
        JSONObject().put("ok", false).put("error", reason)
}
