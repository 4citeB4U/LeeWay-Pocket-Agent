package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test

class AgentLeeTouchRequestGateTest {
 private fun q()=AgentLeeTouchRequestGate.Request("agent-lee","4citeB4U/LEEWAY-DEVICE-BRIDGE","android","LDB-test-device","request-000001","action-000001","tap",true,false,20f,30f,10000L)
 private fun ok(a:AgentLeeTouchRequestGate.Request)=AgentLeeTouchRequestGate.accept(a,"LDB-test-device",10000L)
 @Test fun permittedShape(){assertTrue(ok(q()))}
 @Test fun missingApproval(){assertFalse(ok(q().copy(ownerApproved=false)))}
 @Test fun canceled(){assertFalse(ok(q().copy(cancelled=true)))}
 @Test fun wrongAuthority(){assertFalse(ok(q().copy(authority="other")))}
 @Test fun wrongDeviceId(){assertFalse(ok(q().copy(deviceId="LDB-other-device")))}
 @Test fun wrongDevice(){assertFalse(ok(q().copy(device="pc")))}
 @Test fun badAction(){assertFalse(ok(q().copy(action="swipe")))}
 @Test fun malformedIdentity(){assertFalse(ok(q().copy(requestId="bad")))}
 @Test fun expired(){assertFalse(ok(q().copy(issuedAtMs=1L)))}
 @Test fun invalidPoint(){assertFalse(ok(q().copy(x=Float.NaN)))}
}
