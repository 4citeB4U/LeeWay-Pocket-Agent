package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class RecognitionSessionTest {
    @Test fun typedDialogRejectsQueuedSpeechResultsEvenAfterDismissal() {
        val session=RecognitionSession()
        val old=session.begin()!!
        assertTrue(session.accepts(old))
        session.beginTyping()
        assertFalse(session.accepts(old))
        assertNull(session.begin())
        session.endTyping()
        assertFalse(session.accepts(old))
        val retry=session.begin()!!
        assertTrue(session.accepts(retry))
        assertFalse(session.accepts(old))
    }
    @Test fun cancellingAcceptedResultRejectsDuplicateCallbacks() {
        val session=RecognitionSession()
        val token=session.begin()!!
        session.cancel()
        assertFalse(session.accepts(token))
    }
}
