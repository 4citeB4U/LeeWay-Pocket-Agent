package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class FabricVoiceCatalogTest {
    @Test fun choicesComeFromAvailableApiEntriesOnly(){
        val catalog="""[{"id":"remote-profile","name":"Remote profile","provider":"chatterbox","status":"AVAILABLE","adapterAvailable":true},
          {"id":"unsupported","name":"Unsupported","provider":"other","status":"AVAILABLE","adapterAvailable":false}]"""
        val choices=FabricVoiceCatalog.parse(catalog)
        assertEquals(listOf("remote-profile"),choices.map{it.id})
        assertEquals("Remote profile",choices.single().name)
    }
    @Test fun emptyCatalogDoesNotInventLocalFallback(){assertTrue(FabricVoiceCatalog.parse("[]").isEmpty())}
    @Test fun malformedIdsCannotBecomeExecutableSelection(){
        assertThrows(IllegalArgumentException::class.java){FabricVoiceCatalog.parse("""[{"id":"x');bad()","name":"Bad","provider":"chatterbox","status":"AVAILABLE","adapterAvailable":true}]""")}
    }
}
