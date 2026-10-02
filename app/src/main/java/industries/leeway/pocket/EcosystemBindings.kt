/*
LEEWAY
REGION: POCKET.CONTEXT
TAG: POCKET.LEEWAY.ECOSYSTEM.BINDINGS
WHAT: Read-only Pocket qualification of existing LeeWay Runtime Fabric and registered providers
WHY: Bind Pocket to live canonical services without embedding ecosystem runtimes
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: LeeWay Pocket Agent
WHEN: 2026-10-02
HOW: Existing public Runtime Fabric APIs + existing public provider manifests -> fail-closed cached truth
*/
package industries.leeway.pocket

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object EcosystemBindings {
    private const val URL_BINDINGS="https://4citeb4u.github.io/LeeWay-Pocket-Agent/ecosystem-bindings.json"
    private const val URL_RUNTIME_HEALTH="https://leeway-runtime-fabric.fly.dev/api/health"
    private const val URL_RUNTIME_COMPONENTS="https://leeway-runtime-fabric.fly.dev/api/components"
    private const val URL_RUNTIME_STATUS="https://leeway-runtime-fabric.fly.dev/api/self-hosted/status"
    private const val URL_PHONE_NODE="https://4citeb4u.github.io/LEEWAY-DEVICE-BRIDGE/docs/secondary-workstation-node.json"
    private const val URL_DEVICE_CONTRACT="https://4citeb4u.github.io/LEEWAY-DEVICE-BRIDGE/docs/runtime-contract.json"
    private const val URL_VOICE="https://4citeb4u.github.io/LeeWay-Voice-Fabric/android-bridge.html"
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
        val health=fetch(URL_RUNTIME_HEALTH)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val components=fetch(URL_RUNTIME_COMPONENTS)?.let{runCatching{JSONArray(it)}.getOrNull()}
        val status=fetch(URL_RUNTIME_STATUS)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val phone=fetch(URL_PHONE_NODE)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val deviceContract=fetch(URL_DEVICE_CONTRACT)?.let{runCatching{JSONObject(it)}.getOrNull()}
        val voiceReachable=!fetch(URL_VOICE).isNullOrBlank()

        val runtimeReady=health?.optString("status")=="LEEWAY_CONTROL_PLANE_READY_PASS"
        var providerFabric=false
        if(components!=null)for(i in 0 until components.length()){
            val row=components.optJSONObject(i)?:continue
            if(row.optString("id")=="provider-fabric"){
                providerFabric=true
                break
            }
        }
        val running=status?.optJSONObject("services")?.optJSONArray("running")
        var providerRunning=false
        if(running!=null)for(i in 0 until running.length()){
            if(running.optJSONObject(i)?.optString("id")=="provider-fabric")providerRunning=true
        }
        val phoneNodeRegistered=phone?.optString("nodeId")=="leeway-phone-workstation" &&
            phone.optString("authority")=="4citeB4U/LEEWAY-DEVICE-BRIDGE" &&
            phone.optString("runtimeOwner")=="4citeB4U/Leeway-Runtime-Fabric"
        val deviceContractValid=deviceContract?.optString("authority")=="4citeB4U/LEEWAY-DEVICE-BRIDGE" &&
            deviceContract.optJSONObject("execution")?.optString("privilegedRuntime")=="PHONE_LOCAL_NATIVE_PACKAGE"

        val snapshot=JSONObject().apply{
            put("authority","4citeB4U/Leeway-Runtime-Fabric")
            put("runtimeReady",runtimeReady)
            put("providerFabricRegistered",providerFabric)
            put("providerFabricRunning",providerRunning)
            put("voiceFabricRegistered",voiceReachable)
            put("phoneExecutionNodeRegistered",phoneNodeRegistered)
            put("deviceRuntimeContract",deviceContractValid)
            put("routeByContract",true)
            put("providerFabricFirst",providerFabric&&providerRunning)
            put("universalAdapterContract",true)
            put("bound",runtimeReady&&providerFabric&&providerRunning&&voiceReachable&&phoneNodeRegistered&&deviceContractValid)
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
                "; controlPlane="+runtime.optBoolean("runtimeReady")+
                "; providerFabric="+runtime.optBoolean("providerFabricRunning")+
                "; voice="+runtime.optBoolean("voiceFabricRegistered")+
                "; phoneNode="+runtime.optBoolean("phoneExecutionNodeRegistered")+"\n")
            append(rows.joinToString("\n"))
            append("\nTruth law: Pocket is a thin client. Existing Runtime Fabric and registered providers remain authoritative. ")
            append("Never claim a skill, Formula evaluation, device action, voice, or runtime executed unless the real provider returns evidence.")
        }
    }

    private fun fallback(): JSONObject = JSONObject(
        """{"schemaVersion":"fallback","authority":"4citeB4U/Leeway-Runtime-Fabric","bindings":[
        {"id":"standards","authority":"LeeWay Standards","phoneBinding":"POLICY_CONTEXT","executionClaim":"AUTHORITY_REFERENCE"},
        {"id":"runtime-fabric","authority":"4citeB4U/Leeway-Runtime-Fabric","phoneBinding":"LIVE_CONTROL_PLANE_AND_EXISTING_PROVIDER_FABRIC","executionClaim":"ROUTE_BY_EXISTING_CONTRACTS"},
        {"id":"agent-skills","authority":"4citeB4U/LeeWay-Agent-Skills","phoneBinding":"RUNTIME_FABRIC_SKILLS_UNIVERSITY_AND_CANONICAL_SOURCE","executionClaim":"CONTEXT_OR_EXECUTION_ONLY_WITH_EVIDENCE"},
        {"id":"formula","authority":"4citeB4U/Leeway-formula-live","phoneBinding":"RUNTIME_FABRIC_CONTRACT_ROUTE_TO_CANONICAL_FORMULA","executionClaim":"NOT_EXECUTED_UNLESS_CANONICAL_FORMULA_RECEIPT_RETURNED"},
        {"id":"device-bridge","authority":"4citeB4U/LEEWAY-DEVICE-BRIDGE","phoneBinding":"RUNTIME_FABRIC_EXECUTION_NODE:leeway-phone-workstation;SCOPED_ANDROID_IPC","executionClaim":"EXECUTED_ONLY_WHEN_REGISTERED_NODE_IPC_RESULT_RETURNS"},
        {"id":"voice-fabric","authority":"4citeB4U/LeeWay-Voice-Fabric","phoneBinding":"RUNTIME_FABRIC_ADAPTER:leeway-voice-fabric;ANDROID_WEBVIEW_VOICE_BRIDGE","executionClaim":"SPOKEN_ONLY_AFTER_NATIVE_CALLBACK"}
        ]}"""
    )
}
