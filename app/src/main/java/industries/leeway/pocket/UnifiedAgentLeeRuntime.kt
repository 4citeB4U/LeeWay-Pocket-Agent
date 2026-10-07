/*
REGION: POCKET.RUNTIME.ADAPTER
TAG: LEEWAY_CANONICAL_CONVERSATION_BINDING
WHO: Agent Lee / Creator-authorized Android body
WHAT: Route explicit Continuum reads to this body's retained records; route other turns to the paired provider.
WHEN: A local phone executor is not yet qualified and an authorized cross-body provider is available.
WHERE: Pocket Android adapter; pairing state is private device state, never compiled endpoint/secret.
WHY: A canned answer or device-selected TTS is not Agent Lee.
HOW: Bound local read API and prior-event cutoff; otherwise unchanged pinned paired turn and Voice Fabric check.
SOURCE: Exact live UnifiedAgentLeeRuntime.kt SHA256 160da4844011ecaa354f46e22402183bf904977cc21d0a5e00fb1bcd6a99034b.
LOCAL READ: Extractive source retrieval only; source digests are not Formula or Veritas receipts.
NOTE: This is a current cross-body provider route, NOT standalone-phone acceptance and NOT a second consciousness.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class AgentLeeTurn(val text:String,val audio:ByteArray,val voicePackageId:String,val provider:String,val receiptHash:String,
    val localContinuum:LocalContinuumReply?=null)

class UnifiedAgentLeeRuntime(private val context:Context) {
    fun respond(request:String):String=turn(request).text
    /** Before the existing event writer: an explicit read cannot create its own missing source store. */
    fun requireExistingContinuumBeforeCapture(request:String) {
        LocalContinuumRetrieval.query(request)?.let { query ->
            LocalContinuumRetrieval.validateQuery(query)
            AndroidContinuumRetrieval(context).requireExistingStorage()
        }
    }
    fun turn(request:String,currentContinuumEventId:Long?=null):AgentLeeTurn {
        require(request.isNotBlank()) { "CONVERSATION_REQUEST_REQUIRED" }
        LocalContinuumRetrieval.query(request)?.let { query ->
            val cutoff=currentContinuumEventId?.takeIf{it>0} ?: error("CONTINUUM_CURRENT_EVENT_CUTOFF_REQUIRED")
            val local=AndroidContinuumRetrieval(context).retrieve(query,cutoff)
            // This typed local result is spoken by existing PocketSpeech after its selected Voice Fabric binding passes.
            // It is never sent to /turn and has no fabricated audio, provider identity or governance receipt.
            return AgentLeeTurn(local.displayText,ByteArray(0),"","","",local)
        }
        val json=pairedRequest("/turn",JSONObject().put("text",request),50000)

        val voice=json.getJSONObject("voice")
        val packageId=voice.getString("voicePackageId")
        val provider=voice.getString("provider")
        // The pinned, authenticated existing gateway supplies the Voice Fabric snapshot for this turn.
        // A package-embedded default must not override a newer owner-approved shared selection.
        val snapshotText=voice.getString("selectionJson")
        require(snapshotText.length<=16384){"VOICE_SELECTION_SNAPSHOT_TOO_LARGE"}
        val snapshotHash=MessageDigest.getInstance("SHA-256").digest(snapshotText.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
        require(snapshotHash==voice.getString("selectionRevision")){"VOICE_SELECTION_REVISION_MISMATCH"}
        val selection=JSONObject(snapshotText)
        require(selection.getString("authority")=="LEEWAY_VOICE_FABRIC" && selection.getString("agentId")=="agent-lee"){"VOICE_SELECTION_AUTHORITY_MISMATCH"}
        require(selection.getString("voicePackageId")==packageId && selection.getString("provider")==provider){"VOICE_RENDER_SELECTION_MISMATCH"}
        val projected=JSONObject().put("agentId","agent-lee").put("personaFamily",selection.getString("personaFamily"))
            .put("personaArchetypeId",selection.getString("personaArchetypeId")).put("voicePackageId",packageId)
            .put("state",selection.getString("selectionState")).put("selectedBy",selection.getString("selectedBy"))
        val shared=JSONObject().put("authority","LEEWAY_VOICE_FABRIC").put("bindings",JSONObject().put("agent-lee",projected))
        val binding=AgentVoiceBinding.fromSources(shared,JSONObject(context.assets.open("voice/catalog.v1.json").bufferedReader().use{it.readText()}),allowQualification=BuildConfig.DEBUG)
        if(selection.has("tuning")){
            require(selection.getString("tuningProcessor")=="LEEWAY_STUDIO_DSP_V1"){"VOICE_TUNING_PROCESSOR_MISMATCH"}
            val acoustic=voice.getJSONObject("acousticEvidence")
            require(acoustic.getString("processor")=="LEEWAY_STUDIO_DSP_V1"){"VOICE_TUNING_EVIDENCE_REQUIRED"}
        }

        binding.requireRenderer(packageId,provider)
        val audio=Base64.decode(voice.getString("audioContent"),Base64.DEFAULT);require(audio.size>44){"LEEWAY_VOICE_AUDIO_EMPTY"}
        if(selection.has("tuning")){
            val audioHash=MessageDigest.getInstance("SHA-256").digest(audio).joinToString(""){"%02x".format(it)}
            require(audioHash==voice.getJSONObject("acousticEvidence").getString("processedAudioSha256")){"TUNED_AUDIO_HASH_MISMATCH"}
        }
        context.getSharedPreferences("leeway-voice-observation",Context.MODE_PRIVATE).edit()
            .putString("selectionRevision",snapshotHash).putString("voicePackageId",packageId)
            .putString("state","RECEIVED_VALIDATED_AUDIO_NOT_PLAYBACK_PROOF").putLong("observedAt",System.currentTimeMillis()).apply()
        return AgentLeeTurn(json.getString("text"),audio,packageId,provider,json.optString("receiptHash"))
    }

    fun studioRequest(method:String,path:String,body:String,csrf:String):String {
        val allowed=setOf("GET /api/local/status","GET /api/local/voices","GET /api/provider/status","POST /api/local/synthesize","GET /api/agent-lee/selection/session","POST /api/agent-lee/selection")
        require("$method $path" in allowed && body.length<=8192 && csrf.length<=256){"STUDIO_REQUEST_NOT_ADMITTED"}
        return pairedRequest("/voice-studio/request",JSONObject().put("method",method).put("path",path).put("body",body).put("csrf",csrf),160000).toString()
    }
    private fun pairedRequest(route:String,payload:JSONObject,timeoutMs:Int):JSONObject {
        require(route in setOf("/turn","/voice-studio/request")){"PAIRED_ROUTE_NOT_ADMITTED"}
        val prefs=context.getSharedPreferences("leeway-runtime-pairing",Context.MODE_PRIVATE)
        val endpoint=prefs.getString("endpoint",null)?.trim()?.removeSuffix("/") ?: error("CANONICAL_CONVERSATION_PROVIDER_NOT_BOUND")
        val token=prefs.getString("token",null)?.trim() ?: error("CANONICAL_CONVERSATION_PAIRING_TOKEN_MISSING")
        val expectedPin=prefs.getString("certificateSha256",null)?.lowercase()?.replace(":","") ?: error("CANONICAL_CONVERSATION_CERTIFICATE_PIN_MISSING")
        require(endpoint.startsWith("https://")){"CANONICAL_CONVERSATION_TLS_REQUIRED"}
        val trust=object:X509TrustManager{
            override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain:Array<out X509Certificate>?,authType:String?)=error("CLIENT_CERT_NOT_USED")
            override fun checkServerTrusted(chain:Array<out X509Certificate>?,authType:String?){
                val cert=chain?.firstOrNull() ?: error("RUNTIME_CERTIFICATE_MISSING")
                val actual=MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString(""){"%02x".format(it)}
                require(actual==expectedPin){"RUNTIME_CERTIFICATE_PIN_MISMATCH"}
            }
        }
        val ssl=SSLContext.getInstance("TLS").apply{init(null,arrayOf<TrustManager>(trust),SecureRandom())}
        val connection=(URL("$endpoint$route").openConnection() as HttpsURLConnection).apply{
            requestMethod="POST";connectTimeout=5000;readTimeout=timeoutMs;doOutput=true
            sslSocketFactory=ssl.socketFactory;hostnameVerifier=javax.net.ssl.HostnameVerifier{_,_->true}
            setRequestProperty("Authorization","Bearer $token");setRequestProperty("Content-Type","application/json")
        }
        val body=payload.put("bodyId",AndroidDigitalBrainAdapter.identity(context).deviceId).toString()
        connection.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}
        val code=connection.responseCode;val raw=(if(code in 200..299)connection.inputStream else connection.errorStream).bufferedReader().use{it.readText()};connection.disconnect()
        require(code in 200..299){"CANONICAL_CONVERSATION_PROVIDER_HTTP_$code"}
        return JSONObject(raw)
    }
}
