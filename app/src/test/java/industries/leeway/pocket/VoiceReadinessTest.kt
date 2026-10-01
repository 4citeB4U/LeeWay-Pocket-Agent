package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class VoiceReadinessTest {
    @Test fun timeoutAndReloadCannotRetainPreviousReadyEvidence() {
        val state = VoiceReadiness()
        state.bridgeReady()
        assertTrue(state.modelReady())
        state.unavailable()
        assertFalse(state.ready)
        assertTrue(state.pageReady)
        state.beginPage()
        assertFalse(state.pageReady)
        assertFalse(state.ready)
        state.bridgeReady()
        assertFalse(state.ready)
        assertTrue(state.modelReady())
    }
    @Test fun modelReadyWithoutCurrentPageIsRejected() {
        val state = VoiceReadiness()
        assertFalse(state.modelReady())
        state.bridgeReady()
        assertTrue(state.modelReady())
        state.beginPage()
        assertFalse(state.modelReady())
    }
}
