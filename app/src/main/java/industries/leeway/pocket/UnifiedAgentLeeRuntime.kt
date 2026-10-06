/*
REGION: POCKET.RUNTIME.ADAPTER
TAG: LEEWAY_CANONICAL_CONVERSATION_BINDING
WHO: Agent Lee / Creator-authorized Android body
WHAT: Reserve the existing adapter boundary for canonical conversation execution.
WHEN: During integration; WHERE: Pocket Android candidate.
WHY: A canned status sentence is not a connected consciousness or a conversational answer.
HOW: Fail explicitly until the canonical executor is linked; do not create a parallel reasoner.
LICENSE: MIT
*/
package industries.leeway.pocket

import android.content.Context

class UnifiedAgentLeeRuntime(private val context:Context) {
    fun respond(request:String):String {
        require(request.isNotBlank()) { "CONVERSATION_REQUEST_REQUIRED" }
        throw IllegalStateException("CANONICAL_CONVERSATION_PROVIDER_NOT_BOUND")
    }
}