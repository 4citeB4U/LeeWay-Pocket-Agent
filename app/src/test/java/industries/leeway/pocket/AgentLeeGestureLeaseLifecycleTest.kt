package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test

/** Unit-level lease and callback lifecycle; OS Handler/gesture timing remains physical-test scope. */
class AgentLeeGestureLeaseLifecycleTest {
 @Test fun callbackReleasesLease() {
  val lease=CanonicalPocketCommandLease()
  assertTrue(lease.acquire())
  val result=AgentLeeGestureOutcome.callback("action-000001",true)
  lease.release()
  assertEquals(AgentLeeGestureOutcome.State.GESTURE_COMPLETED_UNVERIFIED,result.state)
  assertTrue(lease.acquire())
 }
 @Test fun timeoutFailureReleasesLease() {
  val lease=CanonicalPocketCommandLease()
  assertTrue(lease.acquire())
  val result=AgentLeeGestureOutcome.callback("action-000001",false)
  lease.release()
  assertEquals(AgentLeeGestureOutcome.State.DISPATCH_FAILED,result.state)
  assertTrue(lease.acquire())
 }
 @Test fun stopInvalidatesQueuedRequest() {
  val lease=CanonicalPocketCommandLease()
  val epoch=AgentLeeStopEpoch()
  val queued=epoch.snapshot()
  assertTrue(lease.acquire())
  epoch.stop()
  lease.release()
  assertFalse(epoch.admits(queued))
  assertTrue(lease.acquire())
 }
}
