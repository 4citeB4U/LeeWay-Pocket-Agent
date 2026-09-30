package industries.leeway.pocket

import org.junit.Assert.*
import org.junit.Test

class SpokenPromptTest {
    @Test fun promptStaysBoundedAndPreservesQuestionAndTruthBoundary() {
        val prompt=SpokenPrompt.build("What is two plus two?","authority".repeat(1000),"skills".repeat(1000),"provenance".repeat(1000))
        assertTrue(prompt.length<=2000)
        assertTrue(prompt.contains("What is two plus two?"))
        assertTrue(prompt.contains("guidance, not execution"))
        assertTrue(prompt.contains("at most 40 words"))
    }
}
