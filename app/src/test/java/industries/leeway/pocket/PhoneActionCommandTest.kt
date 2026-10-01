package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class PhoneActionCommandTest {
    @Test fun onlyExplicitSingleStepGrammarSelectsCommands() {
        assertEquals(PhoneActionCommand("open","Settings"),PhoneActionCommand.parse(" Open Settings "))
        assertEquals(PhoneActionCommand("back"),PhoneActionCommand.parse("go back"))
        assertEquals(PhoneActionCommand("home"),PhoneActionCommand.parse("GO HOME"))
        assertEquals(PhoneActionCommand("recents"),PhoneActionCommand.parse("show recent apps"))
        listOf("do not open Settings","can you go home?","tap 100 200","send an email","open ","open Settings\ngo home","go back then send money").forEach {
            assertNull(it,PhoneActionCommand.parse(it))
        }
        assertNull(PhoneActionCommand.parse("open "+"x".repeat(161)))
    }
    @Test fun concurrentTimeoutDisconnectAndReplyDeliverOnlyOneResult() {
        val state=PhoneActionCompletion();val start=CountDownLatch(1);val done=CountDownLatch(16);val winners=AtomicInteger()
        repeat(16) { Thread { start.await();if(state.finish())winners.incrementAndGet();done.countDown() }.start() }
        start.countDown();done.await();assertEquals(1,winners.get());assertFalse(state.finish());assertTrue(state.isFinished())
    }
}
