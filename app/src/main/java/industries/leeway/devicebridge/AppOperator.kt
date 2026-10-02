/*
REGION: LeeWay Android platform adapter
TAG: LEEWAY-DEVICE-OPERATOR-ANDROID-APPS
WHO: Owner-authorized LeeWay Device Bridge
WHAT: Launch installed applications through Android package manager.
WHEN: A governed device.apps.launch command names a package.
WHERE: Native Android runtime inside canonical LEEWAY-DEVICE-BRIDGE.
WHY: Provide real app launch capability rather than a capability-only claim.
HOW: PackageManager launch intent -> startActivity -> receipt.
LICENSE: MIT
*/
package industries.leeway.devicebridge

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

object AppOperator {
    fun listLaunchable(context: Context): JSONObject {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = context.packageManager.queryIntentActivities(intent, 0)
            .sortedBy { it.loadLabel(context.packageManager).toString().lowercase() }
        val apps = JSONArray()
        for (info in matches) {
            apps.put(JSONObject().apply {
                put("packageName", info.activityInfo.packageName)
                put("activityName", info.activityInfo.name)
                put("label", info.loadLabel(context.packageManager).toString())
            })
        }
        return JSONObject().put("ok", true).put("count", apps.length()).put("apps", apps)
    }

    private val packageNamePattern = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

    fun launch(context: Context, packageName: String): JSONObject {
        val clean = packageName.trim()
        if (!packageNamePattern.matches(clean)) {
            return JSONObject().put("ok", false).put("error", "INVALID_PACKAGE_NAME")
        }
        val intent = context.packageManager.getLaunchIntentForPackage(clean)
            ?: return JSONObject().put("ok", false).put("error", "PACKAGE_NOT_LAUNCHABLE")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ReceiptStore.record(context, "device.apps.launch", "PASS", "Launch request dispatched package=$clean")
        return JSONObject().put("ok", true)
            .put("state", "LAUNCH_REQUESTED")
            .put("packageName", clean)
    }
}
