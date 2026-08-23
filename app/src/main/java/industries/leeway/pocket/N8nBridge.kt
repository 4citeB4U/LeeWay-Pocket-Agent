package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlin.concurrent.thread

class N8nBridge(context: Context) {
    private val prefs = context.getSharedPreferences("leeway-pocket-automation", Context.MODE_PRIVATE)

    data class Result(val ok: Boolean, val message: String)

    fun isConfigured(): Boolean = endpoint().startsWith("http")
    fun endpoint(): String = prefs.getString("n8n_endpoint", "").orEmpty().trim()
    fun token(): String = prefs.getString("n8n_token", "").orEmpty().trim()

    fun configure(endpoint: String, token: String) {
        prefs.edit()
            .putString("n8n_endpoint", endpoint.trim())
            .putString("n8n_token", token.trim())
            .apply()
    }

    fun dispatch(action: String, request: String, callback: (Result) -> Unit) {
        if (!isConfigured()) {
            callback(Result(false, "n8n automation bridge is not configured."))
            return
        }

        thread {
            val result = try {
                val connection = (URL(endpoint()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    if (token().isNotBlank()) setRequestProperty("Authorization", "Bearer ${token()}")
                }

                val body = JSONObject()
                    .put("source", "leeway-pocket-agent")
                    .put("agent", "Agent Lee")
                    .put("action", action)
                    .put("request", request)
                    .put("timestamp", Instant.now().toString())
                    .toString()

                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()

                if (code in 200..299) {
                    val message = try {
                        JSONObject(response).optString("message").ifBlank { "Automation completed." }
                    } catch (_: Exception) {
                        response.ifBlank { "Automation completed." }
                    }
                    Result(true, message)
                } else {
                    Result(false, "Automation returned HTTP $code${if (response.isNotBlank()) ": $response" else ""}")
                }
            } catch (e: Exception) {
                Result(false, e.message ?: "Automation request failed.")
            }

            callback(result)
        }
    }
}
