/* REGION: LEEWAY.UI.OWNER_RECORDS
TAG: READ_ONLY_EXISTING_OWNER_RECORD_INDEX
WHO: owner; WHAT: expose existing on-device receipt categories and current Android permissions.
WHY: menu must not imply PC receipts are available on a disconnected phone.
HOW: bounded local listing and Android permission checks; no new state store. LICENSE: MIT */
package industries.leeway.pocket

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal object OwnerRecordsMenu {
    fun evidence(context: Context): String {
        val groups = JSONArray()
        val roots = listOf(
            "App files" to context.filesDir,
            "App databases" to context.getDatabasePath("placeholder.db").parentFile,
            "App no-backup" to context.noBackupFilesDir
        )
        for ((label, root) in roots) {
            if (root == null || !root.isDirectory) continue
            val children = root.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.take(30) ?: emptyList()
            for (folder in children) {
                val candidates = folder.listFiles()?.filter { it.isFile && (it.extension.lowercase() in setOf("json","jsonl","receipt","txt","db")) }
                    ?.sortedByDescending { it.lastModified() }?.take(12) ?: emptyList()
                groups.put(JSONObject().put("category","$label / ${folder.name}")
                    .put("observedFiles", candidates.size)
                    .put("recent", JSONArray().apply {
                        for (file in candidates) put(JSONObject().put("name",file.name)
                            .put("bytes",file.length()).put("modifiedAtMs",file.lastModified()))
                    }))
            }
        }
        return JSONObject().put("schema","leeway.owner.evidence.index.v1")
            .put("scope","ANDROID_APP_PRIVATE_LOCAL_ONLY").put("readOnly",true)
            .put("groups",groups)
            .put("limitations",JSONArray().put("PC receipts require an authenticated PC owner gateway")
                .put("Only existing app-private categories are listed")
                .put("No global or nested-device inventory implied")).toString()
    }
    fun authority(context: Context): String {
        val permissions=JSONArray()
        for ((label, perm) in listOf(
            "Microphone" to Manifest.permission.RECORD_AUDIO,
            "Camera" to Manifest.permission.CAMERA,
            "Location" to Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            permissions.put(JSONObject().put("label",label)
                .put("granted",context.checkSelfPermission(perm)==PackageManager.PERMISSION_GRANTED))
        }
        permissions.put(JSONObject().put("label","Floating overlay")
            .put("granted",Settings.canDrawOverlays(context)))
        return JSONObject().put("scope","ANDROID_CURRENT_DEVICE")
            .put("permissions",permissions).put("readOnly",true).toString()
    }
}
