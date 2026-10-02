package industries.leeway.devicebridge

import android.content.Context
import android.os.BatteryManager
import org.json.JSONObject

object Diagnostics {
    fun snapshot(context: Context): JSONObject {
        val battery=context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return JSONObject().apply {
            put("authority","LOCAL_DEVICE_RUNTIME")
            put("batteryPercent",battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            put("chargeCounter",battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
            put("currentNow",battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
            put("energyCounter",battery.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER))
            put("passport",DevicePassport.capture(context))
        }
    }
}
