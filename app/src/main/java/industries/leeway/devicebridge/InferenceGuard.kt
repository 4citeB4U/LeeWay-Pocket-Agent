package industries.leeway.devicebridge

import java.util.concurrent.atomic.AtomicBoolean

/** One engine owner at a time, held through native cleanup. Busy callers never queue another engine. */
internal class InferenceGuard {
    private val busy = AtomicBoolean(false)
    fun tryAcquire(): Boolean = busy.compareAndSet(false, true)
    fun release() { busy.set(false) }
    fun isBusy(): Boolean = busy.get()
}
