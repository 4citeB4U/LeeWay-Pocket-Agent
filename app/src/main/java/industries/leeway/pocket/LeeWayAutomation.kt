package industries.leeway.pocket

import android.content.Context
import org.json.JSONObject

class LeeWayAutomation(private val context:Context){
    fun status()=JSONObject()
        .put("authority","4citeB4U/Leeway-Runtime-Fabric")
        .put("formulaFamily","F8")
        .put("bodyId","phone-fold6")
        .put("externalWebhookRequired",false)
        .put("canonicalFormulaState","NOT_EXECUTED")
    fun classify(request:String):String = when {
        request.contains("remind",true)->"reminder"
        request.contains("schedule",true)||request.contains("calendar",true)->"schedule"
        request.contains("search",true)||request.contains("research",true)->"research"
        else->"conversation"
    }
}
