/*
LEEWAY
REGION: POCKET.CONTEXT
TAG: POCKET.LEEWAY.ECOSYSTEM.BINDINGS
WHAT: Read-only Pocket view of existing LeeWay Runtime Fabric authority and bindings
WHY: Bind Pocket to canonical Runtime Fabric registries without copying ecosystem runtimes
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: LeeWay Pocket Agent
WHEN: 2026-10-02
HOW: Existing Runtime Fabric registries + Universal Adapter Contract -> fail-closed cached snapshot
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
    private const val PREFS="leeway-pocket-ecosystem"
    private const val KEY="bindings_json"
    private const val KEY_RUNTIME="runtime_fabric_json"

    private fun fetch(url:String):String?=try{
        val connection=(URL(url).openConnection() as HttpURLConnection).apply{
            requestMethod="GET";connectTimeout=8000;readTimeout=12000
            setRequestProperty("Cache-Control","no-cache")
            setRequestProperty("User-Agent","LeeWay-Pocket-Agent")
        }
        val code=connection.responseCode
        val body=if(code in 200..299)connection.inputStream.bufferedReader().use{it.readText()} else null
        connection.disconnect()
        body?.takeIf{it.isNotBlank()}
    }catch(_:Exception){null}

    fun runtimeFabricSnapshot(context:Context):JSONObject {
        val adapter=fetch(URL_ADAPTER_REGISTRY)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val universal=fetch(URL_UNIVERSAL_CONTRACT)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val nodes=fetch(URL_EXECUTION_NODES)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val agent=fetch(URL_AGENT_MANIFEST)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val providers=fetch(URL_PROVIDER_REGISTRY)?.let{runCatching{JSONObject(it)}.getOrNull()}

        val speech=adapter?.optJSONArray("speech")
        var voiceRegistered=false
        if(speech!=null)for(i in 0 until speech.length()){
            if(speech.optJSONObject(i)?.optString("id")=="leeway-voice-fabric")voiceRegistered=true
        }

        val nodeRows=nodes?.optJSONArray("nodes")
        var phoneNodeRegistered=false
        if(nodeRows!=null)for(i in 0 until nodeRows.length()){
            val row=nodeRows.optJSONObject(i)?:continue
            if(row.optString("nodeId")=="leeway-phone-workstation" &&
                row.optString("provider")=="4citeB4U/LEEWAY-DEVICE-BRIDGE")phoneNodeRegistered=true
        }

        val universalOk=universal?.optString("id")=="LEEWAY_UNIVERSAL_ADAPTER_CONTRACT_V1"
        val routeByContract=agent?.optJSONObject("callPolicy")?.optBoolean("routeByContract")==true
        val providerFirst=agent?.optJSONObject("callPolicy")?.optBoolean("providerFabricFirst")==true
        val providerRegistryOk=providers?.optString("registryId")=="LEEWAY_PROVIDER_REGISTRY"

        val snapshot=JSONObject().apply{
            put("authority","4citeB4U/Leeway-Runtime-Fabric")
            put("universalAdapterContract",universalOk)
            put("voiceFabricRegistered",voiceRegistered)
            put("phoneExecutionNodeRegistered",phoneNodeRegistered)
            put("routeByContract",routeByContract)
            put("providerFabricFirst",providerFirst)
            put("providerRegistry",providerRegistryOk)
            put("bound",universalOk&&voiceRegistered&&phoneNodeRegistered&&routeByContract&&providerFirst&&providerRegistryOk)
        }
        if(snapshot.optBoolean("bound")){
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_RUNTIME,snapshot.toString()).apply()
            return snapshot
        }
        val cached=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_RUNTIME,null)
        return if(!cached.isNullOrBlank())runCatching{JSONObject(cached).put("cached",true)}.getOrElse{snapshot} else snapshot
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
                "; universalAdapter="+runtime.optBoolean("universalAdapterContract")+
                "; providerFabricFirst="+runtime.optBoolean("providerFabricFirst")+
                "; voiceRegistered="+runtime.optBoolean("voiceFabricRegistered")+
                "; phoneNodeRegistered="+runtime.optBoolean("phoneExecutionNodeRegistered")+"\n")
            append(rows.joinToString("\n"))
            append("\nTruth law: Pocket is a thin client. Runtime Fabric routes by existing contract/provider registries; adapters translate but do not become authority. ")
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
