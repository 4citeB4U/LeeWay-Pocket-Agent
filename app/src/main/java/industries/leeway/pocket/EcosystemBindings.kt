/*
LEEWAY
REGION: POCKET.CONTEXT
TAG: POCKET.LEEWAY.ECOSYSTEM.BINDINGS
WHAT: Read-only Pocket qualification of existing LeeWay Runtime Fabric and registered providers
WHY: Bind Pocket to canonical registries plus live Runtime Fabric health without embedding ecosystem runtimes
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: LeeWay Pocket Agent
WHEN: 2026-10-02
HOW: Existing Universal Adapter/Runtime registries + existing live health endpoints -> fail-closed cached truth
*/
package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object EcosystemBindings {
    private const val URL_BINDINGS="https://4citeb4u.github.io/LeeWay-Pocket-Agent/ecosystem-bindings.json"
    private const val URL_ADAPTER_REGISTRY="https://raw.githubusercontent.com/4citeB4U/Leeway-Runtime-Fabric/main/adapter.registry.json"
    private const val URL_UNIVERSAL_CONTRACT="https://raw.githubusercontent.com/4citeB4U/Leeway-Runtime-Fabric/main/contracts/universal-adapter-contract.v1.json"
    private const val URL_EXECUTION_NODES="https://raw.githubusercontent.com/4citeB4U/Leeway-Runtime-Fabric/main/execution-node.registry.json"
    private const val URL_AGENT_MANIFEST="https://raw.githubusercontent.com/4citeB4U/Leeway-Runtime-Fabric/main/agent-interface/agentlee.manifest.json"
    private const val URL_PROVIDER_REGISTRY="https://raw.githubusercontent.com/4citeB4U/Leeway-Runtime-Fabric/main/provider.registry.json"
    private const val URL_RUNTIME_HEALTH="https://leeway-runtime-fabric.fly.dev/runtime/health"
    private const val URL_AGENT_HEALTH="https://leeway-runtime-fabric.fly.dev/agent-lee/health"
    private const val URL_PROVIDER_HEALTH="https://leeway-runtime-fabric.fly.dev/provider-fabric/health"
    private const val URL_PROVIDER_STATUS="https://leeway-runtime-fabric.fly.dev/agent-lee/provider/status"
    private const val URL_DEVICE_CONTRACT="https://4citeb4u.github.io/LEEWAY-DEVICE-BRIDGE/docs/runtime-contract.json"
    private const val URL_VOICE="https://4citeb4u.github.io/LeeWay-Voice-Fabric/android-bridge.html"

    private const val PREFS="leeway-pocket-ecosystem"
    private const val KEY="bindings_json"
    private const val KEY_RUNTIME="runtime_fabric_json"

    private fun fetch(url:String,timeoutMs:Int=12000):String?=try{
        val connection=(URL(url).openConnection() as HttpURLConnection).apply{
            requestMethod="GET"
            connectTimeout=minOf(timeoutMs,8000)
            readTimeout=timeoutMs
            setRequestProperty("Cache-Control","no-cache")
            setRequestProperty("User-Agent","LeeWay-Pocket-Agent")
        }
        val code=connection.responseCode
        val body=if(code in 200..299)connection.inputStream.bufferedReader().use{it.readText()} else null
        connection.disconnect()
        body?.takeIf{it.isNotBlank()}
    }catch(_:Exception){null}

    private fun fetchJson(url:String):JSONObject? =
        fetch(url)?.let{runCatching{JSONObject(it)}.getOrNull()}

    private fun endpointReachable(url:String):Boolean {
        if(!url.startsWith("https://"))return false
        return try{
            val connection=(URL(url).openConnection() as HttpURLConnection).apply{
                requestMethod="GET"
                connectTimeout=3000
                readTimeout=4000
                instanceFollowRedirects=true
                setRequestProperty("User-Agent","LeeWay-Pocket-Agent")
            }
            val code=connection.responseCode
            connection.disconnect()
            code in 200..299
        }catch(_:Exception){false}
    }

    fun runtimeFabricSnapshot(context:Context):JSONObject {
        val adapter=fetchJson(URL_ADAPTER_REGISTRY)
        val universal=fetchJson(URL_UNIVERSAL_CONTRACT)
        val nodes=fetchJson(URL_EXECUTION_NODES)
        val agent=fetchJson(URL_AGENT_MANIFEST)
        val providers=fetchJson(URL_PROVIDER_REGISTRY)
        val runtimeHealth=fetchJson(URL_RUNTIME_HEALTH)
        val agentHealth=fetchJson(URL_AGENT_HEALTH)
        val providerHealth=fetchJson(URL_PROVIDER_HEALTH)
        val providerStatus=fetchJson(URL_PROVIDER_STATUS)
        val deviceContract=fetchJson(URL_DEVICE_CONTRACT)
        val voicePageReachable=!fetch(URL_VOICE,8000).isNullOrBlank()

        val universalOk=universal?.optString("id")=="LEEWAY_UNIVERSAL_ADAPTER_CONTRACT_V1"

        var voiceRegistered=false
        adapter?.optJSONArray("speech")?.let{speech->
            for(i in 0 until speech.length()){
                if(speech.optJSONObject(i)?.optString("id")=="leeway-voice-fabric"){
                    voiceRegistered=true
                    break
                }
            }
        }

        var phoneNodeRegistered=false
        nodes?.optJSONArray("nodes")?.let{rows->
            for(i in 0 until rows.length()){
                val row=rows.optJSONObject(i)?:continue
                if(row.optString("nodeId")=="leeway-phone-workstation" &&
                    row.optString("provider")=="4citeB4U/LEEWAY-DEVICE-BRIDGE"){
                    phoneNodeRegistered=true
                    break
                }
            }
        }

        val callPolicy=agent?.optJSONObject("callPolicy")
        val routeByContract=callPolicy?.optBoolean("routeByContract")==true
        val providerFabricFirst=callPolicy?.optBoolean("providerFabricFirst")==true &&
            callPolicy.optBoolean("requireProviderFabric")==true
        val providerRegistryOk=providers?.optString("registryId")=="LEEWAY_PROVIDER_REGISTRY"

        val runtimeReady=runtimeHealth?.optString("status")=="ok" &&
            runtimeHealth.optString("service")=="leeway-runtime-fabric"
        val agentLeeHealthy=agentHealth?.optString("status")=="ACTIVE_HEALTHY"
        val providerFabricHealthy=providerHealth?.optString("status")=="ACTIVE_HEALTHY"

        val reportedProvider=providerStatus?.optString("provider").orEmpty()
        val reportedProviderBase=providerStatus?.optString("baseUrl").orEmpty()
        val providerReportedConnected=providerStatus?.optBoolean("connected")==true
        val reasoningProviderReachable=
            if(providerReportedConnected && reportedProviderBase.startsWith("https://"))
                endpointReachable(reportedProviderBase)
            else false

        val deviceContractValid=deviceContract?.optString("authority")=="4citeB4U/LEEWAY-DEVICE-BRIDGE" &&
            deviceContract.optJSONObject("execution")?.optString("privilegedRuntime")=="PHONE_LOCAL_NATIVE_PACKAGE"

        val registryBound=universalOk && voiceRegistered && phoneNodeRegistered &&
            routeByContract && providerFabricFirst && providerRegistryOk
        val runtimeConnected=runtimeReady && agentLeeHealthy && providerFabricHealthy
        val bound=registryBound && runtimeConnected && voicePageReachable

        val snapshot=JSONObject().apply{
            put("authority","4citeB4U/Leeway-Runtime-Fabric")
            put("universalAdapterContract",universalOk)
            put("voiceFabricRegistered",voiceRegistered)
            put("voiceFabricReachable",voicePageReachable)
            put("phoneExecutionNodeRegistered",phoneNodeRegistered)
            put("deviceRuntimeContract",deviceContractValid)
            put("routeByContract",routeByContract)
            put("providerFabricFirst",providerFabricFirst)
            put("providerRegistry",providerRegistryOk)
            put("registryBound",registryBound)
            put("runtimeReady",runtimeReady)
            put("agentLeeHealthy",agentLeeHealthy)
            put("providerFabricHealthy",providerFabricHealthy)
            put("runtimeConnected",runtimeConnected)
            put("reasoningProvider",reportedProvider)
            put("reasoningProviderReportedConnected",providerReportedConnected)
            put("reasoningProviderReachable",reasoningProviderReachable)
            put("bound",bound)
        }

        if(bound){
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .edit().putString(KEY_RUNTIME,snapshot.toString()).apply()
            return snapshot
        }

        val cached=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_RUNTIME,null)
        return if(!cached.isNullOrBlank()){
            runCatching{
                JSONObject(cached)
                    .put("cached",true)
                    .put("liveProbeBound",false)
                    .put("liveReasoningProviderReachable",reasoningProviderReachable)
            }.getOrElse{snapshot}
        }else snapshot
    }

    fun refresh(context: Context): JSONObject {
        val text=fetch(URL_BINDINGS)
        if(!text.isNullOrBlank()){
            try{
                val parsed=JSONObject(text)
                context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,text).apply()
                return parsed
            }catch(_:Exception){}
        }
        val cached=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,null)
        if(!cached.isNullOrBlank())try{return JSONObject(cached)}catch(_:Exception){}
        return fallback()
    }

    fun promptContext(context: Context): String {
        val root=refresh(context)
        val runtime=runtimeFabricSnapshot(context)
        val rows=mutableListOf<String>()
        val bindings=root.optJSONArray("bindings")
        if(bindings!=null)for(i in 0 until bindings.length()){
            val item=bindings.optJSONObject(i)?:continue
            rows += item.optString("id")+": authority="+item.optString("authority")+
                "; binding="+item.optString("phoneBinding")+"; claim="+item.optString("executionClaim")
        }
        return buildString{
            append("LEEWAY AUTHORITY/BINDING SNAPSHOT\n")
            append("Runtime Fabric bound="+runtime.optBoolean("bound")+
                "; controlPlane="+runtime.optBoolean("runtimeConnected")+
                "; universalAdapter="+runtime.optBoolean("universalAdapterContract")+
                "; providerFabric="+runtime.optBoolean("providerFabricHealthy")+
                "; voice="+runtime.optBoolean("voiceFabricRegistered")+
                "; phoneNode="+runtime.optBoolean("phoneExecutionNodeRegistered")+
                "; reasoningProviderReported="+runtime.optBoolean("reasoningProviderReportedConnected")+
                "; reasoningProviderReachable="+runtime.optBoolean("reasoningProviderReachable")+"\n")
            append(rows.joinToString("\n"))
            append("\nTruth law: Pocket is a thin client. Existing Runtime Fabric and registered providers remain authoritative. ")
            append("A dead reasoning tunnel is a provider-route failure, not a Runtime Fabric disconnect. ")
            append("Never claim a skill, Formula evaluation, device action, voice, or runtime executed unless the real provider returns evidence.")
        }
    }

    private fun fallback(): JSONObject = JSONObject(
        """{"schemaVersion":"fallback","authority":"4citeB4U/Leeway-Runtime-Fabric","bindings":[
        {"id":"standards","authority":"LeeWay Standards","phoneBinding":"POLICY_CONTEXT","executionClaim":"AUTHORITY_REFERENCE"},
        {"id":"runtime-fabric","authority":"4citeB4U/Leeway-Runtime-Fabric","phoneBinding":"CANONICAL_RUNTIME_FABRIC_REGISTRIES","executionClaim":"ROUTE_BY_CONTRACT_PROVIDER_FABRIC_FIRST"},
        {"id":"agent-skills","authority":"4citeB4U/LeeWay-Agent-Skills","phoneBinding":"RUNTIME_FABRIC_SKILLS_UNIVERSITY_AND_CANONICAL_SOURCE","executionClaim":"CONTEXT_OR_EXECUTION_ONLY_WITH_EVIDENCE"},
        {"id":"formula","authority":"4citeB4U/Leeway-formula-live","phoneBinding":"RUNTIME_FABRIC_CONTRACT_ROUTE_TO_CANONICAL_FORMULA","executionClaim":"NOT_EXECUTED_UNLESS_CANONICAL_FORMULA_RECEIPT_RETURNED"},
        {"id":"device-bridge","authority":"4citeB4U/LEEWAY-DEVICE-BRIDGE","phoneBinding":"RUNTIME_FABRIC_EXECUTION_NODE:leeway-phone-workstation;SCOPED_ANDROID_IPC","executionClaim":"EXECUTED_ONLY_WHEN_REGISTERED_NODE_IPC_RESULT_RETURNS"},
        {"id":"voice-fabric","authority":"4citeB4U/LeeWay-Voice-Fabric","phoneBinding":"RUNTIME_FABRIC_ADAPTER:leeway-voice-fabric;ANDROID_WEBVIEW_VOICE_BRIDGE","executionClaim":"SPOKEN_ONLY_AFTER_NATIVE_CALLBACK"}
        ]}"""
    )
}
