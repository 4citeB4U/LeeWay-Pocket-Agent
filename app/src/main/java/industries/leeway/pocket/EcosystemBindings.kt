/*
LEEWAY
REGION: POCKET.CONTEXT
TAG: POCKET.LEEWAY.ECOSYSTEM.BINDINGS
WHAT: Read-only canonical ecosystem binding snapshot for Pocket Agent turns
WHY: Keep Agent Lee aware of real authority/binding state without inventing execution
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: LeeWay Pocket Agent
WHEN: 2026-09-29
HOW: Fetch GitHub Pages binding registry, cache privately, fall back to fail-closed built-in truth
*/
package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object EcosystemBindings {
    private const val URL_BINDINGS="https://4citeb4u.github.io/LeeWay-Pocket-Agent/ecosystem-bindings.json"
    private const val PREFS="leeway-pocket-ecosystem"
    private const val KEY="bindings_json"

    fun refresh(context: Context): JSONObject {
        val text=try{
            val connection=(URL(URL_BINDINGS).openConnection() as HttpURLConnection).apply{
                requestMethod="GET";connectTimeout=8000;readTimeout=12000
                setRequestProperty("Cache-Control","no-cache")
            }
            val code=connection.responseCode
            val body=if(code in 200..299) connection.inputStream.bufferedReader().use{it.readText()} else ""
            connection.disconnect()
            if(body.isBlank()) null else body
        }catch(_:Exception){null}

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
        val rows=mutableListOf<String>()
        val bindings=root.optJSONArray("bindings")
        if(bindings!=null)for(i in 0 until bindings.length()){
            val item=bindings.optJSONObject(i)?:continue
            rows += item.optString("id")+": authority="+item.optString("authority")+
                "; binding="+item.optString("phoneBinding")+"; claim="+item.optString("executionClaim")
        }
        return buildString{
            append("LEEWAY AUTHORITY/BINDING SNAPSHOT\n")
            append(rows.joinToString("\n"))
            append("\nTruth law: never claim a skill, Formula evaluation, tool, voice, Notebook adapter, or runtime executed unless this turn returns execution evidence. ")
            append("Device Bridge model output is reasoning only; consequential actions require their real provider and receipt.")
        }
    }

    private fun fallback(): JSONObject = JSONObject(
        """{"schemaVersion":"fallback","bindings":[
        {"id":"standards","authority":"4citeB4U/LeeWay-Standards","phoneBinding":"POLICY_CONTEXT","executionClaim":"AUTHORITY_REFERENCE"},
        {"id":"agent-skills","authority":"4citeB4U/LeeWay-Agent-Skills","phoneBinding":"SOURCE_AUTHORITY_BOUND_REMOTE_MCP_PENDING","executionClaim":"SKILL_EXECUTION_NOT_YET_BOUND"},
        {"id":"formula","authority":"4citeB4U/Leeway-formula-live","phoneBinding":"CENTRALIZED_EVALUATOR_REQUIRED","executionClaim":"NOT_EXECUTED_UNLESS_RECEIPT_RETURNED"},
        {"id":"device-bridge","authority":"4citeB4U/LEEWAY-DEVICE-BRIDGE","phoneBinding":"SCOPED_ANDROID_IPC","executionClaim":"EXECUTED_ONLY_WHEN_IPC_RESULT_RETURNS"},
        {"id":"voice-fabric","authority":"4citeB4U/LeeWay-Voice-Fabric","phoneBinding":"ANDROID_WEBVIEW_VOICE_BRIDGE","executionClaim":"SPOKEN_ONLY_AFTER_NATIVE_CALLBACK"}
        ]}"""
    )
}
