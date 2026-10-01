package industries.leeway.pocket

import org.json.JSONArray

/** Only API-returned catalog entries become selectable. No local voice inventory. */
object FabricVoiceCatalog {
    data class Choice(val id:String,val name:String,val provider:String)
    fun parse(json:String):List<Choice>{
        require(json.length<=200_000){"VOICE_CATALOG_TOO_LARGE"}
        val values=JSONArray(json)
        require(values.length()<=200){"VOICE_CATALOG_TOO_LARGE"}
        return (0 until values.length()).mapNotNull { index ->
            val item=values.getJSONObject(index)
            if(!item.optBoolean("adapterAvailable")||item.optString("status")!="AVAILABLE")return@mapNotNull null
            val id=item.getString("id")
            require(id.matches(Regex("[a-z0-9._-]{1,96}"))){"VOICE_CATALOG_ID_INVALID"}
            Choice(id,item.getString("name").take(160),item.getString("provider").take(80))
        }.distinctBy{it.id}
    }
}
