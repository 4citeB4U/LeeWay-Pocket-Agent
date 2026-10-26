package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test
class AgentLeeBridgeSessionGateTest {
 private fun s()=AgentLeeBridgeSessionGate.Session("session-000001","ASSIST_SESSION","SESSION","INTERACTIVE","ACTIVE")
 private fun ok(s:AgentLeeBridgeSessionGate.Session)=AgentLeeBridgeSessionGate.accept(s,"session-000001")
 @Test fun activeSessionAllowedAtShapeLevel(){assertTrue(ok(s()))}
 @Test fun wrongSessionDenied(){assertFalse(ok(s().copy(sessionId="other")))}
 @Test fun idleDenied(){assertFalse(ok(s().copy(status="IDLE")))}
 @Test fun stoppingDenied(){assertFalse(ok(s().copy(status="STOPPING")))}
 @Test fun readOnlyDenied(){assertFalse(ok(s().copy(controlAuthority="GUIDANCE")))}
 @Test fun wrongModeDenied(){assertFalse(ok(s().copy(mode="DIAGNOSTIC")))}
 @Test fun missingScreenAuthorityDenied(){assertFalse(ok(s().copy(screenAuthority="NONE")))}
 @Test fun continuousScreenAllowed(){assertTrue(ok(s().copy(screenAuthority="CONTINUOUS")))}
}
