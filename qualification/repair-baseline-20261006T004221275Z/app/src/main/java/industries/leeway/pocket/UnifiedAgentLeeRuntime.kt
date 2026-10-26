package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject

class UnifiedAgentLeeRuntime(private val context:Context){
    private val automation=LeeWayAutomation(context)
    private val devices=LeeWayDeviceService(context)

    fun respond(request:String):String {
        FoldDigitalBrain.bootstrap(context)
        val intent=automation.classify(request)
        val brain=runCatching{JSONObject(FoldDigitalBrain.snapshot(context))}.getOrElse{JSONObject()}
        return when(intent){
            "research"->"Creator, I have the request. The unified browser/research provider is not yet admitted in this APK build, so I will not claim I executed it."
            "schedule","reminder"->"Creator, I have the automation request. Runtime Fabric F8 authority is bound, but canonical Formula execution is not proven on this phone yet, so I am holding the action rather than faking completion."
            else->"Creator, Agent Lee here on phone-fold6. My Digital Brain is active with "+brain.optJSONObject("counts")?.optLong("nodes",0)+" indexed nodes. Continuum and LDWMD are body-local; Voice and Skills remain shared LeeWay authorities."
        }
    }
}
