package industries.leeway.pocket

import java.util.concurrent.atomic.AtomicLong

/** A pure cancellation epoch shared by queued gesture submissions and Stop. */
internal class AgentLeeStopEpoch {
    private val epoch = AtomicLong(0L)
    fun snapshot(): Long = epoch.get()
    fun stop(): Long = epoch.incrementAndGet()
    fun admits(snapshot: Long): Boolean = snapshot == epoch.get()
}
