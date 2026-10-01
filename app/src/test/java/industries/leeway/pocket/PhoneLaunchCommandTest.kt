package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class PhoneLaunchCommandTest {
    @Test fun onlyExplicitBoundedCommandsDispatch() {
        assertEquals(PhoneLaunchCommand.SETTINGS,PhoneLaunchCommand.parse(" Open Settings "))
        assertEquals(PhoneLaunchCommand.CALCULATOR,PhoneLaunchCommand.parse("open calculator"))
        for(request in listOf("do not open settings","open banking","how do I open settings?","open calculator and send money"))
            assertNull(PhoneLaunchCommand.parse(request))
    }
}
