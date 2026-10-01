package industries.leeway.pocket

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

/** Fixed tensors only: no caller-selected graph, path, provider or executable. */
object NativeDecoderContract {
    const val MAX_JSON = 2_000_000
    const val MAX_SAMPLES = 24_000 * 30
    data class Input(val name:String, val dims:LongArray, val bytes:ByteArray, val int64:Boolean)
    fun decode(json:String):List<Input> {
        require(json.length <= MAX_JSON) { "DECODER_INPUT_TOO_LARGE" }
        val root=JSONObject(json)
        val names=setOf("speech_tokens","speaker_embeddings","speaker_features")
        require(root.keys().asSequence().toSet()==names) { "DECODER_INPUT_NAMES" }
        return names.map { name ->
            val value=root.getJSONObject(name)
            val shape=value.getJSONArray("dims")
            val dims=LongArray(shape.length()) { shape.getLong(it) }
            val valid=when(name){
                "speech_tokens" -> dims.size==2 && dims[0]==1L && dims[1] in 1L..2048L
                "speaker_embeddings" -> dims.contentEquals(longArrayOf(1,192))
                else -> dims.size==3 && dims[0]==1L && dims[1] in 1L..3000L && dims[2]==80L
            }
            require(valid) { "DECODER_INPUT_SHAPE" }
            val int64=name=="speech_tokens"
            require(value.getString("dtype")==if(int64)"int64" else "float32") { "DECODER_INPUT_TYPE" }
            val count=dims.fold(1L){a,b->a*b}
            val encoded=value.getString("data")
            require(encoded.length<=1_300_000) { "DECODER_TENSOR_TOO_LARGE" }
            val bytes=Base64.getDecoder().decode(encoded)
            require(bytes.size.toLong()==count*(if(int64)8 else 4)) { "DECODER_INPUT_BYTES" }
            val buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if(int64)repeat(count.toInt()){require(buffer.long in 0L..6561L){"DECODER_TOKEN_RANGE"}}
            else repeat(count.toInt()){require(buffer.float.isFinite()){"DECODER_NONFINITE_INPUT"}}
            Input(name,dims,bytes,int64)
        }
    }
}

/** Timeout/cancel/completion may race; only one result can leave a request. */
class NativeDecoderJob(val id:String) {
    val cancelled=AtomicBoolean(false)
    private val completed=AtomicBoolean(false)
    fun finish():Boolean=completed.compareAndSet(false,true)
    fun cancel(){cancelled.set(true)}
    fun check(){check(!cancelled.get()){"DECODER_CANCELLED"}}
}
