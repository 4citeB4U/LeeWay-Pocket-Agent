package industries.leeway.devicebridge

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

object BluetoothProvider {
    const val CAPABILITY = "device.bluetooth.list-bonded"

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            emptyArray()
        }

    fun hasRequiredPermissions(context: Context): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun snapshot(context: Context): JSONObject {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        val supported = adapter != null
        val authorized = hasRequiredPermissions(context)
        val enabled = if (supported && authorized) adapter.isEnabled else false
        val devices = JSONArray()

        if (supported && authorized) {
            adapter.bondedDevices
                .sortedBy { it.name ?: it.address }
                .forEach { device ->
                    devices.put(JSONObject().apply {
                        put("name", device.name ?: "UNKNOWN")
                        put("address", device.address)
                        put("bondState", device.bondState)
                        put("type", device.type)
                    })
                }
        }

        return JSONObject().apply {
            put("providerId", "bluetooth")
            put("capability", CAPABILITY)
            put("supported", supported)
            put("available", supported && enabled)
            put("authorized", authorized)
            put("active", enabled)
            put("healthy", supported && enabled)
            put("verified", supported && authorized)
            put("adapterName", if (supported && authorized) adapter.name ?: "UNKNOWN" else "REDACTED_UNTIL_AUTHORIZED")
            put("bondedDeviceCount", devices.length())
            put("bondedDevices", devices)
            put("authority", "PHONE_LOCAL_ANDROID_BLUETOOTH")
        }
    }
}
