/*
REGION: LeeWay Android platform adapter
TAG: LEEWAY-DEVICE-OPERATOR-ANDROID-PACKAGE
WHO: Owner-authorized LeeWay Device Bridge
WHAT: Download, hash-verify, stage and hand an APK to Android's authorized installer.
WHEN: A governed device.apps.install command supplies HTTPS source + expected SHA-256.
WHERE: Native Android runtime inside canonical LEEWAY-DEVICE-BRIDGE.
WHY: Make package delivery deterministic without claiming silent/system install privilege.
HOW: HTTPS -> private cache -> SHA-256 -> FileProvider -> Android package installer UI -> receipt.
LICENSE: MIT
*/
package industries.leeway.devicebridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object PackageInstallBroker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun status(context: Context): JSONObject = JSONObject().apply {
        put("ok", true)
        put("canRequestPackageInstalls", context.packageManager.canRequestPackageInstalls())
        put("authority", "ANDROID_USER_AUTHORIZED_INSTALLER")
    }

    fun installFromUrl(context: Context, url: String, expectedSha256: String): JSONObject {
        if (!url.startsWith("https://")) return blocked("HTTPS_REQUIRED")
        val expected = expectedSha256.trim().lowercase()
        if (!expected.matches(Regex("^[a-f0-9]{64}$"))) return blocked("INVALID_SHA256")

        val dir = File(context.cacheDir, "device-operator-packages").apply { mkdirs() }
        val target = File(dir, "leeway-package-${System.currentTimeMillis()}.apk")
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return blocked("DOWNLOAD_HTTP_${response.code}")
            val body = response.body ?: return blocked("EMPTY_DOWNLOAD")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }

        val actual = sha256(target)
        if (actual != expected) {
            target.delete()
            ReceiptStore.record(context, "device.apps.install", "FAIL", "SHA256 mismatch")
            return JSONObject().put("ok", false).put("error", "SHA256_MISMATCH")
                .put("expectedSha256", expected).put("actualSha256", actual)
        }

        if (!context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            ReceiptStore.record(context, "device.apps.install", "BLOCKED", "Owner authorization required for package installs")
            return JSONObject().put("ok", false).put("error", "OWNER_INSTALL_AUTHORIZATION_REQUIRED")
                .put("sha256", actual)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            target
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(install)
        ReceiptStore.record(context, "device.apps.install.handoff", "PASS", "Verified APK handed to Android installer sha256=$actual; installation not yet verified")
        return JSONObject().put("ok", true)
            .put("state", "INSTALLER_OPENED")
            .put("sha256", actual)
            .put("fileBytes", target.length())
            .put("silentInstall", false)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 64)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun blocked(reason: String) =
        JSONObject().put("ok", false).put("error", reason)
}
