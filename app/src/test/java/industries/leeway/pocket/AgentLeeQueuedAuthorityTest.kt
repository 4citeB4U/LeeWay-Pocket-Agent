package industries.leeway.pocket
import org.junit.Assert.*
import org.junit.Test

class AgentLeeQueuedAuthorityTest {
 private fun admitted(epoch:AgentLeeStopEpoch, queued:Long, owner:Boolean, ui:Boolean, issuer:Boolean) =
  AgentLeeQueuedAuthority.admitted(epoch.admits(queued), owner, ui, issuer)
 @Test fun revokedOwnerDenied(){val e=AgentLeeStopEpoch();val q=e.snapshot();assertFalse(admitted(e,q,false,true,true))}
 @Test fun revokedUiDenied(){val e=AgentLeeStopEpoch();val q=e.snapshot();assertFalse(admitted(e,q,true,false,true))}
 @Test fun missingIssuerDenied(){val e=AgentLeeStopEpoch();val q=e.snapshot();assertFalse(admitted(e,q,true,true,false))}
 @Test fun stoppedQueueDenied(){val e=AgentLeeStopEpoch();val q=e.snapshot();e.stop();assertFalse(admitted(e,q,true,true,true))}
}
