/* REGION: LEEWAY.VOICE.QUALIFICATION; TAG: NATIVE_OUTPUT_INTEGRITY
WHO: Voice qualification. WHAT: Execute actual output validator with explicit sample fixtures.
WHEN: Before physical speaker test. WHERE: JVM only. WHY: Device must not relabel a different speaker.
HOW: PCM fixtures and rejection paths, never counted as audible playback. LICENSE: MIT */
package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
class VerifiedVoiceSampleTest {
 private val binding=AgentVoiceBinding("agent-lee","TEST_PERSONA","TEST_ARCHETYPE","kokoro-test","kokoro","test",true)
 private fun wave():ByteArray{val b=ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);fun s(at:Int,v:String){v.toByteArray().forEachIndexed{i,c->b.put(at+i,c)}};s(0,"RIFF");s(8,"WAVE");s(12,"fmt ");s(36,"data");b.putInt(4,40);b.putInt(16,16);b.putShort(20,1);b.putShort(22,1);b.putInt(24,24000);b.putInt(28,48000);b.putShort(32,2);b.putShort(34,16);b.putInt(40,4);b.putShort(44,100);b.putShort(46,-100);return b.array()}
 private fun meta(bytes:ByteArray)=JSONObject().put("voiceAuthority","LEEWAY_VOICE_FABRIC").put("voicePackageId",binding.voicePackageId).put("provider",binding.provider).put("agentId",binding.agentId).put("personaFamily",binding.personaFamily).put("personaArchetypeId",binding.personaArchetypeId).put("voiceId",binding.voiceId).put("selectionRevision","a".repeat(64)).put("capturedAtMs",10000).put("expiresAtMs",20000).put("audioSha256",MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)})
 @Test fun explicitVoiceAndNonSilentPcmAreAccepted(){val b=wave();assertEquals(meta(b).getString("audioSha256"),VerifiedVoiceSample.validate(meta(b),b,binding,11000))}
 @Test fun androidVoiceSubstitutionIsRejected(){val b=wave();assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b).put("provider","android-tts"),b,binding,11000)}}
 @Test fun anotherPackageCannotMasqueradeAsSelectedVoice(){val b=wave();assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b).put("voicePackageId","other"),b,binding,11000)}}
 @Test fun wrongAuthorityIsRejected(){val b=wave();assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b).put("voiceAuthority","DEVICE_DEFAULT"),b,binding,11000)}}
 @Test fun expiryIsEnforced(){val b=wave();assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b),b,binding,21000)}}
 @Test fun changedWaveBytesAreRejected(){val b=wave();val m=meta(b);b[44]=0;assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(m,b,binding,11000)}}
 @Test fun silentPayloadIsNotAcceptedAsVoice(){val b=wave();b.fill(0,44);assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b),b,binding,11000)}}
 @Test fun invalidWaveHeaderIsRejected(){val b=wave();b[0]=0;assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b),b,binding,11000)}}
 @Test fun personaMustMatchTheEmployeeBinding(){val b=wave();assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b).put("personaArchetypeId","OTHER"),b,binding,11000)}}
 @Test fun revisionMustBeRecorded(){val b=wave();assertThrows(IllegalArgumentException::class.java){VerifiedVoiceSample.validate(meta(b).put("selectionRevision",""),b,binding,11000)}}
}
