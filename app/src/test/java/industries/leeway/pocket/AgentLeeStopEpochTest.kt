package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test

class AgentLeeStopEpochTest {
    @Test fun queuedBeforeStopIsRejected() {
        val gate = AgentLeeStopEpoch()
        val queued = gate.snapshot()
        gate.stop()
        assertFalse(gate.admits(queued))
    }
    @Test fun newQueueAfterStopCanPass() {
        val gate = AgentLeeStopEpoch()
        gate.stop()
        assertTrue(gate.admits(gate.snapshot()))
    }
    @Test fun successiveStopsInvalidateAllOldRequests() {
        val gate = AgentLeeStopEpoch()
        val old = gate.snapshot()
        gate.stop()
        val afterFirst = gate.snapshot()
        gate.stop()
        assertFalse(gate.admits(old))
        assertFalse(gate.admits(afterFirst))
    }
}
