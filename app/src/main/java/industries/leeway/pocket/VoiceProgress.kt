package industries.leeway.pocket

import org.json.JSONObject

/** Only bounded runtime metadata enters diagnostics, never turn text or raw callbacks. */
object VoiceProgress {
    fun safe(value: String): String = value
        .replace(Regex("https?://\\S+"), "[url]")
        .replace(Regex("[A-Za-z0-9_=-]{48,}"), "[identifier]")
        .replace(Regex("[\\r\\n\\t]+"), " ").take(240)

    fun fields(raw: JSONObject): JSONObject = JSONObject().apply {
        put("status", safe(raw.optString("status")))
        put("message", safe(raw.optString("message")))
        put("file", safe(raw.optString("file").substringAfterLast('/').substringAfterLast('\\')))
        val loaded = raw.optDouble("loaded", -1.0)
        val total = raw.optDouble("total", -1.0)
        if (loaded.isFinite() && loaded >= 0) put("loadedBytes", loaded.toLong())
        if (total.isFinite() && total > 0) put("totalBytes", total.toLong())
        val percent = raw.optDouble("progress", if (total > 0 && loaded >= 0) loaded * 100 / total else -1.0)
        if (percent.isFinite() && percent >= 0) put("percent", percent.coerceIn(0.0, 100.0).toInt())
    }

    fun label(state: String, progress: JSONObject): String {
        if (state != "PREPARING_VOICE_ONE") return state.replace('_', ' ')
        val file = progress.optString("file")
        val percent = progress.optInt("percent", -1)
        return "Preparing Voice One" + (if (percent >= 0) ": $percent%" else "") +
            (if (file.isNotBlank()) "\n$file" else "") +
            (if (progress.optString("message").isNotBlank()) "\n" + progress.optString("message") else "") +
            (if (progress.optString("status") == "done") "\nLoading model into memory" else "")
    }
}
