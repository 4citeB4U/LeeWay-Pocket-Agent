package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class EnglishVoicePolicyTest {
    private fun voice(name:String,language:String="en",country:String="US",network:Boolean=false,quality:Int=300,installed:Boolean=true)=
        EnglishVoicePolicy.Choice(name,language,country,network,quality,installed)
    @Test fun prefersInstalledOfflineEnglishAndUsWithinOfflineVoices(){
        val selected=EnglishVoicePolicy.choose(listOf(voice("fr","fr"),voice("online",network=true,quality=500),voice("british",country="GB",quality=500),voice("us")),null)
        assertEquals("us",selected?.name)
    }
    @Test fun retainsOwnerSelectedEnglishVoice(){
        assertEquals("selected",EnglishVoicePolicy.choose(listOf(voice("default"),voice("selected",country="GB",network=true)),"selected")?.name)
    }
    @Test fun neverFallsBackToAnotherLanguageOrMissingVoice(){
        assertNull(EnglishVoicePolicy.choose(listOf(voice("chinese","zh"),voice("missing",installed=false)),"chinese"))
    }
}
