package industries.leeway.pocket

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VoiceProgressTest {
    @Test fun reportsBoundedDownloadProgressWithoutCopyingRawFields() {
        val result=VoiceProgress.fields(JSONObject().put("status","progress")
            .put("file","onnx/voice.onnx").put("loaded",50).put("total",100)
            .put("text","private turn").put("token","secret").put("audio","samples"))
        assertEquals(50,result.getInt("percent"))
        assertEquals("voice.onnx",result.getString("file"))
        assertFalse(result.has("text"))
        assertFalse(result.has("token"))
        assertFalse(result.has("audio"))
        assertTrue(VoiceProgress.label("PREPARING_VOICE_ONE",result).contains("50%"))
    }
    @Test fun removesUrlsAndLongIdentifiersFromDiagnosticMessages() {
        val safe=VoiceProgress.safe("Failed https://example.com/file?token=private " + "a".repeat(64))
        assertFalse(safe.contains("token=private"))
        assertFalse(safe.contains("a".repeat(64)))
        assertTrue(safe.length<=240)
    }
}
