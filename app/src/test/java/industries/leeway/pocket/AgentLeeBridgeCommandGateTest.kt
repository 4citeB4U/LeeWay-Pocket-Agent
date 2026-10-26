package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test
class AgentLeeBridgeCommandGateTest {
 private fun c()=AgentLeeBridgeCommandGate.Command("action-000001","device.ui.tap","OPERATE","session-000001",true)
 private fun ok(c:AgentLeeBridgeCommandGate.Command,approved:Boolean=true)=AgentLeeBridgeCommandGate.accept(c,"action-000001",approved)
 @Test fun completeCommandAdmittedAtShapeLevel(){assertTrue(ok(c()))}
 @Test fun blankSessionDenied(){assertFalse(ok(c().copy(sessionId="")))}
 @Test fun wrongAuthorityTierDenied(){assertFalse(ok(c().copy(authorityTier="READ")))}
 @Test fun mismatchedCommandDenied(){assertFalse(ok(c().copy(commandId="other-action")))}
 @Test fun wrongCapabilityDenied(){assertFalse(ok(c().copy(capability="device.ui.type")))}
 @Test fun absentHumanConfirmationDenied(){assertFalse(ok(c().copy(requiresHumanConfirmation=false)))}
 @Test fun absentOwnerApprovalDenied(){assertFalse(ok(c(),false))}
}
