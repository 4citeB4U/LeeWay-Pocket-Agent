/*
LEEWAY
REGION: POCKET.COGNITION.SHADOW
TAG: POCKET.LEEWAY.MACHINE_CONSCIOUSNESS.L1_SHADOW
WHAT: Observe-only localhost client for the LeeWay Machine Consciousness shadow
WHY: Measure candidate cognition beside each Pocket turn without changing Agent Lee answers
WHO: LeeWay Industries / Agent Lee / Creator
WHERE: Android LeeWay Pocket Agent
WHEN: 2026-10-02
HOW: Async POST to localhost shadow -> bounded evidence trace -> private prefs -> optional UI inspection
*/
package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

object ConsciousnessShadow {
    private const val ENDPOINT="http://localhost:8789/api/ask"
    private const val PREFS="leeway-pocket-consciousness-shadow"

    private fun sha256(value:String)=MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(""){"%02x".format(it)}

    fun observe(context:Context,request:String){
        val app=context.applicationContext
        val requestHash=sha256(request)
        thread(name="leeway-consciousness-shadow",isDaemon=true){
            val trace=JSONObject()
                .put("schemaVersion","0.1.0")
                .put("mode","L1_OBSERVE_ONLY")
                .put("answerEffect",false)
                .put("requestHash",requestHash)
                .put("startedAtMs",System.currentTimeMillis())
            var connection:HttpURLConnection?=null

            try{
                connection=(URL(ENDPOINT).openConnection() as HttpURLConnection).apply{
                    requestMethod="POST"
                    connectTimeout=800
                    readTimeout=1200
                    doOutput=true
                    setRequestProperty("Content-Type","application/json")
                    setRequestProperty("Cache-Control","no-store")
                }
                val payload=JSONObject().put("q",request.take(600)).toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use{it.write(payload)}
                val code=connection.responseCode
                val body=(if(code in 200..299)connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use{it.readText()}.orEmpty()
                val result=if(body.isBlank())JSONObject() else JSONObject(body)
                val state=result.optJSONObject("state")
                trace.put("available",code in 200..299)
                    .put("httpStatus",code)
                    .put("matched",result.optBoolean("matched",false))
                    .put("intent",result.optString("intent","unknown"))
                    .put("shadowAnswer",result.optString("answer").take(500))
                    .put("stateHash",state?.optString("stateHash"))
                    .put("priorStateHash",state?.optString("priorStateHash"))
                    .put("continuityVerified",state?.optBoolean("continuityVerified"))
                    .put("classification",state?.optJSONObject("prism")?.optString("classification"))
                    .put("balance",state?.optJSONObject("prism")?.optDouble("balance"))
                    .put("formulaStatus",state?.optJSONObject("evidence")?.optJSONObject("formula")?.optString("status"))
                    .put("llmDependency",state?.optInt("llmDependency"))
                    .put("shadowAuthority",state?.optJSONObject("authority")?.optString("mode"))
            }catch(error:Exception){
                trace.put("available",false)
                    .put("errorClass",error.javaClass.simpleName)
            }finally{
                connection?.disconnect()
            }
            trace.put("completedAtMs",System.currentTimeMillis())
            persist(app,trace)
        }
    }

    fun recordActual(context:Context,request:String,response:String,ok:Boolean=true){
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val requestHash=sha256(request)
        val actual=JSONObject()
            .put("requestHash",requestHash)
            .put("ok",ok)
            .put("responseHash",sha256(response))
            .put("responsePreview",response.take(500))
            .put("recordedAtMs",System.currentTimeMillis())
        val latest=runCatching{JSONObject(prefs.getString("latest","{}").orEmpty())}.getOrElse{JSONObject()}
        if(latest.optString("requestHash")==requestHash){
            latest.put("actual",actual)
            prefs.edit().putString("latest",latest.toString()).apply()
        }else{
            prefs.edit().putString("pendingActual",actual.toString()).apply()
        }
    }

    private fun persist(context:Context,trace:JSONObject){
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val pending=runCatching{JSONObject(prefs.getString("pendingActual","{}").orEmpty())}.getOrElse{JSONObject()}
        if(pending.optString("requestHash")==trace.optString("requestHash")){
            trace.put("actual",pending)
        }
        val count=prefs.getInt("count",0)+1
        prefs.edit()
            .putInt("count",count)
            .putString("latest",trace.toString())
            .remove("pendingActual")
            .apply()
    }

    fun describe(context:Context):String{
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val count=prefs.getInt("count",0)
        val raw=prefs.getString("latest","").orEmpty()
        if(raw.isBlank())return "No Pocket turn has been observed by the L1 consciousness shadow yet."
        val v=runCatching{JSONObject(raw)}.getOrElse{return "Shadow trace is unreadable."}
        val actual=v.optJSONObject("actual")
        return buildString{
            appendLine("Mode: "+v.optString("mode","UNKNOWN"))
            appendLine("Turns observed: $count")
            appendLine("Shadow reachable: "+v.optBoolean("available",false))
            appendLine("Intent: "+v.optString("intent","unknown"))
            appendLine("Classification: "+v.optString("classification","UNKNOWN"))
            appendLine("Formula health: "+v.optString("formulaStatus","UNVERIFIED"))
            appendLine("Continuity: "+v.optBoolean("continuityVerified",false))
            appendLine("Balance: "+if(v.has("balance"))v.optDouble("balance") else "UNAVAILABLE")
            appendLine("LLM dependency: "+v.optInt("llmDependency",-1))
            appendLine("State hash: "+v.optString("stateHash","UNAVAILABLE").take(16))
            appendLine()
            appendLine("Shadow candidate:")
            appendLine(v.optString("shadowAnswer","NO_CANDIDATE"))
            appendLine()
            appendLine("Actual Agent Lee:")
            append(actual?.optString("responsePreview","PENDING")?:"PENDING")
        }
    }
}
