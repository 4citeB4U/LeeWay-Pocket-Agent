/*
REGION: LEEWAY.UPDATE.RUNTIME
TAG: LEEWAY.UPDATE.STANDARD.V1.ANDROID
WHO: LeeWay Industries / Creator
WHAT: Opt-in automatic GitHub update retrieval with verified private staging and human-approved apply.
WHEN: Startup, periodic background work, or manual update check.
WHERE: Unified LeeWay Agent Lee Android runtime.
WHY: Implement LEEWAY-UPDATE-v1.0 without silent installation or duplicate installer clutter.
HOW: GitHub manifest -> qualify -> download -> SHA/signer/package verify -> private stage -> notify -> human apply.
LICENSE: MIT
*/
package industries.leeway.devicebridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object AgentLeeUpdate {
    const val STANDARD = "LEEWAY-UPDATE-v1.0"
    const val CANONICAL_REPOSITORY = "4citeB4U/LeeWay-Pocket-Agent"
    const val METADATA_URL =
        "https://raw.githubusercontent.com/4citeB4U/LeeWay-Pocket-Agent/main/docs/download/LeeWay-Agent-Lee-latest.json"
    const val ACTION_SHOW_READY = "LEEWAY_SHOW_STAGED_UPDATE"

    private const val PREFS = "leeway-update-standard-v1"
    private const val KEY_AUTO = "automatic_retrieval_enabled"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val KEY_READY_CODE = "ready_version_code"
    private const val KEY_READY_NAME = "ready_version_name"
    private const val KEY_READY_SHA = "ready_sha256"
    private const val KEY_READY_SIGNER = "ready_signer_sha256"
    private const val KEY_READY_COMMIT = "ready_source_commit"
    private const val READY_FILE = "LeeWay-Agent-Lee-ready.apk"
    private const val CHANNEL = "leeway_updates"
    private const val NOTICE_ID = 5411
    private const val PERIODIC_WORK = "leeway-agent-lee-update-periodic"
    private const val IMMEDIATE_WORK = "leeway-agent-lee-update-immediate"
    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val MAX_APK_BYTES = 600L * 1024L * 1024L

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun stageDir(context: Context) =
        File(context.filesDir, "updates").apply { mkdirs() }

    private fun stagedFile(context: Context) = File(stageDir(context), READY_FILE)

    fun automaticRetrievalEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO, false)

    fun setAutomaticRetrieval(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO, enabled).apply()
        syncSchedule(context.applicationContext, enqueueImmediate = enabled)
        ReceiptStore.record(
            context.applicationContext,
            "agent.update.preference",
            "PASS",
            "automaticRetrievalEnabled=$enabled applyRequiresUserApproval=true"
        )
    }

    fun current(context: Context): JSONObject = JSONObject().apply {
        put("ok", true)
        put("standard", STANDARD)
        put("packageName", context.packageName)
        put("versionName", BuildConfig.VERSION_NAME)
        put("versionCode", BuildConfig.VERSION_CODE)
        put("metadataUrl", METADATA_URL)
        put("automaticRetrievalEnabled", automaticRetrievalEnabled(context))
        put("applyRequiresUserApproval", true)
        put("ready", ready(context))
        put("authority", "LEEWAY_SELF_UPDATE")
    }

    fun ready(context: Context): JSONObject {
        val p = prefs(context)
        val code = p.getInt(KEY_READY_CODE, -1)
        val file = stagedFile(context)
        val isReady = code > BuildConfig.VERSION_CODE && file.isFile
        return JSONObject().apply {
            put("ok", true)
            put("state", if (isReady) "READY_FOR_APPROVAL" else "NO_STAGED_UPDATE")
            put("ready", isReady)
            put("versionCode", code)
            put("versionName", p.getString(KEY_READY_NAME, "").orEmpty())
            put("sha256", p.getString(KEY_READY_SHA, "").orEmpty())
            put("signerSha256", p.getString(KEY_READY_SIGNER, "").orEmpty())
            put("sourceCommit", p.getString(KEY_READY_COMMIT, "").orEmpty())
            put("fileBytes", if (file.isFile) file.length() else 0L)
            put("applyRequiresUserApproval", true)
        }
    }

    fun check(context: Context): JSONObject {
        return try {
            val response = client.newCall(
                Request.Builder().url(METADATA_URL).header("Cache-Control", "no-cache").build()
            ).execute()
            val metadata = response.use {
                if (!it.isSuccessful) return blocked("UPDATE_METADATA_HTTP_" + it.code)
                val body = it.body?.string().orEmpty()
                if (body.isBlank()) return blocked("UPDATE_METADATA_EMPTY")
                JSONObject(body)
            }

            if (metadata.optString("standard") != STANDARD) return blocked("UPDATE_STANDARD_MISMATCH")
            if (metadata.optString("canonicalRepository") != CANONICAL_REPOSITORY) {
                return blocked("UPDATE_REPOSITORY_MISMATCH")
            }
            if (metadata.optString("applicationId") != context.packageName) {
                return blocked("UPDATE_APPLICATION_ID_MISMATCH")
            }

            val availableName = metadata.optString("versionName").trim()
            val availableCode = metadata.optInt("versionCode", -1)
            val sha256 = metadata.optString("sha256").trim().lowercase()
            val rawDownload = metadata.optString("downloadUrl").trim()
            val artifactBytes = metadata.optLong("artifactBytes", -1L)
            val signerSha256 = metadata.optString("signerSha256").trim().lowercase()
            if (metadata.optString("channel") != "stable") return blocked("UPDATE_CHANNEL_MISMATCH")
            if (availableName.isBlank() || availableCode < 1) return blocked("UPDATE_METADATA_VERSION_INVALID")
            if (!sha256.matches(Regex("^[a-f0-9]{64}$"))) return blocked("UPDATE_METADATA_SHA256_INVALID")
            if (!signerSha256.matches(Regex("^[a-f0-9]{64}$"))) return blocked("UPDATE_METADATA_SIGNER_INVALID")
            if (artifactBytes > MAX_APK_BYTES) return blocked("UPDATE_ARTIFACT_TOO_LARGE")
            val downloadUrl = resolveDownloadUrl(rawDownload)
                ?: return blocked("UPDATE_METADATA_URL_INVALID")

            JSONObject().apply {
                put("ok", true)
                put("standard", STANDARD)
                put("currentVersionName", BuildConfig.VERSION_NAME)
                put("currentVersionCode", BuildConfig.VERSION_CODE)
                put("availableVersionName", availableName)
                put("availableVersionCode", availableCode)
                put("updateAvailable", availableCode > BuildConfig.VERSION_CODE)
                put("downloadUrl", downloadUrl)
                put("sha256", sha256)
                put("signerSha256", signerSha256)
                put("artifactBytes", artifactBytes)
                put("sourceRunId", metadata.optString("sourceRunId"))
                put("sourceCommit", metadata.optString("sourceCommit"))
                put("state", if (availableCode > BuildConfig.VERSION_CODE) "UPDATE_AVAILABLE" else "CURRENT")
            }
        } catch (error: Exception) {
            blocked("UPDATE_CHECK_FAILED:" + error.javaClass.simpleName)
        }
    }

    fun checkAndStage(context: Context, force: Boolean = false): JSONObject {
        val app = context.applicationContext
        if (!force && !automaticRetrievalEnabled(app)) {
            return JSONObject().put("ok", true).put("state", "DISABLED")
        }
        val p = prefs(app)
        val now = System.currentTimeMillis()
        if (!force && now - p.getLong(KEY_LAST_CHECK, 0L) < CHECK_INTERVAL_MS) {
            val currentReady = ready(app)
            return currentReady.put(
                "state",
                if (currentReady.optBoolean("ready")) "READY_FOR_APPROVAL" else "CHECK_NOT_DUE"
            )
        }
        val checked = check(app)
        ReceiptStore.record(
            app,
            "agent.update.check",
            if (checked.optBoolean("ok")) "PASS" else "BLOCKED",
            if (checked.optBoolean("ok"))
                "current=${BuildConfig.VERSION_CODE} available=${checked.optInt("availableVersionCode")}"
            else checked.optString("error", "UPDATE_CHECK_FAILED")
        )
        if (!checked.optBoolean("ok")) return checked
        p.edit().putLong(KEY_LAST_CHECK, now).apply()
        if (!checked.optBoolean("updateAvailable")) {
            clearReadyIfCurrent(app)
            return checked
        }

        val existing = ready(app)
        if (existing.optBoolean("ready") &&
            existing.optInt("versionCode") == checked.optInt("availableVersionCode") &&
            existing.optString("sha256") == checked.optString("sha256") &&
            verifyStaged(app, stagedFile(app), checked).optBoolean("ok")) {
            return existing
        }
        return downloadVerifyAndStage(app, checked)
    }

    private fun downloadVerifyAndStage(context: Context, checked: JSONObject): JSONObject {
        val dir = stageDir(context)
        val part = File(dir, READY_FILE + ".part")
        val final = stagedFile(context)
        part.delete()
        val request = Request.Builder()
            .url(checked.getString("downloadUrl"))
            .header("Cache-Control", "no-cache")
            .build()

        val downloaded = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return blocked("UPDATE_DOWNLOAD_HTTP_" + response.code)
                val body = response.body ?: return blocked("UPDATE_DOWNLOAD_EMPTY")
                val declared = body.contentLength()
                if (declared > MAX_APK_BYTES) return blocked("UPDATE_ARTIFACT_TOO_LARGE")
                var total = 0L
                val digest = MessageDigest.getInstance("SHA-256")
                body.byteStream().use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count <= 0) break
                            total += count
                            if (total > MAX_APK_BYTES) {
                                part.delete()
                                return blocked("UPDATE_ARTIFACT_TOO_LARGE")
                            }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                        output.flush()
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                JSONObject().put("bytes", total).put("sha256", hash)
            }
        } catch (error: Exception) {
            part.delete()
            return blocked("UPDATE_DOWNLOAD_FAILED:" + error.javaClass.simpleName)
        }

        val expected = checked.getString("sha256")
        if (downloaded.getString("sha256") != expected) {
            part.delete()
            return blocked("UPDATE_SHA256_MISMATCH")
        }
        val expectedBytes = checked.optLong("artifactBytes", -1L)
        if (expectedBytes > 0 && downloaded.getLong("bytes") != expectedBytes) {
            part.delete()
            return blocked("UPDATE_SIZE_MISMATCH")
        }

        val qualified = verifyStaged(context, part, checked)
        if (!qualified.optBoolean("ok")) {
            part.delete()
            return qualified
        }

        final.delete()
        if (!part.renameTo(final)) {
            part.copyTo(final, overwrite = true)
            part.delete()
        }

        prefs(context).edit()
            .putInt(KEY_READY_CODE, checked.getInt("availableVersionCode"))
            .putString(KEY_READY_NAME, checked.getString("availableVersionName"))
            .putString(KEY_READY_SHA, expected)
            .putString(KEY_READY_SIGNER, checked.getString("signerSha256"))
            .putString(KEY_READY_COMMIT, checked.optString("sourceCommit"))
            .apply()

        val result = ready(context)
        ReceiptStore.record(
            context,
            "agent.update.stage",
            "PASS",
            "version=${checked.getInt("availableVersionCode")} sha256=$expected state=READY_FOR_APPROVAL"
        )
        notifyReady(context, checked.getString("availableVersionName"))
        return result
    }

    @Suppress("DEPRECATION")
    private fun verifyStaged(context: Context, file: File, checked: JSONObject): JSONObject {
        if (!file.isFile) return blocked("UPDATE_STAGED_FILE_MISSING")
        if (sha256(file) != checked.getString("sha256")) return blocked("UPDATE_STAGED_HASH_INVALID")
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: return blocked("UPDATE_ARCHIVE_INVALID")
        if (archive.packageName != context.packageName) return blocked("UPDATE_ARCHIVE_PACKAGE_MISMATCH")
        if (archive.longVersionCode != checked.getInt("availableVersionCode").toLong()) {
            return blocked("UPDATE_ARCHIVE_VERSION_MISMATCH")
        }
        val current = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val expectedSigners = signerDigests(current)
        val observedSigners = signerDigests(archive)
        if (expectedSigners.isEmpty() || observedSigners.isEmpty() ||
            expectedSigners.intersect(observedSigners).isEmpty()) {
            return blocked("UPDATE_SIGNER_MISMATCH")
        }
        val manifestSigner = checked.optString("signerSha256").trim().lowercase()
        if (manifestSigner.isNotBlank() && manifestSigner !in observedSigners) {
            return blocked("UPDATE_METADATA_SIGNER_MISMATCH")
        }
        return JSONObject().put("ok", true)
            .put("packageName", archive.packageName)
            .put("versionCode", archive.longVersionCode)
            .put("signerSha256", observedSigners.first())
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signing = info.signingInfo ?: return emptySet()
        val certs = if (signing.hasMultipleSigners()) {
            signing.apkContentsSigners
        } else {
            signing.signingCertificateHistory
        }
        return certs.map { cert ->
            MessageDigest.getInstance("SHA-256")
                .digest(cert.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    fun requestApply(context: Context): JSONObject {
        val app = context.applicationContext
        val staged = ready(app)
        if (!staged.optBoolean("ready")) return blocked("UPDATE_NOT_READY_FOR_APPROVAL")
        val file = stagedFile(app)
        val checked = JSONObject()
            .put("sha256", staged.getString("sha256"))
            .put("signerSha256", staged.getString("signerSha256"))
            .put("availableVersionCode", staged.getInt("versionCode"))
        val qualified = verifyStaged(app, file, checked)
        if (!qualified.optBoolean("ok")) return qualified

        ReceiptStore.record(
            app,
            "agent.update.approval",
            "PASS",
            "Human approved apply version=${staged.getInt("versionCode")}"
        )

        if (!app.packageManager.canRequestPackageInstalls()) {
            app.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${app.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return blocked("OWNER_INSTALL_AUTHORIZATION_REQUIRED")
        }

        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        app.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        ReceiptStore.record(
            app,
            "agent.update.apply.handoff",
            "PASS",
            "Verified staged update handed to Android installer; installation not yet verified"
        )
        return JSONObject().put("ok", true)
            .put("state", "APPLYING")
            .put("installerOpened", true)
            .put("installationVerified", false)
    }

    fun checkAndInstall(context: Context): JSONObject {
        val staged = checkAndStage(context, force = true)
        return if (staged.optBoolean("ready")) requestApply(context) else staged
    }

    @Suppress("DEPRECATION")
    fun reconcileApplied(context: Context): JSONObject {
        val app = context.applicationContext
        val staged = ready(app)
        val target = staged.optInt("versionCode", -1)
        if (target !in 1..BuildConfig.VERSION_CODE) return staged

        val installed = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val installedSigners = signerDigests(installed)
        val expectedSigner = staged.optString("signerSha256").trim().lowercase()
        if (expectedSigner.isNotBlank() && expectedSigner !in installedSigners) {
            val failure = blocked("POST_APPLY_SIGNER_MISMATCH")
                .put("state", "VERIFYING_APPLIED")
            ReceiptStore.record(
                app,
                "agent.update.verify.applied",
                "BLOCKED",
                "installedVersion=${BuildConfig.VERSION_CODE} signerMismatch=true"
            )
            return failure
        }

        val runtimeEnabled = LocalAuthority.agentAccessEnabled(app)
        val health = if (runtimeEnabled) {
            runCatching {
                if (!LocalBridgeServer.isRunning()) LocalBridgeServer.start(app)
                BridgeSelfTest.run(app)
            }.getOrElse { error ->
                JSONObject().put("ok", false)
                    .put("error", "POST_APPLY_HEALTH_EXCEPTION:" + error.javaClass.simpleName)
            }
        } else {
            JSONObject().put("ok", true)
                .put("scope", "APP_STARTUP_ONLY")
                .put("runtime", "OWNER_DISABLED")
        }

        if (!health.optBoolean("ok")) {
            ReceiptStore.record(
                app,
                "agent.update.verify.applied",
                "BLOCKED",
                "installedVersion=${BuildConfig.VERSION_CODE} runtimeHealth=false"
            )
            return JSONObject().put("ok", false)
                .put("state", "VERIFYING_APPLIED")
                .put("health", health)
        }

        clearReady(app)
        ReceiptStore.record(
            app,
            "agent.update.verify.applied",
            "PASS",
            "installedVersion=${BuildConfig.VERSION_CODE} stagedVersion=$target runtimeEnabled=$runtimeEnabled"
        )
        return JSONObject().put("ok", true)
            .put("state", "PASS")
            .put("health", health)
    }

    fun syncSchedule(context: Context, enqueueImmediate: Boolean = false) {
        val app = context.applicationContext
        val manager = WorkManager.getInstance(app)
        if (!automaticRetrievalEnabled(app)) {
            manager.cancelUniqueWork(PERIODIC_WORK)
            manager.cancelUniqueWork(IMMEDIATE_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<AgentLeeUpdateWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
        manager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        if (enqueueImmediate) {
            val immediate = OneTimeWorkRequestBuilder<AgentLeeUpdateWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()
            manager.enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, immediate)
        }
    }

    private fun notifyReady(context: Context, versionName: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    "LeeWay updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Verified LeeWay updates waiting for owner approval"
                }
            )
        }
        val intent = Intent().setClassName(
            context.packageName,
            "industries.leeway.pocket.MainActivity"
        ).putExtra("leeway_action", ACTION_SHOW_READY)
        val pending = PendingIntent.getActivity(
            context,
            NOTICE_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            NOTICE_ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Agent Lee update ready")
                .setContentText("$versionName is verified and ready for your approval.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun clearReadyIfCurrent(context: Context) {
        if (prefs(context).getInt(KEY_READY_CODE, -1) <= BuildConfig.VERSION_CODE) {
            clearReady(context)
        }
    }

    private fun clearReady(context: Context) {
        stagedFile(context).delete()
        prefs(context).edit()
            .remove(KEY_READY_CODE)
            .remove(KEY_READY_NAME)
            .remove(KEY_READY_SHA)
            .remove(KEY_READY_SIGNER)
            .remove(KEY_READY_COMMIT)
            .apply()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun resolveDownloadUrl(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            URI(METADATA_URL).resolve(raw).toString().takeIf {
                it.startsWith("https://github.com/4citeB4U/LeeWay-Pocket-Agent/")
            }
        }.getOrNull()
    }

    private fun blocked(reason: String): JSONObject =
        JSONObject().put("ok", false).put("state", "BLOCKED").put("error", reason)
}
