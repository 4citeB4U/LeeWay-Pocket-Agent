package industries.leeway.pocket

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.json.JSONObject

class LeeWayDeviceService(private val context:Context) {
    fun status():JSONObject=JSONObject()
        .put("authority","INTERNAL_LEEWAY_DEVICE_FABRIC")
        .put("bodyId","phone-fold6")
        .put("commanderProvider","LEEWAY_COMMANDER_ADAPTER")
        .put("externalDeviceBridgeRequired",false)

    fun openSettings():JSONObject = runCatching {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        JSONObject().put("ok",true).put("action","open.settings")
    }.getOrElse{JSONObject().put("ok",false).put("error",it.message)}

    fun openUri(uri:String):JSONObject = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        JSONObject().put("ok",true).put("action","open.uri")
    }.getOrElse{JSONObject().put("ok",false).put("error",it.message)}
}
