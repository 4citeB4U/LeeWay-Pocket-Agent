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
    @Test fun creatorContextAndAllMaximalFieldsStayWithinBudget() {
        val prompt=SpokenPrompt.build("q".repeat(5000),"a".repeat(5000),"s".repeat(5000),"e".repeat(5000),"Creator: Leonard J. Lee; profile is not authentication.")
        assertTrue(prompt.length<=2000)
        assertTrue(prompt.contains("Creator: Leonard J. Lee"))
        assertTrue(prompt.contains("USER REQUEST: "+"q".repeat(600)))
    }
}
