package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test
class AgentLeeTrustedExecutionAdmissionTest {
 @Test fun noCanonicalAuthenticatedIssuerMeansNoPhysicalGesture() {
  assertFalse(AgentLeeTrustedExecutionAdmission.admitted())
 }
}
