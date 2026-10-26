package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test

class AgentLeeCursorAdmissionTest {
 private val h="a".repeat(64)
 private fun event(approved:Boolean=true,ts:Long=10000L)=AgentLeeCursorAdmission.Event(
 "agent-lee","4citeB4U/LEEWAY-DEVICE-BRIDGE","request-000001","action-000001",
 "tap","android",approved,false,24.0,25.0,ts)
 private fun execution()=AgentLeeCursorAdmission.Execution("request-000001","action-000001","android","EXECUTED","LEEWAY_DEVICE_BRIDGE",h)
 private fun observation()=AgentLeeCursorAdmission.Observation("request-000001","action-000001",true,h)
 @Test fun validVerifiedReceiptAdmitted(){assertTrue(AgentLeeCursorAdmission.admit(event(),execution(),observation(),10000L))}
 @Test fun unapprovedDenied(){assertFalse(AgentLeeCursorAdmission.admit(event(false),execution(),observation(),10000L))}
 @Test fun plannedOnlyDenied(){assertFalse(AgentLeeCursorAdmission.admit(event(),execution().copy(state="PLANNED"),observation(),10000L))}
 @Test fun mismatchedReceiptDenied(){assertFalse(AgentLeeCursorAdmission.admit(event(),execution(),observation().copy(receiptHash="b".repeat(64)),10000L))}
 @Test fun unverifiedPostStateDenied(){assertFalse(AgentLeeCursorAdmission.admit(event(),execution(),observation().copy(postStateVerified=false),10000L))}
 @Test fun staleDenied(){assertFalse(AgentLeeCursorAdmission.admit(event(ts=100L),execution(),observation(),10000L))}
 @Test fun canceledDenied(){assertFalse(AgentLeeCursorAdmission.admit(event().copy(cancelled=true),execution(),observation(),10000L))}
 @Test fun wrongDeviceDenied(){assertFalse(AgentLeeCursorAdmission.admit(event().copy(device="pc"),execution(),observation(),10000L))}
 @Test fun invalidCoordinateDenied(){assertFalse(AgentLeeCursorAdmission.admit(event().copy(x=Double.NaN),execution(),observation(),10000L))}
}
