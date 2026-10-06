/*
REGION: POCKET.VOICE.QUALIFICATION
TAG: LEEWAY_SHARED_VOICE_BINDING_TEST
WHO: Agent Lee qualification
WHAT: Exercise identity admission and rejection without synthesizing audio.
WHEN: Android JVM qualification; WHERE: Pocket tests.
WHY: Prove metadata boundaries, not acoustic equivalence.
HOW: Explicit synthetic catalog/binding fixtures; production must reject preview-only binding.
LICENSE: MIT
*/
package industries.leeway.pocket

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AgentVoiceBindingTest {
    private fun binding() = JSONObject().put("authority", "LEEWAY_VOICE_FABRIC").put("bindings",
        JSONObject().put("agent-lee", JSONObject().put("agentId", "agent-lee")
            .put("personaFamily", "AGENT_LEE_CONSTITUTIONAL").put("personaArchetypeId", "ELDER_MALE")
            .put("voicePackageId", "kokoro-am_michael").put("state", "VERIFIED_CATALOG_SELECTION")
            .put("selectedBy", "CREATOR")))
    private fun catalog() = JSONObject().put("packages", JSONArray().put(JSONObject()
        .put("id", "kokoro-am_michael").put("voiceId", "am_michael").put("provider", "kokoro").put("status", "AVAILABLE")))
    private fun reject(reason: String, action: () -> Unit) {
        try { action(); fail("Expected rejection: " + reason) }
        catch (e: IllegalArgumentException) { assertEquals(reason, e.message) }
    }
    @Test fun explicitSelectionResolvesWithoutBodySpecificDefaults() {
        val pc = AgentVoiceBinding.fromSources(binding(), catalog())
        val phone = AgentVoiceBinding.fromSources(binding(), catalog())
        assertEquals(pc, phone); assertEquals("am_michael", phone.voiceId)
        assertFalse(phone.qualificationOnly)
    }
    @Test fun missingSelectionIsBlocked() {
        reject("VOICE_SELECTION_REQUIRED") { AgentVoiceBinding.fromSources(binding().put("bindings", JSONObject()), catalog()) }
    }
    @Test fun arbitraryAuthorityIsRejected() {
        reject("VOICE_BINDING_AUTHORITY_REQUIRED") { AgentVoiceBinding.fromSources(binding().put("authority", "OTHER"), catalog()) }
    }
    @Test fun missingPersonaIsRejected() {
        val b=binding(); b.getJSONObject("bindings").getJSONObject("agent-lee").remove("personaFamily")
        reject("VOICE_BINDING_FIELD_REQUIRED:personaFamily") { AgentVoiceBinding.fromSources(b, catalog()) }
    }
    @Test fun previewDoesNotBecomeProductionSelection() {
        val b=binding();b.getJSONObject("bindings").getJSONObject("agent-lee")
            .put("state","TEMPORARY_VERIFIED_PROVIDER").put("selectedBy","CREATOR_PENDING_FINAL_AUDITION")
        reject("CREATOR_VOICE_SELECTION_REQUIRED") { AgentVoiceBinding.fromSources(b,catalog()) }
        assertTrue(AgentVoiceBinding.fromSources(b,catalog(),true).qualificationOnly)
    }
    @Test fun missingAndDuplicatePackagesAreRejected() {
        reject("VOICE_PACKAGE_MISSING_OR_AMBIGUOUS") { AgentVoiceBinding.fromSources(binding(),JSONObject().put("packages",JSONArray())) }
        val c=catalog(); c.getJSONArray("packages").put(c.getJSONArray("packages").getJSONObject(0))
        reject("VOICE_PACKAGE_MISSING_OR_AMBIGUOUS") { AgentVoiceBinding.fromSources(binding(),c) }
    }
    @Test fun unavailablePackageIsRejected() {
        val c=catalog();c.getJSONArray("packages").getJSONObject(0).put("status","UNAVAILABLE")
        reject("VOICE_PACKAGE_UNAVAILABLE") { AgentVoiceBinding.fromSources(binding(),c) }
    }
    @Test fun androidVoiceCannotSubstituteForSelectedSpeaker() {
        val v=AgentVoiceBinding.fromSources(binding(),catalog())
        reject("VOICE_PACKAGE_IDENTITY_MISMATCH") { v.requireRenderer("android-installed-english","android-tts") }
        reject("VOICE_PROVIDER_IDENTITY_MISMATCH") { v.requireRenderer(v.voicePackageId,"android-tts") }
        v.requireRenderer(v.voicePackageId,"kokoro")
    }
}
