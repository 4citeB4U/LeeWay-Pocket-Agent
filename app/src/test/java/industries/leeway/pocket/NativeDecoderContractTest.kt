package industries.leeway.pocket

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

class NativeDecoderContractTest {
    private fun tensor(type:String,dims:List<Int>,bytes:ByteArray)=JSONObject()
        .put("dtype",type).put("dims",JSONArray(dims)).put("data",Base64.getEncoder().encodeToString(bytes))
    private fun valid()=JSONObject()
        .put("speech_tokens",tensor("int64",listOf(1,1),ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(4299).array()))
        .put("speaker_embeddings",tensor("float32",listOf(1,192),ByteArray(192*4)))
        .put("speaker_features",tensor("float32",listOf(1,2,80),ByteArray(2*80*4)))
    @Test fun acceptsOnlyCanonicalTensorContract(){
        val inputs=NativeDecoderContract.decode(valid().toString())
        assertEquals(3,inputs.size)
        assertTrue(inputs.first().int64)
    }
    @Test fun rejectsWrongShapeBeforeReadingTensor(){
        val input=valid();input.getJSONObject("speaker_features").put("dims",JSONArray(listOf(1,80,2)))
        assertThrows(IllegalArgumentException::class.java){NativeDecoderContract.decode(input.toString())}
    }
    @Test fun rejectsTruncationNonfiniteAndOutOfRangeTokens(){
        val short=valid();short.getJSONObject("speaker_embeddings").put("data","AA==")
        assertThrows(IllegalArgumentException::class.java){NativeDecoderContract.decode(short.toString())}
        val nan=valid();val bytes=ByteBuffer.allocate(192*4).order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.NaN).array()
        nan.getJSONObject("speaker_embeddings").put("data",Base64.getEncoder().encodeToString(bytes))
        assertThrows(IllegalArgumentException::class.java){NativeDecoderContract.decode(nan.toString())}
        val tokens=valid();tokens.getJSONObject("speech_tokens").put("data",Base64.getEncoder().encodeToString(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(-1).array()))
        assertThrows(IllegalArgumentException::class.java){NativeDecoderContract.decode(tokens.toString())}
    }
    @Test fun rejectsOversizedAndUnexpectedInputs(){
        assertThrows(IllegalArgumentException::class.java){NativeDecoderContract.decode("x".repeat(NativeDecoderContract.MAX_JSON+1))}
        assertThrows(IllegalArgumentException::class.java){NativeDecoderContract.decode(valid().put("path","untrusted").toString())}
    }
    @Test fun timeoutOrCancelProducesOneResultAndRejectsLateWork(){
        val job=NativeDecoderJob("test")
        job.cancel();assertTrue(job.finish());assertFalse(job.finish())
        assertThrows(IllegalStateException::class.java){job.check()}
    }
}
