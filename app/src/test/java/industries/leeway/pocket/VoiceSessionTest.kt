package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class VoiceSessionTest {
    @Test fun closingWhilePreparingDropsQueuedSpeech() {
        val lease = VoiceSession<Any>()
        val activity = Any()
        lease.attach(activity)
        assertTrue(lease.queue(activity, "old response"))
        assertTrue(lease.detach(activity))
        assertNull(lease.takePending())
        assertNull(lease.owner)
    }

    @Test fun oldActivityCannotStopOrReceiveNewConversation() {
        val lease = VoiceSession<Any>()
        val old = Any()
        val current = Any()
        lease.attach(old)
        lease.queue(old, "old reply")
        val oldTurn = lease.takePending()!!.first
        lease.attach(current)
        assertFalse(lease.detach(old))
        assertFalse(lease.queue(old, "late result"))
        assertNull(lease.ownerFor(oldTurn))
        assertSame(current, lease.owner)
        lease.queue(current, "current reply")
        val (turn, text) = lease.takePending()!!
        assertEquals("current reply", text)
        assertSame(current, lease.ownerFor(turn))
    }

    @Test fun newerSpeechInvalidatesEarlierCompletion() {
        val lease = VoiceSession<Any>()
        val activity = Any()
        lease.attach(activity)
        lease.queue(activity, "first")
        val first = lease.takePending()!!.first
        lease.queue(activity, "second")
        assertNull(lease.ownerFor(first))
        val second = lease.takePending()!!.first
        assertSame(activity, lease.ownerFor(second))
        lease.detach(activity)
        assertNull(lease.ownerFor(second))
    }
}
