package industries.leeway.devicebridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

object ReceiptStore {
    private const val PREFS="leeway_device_bridge"
    private const val KEY="local_receipts"

    fun record(context: Context, capability: String, result: String, detail: String = ""): JSONObject {
        val receipt=JSONObject().apply {
            put("receiptId", UUID.randomUUID().toString())
            put("timestamp", Instant.now().toString())
            put("capability", capability)
            put("result", result)
            put("detail", detail)
            put("authority", "LOCAL_DEVICE_RUNTIME")
        }
        val prefs=context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list=JSONArray(prefs.getString(KEY,"[]"))
        list.put(receipt)
        prefs.edit().putString(KEY,list.toString()).apply()
        return receipt
    }

    fun list(context: Context): JSONArray =
        JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY,"[]"))
}
