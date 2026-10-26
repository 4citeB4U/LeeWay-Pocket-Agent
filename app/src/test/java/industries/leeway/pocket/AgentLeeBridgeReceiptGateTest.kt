package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test
class AgentLeeBridgeReceiptGateTest {
 private val h="a".repeat(64)
 private fun r()=AgentLeeBridgeReceiptGate.Receipt("receipt-000001","LDB-test-device","device.ui.tap","PASS","owner-approved","PASS","PASS",listOf(h))
 private fun ok(r:AgentLeeBridgeReceiptGate.Receipt)=AgentLeeBridgeReceiptGate.accept(r,"LDB-test-device")
 @Test fun validShape(){assertTrue(ok(r()))}
 @Test fun wrongDevice(){assertFalse(ok(r().copy(deviceId="another")))}
 @Test fun failedResult(){assertFalse(ok(r().copy(result="FAIL")))}
 @Test fun noAuthorization(){assertFalse(ok(r().copy(authorizationBasis="")))}
 @Test fun missingPreGate(){assertFalse(ok(r().copy(preGate="BLOCKED")))}
 @Test fun missingPostGate(){assertFalse(ok(r().copy(postGate="BLOCKED")))}
 @Test fun noEvidence(){assertFalse(ok(r().copy(evidenceHashes=emptyList())))}
 @Test fun invalidHash(){assertFalse(ok(r().copy(evidenceHashes=listOf("fake"))))}
}
