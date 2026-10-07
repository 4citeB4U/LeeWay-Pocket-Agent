/* REGION: LEEWAY.VOICE.OUTPUT_VERIFICATION; TAG: SELECTED_FABRIC_WAVEFORM_ONLY
WHO: Creator-requested speaker test. WHAT: Validate one actual Voice Fabric waveform, never choose a voice.
WHEN: Explicit UI test. WHERE: Portable validation used by native output adapter.
WHY: Real speaker output must not substitute Android TTS or claim conversation execution.
HOW: Expected employee binding, selection revision, expiry and exact PCM hash. LICENSE: MIT */
package industries.leeway.pocket
import org.json.JSONObject
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder
object VerifiedVoiceSample {
    fun validate(meta:JSONObject,bytes:ByteArray,binding:AgentVoiceBinding,now:Long):String {
        require(meta.getString("voiceAuthority")=="LEEWAY_VOICE_FABRIC"){"VOICE_AUTHORITY_REQUIRED"}
        binding.requireRenderer(meta.getString("voicePackageId"),meta.getString("provider"))
        require(meta.getString("agentId")==binding.agentId && meta.getString("personaFamily")==binding.personaFamily && meta.getString("personaArchetypeId")==binding.personaArchetypeId){"VOICE_PERSONA_BINDING_MISMATCH"}
        require(meta.getString("voiceId")==binding.voiceId){"ACOUSTIC_SPEAKER_MISMATCH"}
        require(Regex("[a-f0-9]{64}").matches(meta.getString("selectionRevision"))){"VOICE_SELECTION_REVISION_REQUIRED"}
        val created=meta.getLong("capturedAtMs");val expires=meta.getLong("expiresAtMs")
        require(now>=created-5000 && now<=expires && expires>created && expires-created<=60*60*1000){"VOICE_OUTPUT_SAMPLE_EXPIRED"}
        val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        require(hash==meta.getString("audioSha256")){"VOICE_WAVEFORM_HASH_MISMATCH"}
        require(bytes.size in 46..6000000){"VOICE_WAVEFORM_SIZE_INVALID"}
        val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun tag(at:Int)=String(bytes,at,4,Charsets.US_ASCII)
        require(tag(0)=="RIFF"&&tag(8)=="WAVE"&&tag(12)=="fmt "&&tag(36)=="data"&&b.getInt(4)+8==bytes.size&&b.getInt(40)+44==bytes.size){"VOICE_PCM_CONTAINER_INVALID"}
        require(b.getShort(20).toInt()==1&&b.getShort(22).toInt()==1&&b.getShort(34).toInt()==16&&b.getInt(24)==24000){"VOICE_PCM_FORMAT_INVALID"}
        require((bytes.size-44)%2==0){"VOICE_PCM_LENGTH_INVALID"}
        var peak=0;for(i in 44 until bytes.size-1 step 2)peak=maxOf(peak,kotlin.math.abs(b.getShort(i).toInt()))
        require(peak>0){"VOICE_PCM_SILENT"};return hash
    }
}
