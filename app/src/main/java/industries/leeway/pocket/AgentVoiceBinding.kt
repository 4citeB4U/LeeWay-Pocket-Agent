/*
REGION: POCKET.VOICE.BINDING
TAG: LEEWAY_SHARED_VOICE_BINDING_ADAPTER
WHO: Creator-authorized Agent Lee / Pocket Android body
WHAT: Validate an explicit projection of the existing Voice Fabric employee binding.
WHEN: Before selecting or admitting an Android speech renderer.
WHERE: LeeWay-Pocket-Agent; acoustic authority remains LeeWay-Voice-Fabric.
WHY: A device switch must not silently select Android TTS or a different speaker.
HOW: Match employee identity, persona, catalog package and renderer identity; no fallback.
LICENSE: MIT
*/
package industries.leeway.pocket

import org.json.JSONObject

data class AgentVoiceBinding(
    val agentId: String,
    val personaFamily: String,
    val personaArchetypeId: String,
    val voicePackageId: String,
    val provider: String,
    val voiceId: String?,
    val qualificationOnly: Boolean
) {
    fun requireRenderer(packageId: String, actualProvider: String) {
        require(packageId == voicePackageId) { "VOICE_PACKAGE_IDENTITY_MISMATCH" }
        require(actualProvider == provider) { "VOICE_PROVIDER_IDENTITY_MISMATCH" }
    }

    companion object {
        fun fromSources(bindings: JSONObject, catalog: JSONObject, allowQualification: Boolean = false): AgentVoiceBinding {
            require(bindings.optString("authority") == "LEEWAY_VOICE_FABRIC") { "VOICE_BINDING_AUTHORITY_REQUIRED" }
            val binding = bindings.optJSONObject("bindings")?.optJSONObject("agent-lee")
                ?: throw IllegalArgumentException("VOICE_SELECTION_REQUIRED")
            fun field(name: String): String {
                val value = binding.opt(name)
                require(value is String && value.isNotBlank()) { "VOICE_BINDING_FIELD_REQUIRED:" + name }
                return value
            }
            val agentId = field("agentId")
            require(agentId == "agent-lee") { "VOICE_AGENT_IDENTITY_MISMATCH" }
            val family = field("personaFamily")
            val archetype = field("personaArchetypeId")
            val packageId = field("voicePackageId")
            val state = field("state")
            val selectedBy = field("selectedBy")
            val preview = state == "TEMPORARY_VERIFIED_PROVIDER"
            require(if (preview) allowQualification && selectedBy == "CREATOR_PENDING_FINAL_AUDITION"
                    else state == "VERIFIED_CATALOG_SELECTION" && selectedBy == "CREATOR") {
                "CREATOR_VOICE_SELECTION_REQUIRED"
            }
            val packages = catalog.optJSONArray("packages")
                ?: throw IllegalArgumentException("VOICE_CATALOG_REQUIRED")
            val matches = (0 until packages.length()).mapNotNull { packages.optJSONObject(it) }
                .filter { it.optString("id") == packageId }
            require(matches.size == 1) { "VOICE_PACKAGE_MISSING_OR_AMBIGUOUS" }
            val pkg = matches.single()
            require(pkg.optString("status") == "AVAILABLE") { "VOICE_PACKAGE_UNAVAILABLE" }
            val provider = pkg.optString("provider")
            require(provider == "kokoro" || provider == "chatterbox") { "UNIFIED_VOICE_PROVIDER_REQUIRED" }
            return AgentVoiceBinding(agentId, family, archetype, packageId, provider,
                pkg.optString("voiceId").takeIf { it.isNotBlank() }, preview)
        }
    }
}
