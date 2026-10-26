package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test

class AgentLeeGestureOutcomeTest {
 private val id="action-000001"
 private val hash="a".repeat(64)
 @Test fun deniedRemainsDenied(){ assertEquals(AgentLeeGestureOutcome.State.DENIED,AgentLeeGestureOutcome.denied(id).state) }
 @Test fun unsuccessfulGestureIsFailure(){ assertEquals(AgentLeeGestureOutcome.State.DISPATCH_FAILED,AgentLeeGestureOutcome.callback(id,false).state) }
 @Test fun completedGestureIsNotVerified(){ val r=AgentLeeGestureOutcome.callback(id,true); assertEquals(AgentLeeGestureOutcome.State.GESTURE_COMPLETED_UNVERIFIED,r.state);assertFalse(r.postStateVerified) }
 @Test fun matchingObservationPromotes(){ val r=AgentLeeGestureOutcome.verify(AgentLeeGestureOutcome.callback(id,true),true,hash);assertEquals(AgentLeeGestureOutcome.State.VERIFIED,r.state);assertEquals(hash,r.receiptHash) }
 @Test fun missingObservationDenied(){ assertNotEquals(AgentLeeGestureOutcome.State.VERIFIED,AgentLeeGestureOutcome.verify(AgentLeeGestureOutcome.callback(id,true),false,hash).state) }
 @Test fun invalidReceiptDenied(){ assertNotEquals(AgentLeeGestureOutcome.State.VERIFIED,AgentLeeGestureOutcome.verify(AgentLeeGestureOutcome.callback(id,true),true,"bad").state) }
 @Test fun failureCannotBePromoted(){ assertNotEquals(AgentLeeGestureOutcome.State.VERIFIED,AgentLeeGestureOutcome.verify(AgentLeeGestureOutcome.callback(id,false),true,hash).state) }
}
