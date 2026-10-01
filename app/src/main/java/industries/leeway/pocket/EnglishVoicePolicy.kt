package industries.leeway.pocket

object EnglishVoicePolicy {
    data class Choice(val name:String,val language:String,val country:String,val network:Boolean,val quality:Int,val installed:Boolean=true)
    fun choose(voices:List<Choice>,saved:String?):Choice? {
        val english=voices.filter{it.language=="en"&&it.installed}
        return english.firstOrNull{it.name==saved} ?: english.sortedWith(
            compareBy<Choice>{it.network}.thenBy{it.country!="US"}.thenByDescending{it.quality}.thenBy{it.name}
        ).firstOrNull()
    }
}
